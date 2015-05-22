package com.vless.wificam.Viewer ;

import java.io.BufferedInputStream ;
import java.io.File ;
import java.io.FileNotFoundException ;
import java.io.FileOutputStream ;
import java.io.IOException ;
import java.io.InputStream ;
import java.io.OutputStream ;
import java.net.HttpURLConnection ;
import java.net.MalformedURLException ;
import java.net.URL ;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue ;
import java.util.concurrent.ConcurrentLinkedQueue ;
import java.util.Timer;
import java.util.TimerTask;
import org.videolan.vlc.VLCApplication ;

import android.app.Activity ;
import android.app.AlertDialog ;
import android.app.Fragment ;
import android.content.BroadcastReceiver ;
import android.content.Context ;
import android.content.DialogInterface ;
import android.content.Intent ;
import android.content.IntentFilter ;
import android.content.SharedPreferences ;
import android.content.pm.ActivityInfo ;
import android.content.res.Configuration ;
import android.graphics.Bitmap ;
import android.graphics.BitmapFactory ;
import android.graphics.Canvas ;
import android.graphics.ImageFormat ;
import android.graphics.Paint ;
import android.graphics.PixelFormat ;
import android.media.AudioManager ;
import android.net.DhcpInfo ;
import android.net.wifi.WifiManager ;
import android.os.AsyncTask;
import android.os.Build ;
import android.os.Bundle ;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock ;
import android.preference.PreferenceManager ;
import android.util.Log ;
import android.view.Display ;
import android.view.LayoutInflater ;
import android.view.Surface ;
import android.view.SurfaceHolder ;
import android.view.SurfaceHolder.Callback ;
import android.view.SurfaceView ;
import android.view.View ;
import android.view.ViewGroup ;
import android.view.ViewGroup.LayoutParams ;
import android.view.WindowManager ;
import android.widget.Button ;
import android.widget.CompoundButton ;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener ;
import android.widget.FrameLayout ;
import android.widget.Switch ;

import com.bric.qt.io.JPEGMovWriter ;
import com.bric.qt.io.MovWriter ;
import com.vless.wificam.R;
import com.vless.wificam.CameraCommand;
import com.vless.wificam.MainActivity;

import java.text.SimpleDateFormat;
public class MjpegPlayerFragment extends Fragment {

	public static final String DEFAULT_MJPEG_PUSH_URL     = "/cgi-bin/liveMJPEG" ;
	public static final String DEFAULT_RTSP_MJPEG_AAC_URL = "/liveRTSP/av1";
	public static final String DEFAULT_RTSP_H264_AAC_URL  = "/liveRTSP/av2";
	public static final String DEFAULT_RTSP_H264_URL      = "/liveRTSP/v1";
	public static final String DEFAULT_MJPEG_PULL_URL     = "/cgi-bin/staticMJPEG" ;
	private static final String TAG = "MJPEG Player" ;
	private static String timestamp="";
	private SurfaceView mSurface ;
	private SurfaceHolder mSurfaceHolder ;
	private FrameLayout mSurfaceFrame ;
	private TextView curdate;
	private Button cameraRecordButton;
    private  int mYear;
    private  int mMonth;
    private  int mSecond;
    private  int mMinute;
    private  int mHour;
    private  int mDay;
    private String mTime;
    private String mRecordStatus="";
    private boolean mRecordthread = false;
	private static final int SURFACE_BEST_FIT = 0 ;
	private static final int SURFACE_FIT_HORIZONTAL = 1 ;
	private static final int SURFACE_FIT_VERTICAL = 2 ;
	private static final int SURFACE_FILL = 3 ;
	private static final int SURFACE_16_9 = 4 ;
	private static final int SURFACE_4_3 = 5 ;
	private static final int SURFACE_ORIGINAL = 6 ;
	private int mCurrentSize = SURFACE_BEST_FIT ;

	// size of the video
	private int mVideoHeight ;
	private int mVideoWidth ;

	private String mMediaUrl ;
	private boolean mPushMode ;
	private static final String KEY_MEDIA_URL = "mediaUrl" ;
	private static final String KEY_PUSH_MODE = "pushMode" ;
	private TimeThread timestampthread;
	private Player mPlayer ;

	public static MjpegPlayerFragment newInstance(String mediaUrl, boolean pushMode) {
		MjpegPlayerFragment fragment = new MjpegPlayerFragment() ;

		Bundle args = new Bundle() ;

		if (mediaUrl != null)
			args.putString(KEY_MEDIA_URL, mediaUrl) ;

		args.putBoolean(KEY_PUSH_MODE, pushMode) ;
		fragment.setArguments(args) ;

		return fragment ;
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction() ;
			if (action.equalsIgnoreCase(VLCApplication.SLEEP_INTENT)) {
				Activity activity = getActivity() ;
				if (activity != null)
					activity.finish() ;
			}
		}
	} ;

	private class Player {

		private class ImageData {

			static final int MAX_BUFFER_SIZE = 1024 * 100 ;
			byte[] imageData = new byte[MAX_BUFFER_SIZE] ;
			int length = 0 ;

			void load(InputStream imageStream) throws IOException {

				int size = 0 ;
				length = 0 ;

				while (true) {
					size = imageStream.read(imageData, length, MAX_BUFFER_SIZE - length) ;
					if (size < 0) {
						break ;
					} else {
						length += size ;
					}
				}
			}

			Bitmap decode() {

				return BitmapFactory.decodeByteArray(imageData, 0, length) ;
			}
		}

		private Queue<ImageData> mFreeImageData ;
		private Queue<ImageData> mLoadedImageData ;
		private boolean mRateControl ;

		private class DecodeThread extends Thread {

			private SurfaceHolder mSurfaceHolder ;
			private boolean mPlaying ;

			private int mHeight = 0 ;
			private int mWidth = 0 ;

			private boolean mSnapshot ;
			private String mSnapshotDirectory ;
			private String mSnapshotFilename ;

			private boolean mRecording ;
			private String mRecordingDirectory ;
			private String mRecordingFilename ;

			DecodeThread(SurfaceHolder surfaceHolder) {

				mSurfaceHolder = surfaceHolder ;
				mPlaying = true ;
				mRecording = false ;
				start() ;
			}

			public void stopPlay() {

				mPlaying = false ;
			}

			public boolean startRecord(String directory, String filename) {

				if (mRecording || !mPlaying) {

					return false ;
				}

				mRecordingFilename = filename ;
				mRecordingDirectory = directory ;

				mRecording = true ;
				return true ;
			}

			public boolean stopRecord() {

				if (!mRecording || !mPlaying) {

					return false ;
				}

				mRecording = false ;

				return true ;
			}

			private MjpegFileWriter createWriter(String directory, String filename, double frameRate) {

				try {
					File dir = new File(directory) ;
					if (!dir.exists()) {
						dir.mkdirs() ;
					}
					File file = new File(directory, filename) ;
					if (file.exists()) {
						file.delete() ;
					}
					if (file.createNewFile()) {

						return new MjpegFileWriter(file, mWidth, mHeight, frameRate) ;
					}

				} catch (FileNotFoundException ex) {
					ex.printStackTrace() ;
				} catch (IOException ex) {
					ex.printStackTrace() ;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace() ;
				}
				return null ;
			}

			private MovWriter createMovWriter(String directory, String filename) {

				try {
					File dir = new File(directory) ;
					if (!dir.exists()) {
						dir.mkdirs() ;
					}
					File file = new File(directory, filename) ;
					if (file.exists()) {
						file.delete() ;
					}
					if (file.createNewFile()) {

						return new JPEGMovWriter(file) ;
					}

				} catch (FileNotFoundException ex) {
					ex.printStackTrace() ;
				} catch (IOException ex) {
					ex.printStackTrace() ;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace() ;
				}
				return null ;

			}

			public boolean takeSnapshot(String directory, String filename) {

				if (mSnapshot || !mPlaying)
					return false ;

				mSnapshotFilename = filename ;
				mSnapshotDirectory = directory ;
				mSnapshot = true ;
				return true ;
			}

			private void snapshot(final ImageData imageData, final String directory, final String filename) {

				OutputStream outputStream = null ;

				try {
					File dir = new File(directory) ;
					if (!dir.exists()) {
						dir.mkdirs() ;
					}
					File file = new File(directory, filename) ;
					if (file.exists()) {
						file.delete() ;
					}
					if (file.createNewFile()) {
						outputStream = new FileOutputStream(file) ;
						outputStream.write(imageData.imageData, 0, imageData.length) ;
					}

				} catch (FileNotFoundException ex) {
					ex.printStackTrace() ;
				} catch (IOException ex) {
					ex.printStackTrace() ;
				} finally {
					if (outputStream != null) {
						try {
							outputStream.close() ;
						} catch (Throwable t) {
						}
					}
				}

				long dateTaken = System.currentTimeMillis() ;

				Context context = getActivity() ;

				if (context != null)
					MainActivity.addImageAsApplication(context.getContentResolver(), filename, dateTaken,
							directory, filename) ;
			}

			@Override
			public void run() {

				Paint paint = new Paint() ;
				paint.setAntiAlias(true) ;
				paint.setFilterBitmap(true) ;
				paint.setDither(true) ;

				mSnapshot = false ;
				int count = 0 ;
				boolean isRecording = false ;

				int frames = 0 ;
				double fps = 10.0 ;
				long fpsStartTime = SystemClock.uptimeMillis() ;
				long frameSavedTime = 0 ;

				ImageData imageData ;
				MjpegFileWriter fileWriter = null ;
				MovWriter movWriter = null ;

				while (mPlaying) {

					count++ ;

					if (isRecording != mRecording) {

						if (mRecording) {

							// fileWriter = createWriter(mRecordingDirectory,
							// mRecordingFilename + ".avi", fps) ;
							movWriter = createMovWriter(mRecordingDirectory, mRecordingFilename + ".mov") ;

							frameSavedTime = SystemClock.uptimeMillis() ;

						} else {

							try {
								if (fileWriter != null)
									fileWriter.finishAVI() ;
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace() ;
							}

							fileWriter = null ;

							if (movWriter != null) {
								try {
									movWriter.close(false) ;
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace() ;
								}
							}
							movWriter = null ;
						}

						isRecording = mRecording ;
					}

					imageData = mLoadedImageData.peek() ;

					while (imageData == null) {

						try {
							Thread.sleep(10) ;
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace() ;
						}

						if (!mPlaying) {
							mFreeImageData.clear() ;
							return ;
						}
						imageData = mLoadedImageData.peek() ;
					}

					long start = SystemClock.uptimeMillis() ;
					long decoded, rendered ;

					Bitmap bitmap = imageData.decode() ;

					if (bitmap != null && mSnapshot) {

						mSnapshot = false ;
						snapshot(imageData, mSnapshotDirectory, mSnapshotFilename) ;
					}

					if (bitmap != null && isRecording) {

						try {
							if (fileWriter != null)
								fileWriter.addImage(imageData.imageData) ;
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace() ;
						}

						if (movWriter != null) {

							long now = SystemClock.uptimeMillis() ;

							try {
								movWriter.addFrame(((float) (now - frameSavedTime)) / 1000,
										imageData.imageData, mHeight, mWidth, null) ;
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace() ;
							}

							frameSavedTime = now ;
						}
					}

					mLoadedImageData.poll() ;
					mFreeImageData.add(imageData) ;

					if (bitmap == null) {
						continue ;
					}

					decoded = SystemClock.uptimeMillis() ;

					int height = bitmap.getHeight() ;
					int width = bitmap.getWidth() ;

					if (height != mHeight || width != mWidth) {
						mHeight = height ;
						mWidth = width ;
						Activity activity = getActivity() ;

						if (activity != null)
							activity.runOnUiThread(new Runnable() {

								@Override
								public void run() {

									setSurfaceSize(mWidth, mHeight) ;
								}
							}) ;
					}

					if (mPlaying) {
						Canvas canvas = mSurfaceHolder.lockCanvas() ;

						if (canvas != null) {
							canvas.drawBitmap(bitmap, 0, 0, paint) ;
							mSurfaceHolder.unlockCanvasAndPost(canvas) ;

							frames++ ;
							long now = SystemClock.uptimeMillis() ;
							if (now - fpsStartTime > 10000) {

								fps = frames * (double) 1000 / (now - fpsStartTime) ;

								Log.i(TAG, "FPS = " + fps) ;
								frames = 0 ;
								fpsStartTime = now ;
							}
						}
					}

					rendered = SystemClock.uptimeMillis() ;

					bitmap.recycle() ;

					long finished = SystemClock.uptimeMillis() ;
					long total = finished - start ;

					if (count % 30 == 0) {
						Log.i(TAG, "Render Time used " + total + " " + (decoded - start) + " "
								+ (rendered - decoded)) ;
					}
					if (total < 33 && mRateControl) {
						try {
							Thread.sleep(33 - total) ;
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace() ;
						}
					}
				}

				if (isRecording) {

					try {
						if (fileWriter != null)
							fileWriter.finishAVI() ;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace() ;
					}

					if (movWriter != null) {

						try {
							movWriter.close(false) ;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace() ;
						}
					}
				}
				mFreeImageData.clear() ;
			}
		}

		private class DownloadThread extends Thread {

			private URL mUrl ;
			private boolean mPushMode ;
			private boolean mPlaying ;

			DownloadThread(URL url, boolean pushMode) {

				mUrl = url ;
				mPushMode = pushMode ;
				mPlaying = true ;
				start() ;
			}

			public void stopPlay() {

				mPlaying = false ;
			}

			@Override
			public void run() {

				if (mPushMode) {

					while (mPlaying) {

						int count = 0 ;
						ImageData imageData ;

						MjpegInputStream inputStream = MjpegInputStream.getInputStream(mMediaUrl) ;

						if (inputStream == null) {

							try {
								Thread.sleep(500) ;
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace() ;
							}
							continue ;
						}

						try {

							while (mPlaying) {

								count++ ;
								imageData = mFreeImageData.peek() ;

								while (imageData == null) {

									try {
										Thread.sleep(30) ;
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace() ;
									}

									if (!mPlaying) {
										mLoadedImageData.clear() ;
										return ;
									}
									imageData = mFreeImageData.peek() ;
								}

								long start = SystemClock.uptimeMillis() ;
								long downloaded ;

								try {
									imageData.imageData = inputStream.readRawMjpegFrame() ;

									if (imageData.imageData == null)
										continue ;

									imageData.length = imageData.imageData.length ;

									if (imageData.length == 0)
										continue ;

									downloaded = SystemClock.uptimeMillis() ;

									mFreeImageData.poll() ;
									mLoadedImageData.add(imageData) ;

									long finished = SystemClock.uptimeMillis() ;
									long total = finished - start ;

									if (count % 30 == 0) {
										Log.i(TAG, "Download Time used " + total + " " + (downloaded - start)) ;
									}
									if (total < 33 && mRateControl) {

										try {
											Thread.sleep(33 - total) ;
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace() ;
										}
									}

								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace() ;
									break ;
								}
							}
						} finally {
							MjpegInputStream.disconnect() ;
						}
					}
				} else {

					int count = 0 ;
					ImageData imageData ;

					while (mPlaying) {

						count++ ;

						imageData = mFreeImageData.peek() ;

						while (imageData == null) {

							try {
								Thread.sleep(10) ;
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace() ;
							}

							if (!mPlaying) {
								mLoadedImageData.clear() ;
								return ;
							}
							imageData = mFreeImageData.peek() ;
						}

						try {
							long start = SystemClock.uptimeMillis() ;
							long connected, downloaded ;
							HttpURLConnection httpConn = (HttpURLConnection) mUrl.openConnection() ;
							httpConn.setConnectTimeout(200) ;
							httpConn.setUseCaches(false) ;
							httpConn.connect() ;

							try {
								InputStream imageStream = new BufferedInputStream(httpConn.getInputStream()) ;
								// InputStream imageStream =
								// httpConn.getInputStream() ;

								connected = SystemClock.uptimeMillis() ;

								imageData.load(imageStream) ;

								imageStream.close() ;

								if (imageData.length == 0)
									continue ;

								downloaded = SystemClock.uptimeMillis() ;

								mFreeImageData.poll() ;
								mLoadedImageData.add(imageData) ;

							} finally {
								httpConn.disconnect() ;
							}
							long finished = SystemClock.uptimeMillis() ;
							long total = finished - start ;

							if (count % 30 == 0) {
								Log.i(TAG, "Download Time used " + total + " " + (connected - start) + " "
										+ (downloaded - connected)) ;
							}
							if (total < 33 && mRateControl) {
								try {
									Thread.sleep(33 - total) ;
								} catch (InterruptedException e) {
									e.printStackTrace() ;
								}
							}

						} catch (IOException e) {
							e.printStackTrace() ;
							try {
								Thread.sleep(30) ;
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace() ;
							}
						}
					}
				}
				mLoadedImageData.clear() ;
			}
		}

		DownloadThread mDownloadThread ;
		DecodeThread mDecodeThread ;

		public Player(URL url, SurfaceHolder surfaceHolder, boolean pushMode, boolean rateControl) {
			mFreeImageData = new ConcurrentLinkedQueue<ImageData>() ;
			mLoadedImageData = new ConcurrentLinkedQueue<ImageData>() ;
			mRateControl = rateControl ;

			for (int i = 0 ; i < 50 ; i++) {

				mFreeImageData.add(new ImageData()) ;
			}
			mDownloadThread = new DownloadThread(url, pushMode) ;
			mDecodeThread = new DecodeThread(surfaceHolder) ;
		}

		public void stopPlay() {

			mDownloadThread.stopPlay() ;
			mDecodeThread.stopPlay() ;
			try {
				mDownloadThread.join() ;
				mDecodeThread.join() ;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace() ;
			}
		}

		public boolean takeSnapShot(String directory, String filename) {

			return mDecodeThread.takeSnapshot(directory, filename) ;
		}

		public boolean startRecord(String directory, String filename) {

			setAutoOrientationEnabled(false) ;

			return mDecodeThread.startRecord(directory, filename) ;
		}

		public boolean stopRecord() {

			setAutoOrientationEnabled(true) ;

			return mDecodeThread.stopRecord() ;
		}

		public boolean isRecording() {

			return mDecodeThread.mRecording ;
		}
	}

    public String checkTime(int i)
	{
    	mTime = Integer.toString(i);
    	if (i<10) 
    	{
    		mTime = "0" + mTime;
    	}
    	return mTime;
	}
	//yining
	private class GetTimeStamp extends AsyncTask<URL, Integer, String> {
		@Override
		protected void onPreExecute() {
			setWaitingState(true) ;
			super.onPreExecute() ;
		}
		@Override
		protected String doInBackground(URL... params) {
			URL url = CameraCommand.commandTimeStampUrl() ;
			if (url != null) {		
				return CameraCommand.sendRequest(url) ;
			}
			return null ;
		}
		@Override
		protected void onPostExecute(String result) {
			Activity activity = getActivity() ;
			//Log.d(TAG, "TimeStamp property "+result) ;
			if (result != null) {
				String[] lines;		
				String[] lines_temp = result.split("Camera.Preview.MJPEG.TimeStamp.year=");
				lines = lines_temp[1].split(System.getProperty("line.separator")) ;
				mYear = Integer.valueOf(lines[0]);
				lines_temp = result.split("Camera.Preview.MJPEG.TimeStamp.month=");
				lines = lines_temp[1].split(System.getProperty("line.separator")) ;
				mMonth = Integer.valueOf(lines[0]); 
				lines_temp = result.split("Camera.Preview.MJPEG.TimeStamp.day=");
				lines = lines_temp[1].split(System.getProperty("line.separator")) ;
				mDay = Integer.valueOf(lines[0]); 
				lines_temp = result.split("Camera.Preview.MJPEG.TimeStamp.hour=");
				lines = lines_temp[1].split(System.getProperty("line.separator")) ;
				mHour= Integer.valueOf(lines[0]); 
				lines_temp = result.split("Camera.Preview.MJPEG.TimeStamp.minute=");
				lines = lines_temp[1].split(System.getProperty("line.separator")) ;
				mMinute = Integer.valueOf(lines[0]); 
				lines_temp = result.split("Camera.Preview.MJPEG.TimeStamp.second=");
				lines = lines_temp[1].split(System.getProperty("line.separator")) ;
				mSecond = Integer.valueOf(lines[0]); 
				timestampthread = new TimeThread();
				timestampthread.start();

			}
			else if (activity != null) {
				Toast.makeText(activity, activity.getResources().getString(R.string.message_fail_get_info),
						Toast.LENGTH_LONG).show() ;			
			}
			setWaitingState(false) ;
			setInputEnabled(true) ;
			super.onPostExecute(result) ;

	}
}

	public class GetRecordStatus extends AsyncTask<URL, Integer, String> {
		@Override
		protected void onPreExecute() {
			setWaitingState(true) ;
			super.onPreExecute() ;
		}
		@Override
		protected String doInBackground(URL... params) {
			URL url = CameraCommand.commandRecordStatusUrl() ;
			if (url != null) {
				return CameraCommand.sendRequest(url) ;
			}
			return null ;
		}
		@Override
		protected void onPostExecute(String result) {
			Activity activity = getActivity() ;
			//Log.d(TAG, "TimeStamp property "+result) ;
			if (result != null) {
				String[] lines;		
				String[] lines_temp = result.split("Camera.Preview.MJPEG.status=");
				lines = lines_temp[1].split(System.getProperty("line.separator")) ;
				mRecordStatus = lines[0];

			}
			else if (activity != null) {
				//Toast.makeText(activity, activity.getResources().getString(R.string.message_fail_get_info),
						//Toast.LENGTH_LONG).show() ;			
			}
			setWaitingState(false) ;
			setInputEnabled(true) ;
			super.onPostExecute(result) ;

	}
}	
	
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

		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Log.d(TAG, "Surface Created") ;
			try {
				mPlayer = new Player(new URL(mMediaUrl), holder, mPushMode, false) ;
				new GetTimeStamp().execute();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace() ;
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.d(TAG, "Surface Destroied") ;

			if (mPlayer != null) {

				if (mPlayer.isRecording()) {

					mPlayer.stopRecord() ;
				}
				mPlayer.stopPlay() ;
				//timestampthread.stopPlay();
			}
		}
	} ;
	
    private int getScreenOrientation(Activity activity){
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int rot = display.getRotation() ;
        /*
         * Since getRotation() returns the screen's "natural" orientation,
         * which is not guaranteed to be SCREEN_ORIENTATION_PORTRAIT,
         * we have to invert the SCREEN_ORIENTATION value if it is "naturally"
         * landscape.
         */
        @SuppressWarnings("deprecation")
        boolean defaultWide = display.getWidth() > display.getHeight();
        if(rot == Surface.ROTATION_90 || rot == Surface.ROTATION_270)
            defaultWide = !defaultWide;
        if(defaultWide) {
            switch (rot) {
            case Surface.ROTATION_0:
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            case Surface.ROTATION_90:
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            case Surface.ROTATION_180:
                // SCREEN_ORIENTATION_REVERSE_PORTRAIT only available since API
                // Level 9+
                return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            case Surface.ROTATION_270:
                // SCREEN_ORIENTATION_REVERSE_LANDSCAPE only available since API
                // Level 9+
                return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            default:
                return 0;
            }
        } else {
            switch (rot) {
            case Surface.ROTATION_0:
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            case Surface.ROTATION_90:
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            case Surface.ROTATION_180:
                // SCREEN_ORIENTATION_REVERSE_PORTRAIT only available since API
                // Level 9+
                return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            case Surface.ROTATION_270:
                // SCREEN_ORIENTATION_REVERSE_LANDSCAPE only available since API
                // Level 9+
                return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            default:
                return 0;
            }
        }
    }


	public void setAutoOrientationEnabled(boolean enabled) {
		
		Activity activity = getActivity() ;
		
		if (activity == null)
			return ;
		
		if (enabled) {
			
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR) ;
		} else {
			activity.setRequestedOrientation(getScreenOrientation(activity)) ;
		}
	}

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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState) ;

		Log.d(TAG, "Fragment Created") ;

		mMediaUrl = getArguments().getString(KEY_MEDIA_URL) ;
		mPushMode = getArguments().getBoolean(KEY_PUSH_MODE, false) ;

		if (mMediaUrl == null) {

			WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE) ;

			DhcpInfo dhcpInfo = wifiManager.getDhcpInfo() ;

			if (dhcpInfo != null && dhcpInfo.gateway != 0) {

				String gateway = MainActivity.intToIp(dhcpInfo.gateway) ;

				if (mPushMode)
					mMediaUrl = "http://" + gateway + DEFAULT_MJPEG_PUSH_URL ;
				else
					mMediaUrl = "http://" + gateway + DEFAULT_MJPEG_PULL_URL ;
			}
		}

		IntentFilter filter = new IntentFilter() ;
		filter.addAction(VLCApplication.SLEEP_INTENT) ;
		getActivity().registerReceiver(mReceiver, filter) ;

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		Log.d(TAG, "Fragment View Created") ;

		View view = inflater.inflate(R.layout.preview_player, container, false) ;

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity()) ;

		curdate = (TextView) view.findViewById(R.id.TimeStampLabel);
		mSurface = (SurfaceView) view.findViewById(R.id.player_surface) ;
		mSurfaceHolder = mSurface.getHolder() ;
		mSurfaceFrame = (FrameLayout) view.findViewById(R.id.player_surface_frame) ;
		String chroma = pref.getString("chroma_format", "") ;
		if (chroma.equals("YV12")) {
			mSurfaceHolder.setFormat(ImageFormat.YV12) ;
		} else if (chroma.equals("RV16")) {
			mSurfaceHolder.setFormat(PixelFormat.RGB_565) ;
			PixelFormat info = new PixelFormat() ;
			PixelFormat.getPixelFormatInfo(PixelFormat.RGB_565, info) ;
		} else {
			mSurfaceHolder.setFormat(PixelFormat.RGBX_8888) ;
			PixelFormat info = new PixelFormat() ;
			PixelFormat.getPixelFormatInfo(PixelFormat.RGBX_8888, info) ;
		}
		mSurfaceHolder.addCallback(mSurfaceCallback) ;
/*
		Button snapshotButton = (Button) view.findViewById(R.id.snapshotButton) ;

		snapshotButton.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {

				if (mPlayer == null) {

					return ;
				}

				File path = WifiCamActivity.getAppDir() ;
				String fileName = WifiCamActivity.getSnapshotFileName() ;

				mPlayer.takeSnapShot(path.getPath(), fileName) ;
			}
		}) ;
		final String appRecord = getActivity().getResources().getString(R.string.label_app_record) ;
		final String stopRecord = getActivity().getResources().getString(R.string.label_stop_record) ;

		final Button recordButton = (Button) view.findViewById(R.id.recordButton) ;

		if (mPlayer != null)
			recordButton.setText(mPlayer.isRecording() ? stopRecord : appRecord) ;

		recordButton.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {

				if (mPlayer == null) {

					return ;
				}

				if (!mPlayer.isRecording()) {

					final File path = WifiCamActivity.getAppDir() ;

					Activity activity = getActivity() ;

					if (activity == null)
						return ;

					new AlertDialog.Builder(activity)
							.setTitle(activity.getResources().getString(R.string.message_start_recording))
							.setMessage(
									activity.getResources().getString(R.string.message_save_video) + " \""
											+ WifiCamActivity.sAppName + "\"")
							.setPositiveButton(activity.getResources().getString(R.string.label_ok), new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {

									if (mPlayer == null) {

										return ;
									}

									String fileName = WifiCamActivity.getMJpegFileName() ;

									boolean result = mPlayer.startRecord(path.getPath(), fileName) ;

									if (result) {

										recordButton.setText(stopRecord) ;
									}
								}
							}).show() ;

				} else {

					mPlayer.stopRecord() ;
					recordButton.setText(appRecord) ;
				}
			}
		}) ;
*/

//		Button findCameraButton = (Button) view.findViewById(R.id.findCameraButton) ;
//
//		findCameraButton.setOnClickListener(new Button.OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//
//				URL url = CameraCommand.commandFindCameraUrl() ;
//
//				if (url != null) {
//					new CameraCommand.SendRequest().execute(url) ;
//				}
//			}
//		}) ;

		 cameraRecordButton = (Button) view.findViewById(R.id.cameraRecordButton) ;
		final Button cameraSnapshotButton = (Button) view.findViewById(R.id.cameraSnapshotButton) ;

		cameraRecordButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				URL url = CameraCommand.commandCameraRecordUrl() ;
				if (url != null) {
					new CameraCommand.SendRequest().execute(url) ;
				}	
				
			}
		}) ;
		
		cameraSnapshotButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				URL url = CameraCommand.commandCameraSnapshotUrl() ;
				if (url != null) {
					new CameraCommand.SendRequest().execute(url) ;
				}
			}
		}) ;
	
		cameraRecordButton.setEnabled(true) ;
		cameraSnapshotButton.setEnabled(true) ;
	/*	
		Switch cameraModeSwitch = (Switch) view.findViewById(R.id.cameraModeSwitch) ;
		cameraModeSwitch.setChecked(true) ;
		cameraModeSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

				cameraRecordButton.setEnabled(isChecked) ;
				cameraSnapshotButton.setEnabled(!isChecked) ;
			}
		}) ;
		*/ 
		
		getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC) ;
		//new GetTimeStamp().execute();
		return view ;
	}

	@Override
	public void onDestroy() {
		super.onDestroy() ;
		mRecordthread = false;
		Log.d(TAG, "Fragment destroied") ;
		getActivity().unregisterReceiver(mReceiver) ;

	}

	@Override
	public void onResume() {
		super.onResume() ;
		Log.d(TAG, "Fragment resumed") ;
		mRecordthread = true;
		mSurface.setKeepScreenOn(true) ;
	}

	@Override
	public void onPause() {
		super.onPause() ;
		Log.d(TAG, "Fragment paused") ;
		mRecordthread = false;
		mSurface.setKeepScreenOn(false) ;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		Log.d(TAG, "Fragment config changed") ;

		setSurfaceSize(mVideoWidth, mVideoHeight) ;
		super.onConfigurationChanged(newConfig) ;
	}

	private void changeSurfaceSize() {

		Activity activity = getActivity() ;

		if (activity == null)
			return ;

		// get screen size
		int dw = activity.getWindow().getDecorView().getWidth() ;
		int dh = activity.getWindow().getDecorView().getHeight() ;

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

		double vw = mVideoWidth ;
		double ar = (double) mVideoWidth / (double) mVideoHeight ;

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
			dh = mVideoHeight ;
			dw = (int) vw ;
			break ;
		}

		// force surface buffer size
		mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight) ;

		// set display size
		LayoutParams lp = mSurface.getLayoutParams() ;
		lp.width = dw ;
		lp.height = dh ;
		mSurface.setLayoutParams(lp) ;

		// set frame size (crop if necessary)
		lp = mSurfaceFrame.getLayoutParams() ;
		lp.width = dw ;
		lp.height = dh ;
		mSurfaceFrame.setLayoutParams(lp) ;

		mSurface.invalidate() ;
	}

	public void setSurfaceSize(int width, int height) {
		if (width * height == 0)
			return ;

		// store video size
		mVideoHeight = height ;
		mVideoWidth = width ;
		changeSurfaceSize() ;
	}
	
	private Handler mTimeHandler = new Handler() {
		public void handleMessage(Message msg){
            	mSecond++;
				if(mSecond==60)
				{
					mSecond=0;
					mMinute++;
					if(mMinute==60)
					{
						mHour++;
						mMinute=0;
						if(mHour==24)
						{
							mDay++;
						mHour=0;
						}
					}
				}
				timestamp = checkTime(mYear)+"/"+checkTime(mMonth)+"/"+checkTime(mDay);
				timestamp += " " + checkTime(mHour)+":"+checkTime(mMinute)+":"+checkTime(mSecond);
            	curdate.setText(timestamp);
            	timestamp = " ";
            super.handleMessage(msg);  
		}
	};
	
	private int mRecordCount = 0;
	private Handler mRecordStatusHandler = new Handler() {
		public void handleMessage(Message msg){
			mRecordCount++;
			if(mRecordCount==5)
			{
				mRecordCount = 0;
				new GetRecordStatus().execute();
			
            	if(mRecordStatus.equals("Recording"))
            		cameraRecordButton.setText(getActivity().getResources().getString(R.string.label_camera_stop_record));
            	else
            		cameraRecordButton.setText(getActivity().getResources().getString(R.string.label_camera_record));
			}
            super.handleMessage(msg);  
		}
	};

	public class TimeThread extends Thread{

			boolean mPlaying=true;
			public void run() {
				while(mPlaying)
				{	
					try{				
						Thread.sleep(1000);
					} catch (Exception e){
						e.printStackTrace();
					}
					
					if(mRecordthread)
						mRecordStatusHandler.sendMessage(mRecordStatusHandler.obtainMessage());
					
					mTimeHandler.sendMessage(mTimeHandler.obtainMessage());			
				}
			}
			public void stopPlay(){
				mPlaying=false;
			}
	};
	
}