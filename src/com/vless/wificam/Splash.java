package com.vless.wificam ;

import org.videolan.libvlc.LibVlcException ;
import org.videolan.vlc.Util ;

import com.vless.wificam.R;

import android.app.Activity ;
import android.content.Intent ;
import android.os.Bundle ;
import android.view.Window ;
import android.widget.ImageView ;

public class Splash extends Activity {

	private long ms = 0 ;
	private long splashTime = 3500 ;
	private boolean splashActive = true ;
	private boolean paused = false ;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState) ;

		// Hides the titlebar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE) ;

		setContentView(R.layout.splash) ;

		if (savedInstanceState == null) {
			ImageView title = (ImageView) findViewById(R.id.title) ;

			title.setAlpha(0f) ;
			title.animate().alpha(1f).setDuration(splashTime).setListener(null) ;

			Thread mythread = new Thread() {
				public void run() {
					try {
						while (splashActive && ms < splashTime) {
							if (!paused)
								ms = ms + 100 ;
							sleep(100) ;
						}
					} catch (Exception e) {
					} finally {
						Intent intent = new Intent(Splash.this, MainActivity.class) ;
						startActivity(intent) ;
					}
				}
			} ;
			mythread.start() ;
		}
		
		try {
			Util.getLibVlcInstance() ;
		} catch (LibVlcException e) {
			e.printStackTrace();
		}
	}
}
