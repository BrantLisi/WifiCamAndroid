package com.vless.wificam.FileBrowser ;

import com.vless.wificam.FileBrowser.Model.FileNode;

import android.graphics.Bitmap ;
import android.os.AsyncTask ;
import android.widget.CheckedTextView ;
import android.widget.ImageView ;
import android.widget.TextView ;

public class ViewTag {

	ImageView mThumbnail ;

	TextView mFilename ;
	TextView mTime ;
	TextView mSize ;
	CheckedTextView mCheckBox ;

	AsyncTask<ViewTag, Integer, Bitmap> mThumbnailTask ;

	FileNode mFileNode ;

	ViewTag(ImageView thumbnail, TextView filename, TextView time, TextView size, FileNode fileNode) {

		mThumbnail = thumbnail ;
		mFilename = filename ;
		mTime = time ;
		mSize = size ;
		mFileNode = fileNode ;
	}

	ViewTag(ImageView thumbnail, TextView filename, TextView time, TextView size, FileNode fileNode,
			CheckedTextView checkBox) {
		mThumbnail = thumbnail ;
		mFilename = filename ;
		mTime = time ;
		mSize = size ;
		mFileNode = fileNode ;
		mCheckBox = checkBox ;
	}

	public void setSize(double size) {

		if (size < 1024) {
			mSize.setText(String.format("%.2f", size) + " ") ;
			return ;
		}
		size /= 1024 ;

		if (size < 1024) {
			mSize.setText(String.format("%.2f", size) + " K") ;
			return ;
		}

		size /= 1024 ;

		if (size < 1024) {
			mSize.setText(String.format("%.2f", size) + " M") ;
			return ;
		}

		size /= 1024 ;

		if (size < 1024) {
			mSize.setText(String.format("%.2f", size) + " G") ;
			return ;
		}

		size /= 1024 ;

		if (size < 1024) {
			mSize.setText(String.format("%.2f", size) + " T") ;
			return ;
		}

	}

}