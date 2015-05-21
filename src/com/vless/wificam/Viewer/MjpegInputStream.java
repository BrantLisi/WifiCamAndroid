package com.vless.wificam.Viewer ;

import java.io.BufferedInputStream ;
import java.io.ByteArrayInputStream ;
import java.io.DataInputStream ;
import java.io.IOException ;
import java.io.InputStream ;
import java.net.HttpURLConnection ;
import java.net.MalformedURLException ;
import java.net.URL ;
import java.util.Properties ;

import android.graphics.Bitmap ;
import android.graphics.BitmapFactory ;

public class MjpegInputStream extends DataInputStream {
	private final byte[] SOI_MARKER = { (byte) 0xFF, (byte) 0xD8 } ;
	private final byte[] EOF_MARKER = { (byte) 0xFF, (byte) 0xD9 } ;
	private final String CONTENT_LENGTH = "Content-Length" ;
	private final static int HEADER_MAX_LENGTH = 100 ;
	private final static int FRAME_MAX_LENGTH = 1024*40 + HEADER_MAX_LENGTH ;
	private final static int BUFFER_MAX_LENGTH = 1024 * 1024 ;
	private int mContentLength = -1 ;

	private static HttpURLConnection sHttpConn = null ;

	public static MjpegInputStream getInputStream(String url) {

		try {

			if (sHttpConn != null) {

				sHttpConn.disconnect() ;
				sHttpConn = null ;
			}

			sHttpConn = (HttpURLConnection) new URL(url).openConnection() ;

			if (sHttpConn != null) {

				sHttpConn.setUseCaches(false) ;
				sHttpConn.setConnectTimeout(500) ;
				sHttpConn.setReadTimeout(5000) ;
				sHttpConn.connect() ;

				return new MjpegInputStream(sHttpConn.getInputStream()) ;
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace() ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace() ;
		}
		
		if (sHttpConn != null) {

			sHttpConn.disconnect() ;
			sHttpConn = null ;
		}

		return null ;
	}
	
	public static void disconnect() {
		
		if (sHttpConn != null) {

			sHttpConn.disconnect() ;
			sHttpConn = null ;
		}
	}

	private MjpegInputStream(InputStream in) {
		super(new BufferedInputStream(in, BUFFER_MAX_LENGTH)) ;
	}

	private int getEndOfSeqeunce(DataInputStream in, byte[] sequence) throws IOException {
		int seqIndex = 0 ;
		byte c ;
		for (int i = 0 ; i < FRAME_MAX_LENGTH ; i++) {
			c = (byte) in.readUnsignedByte() ;
			if (c == sequence[seqIndex]) {
				seqIndex++ ;
				if (seqIndex == sequence.length)
					return i + 1 ;
			} else
				seqIndex = 0 ;
		}
		return -1 ;
	}

	private int getStartOfSequence(DataInputStream in, byte[] sequence) throws IOException {
		int end = getEndOfSeqeunce(in, sequence) ;
		return (end < 0) ? (-1) : (end - sequence.length) ;
	}

	private int parseContentLength(byte[] headerBytes) throws IOException, NumberFormatException {
		
		String[] headers = new String(headerBytes).split("\n") ;
		
		for (String header : headers) {
			
			if (header.startsWith(CONTENT_LENGTH)) {
				
				String property[] = header.split(":") ;
				
				if (property.length == 2) {
					
					return Integer.parseInt(property[1].trim()) ;
				}
			}
			
		}

		ByteArrayInputStream headerIn = new ByteArrayInputStream(headerBytes) ;
		Properties props = new Properties() ;
		props.load(headerIn) ;
		return Integer.parseInt(props.getProperty(CONTENT_LENGTH)) ;
	}

	public Bitmap readMjpegFrame() throws IOException {
		mark(FRAME_MAX_LENGTH) ;
		int headerLen = getStartOfSequence(this, SOI_MARKER) ;
		reset() ;
		byte[] header = new byte[headerLen] ;
		readFully(header) ;
		try {
			mContentLength = parseContentLength(header) ;
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace() ;
			mContentLength = getEndOfSeqeunce(this, EOF_MARKER) ;
		}
		reset() ;
		
		if (mContentLength < 0)
			return null ;
		
		byte[] frameData = new byte[mContentLength] ;
		skipBytes(headerLen) ;
		readFully(frameData) ;
		return BitmapFactory.decodeStream(new ByteArrayInputStream(frameData)) ;
	}

	public byte[] readRawMjpegFrame() throws IOException {

		mark(FRAME_MAX_LENGTH) ;
		int headerLen = getStartOfSequence(this, SOI_MARKER) ;
		reset() ;
		byte[] header = new byte[headerLen] ;
		readFully(header) ;
		try {
			mContentLength = parseContentLength(header) ;
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace() ;
			mContentLength = getEndOfSeqeunce(this, EOF_MARKER) ;
		}
		reset() ;
		
		if (mContentLength < 0)
			return null ;
		
		byte[] frameData = new byte[mContentLength] ;
		skipBytes(headerLen) ;
		readFully(frameData) ;

		return frameData ;
	}
}