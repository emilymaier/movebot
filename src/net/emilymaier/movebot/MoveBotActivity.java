package net.emilymaier.movebot;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MoveBotActivity extends Activity implements LocationListener, OnMapReadyCallback
{
	private GoogleMap map;
	private Polyline line;
	private List<LatLng> points;
	private LocationManager lm;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
	}

	@Override
	public void onMapReady(GoogleMap map)
	{
		this.map = map;
		UiSettings ui = this.map.getUiSettings();
		ui.setZoomGesturesEnabled(false);
		ui.setScrollGesturesEnabled(false);
		ui.setTiltGesturesEnabled(false);
		ui.setRotateGesturesEnabled(false);
		PolylineOptions options = new PolylineOptions();
		line = map.addPolyline(options);
		points = new ArrayList<>();
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	}

	@Override
	public void onLocationChanged(Location location)
	{
		LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
		map.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 16));
		points.add(ll);
		line.setPoints(points);
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
