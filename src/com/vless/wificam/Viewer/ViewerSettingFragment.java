package com.vless.wificam.Viewer;

import com.vless.wificam.R;
import com.vless.wificam.MainActivity;
import com.vless.wificam.frags.WifiCamFragment;

import android.app.Fragment ;
import android.content.Context ;
import android.net.DhcpInfo ;
import android.net.wifi.WifiManager ;
import android.os.Bundle ;
import android.view.LayoutInflater ;
import android.view.View ;
import android.view.ViewGroup ;
import android.widget.Button ;
import android.widget.RadioButton ;
import android.widget.RadioGroup ;
import android.widget.TextView ;
import android.widget.Toast;

public class ViewerSettingFragment extends WifiCamFragment {
	
	private String mMediaUrl = "" ;
	private boolean mMjpegPush = true ;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState) ;
	}
	
	private static final String DEFAULT_H264_URL = "/liveRTSP/v1" ;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View view = inflater.inflate(R.layout.viewer_setting, container, false) ;
		
		final RadioGroup streamType = (RadioGroup) view.findViewById(R.id.streamTypeGroup) ;
		final RadioButton mjpeg = (RadioButton) view.findViewById(R.id.radioMjpeg) ;
		final RadioButton h264 = (RadioButton) view.findViewById(R.id.radioH264) ;

		final RadioGroup mjpegMode = (RadioGroup) view.findViewById(R.id.mjpegMode) ;
		final RadioButton mjpegPush = (RadioButton) view.findViewById(R.id.mjpegPush) ;
		final RadioButton mjpegPull = (RadioButton) view.findViewById(R.id.mjpegPull) ;

		final TextView ip = (TextView) view.findViewById(R.id.ip) ;
		final TextView url = (TextView) view.findViewById(R.id.url) ;
		
		final Button connectButton = (Button) view.findViewById(R.id.connectButton) ;

		WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE) ;

		DhcpInfo dhcpInfo = wifiManager.getDhcpInfo() ;
		
		if (dhcpInfo != null && dhcpInfo.gateway != 0) {
		
			ip.setText(MainActivity.intToIp(dhcpInfo.gateway)) ;
		}
		
		streamType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				
				if (checkedId == mjpeg.getId()) {
					
					if (mMjpegPush) {
						url.setText(MjpegPlayerFragment.DEFAULT_MJPEG_PUSH_URL) ;
					} else {
						url.setText(MjpegPlayerFragment.DEFAULT_MJPEG_PULL_URL) ;
					}
					mjpegMode.setEnabled(true) ;
					mjpegPush.setEnabled(true) ;
					mjpegPull.setEnabled(true) ;

				} else if (checkedId == h264.getId()) {
					
					url.setText(DEFAULT_H264_URL) ;
					mjpegMode.setEnabled(false) ;
					mjpegPush.setEnabled(false) ;
					mjpegPull.setEnabled(false) ;
					
				}
			}
		}) ;
		
		mjpegMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				
				if (checkedId == mjpegPush.getId()) {
					
					url.setText(MjpegPlayerFragment.DEFAULT_MJPEG_PUSH_URL) ;
					mMjpegPush = true ;
				} else if (checkedId == mjpegPull.getId()) {
					
					url.setText(MjpegPlayerFragment.DEFAULT_MJPEG_PULL_URL) ;
					mMjpegPush = false ;
				}
			}
		}) ;

		
		url.setText(MjpegPlayerFragment.DEFAULT_MJPEG_PUSH_URL) ;
		
		connectButton.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {

				String gateway = ip.getText().toString() ;
				mMediaUrl = "" ;
				
				if (mjpeg.isChecked()) {
					
					mMediaUrl = "http://" + gateway + url.getText().toString() ;
					Fragment fragment = MjpegPlayerFragment.newInstance(mMediaUrl, mMjpegPush) ;
					Toast.makeText(getActivity(), " MjpegPlayer ÔÚÐÞ¸ÄÖÐ...", Toast.LENGTH_SHORT).show();
//					MainActivity.addFragment(ViewerSettingFragment.this, fragment) ;
					
				} else if (h264.isChecked()) {
				
					mMediaUrl = "rtsp://" + gateway + url.getText().toString() ;
//					WifiCamFragment fragment = StreamPlayerFragment.newInstance(mMediaUrl) ;
					Toast.makeText(getActivity(), " StreamPlayer ok ", Toast.LENGTH_SHORT).show();
//					MainActivity.addFragment(ViewerSettingFragment.this, fragment) ;
				}
			}
		}) ;
		
		return view ;
	}
}
