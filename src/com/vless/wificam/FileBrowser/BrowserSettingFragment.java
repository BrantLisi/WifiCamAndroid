package com.vless.wificam.FileBrowser;

import com.vless.wificam.R;
import com.vless.wificam.MainActivity;
import com.vless.wificam.frags.WifiCamFragment;

import android.content.Context ;
import android.net.DhcpInfo ;
import android.net.wifi.WifiManager ;
import android.os.Bundle ;
import android.view.LayoutInflater ;
import android.view.View ;
import android.view.ViewGroup ;
import android.widget.Button ;
import android.widget.TextView ;

public class BrowserSettingFragment extends WifiCamFragment {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState) ;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View view = inflater.inflate(R.layout.browser_setting, container, false) ;
		
		final TextView ip = (TextView) view.findViewById(R.id.ip) ;
		final TextView path = (TextView) view.findViewById(R.id.url) ;
		final TextView directory = (TextView) view.findViewById(R.id.directory) ;
		
		final Button connectButton = (Button) view.findViewById(R.id.connectButton) ;

		WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE) ;

		DhcpInfo dhcpInfo = wifiManager.getDhcpInfo() ;
		
		if (dhcpInfo != null && dhcpInfo.gateway != 0) {
		
			ip.setText(MainActivity.intToIp(dhcpInfo.gateway)) ;
		}
		
		path.setText(FileBrowserFragment.DEFAULT_PATH) ;
		directory.setText(FileBrowserFragment.DEFAULT_DIR) ;
		
		connectButton.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {

				String ipString = ip.getText().toString() ;
				String urlString = path.getText().toString() ;
				String dirString = directory.getText().toString() ;
				
				WifiCamFragment fragment = FileBrowserFragment.newInstance(ipString, urlString, dirString) ;
				
//				MainActivity.addFragment(BrowserSettingFragment.this, fragment) ;
				
			}
		}) ;
		
		return view ;
	}

}
