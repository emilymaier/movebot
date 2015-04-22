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
import android.location.Location;
import android.util.Xml;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.xmlpull.v1.XmlSerializer;

/**
 * Represents a single running session.
 */
public class Run implements Serializable
{
	private class LocationData implements Serializable
	{
		private static final long serialVersionUID = 0L;
		public double latitude;
		public double longitude;
		public double altitude;
		public long time;
	}

	private static final long serialVersionUID = 0L;
	private long startTime;
	private long endTime;
	private ArrayList<ArrayList<LocationData>> points;
	private double distance = 0.0;

	public long getStartTime()
	{
		return startTime;
	}

	public double getDistance()
	{
		return distance;
	}

	public long getTotalTime()
	{
		long totalTime = 0;
		for(ArrayList<LocationData> currentPoints : points)
		{
			if(currentPoints.size() == 0)
			{
				continue;
			}
			totalTime += currentPoints.get(currentPoints.size() - 1).time - currentPoints.get(0).time;
		}
		return totalTime;
	}

	public double getAverageSpeed()
	{
		return distance * 1000 / getTotalTime();
	}

	public Run()
	{
		startTime = System.currentTimeMillis();
		points = new ArrayList<>();
		newTrack();
	}

	/**
	 * Adds a new GPS coordinate to the running session.
	 * @param location the GPS coordinate
	 * @return the total distance traveled this session
	 */
	public double newPoint(Location location)
	{
		ArrayList<LocationData> currentPoints = points.get(points.size() - 1);
		if(!currentPoints.isEmpty())
		{
			LocationData lastData = currentPoints.get(currentPoints.size() - 1);
			Location lastLoc = new Location("");
			lastLoc.setLatitude(lastData.latitude);
			lastLoc.setLongitude(lastData.longitude);
			lastLoc.setAltitude(lastData.altitude);
			distance += lastLoc.distanceTo(location);
		}
		LocationData curData = new LocationData();
		curData.latitude = location.getLatitude();
		curData.longitude = location.getLongitude();
		curData.altitude = location.getAltitude();
		curData.time = location.getTime();
		currentPoints.add(curData);
		endTime = curData.time;
		return distance;
	}

	/**
	 * Starts a new GPS track after the running session has been paused.
	 */
	public void newTrack()
	{
		points.add(new ArrayList<LocationData>());
	}

	/**
	 * Generates a .gpx representing this running session in track.gpx in
	 * the internal storage.
	 * @param context the application context
	 * @return the filename of the .gpx
	 */
	public String generateGpx(Context context) throws IOException
	{
		String filename = DateFormat.getDateTimeInstance().format(new Date(startTime)) + ".gpx";
		XmlSerializer xml = Xml.newSerializer();
		FileOutputStream xmlStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
		xml.setOutput(xmlStream, "utf-8");
		xml.startDocument("UTF-8", true);
		xml.startTag("", "gpx");
		xml.attribute("", "xmlns", "http://www.topografix.com/GPX/1/1");
		xml.attribute("xmlns", "xsi", "http://www.w3.org/2001/XMLSchema-instance");
		xml.attribute("xsi", "schemaLocation", "http://www.topografix.com/GPX/1/1 gpx.xsd");
		xml.attribute("", "version", "1.1");
		xml.attribute("", "creator", "Move Bot");
		xml.startTag("", "trk");
		for(ArrayList<LocationData> currentPoints : points)
		{
			xml.startTag("", "trkseg");
			for(LocationData location : currentPoints)
			{
				xml.startTag("", "trkpt");
				xml.attribute("", "lat", String.valueOf(location.latitude));
				xml.attribute("", "lon", String.valueOf(location.longitude));
				xml.startTag("", "ele");
				xml.text(String.valueOf(location.altitude));
				xml.endTag("", "ele");
				xml.startTag("", "time");
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");
				xml.text(sdf.format(new Date(location.time)));
				xml.endTag("", "time");
				xml.endTag("", "trkpt");
			}
			xml.endTag("", "trkseg");
		}
		xml.endTag("", "trk");
		xml.endTag("", "gpx");
		xml.endDocument();
		xmlStream.close();
		return filename;
	}
}
