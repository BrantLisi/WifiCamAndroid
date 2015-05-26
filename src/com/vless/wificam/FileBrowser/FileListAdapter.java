package com.vless.wificam.FileBrowser;

import java.util.ArrayList ;

import com.vless.wificam.R;
import com.vless.wificam.FileBrowser.Model.FileNode;

import android.view.LayoutInflater ;
import android.view.View ;
import android.view.ViewGroup ;
import android.widget.BaseAdapter ;
import android.widget.CheckedTextView ;
import android.widget.ImageView ;
import android.widget.TextView ;

public class FileListAdapter extends BaseAdapter {

	private LayoutInflater mInflater ;
	private ArrayList<FileNode> mFileList ;
	
	public FileListAdapter(LayoutInflater inflater, ArrayList<FileNode> fileList) {
		
		mInflater = inflater ;
		mFileList = fileList ;
	}
	
	@Override
	public int getCount() {
		
		return mFileList == null ? 0 : mFileList.size() ;
	}

	@Override
	public Object getItem(int position) {

		return mFileList == null ? null : mFileList.get(position) ;
	}

	@Override
	public long getItemId(int position) {
		
		return position ;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ViewTag viewTag ;

		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.filelist_row, parent,false) ;

			viewTag = new ViewTag((ImageView) convertView.findViewById(R.id.fileListThumbnail),
					(TextView) convertView.findViewById(R.id.fileListName),
					(TextView) convertView.findViewById(R.id.fileListTime),
					(TextView) convertView.findViewById(R.id.fileListSize), mFileList.get(position),
					(CheckedTextView) convertView.findViewById(R.id.fileListCheckBox)) ;

			convertView.setTag(viewTag) ;

		} else {

			viewTag = (ViewTag) convertView.getTag() ;
		}

		viewTag.setmFileNode(mFileList.get(position));
		String filename = viewTag.getmFileNode().mName.substring(viewTag.getmFileNode().mName.lastIndexOf("/") + 1) ;
		viewTag.getmFilename().setText(filename) ;
		viewTag.getmTime().setText(viewTag.getmFileNode().mTime) ;
		viewTag.setSize(viewTag.getmFileNode().mSize) ;
		viewTag.getmCheckBox().setChecked(viewTag.getmFileNode().mSelected) ;

		return convertView ;
	}
	
}
