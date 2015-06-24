package com.vless.wificam;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.VideoView;


/**
 * welcome interface with 2000 animation
 * if user set auto to login in,2000
 * 
 * @author Brant.Fei
 *
 */
public class WelcomeActivity extends Activity{
	
	//animation time
	private final static int DELAY_TIME = 3000;
	private VideoView vv_video;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.welcome);
		
		vv_video = (VideoView) findViewById(R.id.vv_welcome);
		String uri = "android.resource://" + getPackageName() + "/"
				+ R.raw.ecar_welcome;
		if (uri == null || vv_video == null) {
			Log.i("test", ""+uri+"======"+vv_video);
			finish();
		}
		vv_video.setVideoURI(Uri.parse(uri));
		vv_video.start();
		
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(DELAY_TIME);
					init();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			};
		}.start();
	}

	/**
	 * judge auto login in
	 */
	private void init() {
		Intent intent = new Intent(WelcomeActivity.this, MainActivity.class) ;
		startActivity(intent) ;
		finish();
	}

	
	@Override
	protected void onDestroy() {
		
		super.onDestroy();
	}
}
