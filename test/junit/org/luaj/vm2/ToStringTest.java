/*******************************************************************************
 * @author	Dmitri Poterianski
 ******************************************************************************/
package org.luaj.vm2;

import junit.framework.TestCase;

import org.luaj.vm2.lib.jse.JsePlatform;

/**
 * String conversion of Lua values matches Lua 5.2, where numbers use C's "%.14g".
 * @author	Dmitri Poterianski
 * @since	2026-10-31
 */
public class ToStringTest extends TestCase {

	private final Globals globals = JsePlatform.standardGlobals();

	/** Every way of getting the string of a value that has a fixed text. */
	private void check(String expected, LuaValue v) {
		assertEquals(expected, v.tojstring());
		assertEquals(expected, globals.get("tostring").call(v).tojstring());
		// only numbers and strings convert implicitly
		if (v.isnumber() || v.isstring()) {
			assertEquals(expected, v.tostring().tojstring());
			assertEquals(expected, LuaValue.valueOf("<").concat(v).tojstring().substring(1));
			assertEquals(expected, v.strvalue().tojstring());
			assertEquals(expected, v.checkjstring());
			assertEquals(expected, v.optjstring(null));
		}
	}

	private void check(String expected, double d) {
		check(expected, LuaValue.valueOf(d));
	}

	private void checkPrefix(String prefix, LuaValue v) {
		assertTrue(v.tojstring(), v.tojstring().startsWith(prefix));
		assertEquals(v.tojstring(), globals.get("tostring").call(v).tojstring());
	}

	public void testNilAndBooleans() {
		check("nil", LuaValue.NIL);
		check("true", LuaValue.TRUE);
		check("false", LuaValue.FALSE);
	}

	public void testIntegers() {
		check("0", LuaValue.valueOf(0));
		check("-1", LuaValue.valueOf(-1));
		check("2147483647", LuaValue.valueOf(Integer.MAX_VALUE));
		check("-2147483648", LuaValue.valueOf(Integer.MIN_VALUE));
	}

	public void testStrings() {
		check("", LuaValue.valueOf(""));
		check("abc", LuaValue.valueOf("abc"));
		check("a\0b", LuaValue.valueOf("a\0b"));
	}

	public void testReferenceValues() {
		checkPrefix("table: ", new LuaTable());
		checkPrefix("thread: ", new LuaThread(globals, globals.get("print")));
		checkPrefix("function: ", globals.get("print"));
		checkPrefix("function: ", globals.load("return 1", "x").checkfunction());
		Object o = new Object();
		check(o.toString(), new LuaUserdata(o));
	}

	public void testDoubleFractions() {
		check("0.5", 0.5);
		check("-0.5", -0.5);
		check("0.1", 0.1);
		check("0.0001", 1e-4);
		check("0.33333333333333", 1/3.);
		check("33.333333333333", 100/3.);
		check("3.1415926535898", Math.PI);
		check("123456.789", 123456.789);
		check("12345678901.235", 12345678901.2345);
	}

	public void testDoubleIntegral() {
		check("1", 1.0);
		check("-1", -1.0);
		check("4294967296", 4294967296.0);
		check("99999999999999", 99999999999999.0);
	}

	public void testDoubleExponent() {
		check("1e-05", 1e-5);
		check("1e-300", 1e-300);
		check("4.9406564584125e-324", Double.MIN_VALUE);
		check("1e+14", 1e14);
		check("1e+15", 1e15);
		check("1e+100", 1e100);
		check("-1e+100", -1e100);
		check("1.7976931348623e+308", Double.MAX_VALUE);
		check("9.007199254741e+15", Math.pow(2, 53));
		check("9.2233720368548e+18", Math.pow(2, 63));
		check("1.844674407371e+19", Math.pow(2, 64));
	}

	public void testDoubleSpecialValues() {
		check("0", 0.0);
		check("-0", -0.0);
		check("inf", Double.POSITIVE_INFINITY);
		check("-inf", Double.NEGATIVE_INFINITY);
		assertTrue(LuaValue.valueOf(Double.NaN).tojstring().endsWith("nan"));
	}

	public void testRounding() {
		check("1", 1 + 1e-15);
		check("1.0000000000001", 1 + 1e-13);
		check("0.3", 0.1 + 0.2);
	}
}
