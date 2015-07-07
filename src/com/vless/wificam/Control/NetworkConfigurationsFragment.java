package com.vless.wificam.Control;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import com.vless.wificam.R;
import com.vless.wificam.CameraCommand;
import com.vless.wificam.frags.WifiCamFragment;
import com.vless.wificam.impls.OnEcarInfoListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class NetworkConfigurationsFragment extends WifiCamFragment implements
		OnClickListener {
	private LinearLayout llSettSSID, llSettKeyWord;
	private ImageView ivLeft;
	private OnEcarInfoListener onEcarInfoListener;
	private View modSSIDView, modKeyWordView;
	private TextView tv_ssid_old;
	private EditText et_ssid_new, et_passwd_old, et_passwd_new;
	private Button btn_ok_ssid, btn_cnl_ssid, btn_pw_ok, btn_pw_cnl;
	private Dialog ssid_diag, passwd_diag;
	private URL url;
	private boolean isWifiEnabled;
	private int netWorkId;

	@Override
	public void setOnEcarFragListener(OnEcarInfoListener l) {
		onEcarInfoListener = l;
	}

	private class NetworkConfigurationSendRequest extends
			CameraCommand.SendRequest {

		@Override
		protected void onPreExecute() {
			setWaitingState(true);
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(String result) {
			setWaitingState(false);
			super.onPostExecute(result);
		}
	}

	private class GetWifiInfo extends AsyncTask<URL, Integer, String> {

		@Override
		protected void onPreExecute() {

			setWaitingState(true);
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(URL... params) {

			URL url = CameraCommand.commandWifiInfoUrl();

			if (url != null) {

				return CameraCommand.sendRequest(url);
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {

			Activity activity = getActivity();
			if (result != null) {
				Log.i("GetWifiInfo", result);

				String[] lines = result.split(System
						.getProperty("line.separator"));

				for (int i = 0; i + 2 < lines.length; i += 3) {

					if (lines[i + 1].contains("OK")) {

						String[] property = lines[i + 2].split("=", 2);

						if (property.length == 2) {

							if (property[0]
									.equalsIgnoreCase(CameraCommand.PROPERTY_SSID)) {

								mSsid.setText(property[1]);
							} else if (property[0]
									.equalsIgnoreCase(CameraCommand.PROPERTY_ENCRYPTION_KEY)) {

								mEncryptionKey.setText(property[1]);
							}
						}
					}
				}
			} else if (activity != null) {
				Toast.makeText(
						activity,
						activity.getResources().getString(
								R.string.message_fail_get_info),
						Toast.LENGTH_LONG).show();
			}
			setWaitingState(false);

			// checkSsid(mSsid);
			// checkEncryptionKey(mEncryptionKey);

			setInputEnabled(true);

			super.onPostExecute(result);
		}
	}

	private void checkSsid(EditText ssid) {

		String ssidString = ssid.getText().toString();

		if (ssidString.length() == 0) {
			ssid.setError(ssid.getResources().getString(R.string.error_no_ssid));
		} else if (ssidString.length() > 32) {
			ssid.setError(ssid.getResources().getString(
					R.string.error_ssid_too_long));
		} else {
			ssid.setError(null);
		}
	}

	private void checkEncryptionKey(EditText encryptionKey) {

		int keyLen = encryptionKey.getText().toString().length();
		if (keyLen == 0) {
			encryptionKey.setError(encryptionKey.getResources().getString(
					R.string.error_no_key));
		} else if (keyLen < 8) {
			encryptionKey.setError(encryptionKey.getResources().getString(
					R.string.error_key_too_short));
		} else if (keyLen > 63) {
			encryptionKey.setError(encryptionKey.getResources().getString(
					R.string.error_key_too_long));
		} else {
			encryptionKey.setError(null);
		}
	}

	private TextView mSsid;
	private TextView mEncryptionKey;

	private List<View> mViewList = new LinkedList<View>();

	private void setInputEnabled(boolean enabled) {

		for (View view : mViewList) {
			view.setEnabled(enabled);
		}
	}

	private boolean mWaitingState = false;
	private boolean mWaitingVisible = false;

	private void setWaitingState(boolean waiting) {

		if (mWaitingState != waiting) {
			mWaitingState = waiting;
			setWaitingIndicator(mWaitingState, mWaitingVisible);
		}
	}

	private void setWaitingIndicator(boolean waiting, boolean visible) {

		if (!visible)
			return;

		setInputEnabled(!waiting);

		Activity activity = getActivity();

		if (activity != null) {
			activity.setProgressBarIndeterminate(true);
			activity.setProgressBarIndeterminateVisibility(waiting);
		}
	}

	private void clearWaitingIndicator() {
		mWaitingVisible = false;
		setWaitingIndicator(false, true);
	}

	private void restoreWaitingIndicator() {
		mWaitingVisible = true;
		setWaitingIndicator(mWaitingState, true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		isWifiEnabled = getArguments().getBoolean("isWifiEnabled", false);
		netWorkId = getArguments().getInt("netWorkId", -1);

		View view = inflater.inflate(R.layout.network_configurations,
				container, false);
		TextView tvHeaderTitle = (TextView) view.findViewById(R.id.frag_header)
				.findViewById(R.id.header_title);
		tvHeaderTitle.setText(R.string.end_title_setting_wifi);
		ivLeft = (ImageView) view.findViewById(R.id.frag_header).findViewById(
				R.id.header_left);
		ivLeft.setImageResource(R.drawable.left_back);
		ivLeft.setVisibility(View.VISIBLE);
		ivLeft.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ivLeft.setVisibility(View.INVISIBLE);
				onEcarInfoListener.setOnEcarInfoListener(0x03);
			}
		});
		llSettSSID = (LinearLayout) view.findViewById(R.id.llSettSSID);
		llSettKeyWord = (LinearLayout) view.findViewById(R.id.llSettKeyWord);
		llSettSSID.setOnClickListener(this);
		llSettKeyWord.setOnClickListener(this);

		mSsid = (TextView) view.findViewById(R.id.cameraControlWifiName);
		mViewList.add(mSsid);

		mEncryptionKey = (TextView) view
				.findViewById(R.id.cameraControlWifiEncryptionKey);
		mViewList.add(mEncryptionKey);

		// Button resetButton = (Button) view
		// .findViewById(R.id.cameraControlResetButton);
		// mViewList.add(resetButton);
		// resetButton.setOnClickListener(this);

		// modify SSID
		modSSIDView = getActivity().getLayoutInflater().inflate(
				R.layout.set_mod_ssid, null, false);
		tv_ssid_old = (TextView) modSSIDView.findViewById(R.id.tv_mod_ssid_old);
		et_ssid_new = (EditText) modSSIDView.findViewById(R.id.et_mod_ssid_new);
		btn_ok_ssid = (Button) modSSIDView.findViewById(R.id.btn_ssid_ok);
		btn_cnl_ssid = (Button) modSSIDView.findViewById(R.id.btn_ssid_cnl);
		btn_ok_ssid.setOnClickListener(this);
		btn_cnl_ssid.setOnClickListener(this);
		ssid_diag = new AlertDialog.Builder(getActivity()).setView(modSSIDView)
				.create();

		// modify wifi passwd
		modKeyWordView = getActivity().getLayoutInflater().inflate(
				R.layout.set_mod_passwd, null, false);
		et_passwd_old = (EditText) modKeyWordView
				.findViewById(R.id.et_passwd_old_login);
		et_passwd_new = (EditText) modKeyWordView
				.findViewById(R.id.et_passwd_new_login);
		btn_pw_ok = (Button) modKeyWordView
				.findViewById(R.id.btn_passwd_ok_login);
		btn_pw_cnl = (Button) modKeyWordView
				.findViewById(R.id.btn_passwd_cancel_login);
		btn_pw_ok.setOnClickListener(this);
		btn_pw_cnl.setOnClickListener(this);
		passwd_diag = new AlertDialog.Builder(getActivity()).setView(
				modKeyWordView).create();

		if (isWifiEnabled && netWorkId != -1) {
			new GetWifiInfo().execute();
		} else {
			mSsid.setText("");
			mEncryptionKey.setText("");
			new AlertDialog.Builder(getActivity()).setTitle(R.string.dialog_no_connection_title)
					.setMessage(R.string.dialog_no_connection_message).setPositiveButton("确定", null).show();
		}
		return view;
	}

	@Override
	public void onResume() {
		restoreWaitingIndicator();
		super.onResume();
	}

	@Override
	public void onPause() {
		clearWaitingIndicator();
		super.onPause();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.llSettSSID:
			tv_ssid_old.setText(mSsid.getText());
			et_ssid_new.setText(mSsid.getText());
			ssid_diag.show();
			break;
		case R.id.llSettKeyWord:
			passwd_diag.show();
			break;
		// case R.id.cameraControlResetButton:
		// URL url = CameraCommand.commandReactivateUrl();
		// if (url != null) {
		// Log.i("brant", "reconnect --- url : " + url.toString());
		// new NetworkConfigurationSendRequest().execute(url);
		// }
		// break;
		case R.id.btn_ssid_ok:
			modSSID();
			ssid_diag.dismiss();
			break;
		case R.id.btn_ssid_cnl:
			ssid_diag.dismiss();
			break;
		case R.id.btn_passwd_ok_login:
			modKeyWord();
			passwd_diag.dismiss();
			break;
		case R.id.btn_passwd_cancel_login:
			et_passwd_old.setText("");
			et_passwd_new.setText("");
			passwd_diag.dismiss();
			break;
		default:
			break;
		}
	}

	private void modKeyWord() {
		String passwd_old = et_passwd_old.getText() + "";
		String passwd_new = et_passwd_new.getText() + "";

		if (!mEncryptionKey.getText().equals(passwd_old)) {
			Toast.makeText(getActivity(), "旧密码不对，请重新修改，谢谢。", Toast.LENGTH_SHORT)
					.show();
		} else {
			try {
				url = new URL(
						"http://192.72.1.1/cgi-bin/Config.cgi?action=set&property=Net.WIFI_AP.CryptoKey&value="
								+ passwd_new);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			if (url != null) {
				Log.i("brant", "reconnect --- url : " + url.toString());
				new NetworkConfigurationSendRequest().execute(url);
			}
		}
		et_passwd_old.setText("");
		et_passwd_new.setText("");
	}

	private void modSSID() {
		String ssid_new = et_ssid_new.getText() + "";
		try {
			url = new URL(
					"http://192.72.1.1/cgi-bin/Config.cgi?action=set&property=Net.WIFI_AP.SSID&value="
							+ ssid_new);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		if (url != null) {
			Log.i("brant", "reconnect --- url : " + url.toString());
			new NetworkConfigurationSendRequest().execute(url);
		}
	}

}
