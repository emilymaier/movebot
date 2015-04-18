package net.emilymaier.movebot;

import android.content.Context;
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
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

public class Tracker implements LocationListener
{
	private Context context;
	private GoogleMap map;
	private Polyline line;
	private List<LatLng> points;

	private XmlSerializer xml;
	private FileOutputStream xmlStream;

	private LocationManager lm;

	public boolean locating = false;
	public boolean tracking = false;

	public Tracker(Context context, GoogleMap map)
	{
		this.context = context;
		this.map = map;
		UiSettings ui = this.map.getUiSettings();
		ui.setZoomGesturesEnabled(false);
		ui.setScrollGesturesEnabled(false);
		ui.setTiltGesturesEnabled(false);
		ui.setRotateGesturesEnabled(false);
		PolylineOptions options = new PolylineOptions();
		line = this.map.addPolyline(options);

		xml = Xml.newSerializer();

		lm = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
	}

	public synchronized void startLocating()
	{
		if(locating)
		{
			return;
		}
		locating = true;
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	}

	public synchronized void startTracking()
	{
		if(tracking)
		{
			return;
		}
		tracking = true;
		points = new ArrayList<>();
		try
		{
			xmlStream = context.openFileOutput("track.gpx", Context.MODE_PRIVATE);
			xml.setOutput(xmlStream, "UTF-8");
			xml.startDocument("UTF-8", true);
			xml.startTag("", "gpx");
			xml.attribute("", "version", "1.1");
			xml.attribute("", "creator", "Move Bot");
			xml.startTag("", "trk");
			xml.startTag("", "trkSeg");
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
			xml.endTag("", "trkSeg");
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
			points.add(ll);
			line.setPoints(points);
			try
			{
				xml.startTag("", "wpt");
				xml.attribute("", "lat", String.valueOf(ll.latitude));
				xml.attribute("", "lon", String.valueOf(ll.longitude));
				xml.endTag("", "wpt");
			}
			catch(IOException e)
			{
				throw new RuntimeException("IOException", e);
			}
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
