package com.vless.wificam.Control ;

import java.net.URL ;
import java.util.LinkedList ;
import java.util.List ;

import com.vless.wificam.R;
import com.vless.wificam.CameraCommand;

import android.app.Activity ;
import android.app.Fragment ;
import android.os.AsyncTask ;
import android.os.Bundle ;
import android.text.Editable ;
import android.text.TextWatcher ;
import android.util.Log;
import android.view.LayoutInflater ;
import android.view.View ;
import android.view.ViewGroup ;
import android.widget.Button ;
import android.widget.EditText ;
import android.widget.Toast ;

public class NetworkConfigurationsFragment extends Fragment {

	private class NetworkConfigurationSendRequest extends CameraCommand.SendRequest {

		@Override
		protected void onPreExecute() {
			setWaitingState(true) ;
			super.onPreExecute() ;
		}

		@Override
		protected void onPostExecute(String result) {
			setWaitingState(false) ;
			super.onPostExecute(result);
		}
	}

	private class GetWifiInfo extends AsyncTask<URL, Integer, String> {

		@Override
		protected void onPreExecute() {

			setWaitingState(true) ;
			super.onPreExecute() ;
		}

		@Override
		protected String doInBackground(URL... params) {

			URL url = CameraCommand.commandWifiInfoUrl() ;
			
			if (url != null) {
				
				return CameraCommand.sendRequest(url) ;
			}
			return null ;
		}

		@Override
		protected void onPostExecute(String result) {

			Activity activity = getActivity() ;

			if (result != null) {

				String[] lines = result.split(System.getProperty("line.separator")) ;

				for (int i = 0 ; i + 2 < lines.length ; i += 3) {

					if (lines[i + 1].contains("OK")) {

						String[] property = lines[i + 2].split("=", 2) ;

						if (property.length == 2) {

							if (property[0].equalsIgnoreCase(CameraCommand.PROPERTY_SSID)) {

								mSsid.setText(property[1]) ;
							} else if (property[0].equalsIgnoreCase(CameraCommand.PROPERTY_ENCRYPTION_KEY)) {

								mEncryptionKey.setText(property[1]) ;
							}
						}
					}
				}
			} else if (activity != null) {
				Toast.makeText(activity, activity.getResources().getString(R.string.message_fail_get_info),
						Toast.LENGTH_LONG).show() ;
			}
			setWaitingState(false) ;

			checkSsid(mSsid) ;
			checkEncryptionKey(mEncryptionKey) ;

			setInputEnabled(true) ;

			super.onPostExecute(result) ;
		}
	}

	private void checkSsid(EditText ssid) {

		String ssidString = ssid.getText().toString() ;

		if (ssidString.length() == 0) {
			ssid.setError(ssid.getResources().getString(R.string.error_no_ssid)) ;
		} else if (ssidString.length() > 32) {
			ssid.setError(ssid.getResources().getString(R.string.error_ssid_too_long)) ;
		} else {
			ssid.setError(null) ;
		}
	}

	private void checkEncryptionKey(EditText encryptionKey) {

		int keyLen = encryptionKey.getText().toString().length() ;
		if (keyLen == 0) {
			encryptionKey.setError(encryptionKey.getResources().getString(R.string.error_no_key)) ;
		} else if (keyLen < 8) {
			encryptionKey.setError(encryptionKey.getResources().getString(R.string.error_key_too_short)) ;
		} else if (keyLen > 63) {
			encryptionKey.setError(encryptionKey.getResources().getString(R.string.error_key_too_long)) ;
		} else {
			encryptionKey.setError(null) ;
		}
	}

	private EditText mSsid ;
	private EditText mEncryptionKey ;

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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.network_configurations, container, false) ;

		mSsid = (EditText) view.findViewById(R.id.cameraControlWifiName) ;
		mViewList.add(mSsid) ;

		mSsid.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				checkSsid(mSsid);
			}
			@Override
			public void afterTextChanged(Editable s) {
			}
		}) ;

		mEncryptionKey = (EditText) view.findViewById(R.id.cameraControlWifiEncryptionKey) ;
		mViewList.add(mEncryptionKey) ;

		mEncryptionKey.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				checkEncryptionKey(mEncryptionKey) ;
			}
			@Override
			public void afterTextChanged(Editable s) {
			}
		}) ;

		Button updateButton = (Button) view.findViewById(R.id.cameraControlUpdateButton) ;
		mViewList.add(updateButton) ;

		updateButton.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				
				URL url = CameraCommand.commandUpdateUrl(mSsid.getText().toString(), mEncryptionKey.getText()
						.toString()) ;
				
				if (url != null) {
					Log.i("brant", "update config --- url : "+url.toString());
					new NetworkConfigurationSendRequest().execute(url);
				}
			}
		}) ;

		Button resetButton = (Button) view.findViewById(R.id.cameraControlResetButton) ;
		mViewList.add(resetButton) ;

		resetButton.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {

				URL url = CameraCommand.commandReactivateUrl();
				
				if (url != null) {
					Log.i("brant", "reconnect --- url : "+url.toString());
					new NetworkConfigurationSendRequest().execute(url);
				}
			}
		}) ;

		new GetWifiInfo().execute() ;

		return view ;
	}

	@Override
	public void onResume() {
		restoreWaitingIndicator() ;
		super.onResume() ;
	}

	@Override
	public void onPause() {
		clearWaitingIndicator() ;
		super.onPause() ;
	}

}
