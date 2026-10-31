package org.luaj.vm2.lib.jse;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.luaj.vm2.compat.JavaCompat;

public class JavaCompatJSE extends JavaCompat {
	public long doubleToRawLongBits(double x) {
		return Double.doubleToRawLongBits(x);
	}
	
	// Lua's number format, C's "%.14g": 14 significant digits, rounded half-even from the exact value.
	private static final MathContext DIGITS14 = new MathContext(14, RoundingMode.HALF_EVEN);
	
	// Partially improve the C Lua output mismatch situation on JSE platforms. -Dmitri Poterianski
	public String doubleToString(double x) {
		// whole numbers below 10^14, and in this range the text Java writes, as plain decimal notation
		// like %g, when it has at most 14 digits, need no rounding
		double ax = Math.abs(x);
		if (ax < 1e14 && ax == (long) ax)
			return Long.toString((long) x);
		if (ax >= 1e-3 && ax < 1e7) {
			String s = Double.toString(x);
			int digits = 0;
			boolean leading = true;
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (c < '0' || c > '9' || (leading && c == '0'))
					continue;
				leading = false;
				digits++;
			}
			if (digits <= 14)
				return s;
		}

		BigDecimal d = new BigDecimal(x).round(DIGITS14);
		int exp = d.precision() - d.scale() - 1;
		if (exp < -4 || exp >= 14) {
			String digits = d.unscaledValue().abs().toString();
			int end = digits.length();
			while (end > 1 && digits.charAt(end - 1) == '0')
				end--;
			StringBuffer buf = new StringBuffer(24);
			if (x < 0)
				buf.append('-');
			buf.append(digits.charAt(0));
			if (end > 1)
				buf.append('.').append(digits.substring(1, end));
			buf.append(exp < 0? "e-": "e+");
			int abs = Math.abs(exp);
			if (abs < 10)
				buf.append('0');
			return buf.append(abs).toString();
		}
		return d.stripTrailingZeros().toPlainString();
	}
}
