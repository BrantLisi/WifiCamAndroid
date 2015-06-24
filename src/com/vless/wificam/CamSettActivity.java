package com.vless.wificam;

import com.vless.wificam.Control.CameraSettingsFragment;
import com.vless.wificam.frags.WifiCamFragment;
import com.vless.wificam.impls.OnEcarInfoListener;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Window;

public class CamSettActivity extends FragmentActivity {

	private WifiManager wifiManager;
	private WifiCamFragment camSettFrg;
	private OnEcarInfoListener onDataListener = new OnEcarInfoListener() {
		@Override
		public void setOnEcarInfoListener(int what) {
			Log.i("onDataListener", "what : " + what);
			onBackPressed();
			finish();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		wifiManager = (WifiManager) getApplicationContext().getSystemService(
				Context.WIFI_SERVICE);
		Bundle bundle = new Bundle();
		bundle.putBoolean("isWifiEnabled", wifiManager.isWifiEnabled());
		bundle.putInt("netWorkId", wifiManager.getConnectionInfo()
				.getNetworkId());

		setContentView(R.layout.net_settings);
		FragmentTransaction fragTransaction = getSupportFragmentManager()
				.beginTransaction();
		fragTransaction.add(R.id.llNetSettings,
				camSettFrg = new CameraSettingsFragment());
		camSettFrg.setArguments(bundle);
		camSettFrg.setOnEcarFragListener(onDataListener);
		fragTransaction.commit();
	}
}
