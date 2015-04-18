package net.emilymaier.movebot;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;

import java.util.Timer;
import java.util.TimerTask;

public class MoveBotActivity extends Activity implements OnMapReadyCallback, ImageButton.OnClickListener
{
	private Tracker tracker;
	private ImageButton startStop;
	private Timer locationStopTimer;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		locationStopTimer = new Timer();
		locationStopTimer.schedule(new TimerTask() {
			@Override
			public void run()
			{
				tracker.stopLocating();
			}
		}, 2 * 60 * 1000);
	}

	@Override
	protected void onRestart()
	{
		super.onRestart();
		if(locationStopTimer != null)
		{
			locationStopTimer.cancel();
		}
		tracker.startLocating();
	}

	@Override
	public void onClick(View view)
	{
		synchronized(tracker)
		{
			if(!tracker.tracking)
			{
				tracker.startTracking();
				startStop.setImageResource(android.R.drawable.ic_media_pause);
			}
			else
			{
				tracker.stopTracking();
				startStop.setImageResource(android.R.drawable.ic_media_play);
			}
		}
	}

	@Override
	public void onMapReady(GoogleMap map)
	{
		tracker = new Tracker(this, map);
		tracker.startLocating();
		startStop = (ImageButton) findViewById(R.id.startStop);
		startStop.setOnClickListener(this);
	}
}
