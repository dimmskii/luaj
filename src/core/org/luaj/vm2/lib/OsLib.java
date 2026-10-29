/*******************************************************************************
* Copyright (c) 2009 Luaj.org. All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
******************************************************************************/
package org.luaj.vm2.lib;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.luaj.vm2.Buffer;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

/**
 * Subclass of {@link LibFunction} which implements the standard lua {@code os} library.
 * <p>
 * It is a usable base with simplified stub functions
 * for library functions that cannot be implemented uniformly
 * on Jse and Jme.
 * <p>
 * This can be installed as-is on either platform, or extended
 * and refined to be used in a complete Jse implementation.
 * <p>
 * Because the nature of the {@code os} library is to encapsulate
 * os-specific features, the behavior of these functions varies considerably
 * from their counterparts in the C platform.
 * <p>
 * The following functions have limited implementations of features
 * that are not supported well on Jme:
 * <ul>
 * <li>{@code execute()}</li>
 * <li>{@code remove()}</li>
 * <li>{@code rename()}</li>
 * <li>{@code tmpname()}</li>
 * </ul>
 * <p>
 * Typically, this library is included as part of a call to either
 * {@link org.luaj.vm2.lib.jse.JsePlatform#standardGlobals()} or {@link org.luaj.vm2.lib.jme.JmePlatform#standardGlobals()}
 * <pre> {@code
 * Globals globals = JsePlatform.standardGlobals();
 * System.out.println( globals.get("os").get("time").call() );
 * } </pre>
 * In this example the platform-specific {@link org.luaj.vm2.lib.jse.JseOsLib} library will be loaded, which will include
 * the base functionality provided by this class.
 * <p>
 * To instantiate and use it directly,
 * link it into your globals table via {@link LuaValue#load(LuaValue)} using code such as:
 * <pre> {@code
 * Globals globals = new Globals();
 * globals.load(new JseBaseLib());
 * globals.load(new PackageLib());
 * globals.load(new OsLib());
 * System.out.println( globals.get("os").get("time").call() );
 * } </pre>
 * <p>
  * @see LibFunction
 * @see org.luaj.vm2.lib.jse.JseOsLib
 * @see org.luaj.vm2.lib.jse.JsePlatform
 * @see org.luaj.vm2.lib.jme.JmePlatform
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.8">http://www.lua.org/manual/5.1/manual.html#5.8</a>
 */
public class OsLib extends TwoArgFunction {
	public static final String TMP_PREFIX    = ".luaj";
	public static final String TMP_SUFFIX    = "tmp";

	private static final int CLOCK     = 0;
	private static final int DATE      = 1;
	private static final int DIFFTIME  = 2;
	private static final int EXECUTE   = 3;
	private static final int EXIT      = 4;
	private static final int GETENV    = 5;
	private static final int REMOVE    = 6;
	private static final int RENAME    = 7;
	private static final int SETLOCALE = 8;
	private static final int TIME      = 9;
	private static final int TMPNAME   = 10;

	private static final String[] NAMES = {
		"clock",
		"date",
		"difftime",
		"execute",
		"exit",
		"getenv",
		"remove",
		"rename",
		"setlocale",
		"time",
		"tmpname",
	};
	
	private static final long t0 = System.currentTimeMillis();
	private static long tmpnames = t0;

	protected Globals globals;
	
	/**
	 * Create and OsLib instance.
	 */
	public OsLib() {
	}
	
	/** Perform one-time initialization on the library by creating a table
	 * containing the library functions, adding that table to the supplied environment,
	 * adding the table to package.loaded, and returning table as the return value.
	 * @param modname the module name supplied if this is loaded via 'require'.
	 * @param env the environment to load into, typically a Globals instance.
	 */
	public LuaValue call(LuaValue modname, LuaValue env) {
		globals = env.checkglobals();
		LuaTable os = new LuaTable();
		for (int i = 0; i < NAMES.length; ++i)
			os.set(NAMES[i], new OsLibFunc(i, NAMES[i]));
		env.set("os", os);
		if (!env.get("package").isnil()) env.get("package").get("loaded").set("os", os);
		return os;
	}

	class OsLibFunc extends VarArgFunction {
		public OsLibFunc(int opcode, String name) {
			this.opcode = opcode;
			this.name = name;
		}
		public Varargs invoke(Varargs args) {
			try {
				switch ( opcode ) {
				case CLOCK:
					return valueOf(clock());
				case DATE: {
					String s = args.optjstring(1, "%c");
					double t = args.isnumber(2)? args.todouble(2): time(null);
					if (s.equals("*t") || s.equals("!*t")) {
						Calendar d = calendar(t, s.startsWith("!"));
						LuaTable tbl = LuaValue.tableOf();
						tbl.set("year", LuaValue.valueOf(d.get(Calendar.YEAR)));
						tbl.set("month", LuaValue.valueOf(d.get(Calendar.MONTH)+1));
						tbl.set("day", LuaValue.valueOf(d.get(Calendar.DAY_OF_MONTH)));
						tbl.set("hour", LuaValue.valueOf(d.get(Calendar.HOUR_OF_DAY)));
						tbl.set("min", LuaValue.valueOf(d.get(Calendar.MINUTE)));
						tbl.set("sec", LuaValue.valueOf(d.get(Calendar.SECOND)));
						tbl.set("wday", LuaValue.valueOf(d.get(Calendar.DAY_OF_WEEK)));
						tbl.set("yday", LuaValue.valueOf(dayOfYear(d)+1));
						tbl.set("isdst", LuaValue.valueOf(isDaylightSavingsTime(d)));
						return tbl;
					}
					return valueOf( date(s, t==-1? time(null): t) );
				}
				case DIFFTIME:
					return valueOf(difftime(args.checkdouble(1),args.checkdouble(2)));
				case EXECUTE:
					return execute(args.optjstring(1, null));
				case EXIT:
					exit(args.optint(1, 0));
					return NONE;
				case GETENV: {
					final String val = getenv(args.checkjstring(1));
					return val!=null? valueOf(val): NIL;
				}
				case REMOVE:
					remove(args.checkjstring(1));
					return LuaValue.TRUE;
				case RENAME:
					rename(args.checkjstring(1), args.checkjstring(2));
					return LuaValue.TRUE;
				case SETLOCALE: {
					String s = setlocale(args.optjstring(1,null), args.optjstring(2, "all"));
					return s!=null? valueOf(s): NIL;
				}
				case TIME:
					return valueOf(time(args.opttable(1, null)));
				case TMPNAME:
					return valueOf(tmpname());
				}
				return NONE;
			} catch ( IOException e ) {
				return varargsOf(NIL, valueOf(e.getMessage()));
			}
		}
	}

	/**
	 * @return an approximation of the amount in seconds of CPU time used by
	 * the program.  For luaj this simple returns the elapsed time since the
	 * OsLib class was loaded.
	 */
	protected double clock() {
		return (System.currentTimeMillis()-t0) / 1000.;
	}

	/**
	 * Returns the number of seconds from time t1 to time t2.
	 * In POSIX, Windows, and some other systems, this value is exactly t2-t1.
	 * @param t2
	 * @param t1
	 * @return diffeence in time values, in seconds
	 */
	protected double difftime(double t2, double t1) {
		return t2 - t1;
	}

	/**
	 * If the time argument is present, this is the time to be formatted
	 * (see the os.time function for a description of this value).
	 * Otherwise, date formats the current time.
	 * 
	 * Date returns the date as a string,
	 * formatted according to the same rules as the C99 strftime
	 * in the C locale, with the conversions and modifiers that Lua 5.2 accepts.
	 * 
	 * When called without arguments, date returns a reasonable date and
	 * time representation that depends on the host system and on the
	 * current locale (that is, os.date() is equivalent to os.date("%c")).
	 * 
	 * @param format
	 * @param time time since epoch, or -1 if not supplied
	 * @return a LString or a LTable containing date and time,
	 * formatted according to the given string format.
	 */
	public String date(String format, double time) {
		boolean utc = format.startsWith("!");
		if (utc)
			format = format.substring(1);
		return date(format, calendar(time, utc));
	}

	private String date(String format, Calendar d) {
		byte[] fmt = format.getBytes();
		final int n = fmt.length;
		Buffer result = new Buffer(n);
		byte c;
		for ( int i = 0; i < n; ) {
			switch ( c = fmt[i++ ] ) {
			case '\n':
				result.append( "\n" );
				break;
			default:
				result.append( c );
				break;
			case '%':
				final int conversion = i;
				c = i < n? fmt[i++]: 0;
				if ((c == 'E' && i < n && "cCxXyY".indexOf(fmt[i]) >= 0)
						|| (c == 'O' && i < n && "deHImMSuUVwWy".indexOf(fmt[i]) >= 0))
					c = fmt[i++]; // the C locale has no alternative representations
				else if (c == 'E' || c == 'O')
					c = 0;
				int wday = d.get(Calendar.DAY_OF_WEEK)-1;
				switch ( c ) {
				default:
					LuaValue.argerror(1, "invalid conversion specifier '%"+new String(fmt, conversion, n-conversion)+"'");
					break;
				case '%':
					result.append( (byte)'%' );
					break;
				case 'a':
					result.append(WeekdayNameAbbrev[wday]);
					break;
				case 'A':
					result.append(WeekdayName[wday]);
					break;
				case 'b':
				case 'h':
					result.append(MonthNameAbbrev[d.get(Calendar.MONTH)]);
					break;
				case 'B':
					result.append(MonthName[d.get(Calendar.MONTH)]);
					break;
				case 'c':
					result.append(date("%a %b %e %H:%M:%S %Y", d));
					break;
				case 'C':
					result.append(String.valueOf(d.get(Calendar.YEAR)/100));
					break;
				case 'd':
					result.append(String.valueOf(100+d.get(Calendar.DAY_OF_MONTH)).substring(1));
					break;
				case 'D':
				case 'x':
					result.append(date("%m/%d/%y", d));
					break;
				case 'e': {
					int day = d.get(Calendar.DAY_OF_MONTH);
					result.append((day < 10? " ": "") + day);
					break;
				}
				case 'F':
					result.append(date("%Y-%m-%d", d));
					break;
				case 'g':
					result.append(String.valueOf(100+isoWeek(d)[0]%100).substring(1));
					break;
				case 'G':
					result.append(String.valueOf(isoWeek(d)[0]));
					break;
				case 'H':
					result.append(String.valueOf(100+d.get(Calendar.HOUR_OF_DAY)).substring(1));
					break;
				case 'I':
					result.append(String.valueOf(100+((d.get(Calendar.HOUR_OF_DAY)+11)%12+1)).substring(1));
					break;
				case 'j':
					result.append(String.valueOf(1001+dayOfYear(d)).substring(1));
					break;
				case 'm':
					result.append(String.valueOf(101+d.get(Calendar.MONTH)).substring(1));
					break;
				case 'M':
					result.append(String.valueOf(100+d.get(Calendar.MINUTE)).substring(1));
					break;
				case 'n':
					result.append( "\n" );
					break;
				case 'p':
					result.append(d.get(Calendar.HOUR_OF_DAY) < 12? "AM": "PM");
					break;
				case 'r':
					result.append(date("%I:%M:%S %p", d));
					break;
				case 'R':
					result.append(date("%H:%M", d));
					break;
				case 'S':
					result.append(String.valueOf(100+d.get(Calendar.SECOND)).substring(1));
					break;
				case 't':
					result.append( "\t" );
					break;
				case 'T':
				case 'X':
					result.append(date("%H:%M:%S", d));
					break;
				case 'u':
					result.append(String.valueOf((wday+6)%7+1));
					break;
				case 'U': // weeks start on Sunday, days before the first Sunday are in week 0
					result.append(String.valueOf(100+(dayOfYear(d)+7-wday)/7).substring(1));
					break;
				case 'V':
					result.append(String.valueOf(100+isoWeek(d)[1]).substring(1));
					break;
				case 'w':
					result.append(String.valueOf(wday));
					break;
				case 'W': // weeks start on Monday, days before the first Monday are in week 0
					result.append(String.valueOf(100+(dayOfYear(d)+7-(wday+6)%7)/7).substring(1));
					break;
				case 'y':
					result.append(String.valueOf(100+d.get(Calendar.YEAR)%100).substring(1));
					break;
				case 'Y':
					result.append(String.valueOf(d.get(Calendar.YEAR)));
					break;
				case 'z': {
					final int tzo = timeZoneOffset(d) / 60;
					final int a = Math.abs(tzo);
					final String h = String.valueOf(100 + a / 60).substring(1);
					final String m = String.valueOf(100 + a % 60).substring(1);
					result.append((tzo>=0? "+": "-") + h + m);
					break;
				}
				case 'Z':
					result.append(timeZoneName(d));
					break;
				}
			}
		}
		return result.tojstring();
	}
	
	private static final String[] WeekdayNameAbbrev = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
	private static final String[] WeekdayName = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
	private static final String[] MonthNameAbbrev = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
	private static final String[] MonthName = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };
	private static final int[] DaysBeforeMonth = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };

	/** The time, in seconds since the epoch, in the local time zone or in UTC */
	private Calendar calendar(double time, boolean utc) {
		Calendar d = utc? Calendar.getInstance(TimeZone.getTimeZone("GMT")): Calendar.getInstance();
		d.setTime(new Date((long)time * 1000L));
		return d;
	}

	/** Day of the year, 0 for January 1st */
	private static int dayOfYear(Calendar d) {
		int month = d.get(Calendar.MONTH);
		return DaysBeforeMonth[month] + (month > 1 && isLeapYear(d.get(Calendar.YEAR))? 1: 0) + d.get(Calendar.DAY_OF_MONTH) - 1;
	}

	private static boolean isLeapYear(int year) {
		return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
	}

	/** The ISO 8601 week-based year and week number (1 to 53): weeks start on Monday, week 1 has the year's first Thursday */
	private static int[] isoWeek(Calendar d) {
		int year = d.get(Calendar.YEAR);
		int yday = dayOfYear(d);
		int wday = d.get(Calendar.DAY_OF_WEEK)-1;
		int days = isoWeekDays(yday, wday);
		if (days < 0) {
			year--;
			days = isoWeekDays(yday + (isLeapYear(year)? 366: 365), wday);
		} else {
			int next = isoWeekDays(yday - (isLeapYear(year)? 366: 365), wday);
			if (next >= 0) {
				year++;
				days = next;
			}
		}
		return new int[] { year, days / 7 + 1 };
	}

	/** Days since the Monday that starts the year's first ISO week (negative before it) */
	private static int isoWeekDays(int yday, int wday) {
		return yday - (yday - wday + 4 + 378) % 7 + 3;
	}

	/**
	 * The name of the time zone for {@code %Z}, its ID by default; JseOsLib returns the abbreviation, as in C.
	 * @param d the date, in the time zone
	 */
	protected String timeZoneName(Calendar d) {
		return d.getTimeZone().getID();
	}
	
	private int timeZoneOffset(Calendar d) {
		int localStandarTimeMillis = (
				d.get(Calendar.HOUR_OF_DAY) * 3600 +
				d.get(Calendar.MINUTE) * 60 +
				d.get(Calendar.SECOND)) * 1000;
		return d.getTimeZone().getOffset(
				1,
				d.get(Calendar.YEAR),
				d.get(Calendar.MONTH),
				d.get(Calendar.DAY_OF_MONTH),
				d.get(Calendar.DAY_OF_WEEK),
				localStandarTimeMillis) / 1000;
	}
	
	private boolean isDaylightSavingsTime(Calendar d) {
		return timeZoneOffset(d) != d.getTimeZone().getRawOffset() / 1000;
	}
	
	/**
	 * This function is equivalent to the C function system.
	 * It passes command to be executed by an operating system shell.
	 * It returns a status code, which is system-dependent.
	 * If command is absent, then it returns nonzero if a shell
	 * is available and zero otherwise.
	 * @param command command to pass to the system
	 */
	protected Varargs execute(String command) {
		return varargsOf(NIL, valueOf("exit"), ONE);
	}

	/**
	 * Calls the C function exit, with an optional code, to terminate the host program.
	 * @param code
	 */
	protected void exit(int code) {
		System.exit(code);
	}

	/**
	 * Returns the value of the process environment variable varname,
	 * or the System property value for varname,
	 * or null if the variable is not defined in either environment.
	 * 
	 * The default implementation, which is used by the JmePlatform,
	 * only queryies System.getProperty().
	 * 
	 * The JsePlatform overrides this behavior and returns the
	 * environment variable value using System.getenv() if it exists,
	 * or the System property value if it does not.
	 * 
	 * A SecurityException may be thrown if access is not allowed
	 * for 'varname'.
	 * @param varname
	 * @return String value, or null if not defined
	 */
	protected String getenv(String varname) {
		return System.getProperty(varname);
	}

	/**
	 * Deletes the file or directory with the given name.
	 * Directories must be empty to be removed.
	 * If this function fails, it throws and IOException
	 * 
	 * @param filename
	 * @throws IOException if it fails
	 */
	protected void remove(String filename) throws IOException {
		throw new IOException( "not implemented" );
	}

	/**
	 * Renames file or directory named oldname to newname.
	 * If this function fails,it throws and IOException
	 * 
	 * @param oldname old file name
	 * @param newname new file name
	 * @throws IOException if it fails
	 */
	protected void rename(String oldname, String newname) throws IOException {
		throw new IOException( "not implemented" );
	}

	/**
	 * Sets the current locale of the program. locale is a string specifying
	 * a locale; category is an optional string describing which category to change:
	 * "all", "collate", "ctype", "monetary", "numeric", or "time"; the default category
	 * is "all".
	 * 
	 * If locale is the empty string, the current locale is set to an implementation-
	 * defined native locale. If locale is the string "C", the current locale is set
	 * to the standard C locale.
	 * 
	 * When called with null as the first argument, this function only returns the
	 * name of the current locale for the given category.
	 * 
	 * @param locale
	 * @param category
	 * @return the name of the new locale, or null if the request
	 * cannot be honored.
	 */
	protected String setlocale(String locale, String category) {
		return "C";
	}

	/**
	 * Returns the current time when called without arguments,
	 * or a time representing the date and time specified by the given table.
	 * This table must have fields year, month, and day,
	 * and may have fields hour, min, sec, and isdst
	 * (for a description of these fields, see the os.date function).
	 * @param table
	 * @return long value for the time
	 */
	protected double time(LuaTable table) {
		java.util.Date d;
		if (table == null) {
			d = new java.util.Date();
		} else {
			Calendar c = Calendar.getInstance();
			c.set(Calendar.YEAR, table.get("year").checkint());
			c.set(Calendar.MONTH, table.get("month").checkint()-1);
			c.set(Calendar.DAY_OF_MONTH, table.get("day").checkint());
			c.set(Calendar.HOUR_OF_DAY, table.get("hour").optint(12));
			c.set(Calendar.MINUTE, table.get("min").optint(0));
			c.set(Calendar.SECOND, table.get("sec").optint(0));
			c.set(Calendar.MILLISECOND, 0);
			d = c.getTime();
		}
		return d.getTime() / 1000;
	}

	/**
	 * Returns a string with a file name that can be used for a temporary file.
	 * The file must be explicitly opened before its use and explicitly removed
	 * when no longer needed.
	 * 
	 * On some systems (POSIX), this function also creates a file with that name,
	 * to avoid security risks. (Someone else might create the file with wrong
	 * permissions in the time between getting the name and creating the file.)
	 * You still have to open the file to use it and to remove it (even if you
	 * do not use it).
	 * 
	 * @return String filename to use
	 */
	protected String tmpname() {
		synchronized ( OsLib.class ) {
			return TMP_PREFIX+(tmpnames++)+TMP_SUFFIX;
		}
	}
}
