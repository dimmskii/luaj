package org.luaj.vm2.lib.jse;

import org.luaj.vm2.compat.JavaCompat;

public class JavaCompatJSE extends JavaCompat {
	public long doubleToRawLongBits(double x) {
		return Double.doubleToRawLongBits(x);
	}
	
	// Partially improves C Lua output mismatch by overriding original Float.toString with a cast on JSE platforms. -Dmitri Poterianski
	public String doubleToString(double x) {
		return Double.toString(x);
	}
}
