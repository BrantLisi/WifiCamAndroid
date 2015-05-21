package com.vless.wificam;

import com.vless.wificam.R;

import android.app.Dialog ;
import android.content.Context ;
import android.text.InputType ;
import android.view.View ;
import android.view.View.OnClickListener ;
import android.widget.Button ;
import android.widget.CheckBox ;
import android.widget.CompoundButton ;
import android.widget.CompoundButton.OnCheckedChangeListener ;
import android.widget.EditText ;

public class LoginDialog extends SimpleDialog {

	public interface LoginDialogHandler {
		
		void onLogin(String username, String password) ;
		void onCancel() ;
	}
	
	private LoginDialogHandler mHandler ;
	private String mTitle ;
	
	public LoginDialog(Context context, String title, LoginDialogHandler loginDialogHandler) {

		super(context) ;
		mTitle = title ;
		mHandler = loginDialogHandler ;
	}
	
	public void show() {
		
		final Dialog dialog = getDialog() ;
		dialog.setContentView(R.layout.login_dialog) ;
		dialog.setTitle(mTitle) ;

		final EditText username = (EditText) dialog.findViewById(R.id.loginDialogUsername) ;
		username.setText("admin") ;

		final EditText password = (EditText) dialog.findViewById(R.id.loginDialogPassword) ;

		Button dialogLoginButton = (Button) dialog.findViewById(R.id.loginDialogLoginButton) ;

		dialogLoginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				mHandler.onLogin(username.getText().toString(), password.getText().toString()) ;
				dialog.dismiss() ;
			}
		}) ;

		Button dialogCancelButton = (Button) dialog.findViewById(R.id.loginDialogCancelButton) ;

		dialogCancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				mHandler.onCancel() ;
				dialog.dismiss() ;
			}
		}) ;

		CheckBox displayPassword = (CheckBox) dialog.findViewById(R.id.loginDialogDisplayPassword) ;

		displayPassword.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					password.setInputType(InputType.TYPE_CLASS_TEXT) ;
				} else {
					password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD) ;
				}
			}
		}) ;

		dialog.setCancelable(false) ;
		
		dialog.show() ;
	}
}
