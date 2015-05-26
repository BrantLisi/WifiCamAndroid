package com.vless.wificam.FileBrowser;

import com.vless.wificam.FileBrowser.Model.FileNode;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;

public class ViewTag {

	private ImageView mThumbnail;

	private TextView mFilename;
	private TextView mTime;
	private TextView mSize;
	private CheckedTextView mCheckBox;

	AsyncTask<ViewTag, Integer, Bitmap> mThumbnailTask;

	private FileNode mFileNode;

	ViewTag(ImageView thumbnail, TextView filename, TextView time,
			TextView size, FileNode fileNode) {

		mThumbnail = thumbnail;
		mFilename = filename;
		mTime = time;
		mSize = size;
		mFileNode = fileNode;
	}

	ViewTag(ImageView thumbnail, TextView filename, TextView time,
			TextView size, FileNode fileNode, CheckedTextView checkBox) {
		mThumbnail = thumbnail;
		mFilename = filename;
		mTime = time;
		mSize = size;
		mFileNode = fileNode;
		mCheckBox = checkBox;
	}

	public void setSize(double size) {

		if (size < 1024) {
			mSize.setText(String.format("%.2f", size) + " ");
			return;
		}
		size /= 1024;

		if (size < 1024) {
			mSize.setText(String.format("%.2f", size) + " K");
			return;
		}

		size /= 1024;

		if (size < 1024) {
			mSize.setText(String.format("%.2f", size) + " M");
			return;
		}

		size /= 1024;

		if (size < 1024) {
			mSize.setText(String.format("%.2f", size) + " G");
			return;
		}

		size /= 1024;

		if (size < 1024) {
			mSize.setText(String.format("%.2f", size) + " T");
			return;
		}

	}

	public ImageView getmThumbnail() {
		return mThumbnail;
	}

	public void setmThumbnail(ImageView mThumbnail) {
		this.mThumbnail = mThumbnail;
	}

	public TextView getmFilename() {
		return mFilename;
	}

	public void setmFilename(TextView mFilename) {
		this.mFilename = mFilename;
	}

	public TextView getmTime() {
		return mTime;
	}

	public void setmTime(TextView mTime) {
		this.mTime = mTime;
	}

	public TextView getmSize() {
		return mSize;
	}

	public void setmSize(TextView mSize) {
		this.mSize = mSize;
	}

	public CheckedTextView getmCheckBox() {
		return mCheckBox;
	}

	public void setmCheckBox(CheckedTextView mCheckBox) {
		this.mCheckBox = mCheckBox;
	}

	public AsyncTask<ViewTag, Integer, Bitmap> getmThumbnailTask() {
		return mThumbnailTask;
	}

	public void setmThumbnailTask(AsyncTask<ViewTag, Integer, Bitmap> mThumbnailTask) {
		this.mThumbnailTask = mThumbnailTask;
	}

	public FileNode getmFileNode() {
		return mFileNode;
	}

	public void setmFileNode(FileNode mFileNode) {
		this.mFileNode = mFileNode;
	}

}