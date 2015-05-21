/*
 * @(#)Text.java
 *
 * $Date: 2012-07-03 01:10:05 -0500 (Tue, 03 Jul 2012) $
 *
 * Copyright (c) 2011 by Jeremy Wood.
 * All rights reserved.
 *
 * The copyright of this software is owned by Jeremy Wood. 
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * Jeremy Wood. For details see accompanying license terms.
 * 
 * This software is probably, but not necessarily, discussed here:
 * http://javagraphics.java.net/
 * 
 * That site should also contain the most recent official version
 * of this software.  (See the SVN repository for more details.)
 */
package com.bric.util;

import java.util.Vector;

/** Static methods related to <code>Strings</code> and text. */
public class Text {

	public static boolean isWhiteSpace(String s) {
		for(int a = 0; a<s.length(); a++) {
			if(Character.isWhitespace(s.charAt(a))==false)
				return false;
		}
		return true;
	}
	
	public static String[] getParagraphs(String s) {
		int index = 0;
		Vector<String> list = new Vector<String>();
		while(index<s.length()) {
			int i1 = s.indexOf('\n',index);
			int i2 = s.indexOf('\r',index);
			int i;
			if(i1==-1 && i2!=-1) {
				i = i2;
			} else if(i1!=-1 && i2==-1) {
				i = i1;
			} else {
				i = Math.min(i1,i2);
			}
			if(i==-1) {
				list.add(s.substring(index));
				index = s.length();
			} else {
				list.add(s.substring(index,i));
				i++;
				index = i;
			}
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * This replaces all the occurrences of one substring with another.
	 * <P>
	 * A comparable method is built into the <code>String</code> class in Java
	 * 1.5, but this method is provided to maintain compatibility with Java 1.4.
	 */
	public static String replace(String text, String searchFor,
			String replaceWith) {
		if(text==null)
			throw new NullPointerException();
		if(searchFor==null)
			throw new NullPointerException();
		if(replaceWith==null)
			throw new NullPointerException();
		if(searchFor.equals(replaceWith))
			return text;
		
		int i;
		while ((i = text.indexOf(searchFor)) != -1) {
			text = text.substring(0, i) + replaceWith
					+ text.substring(i + searchFor.length());
		}
		return text;
	}
}
