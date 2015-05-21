package com.vless.wificam ;

import android.app.AlertDialog ;
import android.app.Dialog ;
import android.content.Context ;
import android.content.DialogInterface ;

public abstract class SimpleDialog {

	public interface YesHandler {
		
		void onYes() ;
	}
	
	public interface NoHandler {
		
		void onNo() ;
	}

	public interface CancelHandler {
		
		void onCancel() ;
	}
	
	public interface YesCancelHandler extends YesHandler, CancelHandler {

	}
	
	public interface YesNoCancelHandler extends YesHandler, NoHandler, CancelHandler {
		
	}

	protected Context mContext ;

	protected SimpleDialog(Context context) {

		mContext = context ;
	}

	protected Dialog getDialog() {
		Dialog dialog = new Dialog(mContext) ;
		dialog.setCancelable(false) ;

		return dialog ;
	}

	private AlertDialog getEmptyDialog() {
		AlertDialog dialog = new AlertDialog.Builder(mContext).create() ;
		dialog.setCancelable(false) ;
		return dialog ;
	}

	protected AlertDialog getYesDialog(String title, String message, String confirmButtonCaption,
			final YesHandler handler) {

		AlertDialog dialog = getEmptyDialog() ;

		dialog.setTitle(title) ;
		dialog.setMessage(message) ;

		dialog.setButton(AlertDialog.BUTTON_POSITIVE, confirmButtonCaption,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (handler != null) {

							handler.onYes() ;
						}
					}
				}) ;

		return dialog ;
	}

	protected AlertDialog getYesCancelDialog(String title, String message, String confirmButtonCaption,
			String cancelButtonCaption, final YesCancelHandler handler) {

		AlertDialog dialog = getYesDialog(title, message, confirmButtonCaption, handler) ;

		dialog.setButton(AlertDialog.BUTTON_NEUTRAL, cancelButtonCaption,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (handler != null) {

							handler.onCancel() ;
						}
					}
				}) ;

		return dialog ;
	}

	public abstract void show() ;
}
