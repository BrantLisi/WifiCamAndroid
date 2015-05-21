/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vless.wificam;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import com.vless.wificam.CameraPeeker;

import android.util.Log;
import android.app.Fragment ;

/**
 * Implements some simple tests for datagrams. Not as excessive as the core
 * tests, but good enough for the harness.
 */
public class CameraSniffer extends Thread {
	private final String	listenIP = "0.0.0.0";
	private final int		port     = 49142;
    private static DatagramSocket socket = null;
    private static byte[] buffer = new byte[4096];
    private static DatagramPacket packet;
    private CameraPeeker thePeeker = null;
    private Fragment theFrag;
    private static int ticket;
    private static int utime;
    public static boolean alive = true;
	
	public CameraSniffer() {
		if (socket != null) return;
        try {
            packet = new DatagramPacket(buffer, buffer.length);
            socket = new DatagramSocket(port, InetAddress.getByName(listenIP));
            socket.setBroadcast(true);
            socket.setSoTimeout(500);
        	Log.i("CAMERASNIFFER", "===Create DatagramSocket Successfully!");
        } catch (IOException ex) {
        	alive = false;
        	Log.i("CAMERASNIFFER", "===Create DatagramSocket Failed!");
//            throw new RuntimeException(
//                    "Creating datagram reflector failed", ex);
        }
	}
    public CameraSniffer(int port, InetAddress address) {
		if (socket != null) return;
        try {
            packet = new DatagramPacket(buffer, buffer.length);
            socket = new DatagramSocket(port, address);
            socket.setBroadcast(true);
            socket.setSoTimeout(500);
        } catch (IOException ex) {
        	alive = false;
//            throw new RuntimeException(
//                    "Creating datagram reflector failed", ex);
        }
    }

	public void finalize() throws Throwable{
			alive = false;
			if (socket != null) {
				socket.close();
			}
			socket = null;
	}
	public void SetPeeker(CameraPeeker peeker) {
		thePeeker = peeker;
	}
    public void Update(String s) {
    	//Log.i("CAMERASNIFFER", s.toString());
    	if (thePeeker != null) {
    		thePeeker.Update(s);
    	}
    }
	private int VerifyData(String s)
	{
		String info[];
		int		sum, i;
		char[]	achar;

		try {
			info = s.split("CHKSUM=");
			sum = Integer.parseInt(info[1]);
			achar = info[0].toCharArray();
			for (i = 0; i < achar.length; i++)
				sum -= achar[i];
		} catch (Exception e) {
			return 1;
		}
		return sum;
	}
	// Get UI Mode [VIDEO|CAMERA|BURST|TIMELAPSE|SETTING]
	public static String GetUIMode(String s)
	{
		String[]	info;
		try {
			info = s.split("UIMode=");
			return info[1].split("\n")[0];
		} catch (Exception e) {
		}
		return null;
	}
	// Get Video Resolution [4K15|2.7K30|1080P60|1080P50...]
	public static String GetVideoRes(String s)
	{
		String[]	info;
		try {
			info = s.split("Videores=");
			return info[1].split("\n")[0];
		} catch (Exception e) {
		}
		return null;
	}
	// Get Image Resolution [8MP|6MPW|6MP]
	public static String GetImageRes(String s)
	{
		String[]	info;
		try {
			info = s.split("Imageres=");
			return info[1].split("\n")[0];
		} catch (Exception e) {
		}
		return null;
	}
	// Get White Balance [AUTO|DAYLIGHT|CLOUDY|...]
	public static String GetWhiteBalance(String s)
	{
		String[]	info;
		try {
			info = s.split("AWB=");
			return info[1].split("\n")[0];
		} catch (Exception e) {
		}
		return null;
	}
	// Get Flicker [50Hz|60Hz]
	public static String GetFlicker(String s)
	{
		String[]	info;
		try {
			info = s.split("Flicker=");
			return info[1].split("\n")[0];
		} catch (Exception e) {
		}
		return null;
	}
	// Get EV [EVN200|EVN167|..|EV0|EVP033|...|EVP200]
	public static String GetEV(String s)
	{
		String[]	info;
		try {
			info = s.split("EV=");
			return info[1].split("\n")[0];
		} catch (Exception e) {
		}
		return null;
	}
	// Get Recording [NO|YES]
	public static String GetRecording(String s)
	{
		String[]	info;
		try {
			info = s.split("Recording=");
			return info[1].split("\n")[0];
		} catch (Exception e) {
		}
		return null;
	}
	// Get Streaming [NO|YES]
	public static String GetStreaming(String s)
	{
		String[]	info;
		try {
			info = s.split("Streaming=");
			return info[1].split("\n")[0];
		} catch (Exception e) {
		}
		return null;
	}
	enum INFO_STATUS { OLD, NEW, BAD };
	private INFO_STATUS CheckTicketTime(String s)
	{
		// get ticket
		String[] info;
		try {
			int		newticket;
			int		newtime;
			
			info = s.split("ticket=");
			newticket = Integer.parseInt(info[1].split("\n")[0]);
			info = s.split("time=");
			newtime   = Integer.parseInt(info[1].split("\n")[0]);
			if (ticket != newticket || (ticket == newticket && utime != newtime)) {
				ticket = newticket;
				utime  = newtime;
				return INFO_STATUS.NEW;
			}
			return INFO_STATUS.OLD;
		} catch (Exception e) {
			return INFO_STATUS.BAD;
		}
	}
    /**
     * Main loop. Receives datagrams and reflects them.
     */
    @Override
    public void run() {
    	ticket = -1;
    	utime  = -1;
        try {
            while (alive) {
                try {
                    packet.setLength(buffer.length);
                    socket.receive(packet);
                    String s = stringFromPacket(packet);
                    Log.i("SNIFFER", "== GET DATA");
                    if (VerifyData(s) == 0) {
                    	if (CheckTicketTime(s) == INFO_STATUS.NEW) {
                            Log.i("SNIFFER", "== UPDATE");
                    		Update(s);
                    	} else {
                            Log.i("SNIFFER", "== OLD");
                    	}
                    } else {
            			Log.i("VERIFY", "Check sum Error or Data Lost!!!");
                    }
                } catch (java.io.InterruptedIOException e) {
                }
            }
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        } finally {
        	Log.i("SNIFFER", "==CLOSE SOCKET");
        	if (socket != null)
        		socket.close();
        	socket = null;
        }
    }

    /**
     * Converts a given datagram packet's contents to a String.
     */
    static String stringFromPacket(DatagramPacket packet) {
        return new String(packet.getData(), 0, packet.getLength());
    }

    /**
     * Converts a given String into a datagram packet.
     */
    static void stringToPacket(String s, DatagramPacket packet) {
        byte[] bytes = s.getBytes();
        System.arraycopy(bytes, 0, packet.getData(), 0, bytes.length);
        packet.setLength(bytes.length);
    }
}