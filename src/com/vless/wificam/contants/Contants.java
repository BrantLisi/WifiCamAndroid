package com.vless.wificam.contants;

public interface Contants {
	public static int SCONNECTIONDELAY = 2000;

	// public static String VIDEO_RESOLUTION_HIGH =
	// "http://192.72.1.1/cgi-bin/Config.cgi?action=set&property=Videores&value=1080P30fps&property=Imageres&value=14M";
	// public static String VIDEO_RESOLUTION_MIDDLE =
	// "http://192.72.1.1/cgi-bin/Config.cgi?action=set&property=Videores&value=720P30fps&property=Imageres&value=5M";
	// public static String VIDEO_RESOLUTION_LOW =
	// "http://192.72.1.1/cgi-bin/Config.cgi?action=set&property=Videores&value=VGA&property=Imageres&value=1.2M";

	public static String VIDEO_RESOLUTION_HIGH = "http://192.72.1.1/cgi-bin/Config.cgi?action=set&property=Videores&value=1080P30fps";
	public static String VIDEO_RESOLUTION_MIDDLE = "http://192.72.1.1/cgi-bin/Config.cgi?action=set&property=Videores&value=720P30fps";
	public static String VIDEO_RESOLUTION_LOW = "http://192.72.1.1/cgi-bin/Config.cgi?action=set&property=Videores&value=VGA";

	public static String MOTION_SENSE_OFF = "http://192.72.1.1/cgi-bin/Config.cgi?action=set&property=MTD&value=Off";
	public static String MOTION_SENSE_ON = "http://192.72.1.1/cgi-bin/Config.cgi?action=set&property=MTD&value=Low";

	public static String VOICE_RECORD_ON = "http://192.72.1.1/cgi-bin/Config.cgi?action=set&property=Flicker&value=50Hz";
	public static String VOICE_RECORD_OFF = "http://192.72.1.1/cgi-bin/Config.cgi?action=set&property=Flicker&value=60Hz";

	public static String WIFI_SD_FORMARTTING = "http://192.72.1.1/cgi-bin/Config.cgi?action=set&property=Videores&value=720P60fps";
}
