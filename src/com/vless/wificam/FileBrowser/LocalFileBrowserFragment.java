package com.vless.wificam.FileBrowser;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.baoyz.swipemenulistview.SwipeMenuListView.OnMenuItemClickListener;
import com.vless.wificam.R;
import com.vless.wificam.MainActivity;
import com.vless.wificam.FileBrowser.Model.FileNode;
import com.vless.wificam.FileBrowser.Model.FileBrowserModel.ModelException;
import com.vless.wificam.FileBrowser.Model.FileNode.Format;
import com.vless.wificam.contants.Contants;
import com.vless.wificam.frags.WifiCamFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class LocalFileBrowserFragment extends WifiCamFragment {

	private SharedPreferences sp;
	private Editor edit;

	private ArrayList<FileNode> mFileList = new ArrayList<FileNode>();
	private List<FileNode> mSelectedFiles = new LinkedList<FileNode>();

	private LocalFileListAdapter mFileListAdapter;
	private SwipeMenuCreator creator;
	private SwipeMenuListView fileListView;

	private int dp2px(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
				getResources().getDisplayMetrics());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		sp = getActivity().getSharedPreferences(Contants.USER_INFO,
				Activity.MODE_PRIVATE);
		edit = sp.edit();

		View view = inflater.inflate(R.layout.local_browser, container, false);
		TextView tvHeaderTitle = (TextView) view.findViewById(R.id.frag_header)
				.findViewById(R.id.header_title);
		tvHeaderTitle.setText(R.string.end_title_localfile);
		mFileListAdapter = new LocalFileListAdapter(inflater, mFileList);

		fileListView = (SwipeMenuListView) view.findViewById(R.id.browserList);
		fileListView.setAdapter(mFileListAdapter);

		// step 1. create a MenuCreator
		creator = new SwipeMenuCreator() {

			@Override
			public void create(SwipeMenu menu) {
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
		fileListView.setMenuCreator(creator);
		fileListView.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public void onMenuItemClick(int position, SwipeMenu menu, int index) {
				FileNode item = mFileList.get(position);
				switch (index) {
				case 0:
					// delete
					// Toast.makeText(getActivity(), "delete : " + index,
					// Toast.LENGTH_SHORT).show();
					new File(item.mName).delete();
					new LoadFileListTask().execute();
					break;
				default:
					break;
				}
			}

		});

		fileListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				edit.putBoolean("isFromMain", true);
				edit.commit();
				openFileNode(mFileList.get(position));
			}
		});
		return view;
	}

	private void openFileNode(FileNode item) {
		File file = new File(item.mName);
		Intent intent = new Intent(Intent.ACTION_VIEW);
		if (item.mFormat == Format.mov) {
			/* CarDV WiFi Support Video is 3GP (.MOV) */
			intent.setDataAndType(Uri.fromFile(file), "video/3gp");
			startActivity(intent);
		} else if (item.mFormat == Format.avi) {
			/* call self player */
			VideoPlayerActivity.start(getActivity(), "file://" + item.mName);
		} else if (item.mFormat == Format.jpeg) {
			intent.setDataAndType(Uri.fromFile(file), "image/jpeg");
			startActivity(intent);
		}
	}

	private class LoadFileListTask extends
			AsyncTask<Integer, Integer, ArrayList<FileNode>> {

		@Override
		protected void onPreExecute() {

			setWaitingState(true);
			mFileList.clear();
			mFileListAdapter.notifyDataSetChanged();

			mSelectedFiles.clear();

			Log.i("LocalFileBrowserFragment", "pre execute");

			super.onPreExecute();
		}

		@Override
		protected ArrayList<FileNode> doInBackground(Integer... params) {

			Log.i("LocalFileBrowserFragment", "background");
			File directory = MainActivity.getAppDir();
			Log.i("LocalFileBrowserFragment", "get app dir");
			File[] files = directory.listFiles();
			Log.i("LocalFileBrowserFragment", "list file");

			ArrayList<FileNode> fileList = new ArrayList<FileNode>();

			for (File file : files) {
				String name = file.getName();
				String ext = name.substring(name.lastIndexOf(".") + 1);
				String attr = (file.canRead() ? "r" : "")
						+ (file.canWrite() ? "w" : "");
				long size = file.length();
				String time = new SimpleDateFormat("yyyy-MM-dd HH:mm",
						Locale.US).format(new Date(file.lastModified()));

				FileNode.Format format = FileNode.Format.all;

				if (ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("jpg")) {
					format = FileNode.Format.jpeg;
				} else if (ext.equalsIgnoreCase("avi")) {
					format = FileNode.Format.avi;
				} else if (ext.equalsIgnoreCase("mov")
						|| ext.equalsIgnoreCase("3gp")) {
					format = FileNode.Format.mov;
				}

				if (format != FileNode.Format.all) {
					try {
						FileNode fileNode = new FileNode(file.getPath(),
								format, (int) size, attr, time);
						fileList.add(fileNode);

					} catch (ModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			Log.i("LocalFileBrowserFragment", "file parsed");

			return fileList;
		}

		@Override
		protected void onPostExecute(ArrayList<FileNode> result) {

			Log.i("LocalFileBrowserFragment", "post exec");

			mFileList.addAll(result);
			mFileListAdapter.notifyDataSetChanged();
			setWaitingState(false);
			super.onPostExecute(result);
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
		edit.putBoolean("isFromMain", false);
		edit.commit();
		restoreWaitingIndicator();
		new LoadFileListTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
		super.onResume();
	}

	@Override
	public void onPause() {
		clearWaitingIndicator();
		super.onPause();
	}
}
