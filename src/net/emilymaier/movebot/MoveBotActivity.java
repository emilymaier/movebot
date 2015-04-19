package net.emilymaier.movebot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.Timer;
import java.util.TimerTask;

public class MoveBotActivity extends FragmentActivity implements OnMapReadyCallback, ImageButton.OnClickListener
{
	private class MainPagerAdapter extends FragmentPagerAdapter
	{
		public MainPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public int getCount()
		{
			return 2;
		}

		@Override
		public Fragment getItem(int position)
		{
			switch(position)
			{
				case 0:
					return controlFragment;
				case 1:
					return mapFragment;
				default:
					return null;
			}
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			switch(position)
			{
				case 0:
					return "Control";
				case 1:
					return "Map";
				default:
					return null;
			}
		}
	}

	private class ControlFragment extends Fragment
	{
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View rootView = inflater.inflate(R.layout.main_control, container, false);
			startStop = (ImageButton) rootView.findViewById(R.id.startStop);
			return rootView;
		}
	}

	private ViewPager pager;
	private FragmentPagerAdapter adapter;
	private ControlFragment controlFragment;
	private SupportMapFragment mapFragment;

	private Tracker tracker;
	private ImageButton startStop;
	private Timer locationStopTimer;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		pager = (ViewPager) findViewById(R.id.pager);
		adapter = new MainPagerAdapter(getSupportFragmentManager());
		controlFragment = new ControlFragment();
		mapFragment = SupportMapFragment.newInstance();
		pager.setAdapter(adapter);
		mapFragment.getMapAsync(this);
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
				String gpx = tracker.stopTracking();
				startStop.setImageResource(android.R.drawable.ic_media_play);
				Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_TEXT, gpx);
				sendIntent.setType("application/xml");
				startActivity(sendIntent);
			}
		}
	}

	@Override
	public void onMapReady(GoogleMap map)
	{
		tracker = new Tracker(this, map);
		tracker.startLocating();
		startStop.setOnClickListener(this);
	}
}
