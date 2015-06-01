package com.vless.wificam.FileBrowser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.baoyz.swipemenulistview.SwipeMenuListView.OnMenuItemClickListener;
import com.vless.wificam.R;
import com.vless.wificam.CameraCommand;
import com.vless.wificam.CameraPeeker;
import com.vless.wificam.CameraSniffer;
import com.vless.wificam.MainActivity;
import com.vless.wificam.FileBrowser.Model.FileNode;
import com.vless.wificam.FileBrowser.Model.FileNode.Format;
import com.vless.wificam.Viewer.StreamPlayerFragment;
import com.vless.wificam.frags.WifiCamFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FileBrowserFragment extends WifiCamFragment {

	public final static String TAG = "FileBrowserFragment";
	public static final String DEFAULT_PATH = "/cgi-bin/Config.cgi";
	public static final String DEFAULT_DIR = "DCIM";
	private ProgressDialog mProgDlg;
	private int mTotalFile;
	private int mfrom;
	private boolean mCancelDelete;

	class MyPeeker extends CameraPeeker {
		FileBrowserFragment theFrag;

		MyPeeker(FileBrowserFragment frag) {
			theFrag = frag;
		}

		public void Update(String s) {
			theFrag.Reception(s);
		}
	}

	//
	public FileBrowserFragment() {
		CameraSniffer sniffer = MainActivity.sniffer;
		MyPeeker peeker = new MyPeeker(this);
		sniffer.SetPeeker(peeker);
	}

	public void Reception(String s) {
		// Log.i("BROWSER", s);
	}

	private class DownloadFileListTask extends
			AsyncTask<FileBrowser, Integer, FileBrowser> {

		private class ContiunedDownloadTask extends
				AsyncTask<FileBrowser, Integer, FileBrowser> {

			@Override
			protected FileBrowser doInBackground(FileBrowser... browsers) {

				mfrom = browsers[0].retrieveFileList(mDirectory,
						FileNode.Format.all, mfrom);

				return browsers[0];
			}

			@Override
			protected void onPostExecute(FileBrowser result) {

				Activity activity = getActivity();

				if (activity == null)
					return;

				if (activity != null) {

					List<FileNode> fileList = result.getFileList();

					sFileList.addAll(fileList);
					mFileListAdapter.notifyDataSetChanged();

					if (!result.isCompleted() && fileList.size() != 0) {
						new ContiunedDownloadTask().execute(result);
					} else {
						setWaitingState(false);
					}
				}
			}
		}

		@Override
		protected void onPreExecute() {

			setWaitingState(true);

			sFileList.clear();
			mFileListAdapter.notifyDataSetChanged();

			sSelectedFiles.clear();
			mfrom = 0;
			super.onPreExecute();
		}

		@Override
		protected FileBrowser doInBackground(FileBrowser... browsers) {
			Log.i("Browser", "From" + mfrom);
			mfrom = browsers[0].retrieveFileList(mDirectory,
					FileNode.Format.all, 0);
			return browsers[0];
		}

		@Override
		protected void onPostExecute(FileBrowser result) {

			Activity activity = getActivity();

			if (activity != null) {

				List<FileNode> fileList = result.getFileList();

				sFileList.addAll(fileList);
				mFileListAdapter.notifyDataSetChanged();

				if (!result.isCompleted() && fileList.size() > 0) {
					new ContiunedDownloadTask().execute(result);
				} else {
					setWaitingState(false);
				}
			}
		}
	}

	private String mIp;
	private String mPath;
	private String mDirectory;

	private static final String KEY_IP = "ip";
	private static final String KEY_PATH = "path";
	private static final String KEY_DIRECTORY = "directory";

	private static ArrayList<FileNode> sFileList = new ArrayList<FileNode>();
	private static List<FileNode> sSelectedFiles = new LinkedList<FileNode>();
	private static FileListAdapter mFileListAdapter;
	private SwipeMenuCreator creator;
	private SwipeMenuListView mFileListView;

	private String mFileBrowser;
	private String mReading;
	private String mItems;

	private static int sNotificationCount = 0;
	private static DownloadTask sDownloadTask = null;

	private static class DownloadTask extends AsyncTask<URL, Long, Boolean> {

		String mFileName;
		Context mContext;
		WifiLock mWifiLock;
		String mIp;
		boolean mCancelled;
		PowerManager.WakeLock mWakeLock;

		private ProgressDialog mProgressDialog;

		DownloadTask(Context context) {

			mContext = context;
		}

		@Override
		protected void onPreExecute() {

			Log.i("DownloadTask", "onPreExecute");

			WifiManager wm = (WifiManager) mContext
					.getSystemService(Context.WIFI_SERVICE);
			mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL,
					"DownloadTask");
			mWifiLock.acquire();

			PowerManager pm = (PowerManager) mContext
					.getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					"DownloadTask");
			mWakeLock.acquire();

			mCancelled = false;
			showProgress(mContext);

			super.onPreExecute();
		}

		@Override
		protected Boolean doInBackground(URL... urls) {

			Log.i("DownloadTask", "doInBackground " + urls[0]);

			try {
				mIp = urls[0].getHost();

				HttpURLConnection urlConnection = (HttpURLConnection) urls[0]
						.openConnection();
				urlConnection.setRequestMethod("GET");
				urlConnection.setConnectTimeout(3000);
				urlConnection.setReadTimeout(10000);
				urlConnection.setUseCaches(false);
				urlConnection.setDoInput(true);
				urlConnection.connect();
				InputStream inputStream = urlConnection.getInputStream();

				mFileName = urls[0].getFile().substring(
						urls[0].getFile().lastIndexOf(File.separator) + 1);

				File appDir = MainActivity.getAppDir();
				File file = new File(appDir, mFileName);

				Log.i("Path", appDir.getPath() + File.separator + mFileName);

				if (file.exists()) {
					file.delete();
				}

				file.createNewFile();
				FileOutputStream fileOutput = new FileOutputStream(file);

				byte[] buffer = new byte[1024];
				int bufferLength = 0;
				try {
					while ((bufferLength = inputStream.read(buffer)) > 0) {
						publishProgress(
								Long.valueOf(urlConnection.getContentLength()),
								file.length());
						fileOutput.write(buffer, 0, bufferLength);
						if (mCancelled) {
							urlConnection.disconnect();
							break;
						}
					}
				} finally {
					// Log.i("DownloadTask", "doInBackground close START") ;
					// inputStream.close() ;
					// fileOutput.close() ;
					// Log.i("DownloadTask", "doInBackground disconnect START")
					// ;
					// urlConnection.disconnect() ;
					// Log.i("DownloadTask",
					// "doInBackground disconnect FINISHED") ;

				}
				if (mCancelled && file.exists()) {
					file.delete();
					return false;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}

			return true;
		}

		@Override
		protected void onProgressUpdate(Long... values) {

			if (mProgressDialog != null) {
				int max = values[0].intValue();
				int progress = values[1].intValue();
				String unit = "Bytes";

				mProgressDialog.setTitle("Downloading " + mFileName);

				if (max > 1024) {
					max /= 1024;
					progress /= 1024;
					unit = "KB";
				}

				if (max > 1024) {
					max /= 1024;
					progress /= 1024;
					unit = "MB";
				}
				mProgressDialog.setMax(max);
				mProgressDialog.setProgress(progress);
				mProgressDialog.setProgressNumberFormat("%1d/%2d " + unit);
			}
			super.onProgressUpdate(values);
		}

		private void cancelDownload() {

			mCancelled = true;

			for (FileNode fileNode : sSelectedFiles) {
				fileNode.mSelected = false;
			}
			sSelectedFiles.clear();
			mFileListAdapter.notifyDataSetChanged();
		}

		@Override
		protected void onCancelled() {

			Log.i("DownloadTask", "onCancelled");

			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			sDownloadTask = null;

			mWakeLock.release();
			mWifiLock.release();

			super.onCancelled();
		}

		@Override
		protected void onPostExecute(Boolean result) {

			Log.i("DownloadTask", "onPostExecute " + mFileName + " "
					+ (mCancelled ? "CANCELLED" : result ? "SUCCESS" : "FAIL"));

			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			sDownloadTask = null;

			mWakeLock.release();
			mWifiLock.release();

			if (mContext != null) {
				String ext = mFileName
						.substring(mFileName.lastIndexOf(".") + 1).toLowerCase(
								Locale.US);
				int nIcon;
				if (ext.equalsIgnoreCase("jpg")) {
					nIcon = R.drawable.type_photo;
				} else {
					nIcon = R.drawable.type_video;
				}
				if (!result) {
					if (!mCancelled) {
						Notification notification = new Notification.Builder(
								mContext).setContentTitle(mFileName)
								.setSmallIcon(nIcon)
								.setContentText("Download Failed")
								.getNotification();

						NotificationManager notificationManager = (NotificationManager) mContext
								.getSystemService(Context.NOTIFICATION_SERVICE);

						notification.flags |= Notification.FLAG_AUTO_CANCEL;

						notificationManager.notify(sNotificationCount++,
								notification);
					} else {
						Notification notification = new Notification.Builder(
								mContext).setContentTitle(mFileName)
								.setSmallIcon(nIcon)
								.setContentText("Download Cancelled")
								.getNotification();

						NotificationManager notificationManager = (NotificationManager) mContext
								.getSystemService(Context.NOTIFICATION_SERVICE);

						notification.flags |= Notification.FLAG_AUTO_CANCEL;

						notificationManager.notify(sNotificationCount++,
								notification);

					}
				} else {

					Uri uri = Uri.parse("file://" + MainActivity.sAppDir
							+ File.separator + mFileName);

					String mimeType = MimeTypeMap.getSingleton()
							.getMimeTypeFromExtension(ext);

					Log.i("MIME", ext + "  ==>  " + mimeType);

					Log.i("Path", uri.toString());

					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(uri, mimeType);

					PendingIntent pIntent = PendingIntent.getActivity(mContext,
							0, intent, 0);
					Notification notification = new Notification.Builder(
							mContext).setContentTitle(mFileName)
							.setSmallIcon(nIcon)
							.setContentText("Download Completed")
							.setContentIntent(pIntent).getNotification();

					NotificationManager notificationManager = (NotificationManager) mContext
							.getSystemService(Context.NOTIFICATION_SERVICE);

					notification.flags |= Notification.FLAG_AUTO_CANCEL;

					notificationManager.notify(sNotificationCount++,
							notification);
				}
			}
			if (!mCancelled)
				downloadFile(mContext, mIp);

			super.onPostExecute(result);
		}

		void showProgress(Context context) {
			mContext = context;
			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setTitle("Downloading");
			mProgressDialog.setMessage("Please wait");
			mProgressDialog.setCancelable(false);
			mProgressDialog.setMax(100);
			mProgressDialog.setProgress(0);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
					"Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							cancelDownload();
							dialog.dismiss();
						}
					});
			mProgressDialog.show();
		}

		void hideProgress() {
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (mIp == null) {
			WifiManager wifiManager = (WifiManager) getActivity()
					.getSystemService(Context.WIFI_SERVICE);
			DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
			if (dhcpInfo != null && dhcpInfo.gateway != 0) {
				mIp = MainActivity.intToIp(dhcpInfo.gateway);
			}
		}
		if (mPath == null) {
			mPath = DEFAULT_PATH;
		}
		if (mDirectory == null) {
			mDirectory = DEFAULT_DIR;
		}
	}

	private static void downloadFile(final Context context, String ip) {
		if (sSelectedFiles.size() == 0) {
			return;
		}
		FileNode fileNode = sSelectedFiles.remove(0);
		fileNode.mSelected = false;
		mFileListAdapter.notifyDataSetChanged();
		final String filename = fileNode.mName.substring(fileNode.mName
				.lastIndexOf("/") + 1);
		final String urlString = "http://" + ip + fileNode.mName;

		File appDir = MainActivity.getAppDir();
		final File file = new File(appDir, filename);

		Log.i("Path", appDir.getPath() + File.separator + filename);

		if (file.exists()) {
			AlertDialog alertDialog = new AlertDialog.Builder(context).create();
			alertDialog.setTitle(filename);
			alertDialog.setMessage("File already exists, overwrite?");
			alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});

			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							file.delete();
							try {
								sDownloadTask = new DownloadTask(context);
								sDownloadTask.execute(new URL(urlString));
							} catch (MalformedURLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							dialog.dismiss();
						}
					});

			alertDialog.show();

		} else {
			try {
				sDownloadTask = new DownloadTask(context);
				sDownloadTask.execute(new URL(urlString));
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private class CameraDeleteFile extends AsyncTask<URL, Integer, String> {
		@Override
		protected void onPreExecute() {
			setWaitingState(true);
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(URL... params) {
			FileNode fileNode = sSelectedFiles.get(0);
			URL url = CameraCommand
					.commandSetdeletesinglefileUrl(fileNode.mName);
			if (url != null) {
				return CameraCommand.sendRequest(url);
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			Activity activity = getActivity();
			FileNode fileNode = sSelectedFiles.remove(0);
			Log.d(TAG, "delete file response:" + result);
			if (result != null && result.equals("709\n?") != true) {
				fileNode.mSelected = false;
				sFileList.remove(fileNode);
				mFileListAdapter.notifyDataSetChanged();
				mProgDlg.setMessage("Please wait, deleteing " + fileNode.mName);
				mProgDlg.setProgress(mTotalFile - sSelectedFiles.size());
				if (sSelectedFiles.size() > 0 && !mCancelDelete) {
					new CameraDeleteFile().execute();
				} else {
					if (mProgDlg != null) {
						mProgDlg.dismiss();
						mProgDlg = null;
					}
					setWaitingState(false);
				}
			} else if (activity != null) {
				Toast.makeText(
						activity,
						activity.getResources().getString(
								R.string.message_command_failed),
						Toast.LENGTH_SHORT).show();
				setWaitingState(false);
			}
			super.onPostExecute(result);
		}
	}

	private int dp2px(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
				getResources().getDisplayMetrics());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.browser, container, false);
		TextView tvHeaderTitle = (TextView) view.findViewById(R.id.frag_header)
				.findViewById(R.id.header_title);
		tvHeaderTitle.setText(R.string.end_title_recordcard);
		mFileListAdapter = new FileListAdapter(inflater, sFileList);

		mFileBrowser = getActivity().getResources().getString(
				R.string.label_file_browser);
		mReading = getActivity().getResources().getString(
				R.string.label_reading);
		mItems = getActivity().getResources().getString(R.string.label_items);

		mFileListView = (SwipeMenuListView) view.findViewById(R.id.browserList);
		mFileListView.setAdapter(mFileListAdapter);
		creator = new SwipeMenuCreator() {
			@Override
			public void create(SwipeMenu menu) {
				// create "open" item
				SwipeMenuItem openItem = new SwipeMenuItem(getActivity()
						.getApplicationContext());
				// set item background
				openItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
						0xCE)));
				// set item width
				openItem.setWidth(dp2px(90));
				// set item title
				openItem.setTitle("load");
				// set item title fontsize
				openItem.setTitleSize(18);
				// set item title font color
				openItem.setTitleColor(Color.WHITE);
				// add to menu
				menu.addMenuItem(openItem);

				// create "delete" item
				SwipeMenuItem deleteItem = new SwipeMenuItem(getActivity()
						.getApplicationContext());
				// set item background
				deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
						0x3F, 0x25)));
				// set item width
				deleteItem.setWidth(dp2px(90));
				// set a icon
				deleteItem.setIcon(R.drawable.ic_delete);
				// add to menu
				menu.addMenuItem(deleteItem);
			}
		};
		mFileListView.setMenuCreator(creator);
		mFileListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		mFileListView.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public void onMenuItemClick(int position, SwipeMenu menu, int index) {
				FileNode item = sFileList.get(position);
				switch (index) {
				case 0:
					// download
					Toast.makeText(getActivity(), "download : " + index,
							Toast.LENGTH_SHORT).show();
					sSelectedFiles.clear();
					sSelectedFiles.add(item);
					downloadFile(getActivity(), mIp);
					break;
				case 1:
					// delete
					Toast.makeText(getActivity(), "delete : " + index,
							Toast.LENGTH_SHORT).show();
					delItem(item);
					break;
				default:
					break;
				}
			}

		});

		mFileListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				FileNode fileNode = sFileList.get(position);
				Intent intent = new Intent(Intent.ACTION_VIEW);
				if (fileNode.mFormat == Format.mov) {
					/* CarDV WiFi Support Video container is 3GP (.MOV) */
					/* For HTTP File Streaming */
					intent.setDataAndType(
							Uri.parse("http://" + mIp + fileNode.mName),
							"video/3gp");
					startActivity(intent);
				} else if (fileNode.mFormat == Format.jpeg) {
					Toast.makeText(getActivity(), "图片请下载再查看",
							Toast.LENGTH_SHORT).show();
				}
			}
		});

		return view;
	}

	private void delItem(FileNode item) {
		sSelectedFiles.clear();
		sSelectedFiles.add(item);
		mProgDlg = new ProgressDialog(getActivity());
		mProgDlg.setCancelable(false);
		mProgDlg.setMax(1);
		mProgDlg.setProgress(0);
		mProgDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mCancelDelete = true;
					}
				});
		mProgDlg.setTitle("Delete file in Camera");
		mProgDlg.setMessage("Please wait ...");
		mCancelDelete = false;
		mProgDlg.show();
		new CameraDeleteFile().execute();
	}

	@Override
	public void onDestroy() {
		CameraSniffer sniffer = MainActivity.sniffer;
		sniffer.SetPeeker(null);
		super.onDestroy();
	}

	private boolean mWaitingState = false;
	private boolean mWaitingVisible = false;

	private void setWaitingState(boolean waiting) {

		mFileListView.setClickable(!waiting);

		if (mWaitingState != waiting) {

			mWaitingState = waiting;
			setWaitingIndicator(mWaitingState, mWaitingVisible);
		}
	}

	private void setWaitingIndicator(boolean waiting, boolean visible) {

		if (!visible)
			return;

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
	public void onResume() {

		restoreWaitingIndicator();

		if (sDownloadTask != null) {

			sDownloadTask.showProgress(getActivity());

		} else {

			try {
				new DownloadFileListTask().execute(new FileBrowser(new URL(
						"http://" + mIp + mPath), FileBrowser.COUNT_MAX));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		super.onResume();
	}

	@Override
	public void onPause() {
		clearWaitingIndicator();

		if (sDownloadTask != null) {

			sDownloadTask.hideProgress();
		}

		super.onPause();
	}
}
