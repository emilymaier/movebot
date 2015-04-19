package net.emilymaier.movebot;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Xml;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

public class Tracker implements LocationListener
{
	private MoveBotActivity act;

	private GoogleMap map;
	private Polyline line;
	private List<Location> points;
	private List<LatLng> latLngs;

	private XmlSerializer xml;
	private FileOutputStream xmlStream;

	private LocationManager lm;
	private Criteria criteria;

	public boolean locating = false;
	public boolean tracking = false;

	private double distance = 0.0;

	public Tracker(MoveBotActivity act, GoogleMap map)
	{
		this.act = act;
		this.map = map;
		UiSettings ui = this.map.getUiSettings();
		ui.setZoomGesturesEnabled(false);
		ui.setScrollGesturesEnabled(false);
		ui.setTiltGesturesEnabled(false);
		ui.setRotateGesturesEnabled(false);
		PolylineOptions options = new PolylineOptions();
		line = this.map.addPolyline(options);

		xml = Xml.newSerializer();

		lm = (LocationManager) this.act.getSystemService(Context.LOCATION_SERVICE);
		criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(true);
		criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
		criteria.setSpeedRequired(true);
		criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);
	}

	public synchronized void startLocating()
	{
		if(locating)
		{
			return;
		}
		locating = true;
		lm.requestLocationUpdates(0, 0, criteria, this, null);
	}

	public synchronized void startTracking()
	{
		if(tracking)
		{
			return;
		}
		tracking = true;
		distance = 0.0;
		points = new ArrayList<>();
		latLngs = new ArrayList<>();
		try
		{
			xmlStream = act.openFileOutput("track.gpx", Context.MODE_PRIVATE);
			xml.setOutput(xmlStream, "utf-8");
			xml.startDocument("UTF-8", true);
			xml.startTag("", "gpx");
			xml.attribute("", "xmlns", "http://www.topografix.com/GPX/1/1");
			xml.attribute("xmlns", "xsi", "http://www.w3.org/2001/XMLSchema-instance");
			xml.attribute("xsi", "schemaLocation", "http://www.topografix.com/GPX/1/1 gpx.xsd");
			xml.attribute("", "version", "1.1");
			xml.attribute("", "creator", "Move Bot");
			xml.startTag("", "trk");
			xml.startTag("", "trkseg");
		}
		catch(IOException e)
		{
			throw new RuntimeException("IOException", e);
		}
	}

	public synchronized void stopTracking()
	{
		if(!tracking)
		{
			return;
		}
		tracking = false;
		try
		{
			xml.endTag("", "trkseg");
			xml.endTag("", "trk");
			xml.endTag("", "gpx");
			xml.endDocument();
			xmlStream.close();
		}
		catch(IOException e)
		{
			throw new RuntimeException("IOException", e);
		}
	}

	public synchronized void stopLocating()
	{
		if(!locating)
		{
			return;
		}
		locating = false;
		lm.removeUpdates(this);
	}

	@Override
	public synchronized void onLocationChanged(Location location)
	{
		LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
		map.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 16));
		if(tracking)
		{
			if(!points.isEmpty())
			{
				Location lastLoc = points.get(points.size() - 1);
				distance += lastLoc.distanceTo(location);
			}
			points.add(location);
			latLngs.add(ll);
			line.setPoints(latLngs);
			try
			{
				xml.startTag("", "trkpt");
				xml.attribute("", "lat", String.valueOf(ll.latitude));
				xml.attribute("", "lon", String.valueOf(ll.longitude));
				xml.startTag("", "ele");
				xml.text(String.valueOf(location.getAltitude()));
				xml.endTag("", "ele");
				xml.startTag("", "time");
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-DD'T'kk:mm:ss");
				xml.text(sdf.format(new Date(location.getTime())));
				xml.endTag("", "time");
				xml.endTag("", "trkpt");
			}
			catch(IOException e)
			{
				throw new RuntimeException("IOException", e);
			}
			act.updateSpeed(location.getSpeed());
			act.updateDistance(distance);
		}
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
