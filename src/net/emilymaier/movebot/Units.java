/**
 * Copyright © 2015 Emily Maier
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of version 2 of the GNU General Public License as published by the
 * Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Linking MoveBot statically or dynamically with other modules is making a
 * combined work based on MoveBot. Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of MoveBot give
 * you permission to combine MoveBot with code included in the standard release
 * of the Google Play Services Library. You may copy and distribute such a
 * system following the terms of the GNU GPL for MoveBot and the licenses of
 * the Google Play Services Library.
 *
 * Note that people who make modified versions of MoveBot are not obligated to
 * grant this special exception for their modified versions; it is their choice
 * whether to do so. The GNU General Public License gives permission to release
 * a modified version without this exception; this exception also makes it
 * possible to release a modified version which carries forward this exception.
 */

package net.emilymaier.movebot;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.DecimalFormat;

/**
 * Converts the application units into display strings.
 */
public class Units
{
	private static SharedPreferences prefs;

	/**
	 * Initialize the preferences to grab the units.
	 * @param context the application context
	 */
	public static void initialize(Context context)
	{
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	/**
	 * Duration conversion.
	 * @param millis the elapsed time, in milliseconds
	 * @return the string representation
	 */
	public static String duration(long millis)
	{
		long seconds = millis / 1000;
		String durationString = "";
		if(seconds >= 60 * 60)
		{
			durationString = String.valueOf(seconds / 60 / 60) + ":";
			seconds %= 60 * 60;
		}
		DecimalFormat df = new DecimalFormat("00:");
		durationString += df.format(seconds / 60);
		seconds %= 60;
		df = new DecimalFormat("00");
		durationString += df.format(seconds);
		return durationString;
	}

	/**
	 * Distance conversion.
	 * @param meters the distance traveled, in meters
	 * @return the string representation
	 */
	public static String distance(double meters)
	{
		DecimalFormat df = new DecimalFormat("##0.00");
		String units = prefs.getString("units", "");
		if(units.equals("us"))
		{
			return df.format(meters * 0.000621371);
		}
		if(units.equals("metric"))
		{
			return df.format(meters * 0.001);
		}
		throw new RuntimeException("invalid units");
	}

	/**
	 * Distance units.
	 */
	public static String distanceUnits()
	{
		String units = prefs.getString("units", "");
		if(units.equals("us"))
		{
			return "miles";
		}
		if(units.equals("metric"))
		{
			return "kilometers";
		}
		throw new RuntimeException("invalid units");
	}

	/**
	 * Speed conversion.
	 * @param ms the speed traveled, in meters per second
	 * @return the string representation
	 */
	public static String speed(double ms)
	{
		DecimalFormat df = new DecimalFormat("#0.0");
		String units = prefs.getString("units", "");
		if(units.equals("us"))
		{
			return df.format(ms * 2.23694);
		}
		if(units.equals("metric"))
		{
			return df.format(ms * 3.6);
		}
		throw new RuntimeException("invalid units");
	}

	/**
	 * Speed units.
	 */
	public static String speedUnits()
	{
		String units = prefs.getString("units", "");
		if(units.equals("us"))
		{
			return "mph";
		}
		if(units.equals("metric"))
		{
			return "kph";
		}
		throw new RuntimeException("invalid units");
	}

	/**
	 * Pace conversion.
	 * @param ms the speed traveled, in meters per second
	 * @return the string representation of the equivalent pace
	 */
	public static String pace(double ms)
	{
		if(ms == 0)
		{
			return "--:--";
		}
		String units = prefs.getString("units", "");
		if(units.equals("us"))
		{
			return duration((long) (60 * 60 * 1000/ (ms * 2.23694)));
		}
		if(units.equals("metric"))
		{
			return duration((long) (60 * 60 * 1000/ (ms * 3.6)));
		}
		throw new RuntimeException("invalid units");
	}

	/**
	 * Pace units.
	 */
	public static String paceUnits()
	{
		String units = prefs.getString("units", "");
		if(units.equals("us"))
		{
			return "min/mi";
		}
		if(units.equals("metric"))
		{
			return "min/km";
		}
		throw new RuntimeException("invalid units");
	}
}
