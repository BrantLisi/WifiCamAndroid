package com.vless.wificam;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.vless.wificam.R;
import com.vless.wificam.Control.CameraControlFragment;
import com.vless.wificam.FileBrowser.FileBrowserFragment;
import com.vless.wificam.FileBrowser.LocalFileBrowserFragment;
import com.vless.wificam.Viewer.MjpegPlayerFragment;
import com.vless.wificam.Viewer.StreamPlayerFragment;
import com.vless.wificam.Viewer.ViewerSettingFragment;
import com.vless.wificam.adapts.FragmentTabAdapter;
import com.vless.wificam.frags.WifiCamFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.Window;
import android.widget.RadioGroup;

public class MainActivity extends FragmentActivity {

	public static String intToIp(int addr) {

		return ((addr & 0xFF) + "." + ((addr >>>= 8) & 0xFF) + "."
				+ ((addr >>>= 8) & 0xFF) + "." + ((addr >>>= 8) & 0xFF));
	}

	public static String getSnapshotFileName() {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss",
				Locale.US);
		String currentDateandTime = sdf.format(new Date());

		return currentDateandTime + ".jpg";
	}

	public static String getMJpegFileName() {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss",
				Locale.US);
		String currentDateandTime = sdf.format(new Date());

		return currentDateandTime;
	}

	public static String sAppName = "";
	public static String sAppDir = "";

	public static File getAppDir() {
		File appDir = new File(sAppDir);
		if (!appDir.exists()) {
			appDir.mkdirs();
		}
		return appDir;
	}

	public static Uri addImageAsApplication(ContentResolver contentResolver,
			String name, long dateTaken, String directory, String filename) {

		String filePath = directory + File.separator + filename;

		String[] imageProjection = new String[] {
				"DISTINCT " + BaseColumns._ID, MediaColumns.DATA,
				MediaColumns.DISPLAY_NAME };

		String imageSelection = new String(Images.Media.TITLE + "=? AND "
				+ Images.Media.DISPLAY_NAME + "=?");

		String[] imageSelectionArgs = new String[] { name, filename };

		Cursor cursor = contentResolver.query(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageProjection,
				imageSelection, imageSelectionArgs, null);

		if (cursor == null || cursor.getCount() == 0) {

			ContentValues values = new ContentValues(7);
			values.put(Images.Media.TITLE, name);
			values.put(Images.Media.DISPLAY_NAME, filename);
			values.put(Images.Media.DATE_TAKEN, dateTaken);
			values.put(Images.Media.MIME_TYPE, "image/jpeg");
			values.put(Images.Media.DATA, filePath);

			return contentResolver.insert(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		} else {

			int idColumn = cursor.getColumnIndex(MediaColumns._ID);

			if (idColumn == -1)
				return null;

			cursor.moveToFirst();

			Long id = cursor.getLong(idColumn);

			return Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI
					.toString() + "/" + String.valueOf(id));
		}
	}

	private static Locale sDefaultLocale;
	private static Locale sSelectedLocale;

	static {
		sDefaultLocale = Locale.getDefault();
	}

	public static Locale getDefaultLocale() {
		return sDefaultLocale;
	}

	public static void setAppLocale(Locale locale) {
		Locale.setDefault(locale);
		sSelectedLocale = locale;
	}

	public static Locale getAppLocale() {
		return sSelectedLocale == null ? sDefaultLocale : sSelectedLocale;
	}

	
	/* Query property of RTSP AV1 */
	private class GetRTPS_AV1 extends AsyncTask<URL, Integer, String> {

		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(URL... params) {
			URL url = CameraCommand.commandQueryAV1Url();
			if (url != null) {
				return CameraCommand.sendRequest(url);
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			String liveStreamUrl;
			WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
			DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
			if (dhcpInfo == null || dhcpInfo.gateway == 0) {
				AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
						.create();
				alertDialog.setTitle(getResources().getString(
						R.string.dialog_DHCP_error));
				alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,
						getResources().getString(R.string.label_ok),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
							}
						});
				alertDialog.show();
				addFragments("");
				return;
			}
			String gateway = MainActivity.intToIp(dhcpInfo.gateway);
			// set http push as default for streaming
			liveStreamUrl = "http://" + gateway
					+ MjpegPlayerFragment.DEFAULT_MJPEG_PUSH_URL;
			if (result != null) {
				String[] lines;
				try {
					String[] lines_temp = result
							.split("Camera.Preview.RTSP.av=");
					String str = System.getProperty("line.separator");
					lines = lines_temp[1].split(System
							.getProperty("line.separator"));
					int av = Integer.valueOf(lines[0]);
					switch (av) {
					case 1: // liveRTSP/av1 for RTSP MJPEG+AAC
						liveStreamUrl = "rtsp://"
								+ gateway
								+ MjpegPlayerFragment.DEFAULT_RTSP_MJPEG_AAC_URL;
						break;
					case 2: // liveRTSP/v1 for RTSP H.264
						liveStreamUrl = "rtsp://" + gateway
								+ MjpegPlayerFragment.DEFAULT_RTSP_H264_URL;
						break;
					case 3: // liveRTSP/av2 for RTSP H.264+AAC
						liveStreamUrl = "rtsp://" + gateway
								+ MjpegPlayerFragment.DEFAULT_RTSP_H264_AAC_URL;
						break;
					}
				} catch (Exception e) {
					/* not match, for firmware of MJPEG only */
				}
			}
			Log.i("liveStreamUrl", " liveStreamUrl: " + liveStreamUrl);
			addFragments(liveStreamUrl);
			super.onPostExecute(result);
		}
	}
	
	public static CameraSniffer sniffer = null;
	private List<WifiCamFragment> fragments = new ArrayList<WifiCamFragment>();
	private FragmentTabAdapter tabAdapter;
	private RadioGroup rgs;
	private int selectedIndex;
	private WifiCamFragment strmPlyFrg,filBrwFrg,locBrwFrg,camCtlFrg;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		Log.i("Fragment Activity", "ON CREATE " + savedInstanceState);

		System.setProperty("http.keepAlive", "false");

		super.onCreate(savedInstanceState);
		
		if (sniffer == null) {
			sniffer = new CameraSniffer();
			sniffer.start();
		}
		
		if (sSelectedLocale == null) {
			sSelectedLocale = sDefaultLocale;
		}

		Locale.setDefault(Locale.ENGLISH);
		Configuration config = new Configuration();
		config.locale = Locale.ENGLISH;
		getResources().updateConfiguration(config, null);

		sAppName = getResources().getString(R.string.app_name);

		Locale.setDefault(sSelectedLocale);
		config = new Configuration();
		config.locale = sSelectedLocale;
		getResources().updateConfiguration(config, null);

		sAppDir = Environment.getExternalStorageDirectory().getPath()
				+ File.separator + sAppName;
		Log.i("Fragment Activity", sAppDir);
		File appDir = new File(sAppDir);
		if (!appDir.exists()) {
			appDir.mkdirs();
		}

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.wifi_cam_main);

		setTitle(getResources().getString(R.string.app_name));

		getActionBar().setDisplayHomeAsUpEnabled(false);

		setProgressBarIndeterminateVisibility(false);

		if (savedInstanceState == null) {

			WifiManager wifiManager = (WifiManager) getApplicationContext()
					.getSystemService(Context.WIFI_SERVICE);

			Log.i("brant", wifiManager.getConnectionInfo().toString());
			Log.i("brant", wifiManager.isWifiEnabled() + " --- "
					+ wifiManager.getConnectionInfo().getNetworkId() + " --- ");

			if (wifiManager.isWifiEnabled()
					&& wifiManager.getConnectionInfo().getNetworkId() != -1) {
				initView();
			} else {
				String title = getResources().getString(
						R.string.dialog_no_connection_title);
				String message = getResources().getString(
						R.string.dialog_no_connection_message);

				new AlertDialog.Builder(this)
						.setTitle(title)
						.setMessage(message)
						.setPositiveButton(
								getResources().getString(R.string.label_ok),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
										initView();
									}
								}).show();
			}
		}
	}

	private void initView() {
		new GetRTPS_AV1().execute();
//		addFragments("");
	}

	private void addFragments(String liveStreamUrl) {
		strmPlyFrg = StreamPlayerFragment.newInstance(liveStreamUrl);
		filBrwFrg = FileBrowserFragment.newInstance(null, null,
				null);
		locBrwFrg = new LocalFileBrowserFragment();
		camCtlFrg = new CameraControlFragment();
		
		fragments.add(strmPlyFrg);
		fragments.add(filBrwFrg);
		fragments.add(locBrwFrg);
		fragments.add(camCtlFrg);
		
		rgs = (RadioGroup) findViewById(R.id.tabs_rg);

		tabAdapter = new FragmentTabAdapter(this, fragments, R.id.tab_content,
				rgs);
		tabAdapter
				.setOnRgsExtraCheckedChangedListener(new FragmentTabAdapter.OnRgsExtraCheckedChangedListener() {
					@Override
					public void OnRgsExtraCheckedChanged(RadioGroup radioGroup,
							int checkedId, int index) {
						selectedIndex = index;
					}
				});
	}

	private SubMenu mLanguageSubMenu;
	private String[] mLanguageNames;
	private Locale[] mLocales;

	public static int sConnectionDelay = 2000;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		mLanguageNames = new String[] {
				getResources().getString(R.string.label_default), "English",
				getResources().getString(R.string.label_language_TChinese),
				getResources().getString(R.string.label_language_SChinese),
				getResources().getString(R.string.label_language_Russian),
				getResources().getString(R.string.label_language_Spanish),
				getResources().getString(R.string.label_language_Portuguese),
				getResources().getString(R.string.label_language_French),
				getResources().getString(R.string.label_language_Italian),
				getResources().getString(R.string.label_language_Germany),
				getResources().getString(R.string.label_language_Czech),
				getResources().getString(R.string.label_language_Japanese),
				getResources().getString(R.string.label_language_Korean),
				getResources().getString(R.string.label_language_Latvian),
				getResources().getString(R.string.label_language_Polish),
				getResources().getString(R.string.label_language_Romanian),
				getResources().getString(R.string.label_language_Slovak),
				getResources().getString(R.string.label_language_Ukrainian) };

		mLocales = new Locale[] { MainActivity.getDefaultLocale(),
				Locale.ENGLISH, Locale.TRADITIONAL_CHINESE,
				Locale.SIMPLIFIED_CHINESE, new Locale("ru", "RU"),
				new Locale("es", "ES"), new Locale("pt", "PT"), Locale.FRANCE,
				Locale.ITALY, Locale.GERMANY, new Locale("cs", "CZ"),
				Locale.JAPAN, Locale.KOREA, new Locale("lv", "LV"),
				new Locale("pl", "PL"), new Locale("ro", "RO"),
				new Locale("sk", "SK"), new Locale("uk", "UA") };

		mLanguageSubMenu = menu.addSubMenu(0, 0, 0,
				getResources().getString(R.string.label_language));

		int i = 0;
		for (String language : mLanguageNames) {
			MenuItem item = mLanguageSubMenu.add(0, i++, 0, language);
			item.setCheckable(true);
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		if (mLanguageSubMenu != null) {

			int size = mLanguageSubMenu.size();

			for (int i = 0; i < size; i++) {
				MenuItem item = mLanguageSubMenu.getItem(i);

				if (i > 0 && getAppLocale().equals(mLocales[i])) {
					item.setChecked(true);
				} else {
					item.setChecked(false);
				}

				item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem checkedItem) {

						int size = mLanguageSubMenu.size();

						for (int i = 0; i < size; i++) {
							MenuItem item = mLanguageSubMenu.getItem(i);
							if (checkedItem == item
									&& item.isChecked() == false) {
								item.setChecked(true);

								setAppLocale(mLocales[i]);

								Intent intent = getIntent();
								finish();
								startActivity(intent);
							} else {
								item.setChecked(false);
							}
						}
						return true;
					}
				});
			}
		}

		return super.onPrepareOptionsMenu(menu);
	}

//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//		case android.R.id.home:
//			// backToFristFragment(this) ;
//			return true;
//		}
//		return super.onOptionsItemSelected(item);
//	}
//
//	@Override
//	public void onConfigurationChanged(Configuration newConfig) {
//		super.onConfigurationChanged(newConfig);
//	}

}
