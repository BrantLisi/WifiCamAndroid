package com.vless.wificam;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.vless.wificam.R;
import com.vless.wificam.Control.CameraControlFragment;
import com.vless.wificam.FileBrowser.FileBrowserFragment;
import com.vless.wificam.FileBrowser.LocalFileBrowserFragment;
import com.vless.wificam.Viewer.StreamPlayerFragment;
import com.vless.wificam.adapts.FragmentTabAdapter;
import com.vless.wificam.frags.WifiCamFragment;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {

	public static String intToIp(int addr) {

		return ((addr & 0xFF) + "." + ((addr >>>= 8) & 0xFF) + "."
				+ ((addr >>>= 8) & 0xFF) + "." + ((addr >>>= 8) & 0xFF));
	}

	public static String getSnapshotFileName() {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss",
				Locale.US);
		String currentDateandTime = sdf.format(new Date());

		return currentDateandTime + ".jpg";
	}

	public static String getMJpegFileName() {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss",
				Locale.US);
		String currentDateandTime = sdf.format(new Date());

		return currentDateandTime;
	}

	public static String sAppName = "";
	public static String sAppDir = "";

	public static File getAppDir() {
		File appDir = new File(sAppDir);
		if (!appDir.exists()) {
			appDir.mkdirs();
		}
		return appDir;
	}

	public static Uri addImageAsApplication(ContentResolver contentResolver,
			String name, long dateTaken, String directory, String filename) {

		String filePath = directory + File.separator + filename;

		String[] imageProjection = new String[] {
				"DISTINCT " + BaseColumns._ID, MediaColumns.DATA,
				MediaColumns.DISPLAY_NAME };

		String imageSelection = new String(Images.Media.TITLE + "=? AND "
				+ Images.Media.DISPLAY_NAME + "=?");

		String[] imageSelectionArgs = new String[] { name, filename };

		Cursor cursor = contentResolver.query(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageProjection,
				imageSelection, imageSelectionArgs, null);

		if (cursor == null || cursor.getCount() == 0) {

			ContentValues values = new ContentValues(7);
			values.put(Images.Media.TITLE, name);
			values.put(Images.Media.DISPLAY_NAME, filename);
			values.put(Images.Media.DATE_TAKEN, dateTaken);
			values.put(Images.Media.MIME_TYPE, "image/jpeg");
			values.put(Images.Media.DATA, filePath);

			return contentResolver.insert(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		} else {

			int idColumn = cursor.getColumnIndex(MediaColumns._ID);

			if (idColumn == -1)
				return null;

			cursor.moveToFirst();

			Long id = cursor.getLong(idColumn);

			return Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI
					.toString() + "/" + String.valueOf(id));
		}
	}

	private WifiManager wifiManager;
	public static CameraSniffer sniffer = null;
	private List<WifiCamFragment> fragments = new ArrayList<WifiCamFragment>();
	private FragmentTabAdapter tabAdapter;
	private RadioGroup rgs;
	private WifiCamFragment strmPlyFrg, filBrwFrg, locBrwFrg, camCtlFrg;
	private RadioButton rb_curr, rb_sd, rb_local, rb_sett;

	// private boolean isDoubleBack = true;
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		Log.i("Fragment Activity", "ON CREATE " + savedInstanceState);
		System.setProperty("http.keepAlive", "false");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		if (sniffer == null) {
			sniffer = new CameraSniffer();
			sniffer.start();
		}

		sAppName = getResources().getString(R.string.app_name);
		sAppDir = Environment.getExternalStorageDirectory().getPath()
				+ File.separator + sAppName;
		Log.i("Fragment Activity", sAppDir);
		File appDir = new File(sAppDir);
		if (!appDir.exists()) {
			appDir.mkdirs();
		}

		setContentView(R.layout.wifi_cam_main);

		if (savedInstanceState == null) {

			wifiManager = (WifiManager) getApplicationContext()
					.getSystemService(Context.WIFI_SERVICE);

			if (wifiManager.isWifiEnabled()
					&& wifiManager.getConnectionInfo().getNetworkId() != -1) {
				initView(wifiManager.isWifiEnabled(), wifiManager
						.getConnectionInfo().getNetworkId());
			} else {
				String title = getResources().getString(
						R.string.dialog_no_connection_title);
				String message = getResources().getString(
						R.string.dialog_no_connection_message);

				new AlertDialog.Builder(this)
						.setTitle(title)
						.setMessage(message)
						.setPositiveButton(
								getResources().getString(R.string.label_ok),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
										initView(wifiManager.isWifiEnabled(),
												wifiManager.getConnectionInfo()
														.getNetworkId());
									}
								}).show();
			}
		}
	}

	private void initView(boolean isWifiEnabled, int netWorkId) {
		addFragments(isWifiEnabled, netWorkId);
	}

	private void addFragments(boolean isWifiEnabled, int netWorkId) {
		Bundle bundle = new Bundle();
		bundle.putBoolean("isWifiEnabled", isWifiEnabled);
		bundle.putInt("netWorkId", netWorkId);
		strmPlyFrg = new StreamPlayerFragment();
		strmPlyFrg.setArguments(bundle);
		filBrwFrg = new FileBrowserFragment();
		filBrwFrg.setArguments(bundle);
		locBrwFrg = new LocalFileBrowserFragment();
		locBrwFrg.setArguments(bundle);
		camCtlFrg = new CameraControlFragment();
		camCtlFrg.setArguments(bundle);

		fragments.add(strmPlyFrg);
		fragments.add(filBrwFrg);
		fragments.add(locBrwFrg);
		fragments.add(camCtlFrg);

		rgs = (RadioGroup) findViewById(R.id.tabs_rg);
		rb_curr = (RadioButton) findViewById(R.id.tab_rb_a);
		rb_sd = (RadioButton) findViewById(R.id.tab_rb_b);
		rb_local = (RadioButton) findViewById(R.id.tab_rb_c);
		rb_sett = (RadioButton) findViewById(R.id.tab_rb_d);

		tabAdapter = new FragmentTabAdapter(this, fragments, R.id.tab_content,
				rgs);
		tabAdapter
				.setOnRgsExtraCheckedChangedListener(new FragmentTabAdapter.OnRgsExtraCheckedChangedListener() {
					@Override
					public void OnRgsExtraCheckedChanged(RadioGroup radioGroup,
							final int checkedId, int index) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								getCurFragFoucs(checkedId);
							}
						});
					}
				});
	}

	private void getCurFragFoucs(int checkedId) {
		switch (checkedId) {
		case R.id.tab_rb_a:
			setTitle(getResources().getString(R.string.app_name));
			rb_curr.setBackgroundResource(R.drawable.currvideo_on);
			rb_sd.setBackgroundResource(R.drawable.sd_off);
			rb_local.setBackgroundResource(R.drawable.loc_off);
			rb_sett.setBackgroundResource(R.drawable.sett_off);
			break;
		case R.id.tab_rb_b:
			setTitle(getResources().getString(R.string.end_title_recordcard));
			rb_curr.setBackgroundResource(R.drawable.currvideo_off);
			rb_sd.setBackgroundResource(R.drawable.sd_on);
			rb_local.setBackgroundResource(R.drawable.loc_off);
			rb_sett.setBackgroundResource(R.drawable.sett_off);
			break;
		case R.id.tab_rb_c:
			setTitle(getResources().getString(R.string.end_title_localfile));
			rb_curr.setBackgroundResource(R.drawable.currvideo_off);
			rb_sd.setBackgroundResource(R.drawable.sd_off);
			rb_local.setBackgroundResource(R.drawable.loc_on);
			rb_sett.setBackgroundResource(R.drawable.sett_off);
			break;
		case R.id.tab_rb_d:
			setTitle(getResources().getString(R.string.end_title_setting));
			rb_curr.setBackgroundResource(R.drawable.currvideo_off);
			rb_sd.setBackgroundResource(R.drawable.sd_off);
			rb_local.setBackgroundResource(R.drawable.loc_off);
			rb_sett.setBackgroundResource(R.drawable.sett_on);
			break;
		default:
			setTitle(getResources().getString(R.string.app_name));
			rb_curr.setBackgroundResource(R.drawable.currvideo_on);
			rb_sd.setBackgroundResource(R.drawable.sd_off);
			rb_local.setBackgroundResource(R.drawable.loc_off);
			rb_sett.setBackgroundResource(R.drawable.sett_off);
			break;
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		// if(isDoubleBack){
		// isDoubleBack = false;
		// Toast.makeText(this,"ÔÙµã»÷ÍË³ö", Toast.LENGTH_SHORT).show();
		// }

	}
}
