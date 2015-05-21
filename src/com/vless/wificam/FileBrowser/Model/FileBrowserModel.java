package com.vless.wificam.FileBrowser.Model ;

import java.io.StringWriter ;
import java.util.List ;

import javax.xml.transform.Transformer ;
import javax.xml.transform.TransformerException ;
import javax.xml.transform.TransformerFactory ;
import javax.xml.transform.dom.DOMSource ;
import javax.xml.transform.stream.StreamResult ;

import org.w3c.dom.Document ;
import org.w3c.dom.Element ;
import org.w3c.dom.Node ;
import org.w3c.dom.NodeList ;

import com.vless.wificam.FileBrowser.Model.FileViewerElement.DirectoryElement;


import android.util.Log ;

public class FileBrowserModel {

	public static class ModelException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5682496499257041212L ;


	}
	
	public static int parseDirectoryModel(Document document, String directory, List<FileNode> fileList)
			throws ModelException {

		Element element = document.getDocumentElement() ;

		if (element.getNodeName().equalsIgnoreCase(directory)) {

			return parseDirectory(element, fileList) ;

		} else {

			throw new ModelException() ;
		}
	}

	private static int parseDirectory(Node node, List<FileNode> fileList) {

		NodeList children = node.getChildNodes() ;
		
		int amount = 0 ;
		
		for (int i = 0 ; i < children.getLength() ; i++) {
			Node child = children.item(i) ;

			if (child.getNodeType() != Node.ELEMENT_NODE)
				continue ;

			if (DirectoryElement.file.matchElement(child)) {
				FileNode file ;
				try {
					file = new FileNode(child) ;
					fileList.add(file) ;
				} catch (ModelException e) {
					e.printStackTrace();
				}
			} else if (DirectoryElement.amount.matchElement(child)) {
				
				amount = Integer.valueOf(child.getTextContent()) ;
			}
		}
		return amount ;
	}

	public static void printDocument(Document document) {

		DOMSource domSource = new DOMSource(document) ;
		StringWriter writer = new StringWriter() ;
		StreamResult result = new StreamResult(writer) ;
		TransformerFactory tf = TransformerFactory.newInstance() ;
		Transformer transformer ;
		try {
			transformer = tf.newTransformer() ;
			transformer.transform(domSource, result) ;
			Log.i("FileBrowserModel", writer.toString()) ;

		} catch (TransformerException e) {
			e.printStackTrace() ;
		}
	}

	public static <E extends Enum<E>> E strToEnum(Class<E> enumType, String value) throws ModelException {

		value = value.trim() ;

		for (E enumVal : enumType.getEnumConstants()) {
			if (enumVal.toString().equalsIgnoreCase(value)) {
				return enumVal ;
			}
		}
		throw new ModelException() ;
	}

}
