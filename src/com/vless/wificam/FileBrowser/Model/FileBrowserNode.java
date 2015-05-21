package com.vless.wificam.FileBrowser.Model;

import org.w3c.dom.Node ;

import com.vless.wificam.FileBrowser.Model.FileBrowserModel.ModelException;


public abstract class FileBrowserNode {

	protected FileBrowserNode(Node node) throws ModelException {
		if (node != null)
			parseNode(node) ;
	}
	
	

	protected abstract void parseNode(Node node) throws ModelException ;
}