package com.vless.wificam.FileBrowser;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.videolan.libvlc.LibVlcException;
import org.videolan.vlc.Util;

import com.vless.wificam.R;
import com.vless.wificam.FileBrowser.Model.FileNode;
import com.vless.wificam.FileBrowser.Model.FileNode.Format;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;

public class LocalFileListAdapter extends BaseAdapter {

	private LayoutInflater mInflater;
	private ArrayList<FileNode> mFileList;

	public LocalFileListAdapter(LayoutInflater inflater,
			ArrayList<FileNode> fileList) {

		mInflater = inflater;
		mFileList = fileList;
	}

	@Override
	public int getCount() {

		return mFileList == null ? 0 : mFileList.size();
	}

	@Override
	public Object getItem(int position) {

		return mFileList == null ? null : mFileList.get(position);
	}

	@Override
	public long getItemId(int position) {

		return position;
	}

	private List<ExtractThumbnail> thumbnailTaskList = new LinkedList<ExtractThumbnail>();

	@Override
	public void notifyDataSetChanged() {
		for (ExtractThumbnail task : thumbnailTaskList) {

			task.cancel(false);
		}
		thumbnailTaskList.clear();
		super.notifyDataSetChanged();
	}

	private class ExtractThumbnail extends AsyncTask<ViewTag, Integer, Bitmap> {

		ViewTag mViewTag;

		@Override
		protected void onPreExecute() {
			thumbnailTaskList.add(this);
			super.onPreExecute();
		}

		@Override
		protected Bitmap doInBackground(ViewTag... params) {

			mViewTag = params[0];

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			options.inDither = false;
			options.inScaled = false;

			BitmapFactory.decodeFile(mViewTag.mFileNode.mName, options);

			int imageHeight = options.outHeight;
			int imageWidth = options.outWidth;
			int requestedHeight = 64;
			int requestedWidth = 64;

			int scaleDownFactor = 0;

			options.inJustDecodeBounds = false;

			while (true) {

				scaleDownFactor++;
				if (imageHeight / scaleDownFactor <= requestedHeight
						|| imageWidth / scaleDownFactor <= requestedWidth) {

					scaleDownFactor--;
					break;
				}
			}

			options.inSampleSize = scaleDownFactor;

			options.inPreferredConfig = Bitmap.Config.ARGB_8888;

			float scaleFactor = (float) requestedHeight / imageHeight;
			scaleFactor = Math.max(scaleFactor, (float) requestedWidth
					/ imageWidth);

			Bitmap originalBitmap = BitmapFactory.decodeFile(
					mViewTag.mFileNode.mName, options);

			if (originalBitmap == null) {

				try {
					byte[] data = Util.getLibVlcInstance().getThumbnail(
							"file://" + mViewTag.mFileNode.mName,
							requestedWidth, requestedHeight);
					if (data != null) {

						Bitmap thumbnail = Bitmap.createBitmap(requestedWidth,
								requestedHeight, Bitmap.Config.ARGB_8888);

						thumbnail.copyPixelsFromBuffer(ByteBuffer.wrap(data));
						thumbnail = Util.cropBorders(thumbnail, requestedWidth,
								requestedHeight);

						return thumbnail;
					}

				} catch (LibVlcException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}

			Bitmap thumbnail = ThumbnailUtils.extractThumbnail(originalBitmap,
					requestedWidth, requestedHeight);
			originalBitmap.recycle();

			return thumbnail;
		}

		@Override
		protected void onPostExecute(Bitmap thumbnail) {
			if (thumbnail != null) {
				mViewTag.mThumbnail.setImageBitmap(thumbnail);
			}
			thumbnailTaskList.remove(this);
			mViewTag.mThumbnailTask = null;

			super.onPostExecute(thumbnail);
		}

	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewTag viewTag;

		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.local_filelist_row, null);

			viewTag = new ViewTag(
					(ImageView) convertView
							.findViewById(R.id.fileListThumbnail),
					(TextView) convertView.findViewById(R.id.fileListName),
					(TextView) convertView.findViewById(R.id.fileListTime),
					(TextView) convertView.findViewById(R.id.fileListSize),
					mFileList.get(position), (CheckedTextView) convertView
							.findViewById(R.id.fileListCheckBox));

			convertView.setTag(viewTag);

		} else {

			viewTag = (ViewTag) convertView.getTag();

			if (viewTag.mThumbnailTask != null) {

				viewTag.mThumbnailTask.cancel(false);
				thumbnailTaskList.remove(viewTag.mThumbnailTask);
				viewTag.mThumbnailTask = null;
			}
		}

		viewTag.mFileNode = mFileList.get(position);
		String filename = viewTag.mFileNode.mName
				.substring(viewTag.mFileNode.mName.lastIndexOf("/") + 1);
		viewTag.mFilename.setText(filename);
		viewTag.mTime.setText(viewTag.mFileNode.mTime);
		viewTag.setSize(viewTag.mFileNode.mSize);
		if (viewTag.mFileNode.mFormat == Format.mov ||
			viewTag.mFileNode.mFormat == Format.avi) {
			viewTag.mThumbnail.setImageResource(R.drawable.type_all); // for temporary, it should be type_video
		} else {
			viewTag.mThumbnail.setImageResource(R.drawable.type_all); // for temporary, it should be type_photo
		}
		viewTag.mThumbnailTask = new ExtractThumbnail();
		// viewTag.mThumbnailTask.execute(viewTag) ;
		viewTag.mThumbnailTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
				viewTag);

		return convertView;
	}
}
