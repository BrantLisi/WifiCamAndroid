package com.vless.wificam.services;

import java.util.List;
import java.util.UUID;

import com.vless.wificam.impls.SetOnDiscoveredDeviceListener;
import com.vless.wificam.impls.SetOnFragListener;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class MyService extends Service {

	private final static String TAG = "MyService";

	private BluetoothGatt connectedDevice;
	private String localAddresses;

	public void connect(String address) {
		
	}

	public void startLeScan() {
	}

	public void stopLeScan() {
	}


	
	private SetOnFragListener onFragListener;
	public void setFragComunicat(SetOnFragListener onFragListener){
		this.onFragListener = onFragListener;
	}


	/**
	 * 对蓝牙发送数据
	 * 
	 * @param gatt
	 * @param hex
	 */
	public void sendData(String hex) {
	}

	public class MyBinder extends Binder {
		public MyService getMyService() {
			Log.i(TAG, "-----MyService-----getMyService------");
			return MyService.this;
		}
	}

	public BluetoothGatt getConnectedGatts() {
		return connectedDevice;
	}

	public void setConnectedGatts(BluetoothGatt connectedGatts) {
		this.connectedDevice = connectedGatts;
	}

	public String getLocalAddresses() {
		return localAddresses;
	}

	public void setLocalAddresses(String localAddresses) {
		this.localAddresses = localAddresses;
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "-----MyService-----onBind------");
		return new MyBinder();
	}

	@Override
	public void onCreate() {
		
		super.onCreate();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "-----MyService-----onUnbind------");
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "-----MyService-----onDestroy------");
		super.onDestroy();
	}

}
