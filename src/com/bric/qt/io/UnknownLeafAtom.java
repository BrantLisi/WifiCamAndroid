/*
 * @(#)UnknownLeafAtom.java
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
package com.bric.qt.io;

import java.io.IOException;

import com.bric.io.GuardedInputStream;
import com.bric.io.GuardedOutputStream;

public class UnknownLeafAtom extends LeafAtom {
	byte[] data;
	String id;
	
	public UnknownLeafAtom(String id,byte[] data) {
		super(null);
		this.id = id;
		this.data = data;
	}
	
	public UnknownLeafAtom(Atom parent,String id,GuardedInputStream in) throws IOException {
		super(parent);
		this.id = id;
		data = new byte[(int)in.getRemainingLimit()];
		read(in,data);
	}
	
	@Override
	protected String getIdentifier() {
		return id;
	}

	@Override
	protected long getSize() {
		return 8+data.length;
	}

	@Override
	protected void writeContents(GuardedOutputStream out) throws IOException {
		out.write(data);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int a = 0; a<Math.min(data.length,64); a++) {
			sb.append( (char)data[a] );
		}
		if(data.length>64)
			sb.append("...");
		return "UnknownLeafAtom[ \""+sb.toString()+"\" ]";
	}
}
