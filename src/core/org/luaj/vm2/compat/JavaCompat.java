package org.luaj.vm2.compat;

import org.luaj.vm2.LuaDouble;

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
	public String doubleToString(double v) {
		if ( v == 0.0 ) // never occurs on J2ME
			return (doubleToRawLongBits(v)<0? "-0": "0");
		long l = (long) v;
		if ( l == v )
			return Long.toString(l);
		if ( Double.isNaN(v) )
			return (doubleToRawLongBits(v)<0? LuaDouble.JSTR_NEGNAN: LuaDouble.JSTR_NAN);
		if ( Double.isInfinite(v) ) 
			return (v<0? LuaDouble.JSTR_NEGINF: LuaDouble.JSTR_POSINF);
		return Float.toString((float)v);
	}
}
