package com.vless.wificam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.videolan.vlc.VLCApplication;

import com.vless.wificam.R;
import com.vless.wificam.Viewer.StreamPlayerFragment;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;


public class CameraCommand {

	private static String CGI_PATH = "/cgi-bin/Config.cgi";

	private static String ACTION_SET = "set";
	private static String ACTION_GET = "get";
	private static String ACTION_DEL = "del";
	private static String ACTION_PLAY = "play";

	public static String PROPERTY_NET = "Net";
	public static String PROPERTY_SSID = "Net.WIFI_AP.SSID";
	public static String PROPERTY_ENCRYPTION_KEY = "Net.WIFI_AP.CryptoKey";

	public static String PROPERTY_TIMESTAMP_YEAR = "Camera.Preview.MJPEG.TimeStamp.year";
	public static String PROPERTY_TIMESTAMP_MONTH = "Camera.Preview.MJPEG.TimeStamp.month";
	public static String PROPERTY_TIMESTAMP_DAY = "Camera.Preview.MJPEG.TimeStamp.day";
	public static String PROPERTY_TIMESTAMP = "Camera.Preview.MJPEG.TimeStamp";
	public static String PROPERTY_RTSP_AV = "Camera.Preview.RTSP.av";
	public static String PROPERTY_RECORDSTATUS = "Camera.Preview.MJPEG.status";

	public static String PROPERTY_VIDEO = "Videores";
	public static String PROPERTY_IMAGE = "Imageres";
	public static String PROPERTY_EV = "Exposure";
	public static String PROPERTY_MTD = "MTD";
	public static String PROPERTY_FILESTREAMING = "DCIM$100__DSC$";

	private static String COMMAND_FIND_CAMERA = "findme";
	private static String COMMAND_RESET = "reset";
	private static String COMMAND_MOVIERES = "720P60fps";
	private static String COMMAND_IMAGERES = "5M";
	private static String COMMAND_VIDEORECORD = "record";
	private static String COMMAND_VIDEOCAPTURE = "capture";
	public static String PROPERTY_VIDEORECORD = "Video";
	private static String COMMAND_EV = "EV0";
	private static String COMMAND_MTD = "Off";
	public static String PROPERTY_FLICKER = "Flicker";
	private static String COMMAND_FLICKER = "50Hz";
	public static String PROPERTY_AWB = "AWB";
	private static String COMMAND_AWB = "Auto";
	public static String PROPERTY_DELETEFILES = "$DCIM$*";
	private static String COMMAND_DELETEFILES = "";
	public static String PROPERTY_DEFAULTVALUE = "Camera.Menu.*";
	public static String PROPERTY_CAMERAPREVIEW = "Camera.Preview.*";
	public static String PROPERTY_TIMESETTING = "TimeSettings";
	private static String COMMAND_TIMESTRING = "2014/01/01 00:00:00";

	private static String COMMAND_FILESTREAMING = "";

	public static String getCameraIp() {

		Context context = VLCApplication.getAppContext();
		if (context == null)
			return null;

		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

		if (dhcpInfo != null && dhcpInfo.gateway != 0) {

			return MainActivity.intToIp(dhcpInfo.gateway);
		}
		return null;
	}

	private static String buildArgument(String property, String value) {
		try {
			return "property=" + property + "&value="
					+ URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private static String buildArgument(String property) {
		return "property=" + property;
	}

	private static String buildArgumentList(String[] arguments) {

		String argumentList = "";

		for (String argument : arguments) {

			if (argument != null)
				argumentList = argumentList + "&" + argument;
		}
		return argumentList;
	}

	private static URL buildRequestUrl(String path, String action,
			String argumentList) {

		URL url = null;

		try {
			String ip = getCameraIp();
			if (ip != null)
				url = new URL("http://" + ip + path + "?action=" + action
						+ argumentList);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return url;
	}

	public static URL commandUpdateUrl(String ssid, String encryptionKey) {

		String[] arguments = new String[2];

		arguments[0] = buildArgument(PROPERTY_SSID, ssid);
		arguments[1] = buildArgument(PROPERTY_ENCRYPTION_KEY, encryptionKey);

		return buildRequestUrl(CGI_PATH, ACTION_SET,
				buildArgumentList(arguments));

	}

	public static URL commandFindCameraUrl() {

		String[] arguments = new String[1];

		arguments[0] = buildArgument(PROPERTY_NET, COMMAND_FIND_CAMERA);

		return buildRequestUrl(CGI_PATH, ACTION_SET,
				buildArgumentList(arguments));
	}

	public static URL commandCameraRecordUrl() {

		String[] arguments = new String[1];

		arguments[0] = buildArgument(PROPERTY_VIDEORECORD, COMMAND_VIDEORECORD);

		return buildRequestUrl(CGI_PATH, ACTION_SET,
				buildArgumentList(arguments));
	}

	public static URL commandCameraTimeSettingsUrl() {

		String[] arguments = new String[1];

		Calendar calendar = Calendar.getInstance();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
				"yyyy$MM$dd$HH$mm$ss$");

		String TimeString = simpleDateFormat.format(calendar.getTime());

		COMMAND_TIMESTRING = TimeString;

		arguments[0] = buildArgument(PROPERTY_TIMESETTING, COMMAND_TIMESTRING);

		return buildRequestUrl(CGI_PATH, ACTION_SET,
				buildArgumentList(arguments));
	}

	public static URL commandCameraSnapshotUrl() {

		String[] arguments = new String[1];

		arguments[0] = buildArgument(PROPERTY_VIDEORECORD, COMMAND_VIDEOCAPTURE);

		return buildRequestUrl(CGI_PATH, ACTION_SET,
				buildArgumentList(arguments));
	}

	public static URL commandReactivateUrl() {

		String[] arguments = new String[1];

		arguments[0] = buildArgument(PROPERTY_NET, COMMAND_RESET);

		return buildRequestUrl(CGI_PATH, ACTION_SET,
				buildArgumentList(arguments));
	}

	public static URL commandSetmovieresolutionUrl(int pos, String arg) {
		String[] arguments = new String[1];
		if (arg == null) {
			switch (pos) {
			case 0:
				COMMAND_MOVIERES = "1080P30fps";
				break;
			case 1:
				COMMAND_MOVIERES = "720P30fps";
				break;
			case 2:
				COMMAND_MOVIERES = "720P60fps";
				break;
			case 3:
				COMMAND_MOVIERES = "VGA";
				break;
			default:
				COMMAND_MOVIERES = "1080P30fps";
				break;
			}
		} else {
			COMMAND_MOVIERES = arg;
		}
		arguments[0] = buildArgument(PROPERTY_VIDEO, COMMAND_MOVIERES);
		return buildRequestUrl(CGI_PATH, ACTION_SET,
				buildArgumentList(arguments));
	}

	public static URL commandSetimageresolutionUrl(int pos, String arg) {
		String[] arguments = new String[1];
		if (arg == null) {
			switch (pos) {
			case 0:
				COMMAND_IMAGERES = "14M";
				break;
			case 1:
				COMMAND_IMAGERES = "12M";
				break;
			case 2:
				COMMAND_IMAGERES = "8M";
				break;
			case 3:
				COMMAND_IMAGERES = "5M";
				break;
			case 4:
				COMMAND_IMAGERES = "3M";
				break;
			case 5:
				COMMAND_IMAGERES = "2M";
				break;
			case 6:
				COMMAND_IMAGERES = "1.2M";
				break;
			case 7:
				COMMAND_IMAGERES = "VGA";
				break;
			default:
				COMMAND_IMAGERES = "5M";
				break;
			}
		} else {
			COMMAND_IMAGERES = arg;
		}
		arguments[0] = buildArgument(PROPERTY_IMAGE, COMMAND_IMAGERES);
		return buildRequestUrl(CGI_PATH, ACTION_SET,
				buildArgumentList(arguments));
	}

	public static URL commandSetEVUrl(int pos) {
		String[] arguments = new String[1];
		switch (pos) {
		case 0:
			COMMAND_EV = "EVN200";
			break;
		case 1:
			COMMAND_EV = "EVN167";
			break;
		case 2:
			COMMAND_EV = "EVN133";
			break;
		case 3:
			COMMAND_EV = "EVN100";
			break;
		case 4:
			COMMAND_EV = "EVN067";
			break;
		case 5:
			COMMAND_EV = "EVN033";
			break;
		case 6:
			COMMAND_EV = "EV0";
			break;
		case 7:
			COMMAND_EV = "EVP033";
			break;
		case 8:
			COMMAND_EV = "EVP067";
			break;
		case 9:
			COMMAND_EV = "EVP100";
			break;
		case 10:
			COMMAND_EV = "EVP133";
			break;
		case 11:
			COMMAND_EV = "EVP167";
			break;
		case 12:
			COMMAND_EV = "EVP200";
			break;
		default:
			COMMAND_EV = "EV0";
			break;
		}
		arguments[0] = buildArgument(PROPERTY_EV, COMMAND_EV);
		return buildRequestUrl(CGI_PATH, ACTION_SET,
				buildArgumentList(arguments));
	}

	public static URL commandSetmotiondetectionUrl(int pos, String arg) {
		String[] arguments = new String[1];
		if (arg == null) {
			switch (pos) {
			case 0:
				COMMAND_MTD = "Off";
				break;
			case 1:
				COMMAND_MTD = "Low";
				break;
			case 2:
				COMMAND_MTD = "Middle";
				break;
			case 3:
				COMMAND_MTD = "High";
				break;
			default:
				COMMAND_EV = "Off";
				break;
			}
		} else {
			COMMAND_MTD = arg;
		}
		arguments[0] = buildArgument(PROPERTY_MTD, COMMAND_MTD);
		return buildRequestUrl(CGI_PATH, ACTION_SET,
				buildArgumentList(arguments));
	}

	public static URL commandSetdeletefilesUrl(int pos) {

		String[] arguments = new String[1];

		switch (pos) {
		case 0:
			COMMAND_DELETEFILES = ".avi";
			break;
		case 1:
			COMMAND_DELETEFILES = ".jpg";
			break;
		default:
			break;
		}

		arguments[0] = buildArgument(PROPERTY_DELETEFILES + COMMAND_DELETEFILES);

		return buildRequestUrl(CGI_PATH, ACTION_DEL,
				buildArgumentList(arguments));
	}

	public static URL commandSetdeletesinglefileUrl(String filename) {

		String[] arguments = new String[1];

		arguments[0] = buildArgument(filename.replaceAll("/", "\\$"));

		return buildRequestUrl(CGI_PATH, ACTION_DEL,
				buildArgumentList(arguments));
	}

	public static URL commandPlayfilestreamingUrl(String filename) {

		String[] arguments = new String[1];

		COMMAND_FILESTREAMING = PROPERTY_FILESTREAMING + filename;

		arguments[0] = buildArgument(COMMAND_FILESTREAMING);

		return buildRequestUrl(CGI_PATH, ACTION_PLAY,
				buildArgumentList(arguments));
	}

	public static URL commandSetflickerfrequencyUrl(int pos, String arg) {
		String[] arguments = new String[1];
		if (arg == null) {
			switch (pos) {
			case 0:
				COMMAND_FLICKER = "50Hz";
				break;
			case 1:
				COMMAND_FLICKER = "60Hz";
				break;
			default:
				COMMAND_FLICKER = "50Hz";
				break;
			}
		} else {
			COMMAND_FLICKER = arg;
		}
		arguments[0] = buildArgument(PROPERTY_FLICKER, COMMAND_FLICKER);
		return buildRequestUrl(CGI_PATH, ACTION_SET,
				buildArgumentList(arguments));
	}

	public static URL commandSetAWBUrl(int pos, String arg) {
		String[] arguments = new String[1];
		if (arg == null) {
			switch (pos) {
			case 0:
				COMMAND_AWB = "Auto";
				break;
			case 1:
				COMMAND_AWB = "Daylight";
				break;
			case 2:
				COMMAND_AWB = "Cloudy";
				break;
			case 3:
				COMMAND_AWB = "Fluorescent1";
				break;
			case 4:
				COMMAND_AWB = "Fluorescent2";
				break;
			case 5:
				COMMAND_AWB = "Fluorescent3";
				break;
			case 6:
				COMMAND_AWB = "Incandescent";
				break;
			default:
				COMMAND_AWB = "Auto";
				break;
			}
		} else {
			COMMAND_AWB = arg;
		}
		arguments[0] = buildArgument(PROPERTY_AWB, COMMAND_AWB);

		return buildRequestUrl(CGI_PATH, ACTION_SET,
				buildArgumentList(arguments));
	}

	public static URL commandWifiInfoUrl() {

		String[] arguments = new String[2];

		arguments[0] = buildArgument(PROPERTY_SSID);
		arguments[1] = buildArgument(PROPERTY_ENCRYPTION_KEY);

		return buildRequestUrl(CGI_PATH, ACTION_GET,
				buildArgumentList(arguments));
	}

	// yining
	public static URL commandTimeStampUrl() {

		String[] arguments = new String[1];

		arguments[0] = buildArgument(PROPERTY_TIMESTAMP);
		// arguments[0] = buildArgument(PROPERTY_TIMESTAMP_YEAR) ;
		// arguments[1] = buildArgument(PROPERTY_TIMESTAMP_MONTH) ;
		// arguments[2] = buildArgument(PROPERTY_TIMESTAMP_DAY) ;

		return buildRequestUrl(CGI_PATH, ACTION_GET,
				buildArgumentList(arguments));
	}

	/* Query AV1 property */
	public static URL commandQueryAV1Url() {

		String[] arguments = new String[1];

		arguments[0] = buildArgument(PROPERTY_RTSP_AV);
		return buildRequestUrl(CGI_PATH, ACTION_GET,
				buildArgumentList(arguments));
	}

	/* Query Camera Preview Property of MJPEG and RTSP */
	public static URL commandQueryCameraPreviewProperty() {

		String[] arguments = new String[1];

		arguments[0] = buildArgument(PROPERTY_CAMERAPREVIEW);
		return buildRequestUrl(CGI_PATH, ACTION_GET,
				buildArgumentList(arguments));

	}

	public static URL commandGetMenuSettingsValuesUrl() {

		String[] arguments = new String[1];

		arguments[0] = buildArgument(PROPERTY_DEFAULTVALUE);

		return buildRequestUrl(CGI_PATH, ACTION_GET,
				buildArgumentList(arguments));
	}

	public static URL commandRecordStatusUrl() {

		String[] arguments = new String[1];

		arguments[0] = buildArgument(PROPERTY_RECORDSTATUS);

		return buildRequestUrl(CGI_PATH, ACTION_GET,
				buildArgumentList(arguments));
	}

	public static String sendRequest(URL url) {

		try {

			Log.i("CameraControlFragment", url.toString());

			HttpURLConnection urlConnection = (HttpURLConnection) url
					.openConnection();

			urlConnection.setUseCaches(false);
			urlConnection.setDoInput(true);

			urlConnection.setConnectTimeout(3000);
			urlConnection.setReadTimeout(8000);

			urlConnection.connect();

			int responseCode = urlConnection.getResponseCode();

			Log.i("CameraControlFragment", "responseCode = " + responseCode);

			if (responseCode != HttpURLConnection.HTTP_OK) {

				return null;
			}

			InputStream inputStream = urlConnection.getInputStream();

			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(
						inputStream, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
					Log.i("brant",
							n + " : n --- buffer string : " + writer.toString());
				}
			} finally {
				inputStream.close();
			}

			String string = writer.toString();
			Log.i("CameraControlFragment", "writer string : >" + string
					+ "<---" + string.length());

			return string;

		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static class SendRequest extends AsyncTask<URL, Integer, String> {
		@Override
		protected String doInBackground(URL... params) {
			Log.i("brant", "Camera cmd --- params[0] : " + params[0].toString());
			return sendRequest(params[0]);
		}

		@Override
		protected void onPostExecute(String result) {

			Context context = VLCApplication.getAppContext();

			if (context != null) {
				String pat;
				pat = result;
				Log.i("brant", ">"+ pat + " --- onPostExecute cmd --- result : " + result);
				if (result != null) {
					pat = result.replaceAll("0\\nOK\\n", "");
					Log.i("brant", ">"+ pat + " --- pat --- result : " + result);
				}
				if (pat != null && pat.isEmpty()) {
					Toast.makeText(
							context,
							context.getResources().getString(
									R.string.message_command_succeed),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(
							context,
							context.getResources().getString(
									R.string.message_command_failed),
							Toast.LENGTH_SHORT).show();
				}
			}
			super.onPostExecute(result);
		}
	}

}
