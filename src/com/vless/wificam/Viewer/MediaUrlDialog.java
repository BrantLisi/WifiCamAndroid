package com.vless.wificam.Viewer ;

import com.vless.wificam.R;
import com.vless.wificam.SimpleDialog;

import android.app.AlertDialog ;
import android.content.Context ;
import android.content.DialogInterface ;

public class MediaUrlDialog extends SimpleDialog {

	public interface MediaUrlDialogHandler {
		void onCancel() ;
	}

	public MediaUrlDialog(Context context, String mediaUrl,
			MediaUrlDialogHandler handler) {

		super(context) ;
	}

	public void show() {

		AlertDialog.Builder builder = new AlertDialog.Builder(mContext) ;
		builder.setTitle(mContext.getResources().getString(R.string.label_net_disconnected)) ;
		builder.setCancelable(false) ;
		builder.setInverseBackgroundForced(true) ;
		builder.setNegativeButton(mContext.getResources().getString(R.string.label_Close),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss() ;
					}
				}) ;

		AlertDialog alert = builder.create() ;
		alert.show() ;
	}
}
