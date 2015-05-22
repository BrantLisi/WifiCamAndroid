package com.vless.wificam.tasks;

import java.net.URL;

import com.vless.wificam.CameraCommand;
import com.vless.wificam.MainActivity;
import com.vless.wificam.R;
import com.vless.wificam.Viewer.MjpegPlayerFragment;
import com.vless.wificam.Viewer.StreamPlayerFragment;
import com.vless.wificam.frags.WifiCamFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

public class GetRTPS_AV1 extends AsyncTask<URL, Integer, String> {
	private Activity activity;

	public GetRTPS_AV1(Activity activity) {
		this.activity = activity;
	}

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
		WifiManager wifiManager = (WifiManager) activity
				.getSystemService(Context.WIFI_SERVICE);
		DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
		if (dhcpInfo == null || dhcpInfo.gateway == 0) {
			AlertDialog alertDialog = new AlertDialog.Builder(activity)
					.create();
			alertDialog.setTitle(activity.getResources().getString(
					R.string.dialog_DHCP_error));
			alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, activity
					.getResources().getString(R.string.label_ok),
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
						}
					});
			alertDialog.show();
			return;
		}
		String gateway = MainActivity.intToIp(dhcpInfo.gateway);
		// set http push as default for streaming
		liveStreamUrl = "http://" + gateway
				+ MjpegPlayerFragment.DEFAULT_MJPEG_PUSH_URL;
		if (result != null) {
			String[] lines;
			try {
				String[] lines_temp = result.split("Camera.Preview.RTSP.av=");
				String str = System.getProperty("line.separator");
				lines = lines_temp[1].split(System
						.getProperty("line.separator"));
				int av = Integer.valueOf(lines[0]);
				switch (av) {
				case 1: // liveRTSP/av1 for RTSP MJPEG+AAC
					liveStreamUrl = "rtsp://" + gateway
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
			} catch (Exception e) {/* not match, for firmware of MJPEG only */
			}
		}
		Log.i("GetRTPS_AV1", " liveStreamUrl: " + liveStreamUrl);
		
		super.onPostExecute(result);
	}
}
