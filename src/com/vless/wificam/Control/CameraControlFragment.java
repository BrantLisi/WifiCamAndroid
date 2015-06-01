package com.vless.wificam.Control;

import com.vless.wificam.CamSettActivity;
import com.vless.wificam.NetSettActivity;
import com.vless.wificam.R;
import com.vless.wificam.frags.WifiCamFragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CameraControlFragment extends WifiCamFragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.camera_control, container, false);
		TextView tvHeaderTitle = (TextView) view.findViewById(R.id.frag_header)
				.findViewById(R.id.header_title);
		tvHeaderTitle.setText(R.string.end_title_setting);
		OnTouchListener onTouch = new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					v.setBackgroundResource(R.drawable.selected_background);

				} else if (event.getAction() == MotionEvent.ACTION_UP
						|| event.getAction() == MotionEvent.ACTION_CANCEL) {
					v.setBackgroundResource(R.drawable.group_background);
				}
				return false;
			}
		};

		LinearLayout networkConfigurations = (LinearLayout) view
				.findViewById(R.id.cameraControlNetworkConfigurations);

		networkConfigurations.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(),NetSettActivity.class));
			}
		});

		networkConfigurations.setOnTouchListener(onTouch);

		LinearLayout cameraSettings = (LinearLayout) view
				.findViewById(R.id.cameraControlCameraSettings);

		cameraSettings.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(),CamSettActivity.class));
			}
		});

		cameraSettings.setOnTouchListener(onTouch);

		return view;
	}

}
