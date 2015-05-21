package com.vless.wificam.FileBrowser ;

import java.io.BufferedReader ;
import java.io.ByteArrayInputStream ;
import java.io.InputStream ;
import java.io.InputStreamReader ;
import java.io.Reader ;
import java.io.StringWriter ;
import java.io.Writer ;
import java.net.HttpURLConnection ;
import java.net.MalformedURLException ;
import java.net.URI ;
import java.net.URISyntaxException ;
import java.net.URL ;
import java.util.ArrayList ;
import java.util.List ;

import javax.xml.parsers.DocumentBuilderFactory ;
import javax.xml.parsers.ParserConfigurationException ;

import org.w3c.dom.Document ;
import org.xml.sax.SAXException ;

import com.vless.wificam.FileBrowser.Model.FileBrowserModel;
import com.vless.wificam.FileBrowser.Model.FileNode;
import com.vless.wificam.FileBrowser.Model.FileBrowserModel.ModelException;
import com.vless.wificam.FileBrowser.Model.FileNode.Format;


import android.util.Log ;

public class FileBrowser {

	public enum Action {
		dir ;
	}

	public static final int COUNT_MAX = 16;
	
	private final URL mUrl ;
	private final int mCount ;
	
	private boolean mCompleted ;
	private List<FileNode> mFileList = new ArrayList<FileNode>() ;

	public FileBrowser(URL url, int count) {

		mUrl = url ;
		mCount = count < 1 ? 1 : (count > COUNT_MAX ? COUNT_MAX : count) ;
		mCompleted = false ;
	}
	
	public boolean isCompleted() {
		
		return mCompleted ;
	}
	
	public List<FileNode> getFileList() {
		
		List<FileNode> fileList = mFileList ;
		mFileList = new ArrayList<FileNode>() ;
		
		return fileList ;
	}
	
	private static String buildQuery(String directory, Format aFormat, int aCount, int aFrom) {
		
		String action = "action=" + Action.dir ;
		String property = "property=" + directory ;
		String format = "format=" + aFormat.name() ;
		String count = "count=" + aCount ;
		String from = "from=" + aFrom;
		
		return action + "&" + property + "&" + format + "&" + count + "&" + from;
	}
	
	private Document sendRequest(URL url) {

		HttpURLConnection urlConnection ;
		Document document;
		document = null;
		try {
			urlConnection = (HttpURLConnection) url.openConnection() ;

			urlConnection.setUseCaches(false) ;
			urlConnection.setDoInput(true) ;

			urlConnection.setConnectTimeout(10000) ;
			urlConnection.setReadTimeout(10000) ;

			urlConnection.connect() ;

			int responseCode = urlConnection.getResponseCode() ;

			Log.i("FileBrowser", "responseCode = " + responseCode) ;

			if (responseCode != HttpURLConnection.HTTP_OK) {

				return null ;
			}

			InputStream inputStream = urlConnection.getInputStream() ;
			
			Writer writer = new StringWriter();
			 
            char[] buffer = new char[1024];
			Log.i("FileBrowser", "new StringWriter") ;
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(inputStream, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
            	inputStream.close();
            }
            String string = writer.toString() ;
            string = string.substring(0, string.lastIndexOf(">") +1);
            InputStream updatedStream = new ByteArrayInputStream(string.getBytes("UTF-8"));
            
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance() ;
			//factory.setNamespaceAware(true) ;
			try {
				document = factory.newDocumentBuilder().parse(updatedStream) ;
				//FileBrowserModel.printDocument(document) ;

			} catch (SAXException e) {
				e.printStackTrace() ;
			} catch (ParserConfigurationException e) {
				e.printStackTrace() ;
			} finally {

				urlConnection.disconnect() ;
			}
			
		} catch (Exception e) {
			e.printStackTrace() ;
		}

		return document ;
	}

	public int retrieveFileList(String directory, Format format, int iFrom) {

		if (mCompleted && iFrom != 0) {
			return -1;
		}
		mCompleted = false ;
		mFileList.clear() ;
		
		String query = buildQuery(directory, format, mCount, iFrom) ; 
		URL url = null ;
		try {
			URI uri = new URI(mUrl.getProtocol(), mUrl.getUserInfo(), mUrl.getHost(), mUrl.getPort(),
					mUrl.getPath(), query, mUrl.getRef()) ;
			url = uri.toURL() ;
			
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (url == null) {
			return iFrom;
		}
		Log.i("FileBrowser", url.toString()) ;
		Document document = sendRequest(url) ;
		if (document == null) {
			return iFrom;
		}
		
		try {
			int amount = FileBrowserModel.parseDirectoryModel(document, directory, mFileList) ;
			
			if (amount != mCount) {
				mCompleted = true ;
			}
		} catch (ModelException e) {
			e.printStackTrace();
		}
		return iFrom + mCount;
	}

}
