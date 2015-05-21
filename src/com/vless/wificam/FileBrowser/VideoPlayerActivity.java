/*****************************************************************************
 * VideoPlayerActivity.java
 *****************************************************************************
 * Copyright © 2011-2013 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package com.vless.wificam.FileBrowser ;

import java.io.File ;
import java.io.FileOutputStream ;
import java.io.InputStream ;
import java.io.OutputStream ;
import java.io.UnsupportedEncodingException ;
import java.lang.reflect.Method ;
import java.net.URLDecoder ;
import java.util.Map ;

import org.videolan.libvlc.EventHandler ;
import org.videolan.libvlc.IVideoPlayer ;
import org.videolan.libvlc.LibVLC ;
import org.videolan.libvlc.LibVlcException ;
import org.videolan.vlc.Util ;
import org.videolan.vlc.VLCApplication ;
import org.videolan.vlc.WeakHandler ;
import org.videolan.vlc.interfaces.IPlayerControl ;
import org.videolan.vlc.interfaces.OnPlayerControlListener ;
import org.videolan.vlc.widget.PlayerControlClassic ;

import com.vless.wificam.R;

import android.annotation.TargetApi ;
import android.app.Activity ;
import android.app.AlertDialog ;
import android.app.DownloadManager ;
import android.app.KeyguardManager ;
import android.content.BroadcastReceiver ;
import android.content.Context ;
import android.content.DialogInterface ;
import android.content.Intent ;
import android.content.IntentFilter ;
import android.content.pm.ActivityInfo ;
import android.content.res.Configuration ;
import android.database.Cursor ;
import android.graphics.ImageFormat ;
import android.graphics.PixelFormat ;
import android.media.AudioManager ;
import android.net.Uri ;
import android.os.Build ;
import android.os.Bundle ;
import android.os.Environment ;
import android.os.Handler ;
import android.os.Message ;
import android.provider.MediaStore ;
import android.util.DisplayMetrics ;
import android.util.Log ;
import android.view.Display ;
import android.view.MotionEvent ;
import android.view.Surface ;
import android.view.SurfaceHolder ;
import android.view.SurfaceHolder.Callback ;
import android.view.SurfaceView ;
import android.view.View ;
import android.view.View.OnClickListener ;
import android.view.View.OnSystemUiVisibilityChangeListener ;
import android.view.ViewGroup.LayoutParams ;
import android.view.Window ;
import android.view.WindowManager ;
import android.view.animation.AnimationUtils ;
import android.widget.FrameLayout ;
import android.widget.ImageButton ;
import android.widget.SeekBar ;
import android.widget.SeekBar.OnSeekBarChangeListener ;
import android.widget.TextView ;

public class VideoPlayerActivity extends Activity implements IVideoPlayer {
	public final static String TAG = "VLC/VideoPlayerActivity" ;

	// Internal intent identifier to distinguish between internal launch and
	// external intent.
	private final static String PLAY_FROM_VIDEOGRID = "org.videolan.vlc.gui.video.PLAY_FROM_VIDEOGRID" ;

	private SurfaceView mSurface ;
	private SurfaceHolder mSurfaceHolder ;
	private FrameLayout mSurfaceFrame ;
	private LibVLC mLibVLC ;
	private String mLocation ;

	private static final int SURFACE_BEST_FIT = 0 ;
	private static final int SURFACE_FIT_HORIZONTAL = 1 ;
	private static final int SURFACE_FIT_VERTICAL = 2 ;
	private static final int SURFACE_FILL = 3 ;
	private static final int SURFACE_16_9 = 4 ;
	private static final int SURFACE_4_3 = 5 ;
	private static final int SURFACE_ORIGINAL = 6 ;
	private int mCurrentSize = SURFACE_BEST_FIT ;

	/** Overlay */
	private View mOverlayHeader ;
	private View mOverlayOption ;
	private View mOverlayProgress ;
	private View mOverlayInterface ;
	private static final int OVERLAY_TIMEOUT = 4000 ;
	private static final int OVERLAY_INFINITE = 3600000 ;
	private static final int FADE_OUT = 1 ;
	private static final int SHOW_PROGRESS = 2 ;
	private static final int SURFACE_SIZE = 3 ;
	private static final int FADE_OUT_INFO = 4 ;
	private boolean mDragging ;
	private boolean mShowing ;
	private int mUiVisibility = -1 ;
	private SeekBar mSeekbar ;
	private TextView mCurrentTime ;//yining
	private TextView mTitle ;
	private TextView mTime ;
	private TextView mLength ;
	private TextView mInfo ;
	private IPlayerControl mControls ;
	private boolean mDisplayRemainingTime = false ;
	private ImageButton mAudioTrack ;
	private ImageButton mSize ;
	private int mLastAudioTrack = -1 ;
	private int mLastSpuTrack = -2 ;

	private long mVideoResumeTime = -1 ;

	/**
	 * For uninterrupted switching between audio and video mode
	 */
	private boolean mEndReached ;

	// Playlist
	private int savedIndexPosition = -1 ;

	// size of the video
	private int mVideoHeight ;
	private int mVideoWidth ;
	private int mVideoVisibleHeight ;
	private int mVideoVisibleWidth ;
	private int mSarNum ;
	private int mSarDen ;

	// Volume
	private AudioManager mAudioManager ;
	// Volume Or Brightness
	private boolean mIsAudioOrBrightnessChanged ;
	private int mSurfaceYDisplayRange ;
	private float mTouchY, mTouchX ;

	// Tracks & Subtitles
	private Map<Integer, String> mAudioTracksList ;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState) ;
		this.requestWindowFeature(Window.FEATURE_NO_TITLE) ;//for icon display issue@yining.
		setContentView(R.layout.player) ;
		setTitle(getResources().getString(R.string.app_name)) ;

		getWindow().getDecorView().findViewById(android.R.id.content)
				.setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
					@Override
					public void onSystemUiVisibilityChange(int visibility) {
						if (visibility == mUiVisibility)
							return ;
						setSurfaceSize(mVideoWidth, mVideoHeight, mVideoVisibleWidth, mVideoVisibleHeight,
								mSarNum, mSarDen) ;
						if (visibility == View.SYSTEM_UI_FLAG_VISIBLE && !mShowing) {
							showOverlay() ;
						}
						mUiVisibility = visibility ;
					}
				}) ;

		/** initialize Views an their Events */
		mOverlayHeader = findViewById(R.id.player_overlay_header) ;
		mOverlayOption = findViewById(R.id.option_overlay) ;
		mOverlayProgress = findViewById(R.id.progress_overlay) ;
		mOverlayInterface = findViewById(R.id.interface_overlay) ;

		/* header */
		mTitle = (TextView) findViewById(R.id.player_overlay_title) ;

		// Position and remaining time
		mTime = (TextView) findViewById(R.id.player_overlay_time) ;
		mTime.setOnClickListener(mRemainingTimeListener) ;
		mLength = (TextView) findViewById(R.id.player_overlay_length) ;
		mLength.setOnClickListener(mRemainingTimeListener) ;

		// the info textView is not on the overlay
		mInfo = (TextView) findViewById(R.id.player_overlay_info) ;

		mControls = new PlayerControlClassic(this) ;
		mControls.setOnPlayerControlListener(mPlayerControlListener) ;
		FrameLayout mControlContainer = (FrameLayout) findViewById(R.id.player_control) ;
		mControlContainer.addView((View) mControls) ;

		mAudioTrack = (ImageButton) findViewById(R.id.player_overlay_audio) ;
		mAudioTrack.setVisibility(View.GONE) ;

		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				/*
				 * FIXME The setTracksAndSubtitles method probably doesn't work
				 * in case of many many Tracks and Subtitles Moreover, in a
				 * video stream, if Tracks & Subtitles change, they won't be
				 * updated
				 */
				setESTrackLists() ;
			}
		}, 1500) ;

		mSize = (ImageButton) findViewById(R.id.player_overlay_size) ;
		mSize.setOnClickListener(mSizeListener) ;

		mSurface = (SurfaceView) findViewById(R.id.player_surface) ;
		mSurfaceHolder = mSurface.getHolder() ;
		mSurfaceFrame = (FrameLayout) findViewById(R.id.player_surface_frame) ;
		String chroma = "" ;
		if (Util.isGingerbreadOrLater() && chroma.equals("YV12")) {
			mSurfaceHolder.setFormat(ImageFormat.YV12) ;
		} else if (chroma.equals("RV16")) {
			mSurfaceHolder.setFormat(PixelFormat.RGB_565) ;
		} else {
			mSurfaceHolder.setFormat(PixelFormat.RGBX_8888) ;
		}
		mSurfaceHolder.addCallback(mSurfaceCallback) ;

		mSeekbar = (SeekBar) findViewById(R.id.player_overlay_seekbar) ;
		mSeekbar.setOnSeekBarChangeListener(mSeekListener) ;

		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE) ;
		mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ;

		mEndReached = false ;

		// Clear the resume time, since it is only used for resumes in external
		// videos.

		mVideoResumeTime = -1 ;

		IntentFilter filter = new IntentFilter() ;
		filter.addAction(Intent.ACTION_BATTERY_CHANGED) ;
		filter.addAction(VLCApplication.SLEEP_INTENT) ;
		registerReceiver(mReceiver, filter) ;

		try {
			mLibVLC = Util.getLibVlcInstance() ;
		} catch (LibVlcException e) {
			Log.d(TAG, "LibVLC initialisation failed") ;
			return ;
		}

		EventHandler em = EventHandler.getInstance() ;
		em.addHandler(eventHandler) ;

		this.setVolumeControlStream(AudioManager.STREAM_MUSIC) ;

		setRequestedOrientation(getScreenOrientation()) ;
	}

	@Override
	protected void onStart() {
		super.onStart() ;
	}

	@Override
	protected void onPause() {
		super.onPause() ;

		long time = mLibVLC.getTime() ;
		long length = mLibVLC.getLength() ;
		// remove saved position if in the last 5 seconds
		if (length - time < 5000)
			time = 0 ;
		else
			time -= 5000 ; // go back 5 seconds, to compensate loading time

		/*
		 * Pausing here generates errors because the vout is constantly trying
		 * to refresh itself every 80ms while the surface is not accessible
		 * anymore. To workaround that, we keep the last known position in the
		 * playlist in savedIndexPosition to be able to restore it during
		 * onResume().
		 */
		mLibVLC.stop() ;

		mSurface.setKeepScreenOn(false) ;

		// Save position
		if (time >= 0) {

			mVideoResumeTime = time ;
		}
	}

	@Override
	protected void onStop() {
		super.onStop() ;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy() ;
		unregisterReceiver(mReceiver) ;
		if (mLibVLC != null) {
			mLibVLC.stop() ;
		}

		EventHandler em = EventHandler.getInstance() ;
		em.removeHandler(eventHandler) ;

		mAudioManager = null ;
	}

	@Override
	protected void onResume() {
		super.onResume() ;

		load() ;

		/*
		 * if the activity has been paused by pressing the power button,
		 * pressing it again will show the lock screen. But onResume will also
		 * be called, even if vlc-android is still in the background. To
		 * workaround that, pause playback if the lockscreen is displayed
		 */
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mLibVLC != null && mLibVLC.isPlaying()) {
					KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE) ;
					if (km.inKeyguardRestrictedInputMode())
						mLibVLC.pause() ;
				}
			}
		}, 500) ;

		showOverlay() ;
	}

	public static void start(Context context, String location) {
		start(context, location, null, -1, false, false) ;
	}

	public static void start(Context context, String location, Boolean fromStart) {
		start(context, location, null, -1, false, fromStart) ;
	}

	public static void start(Context context, String location, String title, Boolean dontParse) {
		start(context, location, title, -1, dontParse, false) ;
	}

	public static void start(Context context, String location, String title, int position, Boolean dontParse) {
		start(context, location, title, position, dontParse, false) ;
	}

	public static void start(Context context, String location, String title, int position, Boolean dontParse,
			Boolean fromStart) {
		Intent intent = new Intent(context, VideoPlayerActivity.class) ;
		intent.setAction(VideoPlayerActivity.PLAY_FROM_VIDEOGRID) ;
		intent.putExtra("itemLocation", location) ;
		intent.putExtra("itemTitle", title) ;
		intent.putExtra("dontParse", dontParse) ;
		intent.putExtra("fromStart", fromStart) ;
		intent.putExtra("itemPosition", position) ;

		if (dontParse)
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK) ;

		context.startActivity(intent) ;
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction() ;
			if (action.equalsIgnoreCase(VLCApplication.SLEEP_INTENT)) {
				finish() ;
			}
		}
	} ;

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		showOverlay() ;
		return true ;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		setSurfaceSize(mVideoWidth, mVideoHeight, mVideoVisibleWidth, mVideoVisibleHeight, mSarNum, mSarDen) ;
		super.onConfigurationChanged(newConfig) ;
	}

	@Override
	public void setSurfaceSize(int width, int height, int visible_width, int visible_height, int sar_num,
			int sar_den) {
		if (width * height == 0)
			return ;

		// store video size
		mVideoHeight = height ;
		mVideoWidth = width ;
		mVideoVisibleHeight = visible_height ;
		mVideoVisibleWidth = visible_width ;
		mSarNum = sar_num ;
		mSarDen = sar_den ;
		Message msg = mHandler.obtainMessage(SURFACE_SIZE) ;
		mHandler.sendMessage(msg) ;
	}

	/**
	 * Show text in the info view for "duration" milliseconds
	 * 
	 * @param text
	 * @param duration
	 */
	private void showInfo(String text, int duration) {
		mInfo.setVisibility(View.VISIBLE) ;
		mInfo.setText(text) ;
		mHandler.removeMessages(FADE_OUT_INFO) ;
		mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, duration) ;
	}

	private void showInfo(int textid, int duration) {
		mInfo.setVisibility(View.VISIBLE) ;
		mInfo.setText(textid) ;
		mHandler.removeMessages(FADE_OUT_INFO) ;
		mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, duration) ;
	}

	/**
	 * Show text in the info view
	 * 
	 * @param text
	 */
	private void showInfo(String text) {
		mInfo.setVisibility(View.VISIBLE) ;
		mInfo.setText(text) ;
		mHandler.removeMessages(FADE_OUT_INFO) ;
	}

	/**
	 * hide the info view with "delay" milliseconds delay
	 * 
	 * @param delay
	 */
	private void hideInfo(int delay) {
		mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, delay) ;
	}

	/**
	 * hide the info view
	 */
	private void hideInfo() {
		hideInfo(0) ;
	}

	private void fadeOutInfo() {
		if (mInfo.getVisibility() == View.VISIBLE)
			mInfo.startAnimation(AnimationUtils.loadAnimation(VideoPlayerActivity.this,
					android.R.anim.fade_out)) ;
		mInfo.setVisibility(View.INVISIBLE) ;
	}

	/**
	 * Handle libvlc asynchronous events
	 */
	private final Handler eventHandler = new VideoPlayerEventHandler(this) ;

	private static class VideoPlayerEventHandler extends WeakHandler<VideoPlayerActivity> {
		public VideoPlayerEventHandler(VideoPlayerActivity owner) {
			super(owner) ;
		}

		@Override
		public void handleMessage(Message msg) {
			VideoPlayerActivity activity = getOwner() ;
			if (activity == null)
				return ;

			switch (msg.getData().getInt("event")) {
			case EventHandler.MediaPlayerPlaying:
				Log.i(TAG, "MediaPlayerPlaying") ;
				activity.showOverlay() ;
				activity.setESTracks() ;
				break ;
			case EventHandler.MediaPlayerPaused:
				Log.i(TAG, "MediaPlayerPaused") ;
				break ;
			case EventHandler.MediaPlayerStopped:
				Log.i(TAG, "MediaPlayerStopped") ;
				break ;
			case EventHandler.MediaPlayerEndReached:
				Log.i(TAG, "MediaPlayerEndReached") ;
				activity.endReached() ;
				break ;
			case EventHandler.MediaPlayerVout:
				activity.handleVout(msg) ;
				break ;
			case EventHandler.MediaPlayerPositionChanged:
				// don't spam the logs
				break ;
			case EventHandler.MediaPlayerEncounteredError:
				Log.i(TAG, "MediaPlayerEncounteredError") ;
				activity.encounteredError() ;
				break ;
			default:
				Log.e(TAG, String.format("Event not handled (0x%x)", msg.getData().getInt("event"))) ;
				break ;
			}
			activity.updateOverlayPausePlay() ;
		}
	} ;

	/**
	 * Handle resize of the surface and the overlay
	 */
	private final Handler mHandler = new VideoPlayerHandler(this) ;

	private static class VideoPlayerHandler extends WeakHandler<VideoPlayerActivity> {
		public VideoPlayerHandler(VideoPlayerActivity owner) {
			super(owner) ;
		}

		@Override
		public void handleMessage(Message msg) {
			VideoPlayerActivity activity = getOwner() ;
			if (activity == null) // WeakReference could be GC'ed early
				return ;

			switch (msg.what) {
			case FADE_OUT:
				activity.hideOverlay(false) ;
				break ;
			case SHOW_PROGRESS:
				int pos = activity.setOverlayProgress() ;
				if (activity.canShowProgress()) {
					msg = obtainMessage(SHOW_PROGRESS) ;
					sendMessageDelayed(msg, 1000 - (pos % 1000)) ;
				}
				break ;
			case SURFACE_SIZE:
				activity.changeSurfaceSize() ;
				break ;
			case FADE_OUT_INFO:
				activity.fadeOutInfo() ;
				break ;
			}
		}
	} ;

	private boolean canShowProgress() {
		return !mDragging && mShowing && mLibVLC.isPlaying() ;
	}

	private void endReached() {
		if (mLibVLC.getMediaList().expandMedia(savedIndexPosition) == 0) {
			Log.d(TAG, "Found a video playlist, expanding it") ;
			eventHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					load() ;
				}
			}, 1000) ;
		} else {
			/* Exit player when reaching the end */
			// finish() ;
			
			mEndReached = true ;
		}
	}

	private void encounteredError() {
		/* Encountered Error, exit player with a message */
		AlertDialog dialog = new AlertDialog.Builder(VideoPlayerActivity.this)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						finish() ;
					}
				}).setTitle("Playback Error").setMessage("Encounter an error with the video file").create() ;
		dialog.show() ;
	}

	private void handleVout(Message msg) {
		if (msg.getData().getInt("data") == 0 && !mEndReached) {
			/* Video track lost */
			Log.i(TAG, "Video track lost") ;
		}
	}

	private void changeSurfaceSize() {
		// get screen size
		int dw = getWindow().getDecorView().getWidth() ;
		int dh = getWindow().getDecorView().getHeight() ;

		// getWindow().getDecorView() doesn't always take orientation into
		// account, we have to correct the values
		boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ;
		if (dw > dh && isPortrait || dw < dh && !isPortrait) {
			int d = dw ;
			dw = dh ;
			dh = d ;
		}

		// sanity check
		if (dw * dh == 0 || mVideoWidth * mVideoHeight == 0) {
			Log.e(TAG, "Invalid surface size") ;
			return ;
		}

		// compute the aspect ratio
		double ar, vw ;
		double density = (double) mSarNum / (double) mSarDen ;
		if (density == 1.0) {
			/* No indication about the density, assuming 1:1 */
			vw = mVideoVisibleWidth ;
			ar = (double) mVideoVisibleWidth / (double) mVideoVisibleHeight ;
		} else {
			/* Use the specified aspect ratio */
			vw = mVideoVisibleWidth * density ;
			ar = vw / mVideoVisibleHeight ;
		}

		// compute the display aspect ratio
		double dar = (double) dw / (double) dh ;

		switch (mCurrentSize) {
		case SURFACE_BEST_FIT:
			if (dar < ar)
				dh = (int) (dw / ar) ;
			else
				dw = (int) (dh * ar) ;
			break ;
		case SURFACE_FIT_HORIZONTAL:
			dh = (int) (dw / ar) ;
			break ;
		case SURFACE_FIT_VERTICAL:
			dw = (int) (dh * ar) ;
			break ;
		case SURFACE_FILL:
			break ;
		case SURFACE_16_9:
			ar = 16.0 / 9.0 ;
			if (dar < ar)
				dh = (int) (dw / ar) ;
			else
				dw = (int) (dh * ar) ;
			break ;
		case SURFACE_4_3:
			ar = 4.0 / 3.0 ;
			if (dar < ar)
				dh = (int) (dw / ar) ;
			else
				dw = (int) (dh * ar) ;
			break ;
		case SURFACE_ORIGINAL:
			dh = mVideoVisibleHeight ;
			dw = (int) vw ;
			break ;
		}

		// force surface buffer size
		mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight) ;

		// set display size
		LayoutParams lp = mSurface.getLayoutParams() ;
		lp.width = dw * mVideoWidth / mVideoVisibleWidth ;
		lp.height = dh * mVideoHeight / mVideoVisibleHeight ;
		mSurface.setLayoutParams(lp) ;

		// set frame size (crop if necessary)
		lp = mSurfaceFrame.getLayoutParams() ;
		lp.width = dw ;
		lp.height = dh ;
		mSurfaceFrame.setLayoutParams(lp) ;

		mSurface.invalidate() ;
	}

	/**
	 * show/hide the overlay
	 */

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		DisplayMetrics screen = new DisplayMetrics() ;
		getWindowManager().getDefaultDisplay().getMetrics(screen) ;

		if (mSurfaceYDisplayRange == 0)
			mSurfaceYDisplayRange = Math.min(screen.widthPixels, screen.heightPixels) ;

		float y_changed = event.getRawY() - mTouchY ;
		float x_changed = event.getRawX() - mTouchX ;

		// coef is the gradient's move to determine a neutral zone
		float coef = Math.abs(y_changed / x_changed) ;
		float xgesturesize = ((x_changed / screen.xdpi) * 2.54f) ;

		switch (event.getAction()) {

		case MotionEvent.ACTION_DOWN:
			// Audio
			mTouchY = event.getRawY() ;
			mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) ;
			mIsAudioOrBrightnessChanged = false ;
			// Seek
			mTouchX = event.getRawX() ;
			break ;

		case MotionEvent.ACTION_MOVE:
			if (coef > 2) {

				// Extend the overlay for a little while, so that it doesn't
				// disappear on the user if more adjustment is needed. This
				// is because on devices with soft navigation (e.g. Galaxy
				// Nexus), gestures can't be made without activating the UI.
				if (Util.hasNavBar())
					showOverlay() ;
			}
			// Seek (Right or Left move)
			doSeekTouch(coef, xgesturesize, false) ;
			break ;

		case MotionEvent.ACTION_UP:
			// Audio or Brightness
			if (!mIsAudioOrBrightnessChanged) {
				if (!mShowing) {
					showOverlay() ;
				} else {
					hideOverlay(true) ;
				}
			}
			// Seek
			doSeekTouch(coef, xgesturesize, true) ;
			break ;
		}
		return mIsAudioOrBrightnessChanged ;
	}

	private void doSeekTouch(float coef, float gesturesize, boolean seek) {

		// Always show seekbar when searching
		if (!mShowing)
			showOverlay() ;

		long length = mLibVLC.getLength() ;
		long time = mLibVLC.getTime() ;

		// Size of the jump, 10 minutes max (600000), with a bi-cubic
		// progression, for a 8cm gesture
		int jump = (int) (Math.signum(gesturesize) * ((600000 * Math.pow((gesturesize / 8), 4)) + 3000)) ;

		// Adjust the jump
		if ((jump > 0) && ((time + jump) > length))
			jump = (int) (length - time) ;
		if ((jump < 0) && ((time + jump) < 0))
			jump = (int) -time ;

		// Jump !
		if (seek)
			mPlayerControlListener.onSeekTo(time + jump) ;

		// Show the jump's size
		showInfo(
				String.format("%s%s (%s)", jump >= 0 ? "+" : "", Util.millisToString(jump),
						Util.millisToString(time + jump)), 1000) ;
	}

	/**
	 * handle changes of the seekbar (slicer)
	 */
	private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			mDragging = true ;
			showOverlay(OVERLAY_INFINITE) ;
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			mDragging = false ;
			showOverlay() ;
			hideInfo() ;
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			if (fromUser) {
				mLibVLC.setTime(progress) ;
				setOverlayProgress() ;
				mTime.setText(Util.millisToString(progress)) ;
				showInfo(Util.millisToString(progress)) ;
			}

		}
	} ;

	/**
    *
    */
	private final OnClickListener mAudioTrackListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			final String[] arrList = new String[mAudioTracksList.size()] ;
			int i = 0 ;
			int listPosition = 0 ;
			for (Map.Entry<Integer, String> entry : mAudioTracksList.entrySet()) {
				arrList[i] = entry.getValue() ;
				// map the track position to the list position
				if (entry.getKey() == mLibVLC.getAudioTrack())
					listPosition = i ;
				i++ ;
			}
			AlertDialog dialog = new AlertDialog.Builder(VideoPlayerActivity.this).setTitle("Audio track")
					.setSingleChoiceItems(arrList, listPosition, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int listPosition) {
							int trackID = -1 ;
							// Reverse map search...
							for (Map.Entry<Integer, String> entry : mAudioTracksList.entrySet()) {
								if (arrList[listPosition].equals(entry.getValue())) {
									trackID = entry.getKey() ;
									break ;
								}
							}
							if (trackID < 0)
								return ;

							mLibVLC.setAudioTrack(trackID) ;
							dialog.dismiss() ;
						}
					}).create() ;
			dialog.setCanceledOnTouchOutside(true) ;
			dialog.setOwnerActivity(VideoPlayerActivity.this) ;
			dialog.show() ;
		}
	} ;

	/**
    *
    */
	private final OnPlayerControlListener mPlayerControlListener = new OnPlayerControlListener() {
		@Override
		public void onPlayPause() {
			if (mLibVLC.isPlaying()) {
				pause() ;
			}
			else {
				play() ;
			}
			showOverlay() ;
		}

		@Override
		public void onSeek(int delta) {
			// unseekable stream
			if (mLibVLC.getLength() <= 0)
				return ;

			long position = mLibVLC.getTime() + delta ;
			if (position < 0)
				position = 0 ;
			mLibVLC.setTime(position) ;
			showOverlay() ;
		}

		@Override
		public void onSeekTo(long position) {
			// unseekable stream
			if (mLibVLC.getLength() <= 0)
				return ;
			mLibVLC.setTime(position) ;
			mTime.setText(Util.millisToString(position)) ;
		}

		@Override
		public void onShowInfo(String info) {
			if (info != null)
				showInfo(info) ;
			else {
				hideInfo() ;
				showOverlay() ;
			}
		}
	} ;

	/**
     *
     */
	private final OnClickListener mSizeListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (mCurrentSize < SURFACE_ORIGINAL) {
				mCurrentSize++ ;
			} else {
				mCurrentSize = 0 ;
			}
			changeSurfaceSize() ;
			switch (mCurrentSize) {
			case SURFACE_BEST_FIT:
				showInfo("Best fit", 1000) ;
				break ;
			case SURFACE_FIT_HORIZONTAL:
				showInfo("Fit_horizontal", 1000) ;
				break ;
			case SURFACE_FIT_VERTICAL:
				showInfo("Fit_vertical", 1000) ;
				break ;
			case SURFACE_FILL:
				showInfo("Fill", 1000) ;
				break ;
			case SURFACE_16_9:
				showInfo("16:9", 1000) ;
				break ;
			case SURFACE_4_3:
				showInfo("4:3", 1000) ;
				break ;
			case SURFACE_ORIGINAL:
				showInfo("Original", 1000) ;
				break ;
			}
			showOverlay() ;
		}
	} ;

	private final OnClickListener mRemainingTimeListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			mDisplayRemainingTime = !mDisplayRemainingTime ;
			showOverlay() ;
		}
	} ;

	/**
	 * attach and disattach surface to the lib
	 */
	private final SurfaceHolder.Callback mSurfaceCallback = new Callback() {
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			if (format == PixelFormat.RGBX_8888)
				Log.d(TAG, "Pixel format is RGBX_8888") ;
			else if (format == PixelFormat.RGB_565)
				Log.d(TAG, "Pixel format is RGB_565") ;
			else if (format == ImageFormat.YV12)
				Log.d(TAG, "Pixel format is YV12") ;
			else
				Log.d(TAG, "Pixel format is other/unknown") ;
			mLibVLC.attachSurface(holder.getSurface(), VideoPlayerActivity.this) ;
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			mLibVLC.detachSurface() ;
		}
	} ;

	/**
	 * show overlay the the default timeout
	 */
	private void showOverlay() {
		showOverlay(OVERLAY_TIMEOUT) ;
	}

	/**
	 * show overlay
	 */
	private void showOverlay(int timeout) {
		mHandler.sendEmptyMessage(SHOW_PROGRESS) ;
		if (!mShowing) {
			mShowing = true ;
			mOverlayHeader.setVisibility(View.VISIBLE) ;
			mOverlayOption.setVisibility(View.VISIBLE) ;
			mOverlayInterface.setVisibility(View.VISIBLE) ;
			dimStatusBar(false) ;
			mOverlayProgress.setVisibility(View.VISIBLE) ;
		}
		Message msg = mHandler.obtainMessage(FADE_OUT) ;
		if (timeout != 0) {
			mHandler.removeMessages(FADE_OUT) ;
			mHandler.sendMessageDelayed(msg, timeout) ;
		}
		updateOverlayPausePlay() ;
	}

	/**
	 * hider overlay
	 */
	private void hideOverlay(boolean fromUser) {
		if (mShowing) {
			mHandler.removeMessages(SHOW_PROGRESS) ;
			Log.i(TAG, "remove View!") ;
			if (!fromUser) {
				mOverlayHeader.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out)) ;
				mOverlayOption.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out)) ;
				mOverlayProgress.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out)) ;
				mOverlayInterface.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out)) ;
			}
			mOverlayHeader.setVisibility(View.INVISIBLE) ;
			mOverlayOption.setVisibility(View.INVISIBLE) ;
			mOverlayProgress.setVisibility(View.INVISIBLE) ;
			mOverlayInterface.setVisibility(View.INVISIBLE) ;
			mShowing = false ;
			dimStatusBar(true) ;
		}
	}

	/**
	 * Dim the status bar and/or navigation icons when needed on Android 3.x.
	 * Hide it on Android 4.0 and later
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void dimStatusBar(boolean dim) {
		if (!Util.isHoneycombOrLater() || !Util.hasNavBar())
			return ;
		int layout = 0 ;
		if (!Util.hasCombBar() && Util.isJellyBeanOrLater())
			layout = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE ;
		mSurface.setSystemUiVisibility((dim ? (Util.hasCombBar() ? View.SYSTEM_UI_FLAG_LOW_PROFILE
				: View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) : View.SYSTEM_UI_FLAG_VISIBLE) | layout) ;
	}

	private void updateOverlayPausePlay() {
		if (mLibVLC == null) {
			return ;
		}

		mControls.setState(mLibVLC.isPlaying()) ;
	}

	/**
	 * update the overlay
	 */
	private int setOverlayProgress() {
		if (mLibVLC == null) {
			return 0 ;
		}
		int time = (int) mLibVLC.getTime() ;
		int length = (int) mLibVLC.getLength() ;

		// Update all view elements
		mControls.setSeekable(length > 0) ;
		mSeekbar.setMax(length) ;
		mSeekbar.setProgress(time) ;
		if (time >= 0)
			mTime.setText(Util.millisToString(time)) ;
		if (length >= 0)
			mLength.setText(mDisplayRemainingTime && length > 0 ? "- " + Util.millisToString(length - time)
					: Util.millisToString(length)) ;

		return time ;
	}

	private void setESTracks() {
		if (mLastAudioTrack >= 0) {
			mLibVLC.setAudioTrack(mLastAudioTrack) ;
			mLastAudioTrack = -1 ;
		}
		if (mLastSpuTrack >= -1) {
			mLibVLC.setSpuTrack(mLastSpuTrack) ;
			mLastSpuTrack = -2 ;
		}
	}

	private void setESTrackLists() {
		setESTrackLists(false) ;
	}

	private void setESTrackLists(boolean force) {
		if (mAudioTracksList == null || force) {
			if (mLibVLC.getAudioTracksCount() > 2) {
				mAudioTracksList = mLibVLC.getAudioTrackDescription() ;
				mAudioTrack.setOnClickListener(mAudioTrackListener) ;
				mAudioTrack.setVisibility(View.VISIBLE) ;
			} else {
				mAudioTrack.setVisibility(View.GONE) ;
				mAudioTrack.setOnClickListener(null) ;
			}
		}
	}

	/**
     *
     */
	private void play() {
		
		if (mEndReached) {
			
			mEndReached = false ;
			mLibVLC.playIndex(savedIndexPosition) ;

		} else {
		
			mLibVLC.play() ;
		}
		
		mSurface.setKeepScreenOn(true) ;
	}

	/**
     *
     */
	private void pause() {
		mLibVLC.pause() ;
		mSurface.setKeepScreenOn(false) ;
	}

	/**
	 * External extras: - position (long) - position of the video to start with
	 * (in ms)
	 */
	private void load() {
		mLocation = null ;
		String title = "Tlitle" ;
		boolean dontParse = false ;
		String itemTitle = null ;
		int itemPosition = -1 ; // Index in the media list as passed by
								// AudioServer (used only for vout transition
								// internally)
		long intentPosition = -1 ; // position passed in by intent (ms)

        if (getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            /* Started from external application */
            if (getIntent().getData() != null
                    && getIntent().getData().getScheme() != null
                    && getIntent().getData().getScheme().equals("content")) {
                if(getIntent().getData().getHost().equals("media")) {
                    // Media URI
                    Cursor cursor = managedQuery(getIntent().getData(), new String[]{ MediaStore.Video.Media.DATA }, null, null, null);
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                    if (cursor.moveToFirst())
                        mLocation = LibVLC.PathToURI(cursor.getString(column_index));
                    
                } else if (getIntent().getData().getHost().equals("downloads")) {
                    try {
                        Cursor cursor = getContentResolver().query(getIntent().getData(), new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null);
                        cursor.moveToFirst();
                        String filename = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
                        Log.i(TAG, "Getting file " + filename + " from content:// URI");
                        InputStream is = getContentResolver().openInputStream(getIntent().getData());
                        OutputStream os = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Download/" + filename);
                        byte[] buffer = new byte[1024];
                        int bytesRead = 0;
                        while((bytesRead = is.read(buffer)) >= 0) {
                            os.write(buffer, 0, bytesRead);
                        }
                        os.close();
                        is.close();
                        mLocation = LibVLC.PathToURI(Environment.getExternalStorageDirectory().getPath() + "/Download/" + filename);
                    } catch (Exception e) {
                        Log.e(TAG, "Couldn't download file from mail URI");
                        encounteredError();
                    }

                } else {
                    // other content-based URI (probably file pickers)
                    mLocation = getIntent().getData().getPath();
                }
            } else {
                // Plain URI
                mLocation = getIntent().getDataString();
            }
            if(getIntent().getExtras() != null)
                intentPosition = getIntent().getExtras().getLong("position", -1);
        } else if (getIntent().getAction() != null && getIntent().getAction().equals(PLAY_FROM_VIDEOGRID)
				&& getIntent().getExtras() != null) {
			/* Started from VideoListActivity */
			mLocation = getIntent().getExtras().getString("itemLocation") ;
			itemTitle = getIntent().getExtras().getString("itemTitle") ;
			dontParse = getIntent().getExtras().getBoolean("dontParse") ;
			itemPosition = getIntent().getExtras().getInt("itemPosition", -1) ;
		}

		mSurface.setKeepScreenOn(true) ;

		/* Start / resume playback */
		if (dontParse && itemPosition >= 0) {
			// Provided externally from AudioService
			Log.d(TAG, "Continuing playback from AudioService at index " + itemPosition) ;
			savedIndexPosition = itemPosition ;
			if (!mLibVLC.isPlaying()) {
				// AudioService-transitioned playback for item after sleep and
				// resme
				mLibVLC.playIndex(savedIndexPosition) ;
				dontParse = false ;
			}
		} else if (savedIndexPosition > -1) {
			mLibVLC.setMediaList() ;
			mLibVLC.playIndex(savedIndexPosition) ;
		} else if (mLocation != null && mLocation.length() > 0 && !dontParse) {
			mLibVLC.setMediaList() ;
			mLibVLC.getMediaList().add(mLocation) ;
			savedIndexPosition = mLibVLC.getMediaList().size() - 1 ;
			mLibVLC.playIndex(savedIndexPosition) ;
		}

		if (mLocation != null && mLocation.length() > 0 && !dontParse) {
			// restore last position

			long rTime = mVideoResumeTime ;
			mVideoResumeTime = -1 ;

			if (rTime > 0)
				mLibVLC.setTime(rTime) ;

			if (intentPosition > 0)
				mLibVLC.setTime(intentPosition) ;

			try {
				title = URLDecoder.decode(mLocation, "UTF-8") ;
			} catch (UnsupportedEncodingException e) {
			} catch (IllegalArgumentException e) {
			}
			if (title.startsWith("file:")) {
				title = new File(title).getName() ;
				int dotIndex = title.lastIndexOf('.') ;
				if (dotIndex != -1)
					title = title.substring(0, dotIndex) ;
			}
		} else if (itemTitle != null) {
			title = itemTitle ;
		}
		mTitle.setText(title) ;
	}

	private int getScreenRotation() {
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE) ;
		Display display = wm.getDefaultDisplay() ;

		try {
			Method m = display.getClass().getDeclaredMethod("getRotation") ;
			return (Integer) m.invoke(display) ;
		} catch (Exception e) {
			return Surface.ROTATION_0 ;
		}
	}

	private int getScreenOrientation() {
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE) ;
		Display display = wm.getDefaultDisplay() ;
		int rot = getScreenRotation() ;
		/*
		 * Since getRotation() returns the screen's "natural" orientation, which
		 * is not guaranteed to be SCREEN_ORIENTATION_PORTRAIT, we have to
		 * invert the SCREEN_ORIENTATION value if it is "naturally" landscape.
		 */
		@SuppressWarnings("deprecation")
		boolean defaultWide = display.getWidth() > display.getHeight() ;
		if (rot == Surface.ROTATION_90 || rot == Surface.ROTATION_270)
			defaultWide = !defaultWide ;
		if (defaultWide) {
			switch (rot) {
			case Surface.ROTATION_0:
				return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ;
			case Surface.ROTATION_90:
				return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ;
			case Surface.ROTATION_180:
				return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE ;
			case Surface.ROTATION_270:
				return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT ;
			default:
				return 0 ;
			}
		} else {
			switch (rot) {
			case Surface.ROTATION_0:
				return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ;
			case Surface.ROTATION_90:
				return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ;
			case Surface.ROTATION_180:
				return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT ;
			case Surface.ROTATION_270:
				return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE ;
			default:
				return 0 ;
			}
		}
	}
}
