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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Google map fragment and the GPS information.
 */
public class Tracker implements LocationListener
{
	private MoveBotActivity act;

	private GoogleMap map;
	private Polyline line;
	private List<LatLng> latLngs;

	private LocationManager lm;
	private Criteria criteria;

	private Run run;

	public boolean locating = false;
	public boolean tracking = false;
	public boolean paused = false;

	public Tracker(MoveBotActivity act, GoogleMap map)
	{
		this.act = act;
		this.map = map;
		this.map.setMyLocationEnabled(true);
		PolylineOptions options = new PolylineOptions();
		line = this.map.addPolyline(options);

		lm = (LocationManager) this.act.getSystemService(Context.LOCATION_SERVICE);
		criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(true);
		criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
		criteria.setSpeedRequired(true);
		criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);
	}

	/**
	 * Turn on the GPS.
	 */
	public synchronized void startLocating()
	{
		if(locating)
		{
			return;
		}
		locating = true;
		lm.requestLocationUpdates(0, 0, criteria, this, null);
	}

	/**
	 * Begin tracking GPS coordinates for a running session.
	 */
	public synchronized void startTracking()
	{
		if(tracking)
		{
			return;
		}
		tracking = true;
		paused = false;
		latLngs = new ArrayList<>();
		run = new Run();
	}

	/**
	 * Pause the running session.
	 */
	public synchronized void pauseTracking()
	{
		paused = true;
	}

	/**
	 * Resume the running session.
	 */
	public synchronized void resumeTracking()
	{
		paused = false;
		run.newTrack();
	}

	/**
	 * Stop tracking GPS coordinates to end a running session.
	 * @return the finished running session.
	 */
	public synchronized Run stopTracking()
	{
		if(!tracking)
		{
			return null;
		}
		tracking = false;
		return run;
	}

	/**
	 * Turn off the GPS.
	 */
	public synchronized void stopLocating()
	{
		if(!locating)
		{
			return;
		}
		locating = false;
		lm.removeUpdates(this);
	}

	/**
	 * Called whenever a new GPS coordinate is available. Updates the map,
	 * and updates data for the running session if currently in one.
	 * @param location the GPS location
	 */
	@Override
	public synchronized void onLocationChanged(Location location)
	{
		if(tracking && !paused)
		{
			LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
			latLngs.add(ll);
			line.setPoints(latLngs);
			act.updateStats(location.getSpeed(), run.newPoint(location));
		}
		act.updateGps(location.getAccuracy());
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
	}

	@Override
	public void onProviderEnabled(String provider)
	{
	}

	@Override
	public void onProviderDisabled(String provider)
	{
	}
}
