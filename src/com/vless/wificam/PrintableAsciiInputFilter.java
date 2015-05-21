package com.vless.wificam;

import android.text.InputFilter ;
import android.text.SpannableStringBuilder ;
import android.text.Spanned ;

public class PrintableAsciiInputFilter implements InputFilter {
	@Override
	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart,
			int dend) {

		if (source instanceof SpannableStringBuilder) {
			SpannableStringBuilder sourceAsSpannableBuilder = (SpannableStringBuilder) source ;
			for (int i = end - 1 ; i >= start ; i--) {
				char currentChar = source.charAt(i) ;
				int ascii = currentChar ;
				if (ascii > 127) {
					sourceAsSpannableBuilder.delete(i, i + 1) ;
				}
			}
			return source ;
		} else {
			StringBuilder filteredStringBuilder = new StringBuilder() ;
			for (int i = 0 ; i < end ; i++) {
				char currentChar = source.charAt(i) ;
				int ascii = currentChar ;
				if (ascii <= 127) {
					filteredStringBuilder.append(currentChar) ;
				}
			}
			return filteredStringBuilder.toString() ;
		}
	}
}
