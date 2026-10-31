package org.luaj.vm2.compat;

public class JavaCompat {
	public static final JavaCompat INSTANCE;

	static {
		JavaCompat instance;
		try {
			instance = (JavaCompat) Class.forName("org.luaj.vm2.lib.jse.JavaCompatJSE").newInstance();
		} catch (Throwable t) {
			instance = new JavaCompat();
		}
		INSTANCE = instance;
	}

	public long doubleToRawLongBits(double x) {
		return Double.doubleToLongBits(x);
	}
	
	// Original tail call (together with wholes shortcut) of LuaDouble.tojstring() meant to be overridden by JSE version for "%.14g" fix -Dmitri Poterianski
	public String doubleToString(double x) {
		long l = (long) x;
		if (l == x)
			return Long.toString(l);
		return Float.toString((float) x);
	}
}
