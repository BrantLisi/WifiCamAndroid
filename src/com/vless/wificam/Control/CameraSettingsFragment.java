package com.vless.wificam.Control ;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.videolan.vlc.VLCApplication;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.vless.wificam.R;
import com.vless.wificam.CameraCommand;
import com.vless.wificam.CameraPeeker;
import com.vless.wificam.CameraSniffer;
import com.vless.wificam.MainActivity;
import com.vless.wificam.Control.*;
import com.vless.wificam.FileBrowser.*;
import com.vless.wificam.Viewer.MjpegPlayerFragment;
import com.vless.wificam.Viewer.StreamPlayerFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment ;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ContentResolver ;
import android.content.ContentValues ;
import android.content.Intent ;
import android.content.SharedPreferences ;
import android.content.res.Configuration ;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle ;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater ;
import android.view.View ;
import android.view.ViewGroup ;
import android.widget.ArrayAdapter ;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner ;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.AsyncTask ;

public class CameraSettingsFragment extends Fragment {
	private int mFlickerRet=0;
	private int mAWBRet=0;
	private int mMTDRet=0;
	private int mVideoresRet=0;
	private int mImageresRet=0;
	private int mEVRet=0;
	private String mFWVersionRet="";
	private String mszCameraPreviewInfo="";
	private Spinner videoResolution;
	private Spinner imageResolution;
	private Spinner motiondetection;
	private Spinner flickerfrequency;
	private Spinner AWB;
	//private Spinner deletefiles;
	private SeekBar exposure;
	private ArrayAdapter<String> videoResolutionAdapter = null;
	private ArrayAdapter<String> imageResolutionAdapter = null;
	private ArrayAdapter<String> motiondetectionAdapter = null; 
	private ArrayAdapter<String> flickerfrequencyAdapter= null;
	private ArrayAdapter<String> AWBAdapter;
	//private ArrayAdapter<String> deletefilesAdapter;
	private View view;
	private TextView FWVersion;
	//
	private int GetCameraSettingValue(String statement, MENU_ID menuid)
	{
		String	key;
		/*
			Camera.Menu.AWB=AUTO
			Camera.Menu.EV=LOW
			Camera.Menu.FWversion=3042
			Camera.Menu.Flicker=50Hz
			Camera.Menu.ImageRes=8MP
			Camera.Menu.IsStreaming=NO
			Camera.Menu.MTD=UNKNOWN
			Camera.Menu.UIMode=VIDEO
			Camera.Menu.VideoRes=1080P60
		*/		
		switch (menuid) {
		case menuVIDEO_RES:
			key = "Videores";
			break;
		case menuIMAGE_RES:
			key = "ImageRes";
			break;
		case menuLOOPING_VIDEO:
			key = "VideoClipTime";
			break;
		case menuWHITE_BALANCE:
			key = "AWB";
			break;
		case menuBURST:
			key = "Burst";
			break;
		case menuTIME_LAPSE:
			key = "TimeLapse";
			break;
		case menuHDR:
			key = "HDR";
			break;
		default:
			return -1;
		}
		String[]	line;
		String[]	value;
		try {
			key = key.concat("=");
			line  = statement.split(key);
			value = line[1].split(System.getProperty("line.separator")) ;
			return AutoMenuCheck(menuid, value[0]);
		} catch (Exception e) { /* TODO: Show Error */ }
		return -1;
	}
	private class CameraSettingsSendRequest extends CameraCommand.SendRequest {
	
		@Override
		protected void onPreExecute() {
			setWaitingState(true) ;
			super.onPreExecute() ;
		}

		@Override
		protected void onPostExecute(String result) {
			setWaitingState(false) ;
			super.onPostExecute(result) ;
		}
	}
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg){
			super.handleMessage(msg);
			
			videoResolution.setSelection(mVideoresRet,true);	
			videoResolution.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
				public void onItemSelected(AdapterView parent, View view, int pos, long id){
					if (mVideoresRet == pos) return;
					mVideoresRet = pos;
					Menu theMenu = GetAutoMenu(MENU_ID.menuVIDEO_RES);
					String arg = (theMenu != null)? theMenu.GetMenuItemID(pos) : null;
					URL url = CameraCommand.commandSetmovieresolutionUrl(pos, arg) ;
					if (url != null) {	
						new CameraSettingsSendRequest().execute(url) ;
					}
				}
				public void onNothingSelected(AdapterView parent) {
				} 
			}); 
			imageResolution.setSelection(mImageresRet,true);
			imageResolution.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
				public void onItemSelected(AdapterView parent, View view, int pos, long id){					
					if (mImageresRet == pos) return;
					mImageresRet = pos;
					Menu theMenu = GetAutoMenu(MENU_ID.menuIMAGE_RES);
					String arg = (theMenu != null)? theMenu.GetMenuItemID(pos) : null;
					URL url = CameraCommand.commandSetimageresolutionUrl(pos, arg);		
					if (url != null) {
						new CameraSettingsSendRequest().execute(url) ;
					}
				}
				public void onNothingSelected(AdapterView parent) {
				} 
			});  
			motiondetection.setSelection(mMTDRet,true);
			motiondetection.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
				public void onItemSelected(AdapterView parent, View view, int pos, long id){					
					if (mMTDRet == pos) return;
					mMTDRet = pos;
					URL url = CameraCommand.commandSetmotiondetectionUrl(pos, null) ;		
					if (url != null) {
						new CameraSettingsSendRequest().execute(url) ;
					}
				}
				public void onNothingSelected(AdapterView parent) {
				} 
			}); 
			flickerfrequency.setSelection(mFlickerRet,true);
			flickerfrequency.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
				public void onItemSelected(AdapterView parent, View view, int pos, long id){
					if (mFlickerRet == pos) return;
					mFlickerRet = pos;
					URL url = CameraCommand.commandSetflickerfrequencyUrl(pos, null);		
					if (url != null) {
						new CameraSettingsSendRequest().execute(url) ;
					}
				}
				public void onNothingSelected(AdapterView parent) {
				} 
			});
			
			AWB.setSelection(mAWBRet,true);
			AWB.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
				public void onItemSelected(AdapterView parent, View view, int pos, long id){					
					if (mAWBRet == pos) return;
					mAWBRet = pos;
					Menu theMenu = GetAutoMenu(MENU_ID.menuWHITE_BALANCE);
					String arg = (theMenu != null)? theMenu.GetMenuItemID(pos) : null;
					URL url = CameraCommand.commandSetAWBUrl(pos, arg) ;
					if (url != null) {
						new CameraSettingsSendRequest().execute(url) ;
					}
				}
				public void onNothingSelected(AdapterView parent) {
				} 
			});
			exposure.setProgress(mEVRet);
			FWVersion.setText(mFWVersionRet + mszCameraPreviewInfo);		
		}
	};

	private class GetMenuSettingsValues extends AsyncTask <URL, Integer, String> {
		
		protected void onPreExecute() {
			setWaitingState(true) ;
			super.onPreExecute() ;
		}
		@Override
		protected String doInBackground(URL... params) {
			URL url = CameraCommand.commandGetMenuSettingsValuesUrl() ;
			if (url != null) {		
				return CameraCommand.sendRequest(url) ;
			}
			return null ;
		}
		@Override
		protected void onPostExecute(String result) {
			Activity activity = getActivity();
			//Log.d(TAG, "TimeStamp property "+result) ;
			if (result != null) {
				//String[] value;
				//value = GetCameraSettingValue(result, MENU_ID.menuVIDEO_RES);
				String[] lines;		
				String[] lines_temp;
				try {
					lines_temp = result.split("VideoRes=");
					lines = lines_temp[1].split(System.getProperty("line.separator")) ;
					mVideoresRet = AutoMenuCheck(MENU_ID.menuVIDEO_RES, lines[0]);
				} catch (Exception e) { /* TODO: Show Error */ }
				//
				try {
					lines_temp = result.split("ImageRes=");
					lines = lines_temp[1].split(System.getProperty("line.separator")) ;
					mImageresRet = AutoMenuCheck(MENU_ID.menuIMAGE_RES, lines[0]);
				} catch (Exception e) { /* TODO: Show Error */ }
				//
				try {
					lines_temp = result.split("MTD=");
					lines = lines_temp[1].split(System.getProperty("line.separator")) ;
					mMTDRet = AutoMenuCheck(MENU_ID.menuMTD, lines[0]);
				} catch (Exception e) { mMTDRet = 0; }
				//
				try {
					lines_temp = result.split("AWB=");
					lines = lines_temp[1].split(System.getProperty("line.separator")) ;
					mAWBRet = AutoMenuCheck(MENU_ID.menuWHITE_BALANCE, lines[0]);
				} catch (Exception e) { /* TODO: Show Error */ }
				//
				try {
					lines_temp = result.split("Flicker=");
					lines = lines_temp[1].split(System.getProperty("line.separator")) ;
					mFlickerRet = AutoMenuCheck(MENU_ID.menuFLICKER, lines[0]);
				} catch (Exception e) { /* TODO: Show Error */ }
				//
				try {
					lines_temp = result.split("EV=");
					lines = lines_temp[1].split(System.getProperty("line.separator")) ;
					mEVRet = AutoMenuCheck(MENU_ID.menuEV, lines[0]);
				} catch (Exception e) { /* TODO: Show Error */ }
				//
				try {
					lines_temp = result.split("FWversion=");
					lines = lines_temp[1].split(System.getProperty("line.separator")) ;
					mFWVersionRet = lines[0];
				} catch (Exception e) { /* TODO: Show Error */ }
				mHandler.sendMessage(mHandler.obtainMessage());
			}
			else if (activity != null) {
				Toast.makeText(activity, activity.getResources().getString(R.string.message_fail_get_info),
						Toast.LENGTH_LONG).show() ;			
			}
			setWaitingState(false) ;
			setInputEnabled(true) ;
			super.onPostExecute(result) ;
		}
	}
	
	private class GetCameraPreview_Property extends AsyncTask<URL, Integer, String> {

		protected void onPreExecute() {
			super.onPreExecute() ;
		}
		@Override
		protected String doInBackground(URL... params) {
			URL url = CameraCommand.commandQueryCameraPreviewProperty() ;
			if (url != null) {		
				return CameraCommand.sendRequest(url) ;
			}
			return null ;
		}
		@Override
		protected void onPostExecute(String result) {
			mszCameraPreviewInfo = " [k/MJPEG]";
			if (result != null) {
				String[] lines;
				try {
					String[] lines_temp = result.split("Camera.Preview.RTSP.av=");
					lines = lines_temp[1].split(System.getProperty("line.separator")) ;
					int av = Integer.valueOf(lines[0]);
					switch (av) {
					case 1:	// For RTSP MJPEG+AAC
						mszCameraPreviewInfo = " [RTSP/MJPEG+AAC]";
						break;
					case 2: // For RTSP H.264
						// not reaady yet
						mszCameraPreviewInfo = " [RTSP/H.264]";
						break;
					case 3:
						// not reaady yet
						mszCameraPreviewInfo = " [RTSP/H.264+AAC]";
						break;
					default:
						mszCameraPreviewInfo = " [HTTP/MJPEG]";

					}
				} catch (Exception e) {/* Not support RTSP AV */}
			}
			mHandler.sendMessage(mHandler.obtainMessage());
			super.onPostExecute(result) ;
		}
	}

    private class Menu {
        public final String		title;
        public List<MenuItem>	items;
        
        private Menu(String title) {
            this.title = title;
            items = new ArrayList<MenuItem>();
        }
        public void AddItme(MenuItem item) {
        	items.add(item);
        }
        public int GetNumberItem() {
        	return items.size();
        }
        public String GetMenuItemID(int pos) {
        	if (pos > GetNumberItem() || pos < 0)
        		return null;
        	return items.get(pos).id;
        }
        public List<String> GetMenuItemTitleList() {
        	int num = GetNumberItem();
        	List<String> list = new ArrayList();
        	
        	for (int i = 0; i < num; i++) {
        		list.add(items.get(i).title);
        	}
        	return list;
        }
        public int GetItemId(String sz) {
        	int num = GetNumberItem();
        	for (int i = 0; i < num; i++) {
        		if (items.get(i).id.equalsIgnoreCase(sz)) {
        			return i;
        		}
        	}
        	// Support older version FW to convert sz to integer
        	int val = Integer.valueOf(sz);
        	if (val >= 0 && val < num) {
        		return val;
        	}
        	return -1;
        }
    }

    private class MenuItem {
        public final String title;
        public final String id;
        public final int type;

        private MenuItem(String title, String id, int type) {
            this.title = title;
            this.id    = id;
            this.type  = type;
        }
    }
    
    private String GetMenuVersion() {
    	return version;
    }
    
    enum  MENU_ID { menuVIDEO_RES, menuIMAGE_RES,
    				menuLOOPING_VIDEO,
    				menuBURST, menuTIME_LAPSE,
    				menuWHITE_BALANCE,
    				menuHDR,
    				menuMTD, menuFLICKER, menuEV, menuVersion}; 
    private Menu GetAutoMenu(MENU_ID menuid) {
		if (cammenu == null || cammenu.isEmpty()) {
			return null;
		}
		String	menu_title;
		switch (menuid) {
		case menuVIDEO_RES:
			menu_title = new String("VIDEO RESOLUTION");
			break;
		case menuIMAGE_RES:
			menu_title = new String("CAPTURE RESOLUTION");
			break;
		case menuLOOPING_VIDEO:
			menu_title = new String("LOOPING VIDEO");
			break;
		case menuWHITE_BALANCE:
			menu_title = new String("WHITE BALANCE");
			break;
		case menuBURST:
			menu_title = new String("BURST");
			break;
		case menuTIME_LAPSE:
			menu_title = new String("TIME LAPSE");
			break;
		case menuHDR:
			menu_title = new String("HDR");
			break;
		case menuMTD:
			menu_title = new String("MTD");
			break;
		default:
			return null;
		}
		int num = cammenu.size();
		for (int i = 0; i < num; i++) {
			Menu menu = cammenu.get(i);
			if (menu.title.equalsIgnoreCase(menu_title)) {
				return menu;
			}
		}
		return null;
    }
    
    private static String[] EV_val = {	"EVN200", "EVN167", "EVN133", "EVN100", "EVN067", "EVN033",
    									"EV0",
    									"EVP033", "EVP067", "EVP100", "EVP133", "EVP167", "EVP200"};
    private static String[] FLICKER_val = {"50Hz", "60Hz"};
    private static String[] MTD_val = {"OFF", "LOW", "MID", "HIGH"};
    private int FindItem(String[] valdb, String val) {
    	for (int i = 0; i < valdb.length; i++)
    		if (valdb[i].equalsIgnoreCase(val))
    			return i;
    	return -1;
    }
    private int AutoMenuCheck(MENU_ID menuid, String sz) {
    	if (sz == null) return -1;
    	Menu	menu = GetAutoMenu(menuid);
    	if (menu == null) {
    		int	id;
    		switch (menuid) {
    		case menuFLICKER:
    			id = FindItem(FLICKER_val, sz);
    			if (id != -1) return id;
    			break;
    		case menuEV:
    			id = FindItem(EV_val, sz);
    			if (id != -1) return id;
    			break;
    		case menuMTD:
    			id = FindItem(MTD_val, sz);
    			if (id != -1) return id;
    			break;
    		}
    		return Integer.valueOf(sz);
    	}
    	return menu.GetItemId(sz);
    }
    
    private static String		version = null;
	private static List<Menu>	cammenu = null;
	enum AM_STATUS {UNKNOWN ,READY, NONE};
	private static AM_STATUS amstat = AM_STATUS.UNKNOWN; 
	
	private class GetCameraMenu_Property extends AsyncTask<URL, Integer, String> {

		protected void onPreExecute() {
			super.onPreExecute() ;
		}
		@Override
		protected String doInBackground(URL... params) {
			URL url = null ;
			try {
				String ip = CameraCommand.getCameraIp() ;
				if (ip != null)
					url = new URL("http://" + ip  + "/cammenu.xml") ;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			if (url != null) {		
				return CameraCommand.sendRequest(url) ;
			}
			return null ;
		}
		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				InputStream in = null;
				try {
					in = new ByteArrayInputStream(result.getBytes("UTF-8"));
				} catch (IOException e) {
					return;
				}
		        try {
		        	cammenu = parse(in);
		        	if (cammenu.size() > 0) {
		        		UpdateMenu();
		        		amstat = AM_STATUS.READY;
		        	} else {
		        		amstat = AM_STATUS.NONE;
		        	}
		        } catch (IOException e) {
		            return;
		        } catch (XmlPullParserException e) {
		            return;
		        }
			} else {
				amstat = AM_STATUS.NONE; 
			}
			mHandler.sendMessage(mHandler.obtainMessage());
			super.onPostExecute(result) ;
		}
	    private String ns = null;
	    private List<Menu> parse(InputStream in) throws XmlPullParserException, IOException {
	        try {
	            XmlPullParser parser = Xml.newPullParser();
	            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
	            parser.setInput(in, null);
	            parser.nextTag();
	            return readCamera(parser);
	        } finally {
	            in.close();
	        }
	    }

	    private List<Menu> readCamera(XmlPullParser parser) throws XmlPullParserException, IOException {
	        List<Menu> entries = new ArrayList<Menu>();

	        parser.require(XmlPullParser.START_TAG, ns, "camera");
	        while (parser.next() != XmlPullParser.END_TAG) {
	            if (parser.getEventType() != XmlPullParser.START_TAG) {
	                continue;
	            }
	            String name = parser.getName();
	            // Starts by looking for the entry tag
	            if (name.equals("menu")) {
	                entries.add(readMenu(parser));
	            } else if (name.equals("version")) {
	                version = readVersion(parser);
	            } else {
	                skip(parser);
	            }
	        }
	        return entries;
	    }

	    // Parses the contents of an menu. If it encounters a item tag, hands them
	    // off
	    // to their respective &quot;read&quot; methods for processing. Otherwise, skips the tag.
	    private Menu readMenu(XmlPullParser parser) throws XmlPullParserException, IOException {
	        parser.require(XmlPullParser.START_TAG, ns, "menu");
	        Menu	menu;

	        String title = parser.getAttributeValue(null, "title");
	        menu = new Menu(title);
	        while (parser.next() != XmlPullParser.END_TAG) {
	            if (parser.getEventType() != XmlPullParser.START_TAG) {
	                continue;
	            }
	            String name = parser.getName();
	            if (name.equals("item")) {
	                menu.AddItme(readMenuItem(parser));
	            } else {
	                skip(parser);
	            }
	        }
	        return menu;
	    }

	    // Processes item tags in the menu.
	    private MenuItem readMenuItem(XmlPullParser parser) throws IOException, XmlPullParserException {
	        parser.require(XmlPullParser.START_TAG, ns, "item");
	        String id    = parser.getAttributeValue(null, "id");
	        String title = readText(parser);
	        parser.require(XmlPullParser.END_TAG, ns, "item");
	        return new MenuItem(title, id, 0);
	    }
	    // Processes version tag in the Camera.
	    private String readVersion(XmlPullParser parser) throws IOException, XmlPullParserException {
	        parser.require(XmlPullParser.START_TAG, ns, "version");
	        String ver= readText(parser);
	        parser.require(XmlPullParser.END_TAG, ns, "version");
	        return ver;
	    }

	    // Processes link tags in the feed.
	    private String readLink(XmlPullParser parser) throws IOException, XmlPullParserException {
	        String link = "";
	        parser.require(XmlPullParser.START_TAG, ns, "link");
	        String tag = parser.getName();
	        String relType = parser.getAttributeValue(null, "rel");
	        if (tag.equals("link")) {
	            if (relType.equals("alternate")) {
	                link = parser.getAttributeValue(null, "href");
	                parser.nextTag();
	            }
	        }
	        parser.require(XmlPullParser.END_TAG, ns, "link");
	        return link;
	    }

	    // Processes summary tags in the feed.
	    private String readSummary(XmlPullParser parser) throws IOException, XmlPullParserException {
	        parser.require(XmlPullParser.START_TAG, ns, "summary");
	        String summary = readText(parser);
	        parser.require(XmlPullParser.END_TAG, ns, "summary");
	        return summary;
	    }

	    // For the tags title and summary, extracts their text values.
	    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
	        String result = "";
	        if (parser.next() == XmlPullParser.TEXT) {
	            result = parser.getText();
	            parser.nextTag();
	        }
	        return result;
	    }
	    // Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
	    // if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
	    // finds the matching END_TAG (as indicated by the value of "depth" being 0).
	    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            throw new IllegalStateException();
	        }
	        int depth = 1;
	        while (depth != 0) {
	            switch (parser.next()) {
	            case XmlPullParser.END_TAG:
	                    depth--;
	                    break;
	            case XmlPullParser.START_TAG:
	                    depth++;
	                    break;
	            }
	        }
	    }
	}
	
	private List<View> mViewList = new LinkedList<View>() ;

	private void setInputEnabled(boolean enabled) {

		for (View view : mViewList) {

			view.setEnabled(enabled) ;
		}
	}

	private boolean mWaitingState = false ;
	private boolean mWaitingVisible = false ;

	private void setWaitingState(boolean waiting) {

		if (mWaitingState != waiting) {

			mWaitingState = waiting ;
			setWaitingIndicator(mWaitingState, mWaitingVisible) ;
		}
	}

	private void setWaitingIndicator(boolean waiting, boolean visible) {

		if (!visible)
			return ;

		setInputEnabled(!waiting) ;

		Activity activity = getActivity() ;

		if (activity != null) {
			activity.setProgressBarIndeterminate(true) ;
			activity.setProgressBarIndeterminateVisibility(waiting) ;
		}
	}

	private void clearWaitingIndicator() {

		mWaitingVisible = false ;
		setWaitingIndicator(false, true) ;
	}

	private void restoreWaitingIndicator() {

		mWaitingVisible = true ;
		setWaitingIndicator(mWaitingState, true) ;
	}
	@Override
	public void onDestroy() {
		CameraSniffer sniffer = MainActivity.sniffer;
		sniffer.SetPeeker(null);
		super.onDestroy();
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    view = inflater.inflate(R.layout.camera_settings, container, false) ;
		videoResolution = (Spinner) view.findViewById(R.id.cameraSettingsVideoResolutionSpinner) ;
		videoResolutionAdapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_spinner_item, new String[] { "1080p 30", "720p 30", "720p 60", "VGA" }) ;
		videoResolutionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) ;
		videoResolution.setAdapter(videoResolutionAdapter) ;
		imageResolution = (Spinner) view.findViewById(R.id.cameraSettingsImageResolutionSpinner) ;

		imageResolutionAdapter = new ArrayAdapter<String>(getActivity(),
		android.R.layout.simple_spinner_item, new String[] { "14M" , "12M" , "8M", "5M", "3M", "2M", "1.2M" , "VGA" }) ;
		imageResolutionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) ;
		imageResolution.setAdapter(imageResolutionAdapter) ;
		
		motiondetection = (Spinner) view.findViewById(R.id.cameraSettingsmotiondetectionSpinner) ;
		motiondetectionAdapter = new ArrayAdapter<String>(getActivity(),
		android.R.layout.simple_spinner_item, new String[] { getResources().getString(R.string.label_Off), getResources().getString(R.string.label_Low), 
			getResources().getString(R.string.label_Middle), getResources().getString(R.string.label_High) }) ;
		motiondetectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) ;
		motiondetection.setAdapter(motiondetectionAdapter) ;
		
		flickerfrequency = (Spinner) view.findViewById(R.id.cameraSettingsflickerSpinner) ;
		flickerfrequencyAdapter = new ArrayAdapter<String>(getActivity(),
		android.R.layout.simple_spinner_item, new String[] { getResources().getString(R.string.Flicker_50), getResources().getString(R.string.Flicker_60) }) ;
		flickerfrequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) ;
		flickerfrequency.setAdapter(flickerfrequencyAdapter) ;
		
		AWB = (Spinner) view.findViewById(R.id.cameraSettingswhitebalanceSpinner) ;
		AWBAdapter = new ArrayAdapter<String>(getActivity(),
		android.R.layout.simple_spinner_item, new String[] { getResources().getString(R.string.AWB_Auto), 
		getResources().getString(R.string.AWB_Daylight), getResources().getString(R.string.AWB_Cloudy) , 
		getResources().getString(R.string.AWB_Fluorescent_W) , getResources().getString(R.string.AWB_Fluorescent_N),
		getResources().getString(R.string.AWB_Fluorescent_D) , getResources().getString(R.string.AWB_Incandescent)}) ;
		AWBAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) ;
		AWB.setAdapter(AWBAdapter) ;

		exposure = (SeekBar) view.findViewById(R.id.ExposureseekBar1) ;
		exposure.setMax(12);
		exposure.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				// Wait drawing released then send command
				int pos;
				pos = exposure.getProgress();
				if (mEVRet == pos) return;
				mEVRet = pos;
				URL url = CameraCommand.commandSetEVUrl(mEVRet) ;		
				if (url != null) {		
					new CameraSettingsSendRequest().execute(url) ;
				}
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				
			}
			
			@Override
			public void onProgressChanged(SeekBar arg0, int pos, boolean arg2) {
				// TODO Auto-generated method stub
				/* live setting
				URL url = CameraCommand.commandSetEVUrl(pos) ;		
				if (url != null) {		
					new CameraSettingsSendRequest().execute(url) ;
				}
				*/
			}
		});
		
		FWVersion = (TextView) view.findViewById(R.id.FWVersionNum);
		
		Button CameraTimeSettings = (Button) view.findViewById(R.id.SyncDevice) ;

		CameraTimeSettings.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {

				URL url = CameraCommand.commandCameraTimeSettingsUrl() ;
				if (url != null) {
					new CameraCommand.SendRequest().execute(url) ;
				}

			}
		}) ;
		if (cammenu != null) {
			UpdateMenu();
		}
		return view ;
	}

	private void UpdateMenu() {
		if (videoResolutionAdapter != null) {
			Menu theMenu = GetAutoMenu(MENU_ID.menuVIDEO_RES);
			List<String> list = theMenu.GetMenuItemTitleList();
			ArrayAdapter dataAdapter = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, list);
			dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			videoResolution.setAdapter(dataAdapter);
		}
		if (imageResolutionAdapter != null) {
			Menu theMenu = GetAutoMenu(MENU_ID.menuIMAGE_RES);
			List<String> list = theMenu.GetMenuItemTitleList();
			ArrayAdapter dataAdapter = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, list);
			dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			imageResolution.setAdapter(dataAdapter);
		}
		if (AWBAdapter != null) {
			Menu theMenu = GetAutoMenu(MENU_ID.menuWHITE_BALANCE);
			List<String> list = theMenu.GetMenuItemTitleList();
			ArrayAdapter dataAdapter = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, list);
			dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			AWB.setAdapter(dataAdapter);
		}
		return;
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState) ;
		
		if (amstat == AM_STATUS.UNKNOWN || cammenu == null) {
			new GetCameraMenu_Property().execute();
		}
		new GetCameraPreview_Property().execute();
		new GetMenuSettingsValues().execute();
		CameraSniffer sniffer = MainActivity.sniffer;
		MyPeeker peeker = new MyPeeker(this);
		sniffer.SetPeeker(peeker);
	}
	
	public void Reception(String s) {
		String	val;
		int		id;
		boolean	update = false;
		
		val = CameraSniffer.GetVideoRes(s);
		id  = AutoMenuCheck(MENU_ID.menuVIDEO_RES, val);
		if (id != -1 && mVideoresRet != id) {
			mVideoresRet = id;
			update = true;
			//videoResolution.setSelection(mVideoresRet, true);			
		}
		val = CameraSniffer.GetImageRes(s);
		id  = AutoMenuCheck(MENU_ID.menuIMAGE_RES, val);
		if (id != -1 && mImageresRet != id) {
			mImageresRet = id;
			update = true;
			//imageResolution.setSelection(mImageresRet, true);
		}
		val = CameraSniffer.GetWhiteBalance(s);
		id  = AutoMenuCheck(MENU_ID.menuWHITE_BALANCE, val);
		if (id != -1 && mAWBRet != id) {
			mAWBRet = id;
			update = true;
			//AWB.setSelection(mAWBRet, true);
		}
		val = CameraSniffer.GetFlicker(s);
		id  = AutoMenuCheck(MENU_ID.menuFLICKER, val);
		if (id != -1 && mFlickerRet != id) {
			mFlickerRet = id;
			update = true;
			//flickerfrequency.setSelection(mFlickerRet, true);
		}
		val = CameraSniffer.GetEV(s);
		id  = AutoMenuCheck(MENU_ID.menuEV, val);
		if (id != -1 && mEVRet != id) {
			mEVRet = id;
			update = true;
			//exposure.setProgress(mEVRet);
		}
		if (update)
			mHandler.sendMessage(mHandler.obtainMessage());
		//Log.i("SETTING", s);
	}
	
	class MyPeeker extends CameraPeeker{
		CameraSettingsFragment theFrag;
		MyPeeker(CameraSettingsFragment frag) {
			theFrag = frag;
		}
		public void SetReceiver(CameraSettingsFragment frag) {
			theFrag = frag;
		}
		public void Update(String s) {
			theFrag.Reception(s);
		}
	}
}
