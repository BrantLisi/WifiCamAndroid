package com.vless.wificam.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.util.Log;

public class Utils {
	public static String getVideoMap(File fileList, int width, int height,
			int kind) throws IOException {
		Bitmap bitmap = null;
		// 获取视频的缩略图
		String path = fileList.getAbsolutePath();
		String fileName = fileList.getName().substring(0,
				fileList.getName().length() - 4);
		bitmap = ThumbnailUtils.createVideoThumbnail(path, kind);
		bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
				ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
		File root = Environment.getExternalStorageDirectory();
		File fileMap = new File(root, "temp");
		if (!fileMap.exists()) {
			fileMap.mkdirs();
		}
		fileMap = new File(fileMap, fileName + ".png");
		path = fileMap.getAbsolutePath();
		if (fileMap.exists()) {
			Log.i("test", "getVideoMap-=-=exists");
			return path;
		}
		FileOutputStream fos = new FileOutputStream(fileMap);
		bitmap.compress(CompressFormat.PNG, 0, fos);
		path = fileMap.getAbsolutePath();
		fos.close();
		return path;
	}
	
}
