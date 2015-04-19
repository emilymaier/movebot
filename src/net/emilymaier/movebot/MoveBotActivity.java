package net.emilymaier.movebot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

public class MoveBotActivity extends FragmentActivity implements OnMapReadyCallback
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
			timeLabel = (TextView) rootView.findViewById(R.id.timeLabel);
			timeLabel.setTypeface(font);
			timeText = (Chronometer) rootView.findViewById(R.id.timeText);
			timeText.setTypeface(font);
			speedLabel = (TextView) rootView.findViewById(R.id.speedLabel);
			speedLabel.setTypeface(font);
			speedText = (TextView) rootView.findViewById(R.id.speedText);
			speedText.setTypeface(font);
			speedUnits = (TextView) rootView.findViewById(R.id.speedUnits);
			speedUnits.setTypeface(font);
			distanceLabel = (TextView) rootView.findViewById(R.id.distanceLabel);
			distanceLabel.setTypeface(font);
			distanceText = (TextView) rootView.findViewById(R.id.distanceText);
			distanceText.setTypeface(font);
			distanceUnits = (TextView) rootView.findViewById(R.id.distanceUnits);
			distanceUnits.setTypeface(font);
			startStop = (ImageButton) rootView.findViewById(R.id.startStop);
			startStop.setBackground(play);
			shareButton = (Button) rootView.findViewById(R.id.shareButton);
			return rootView;
		}
	}

	private class StartStopClick implements View.OnClickListener
	{
		@Override
		public void onClick(View view)
		{
			synchronized(tracker)
			{
				if(!tracker.tracking)
				{
					shareButton.setVisibility(View.INVISIBLE);
					tracker.startTracking();
					timeText.setBase(SystemClock.elapsedRealtime());
					timeText.start();
					startStop.setBackground(pause);
				}
				else
				{
					timeText.stop();
					tracker.stopTracking();
					startStop.setBackground(play);
					shareButton.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	private ViewPager pager;
	private FragmentPagerAdapter adapter;
	private ControlFragment controlFragment;
	private SupportMapFragment mapFragment;

	private Tracker tracker;

	private Typeface font;
	private Drawable play;
	private Drawable pause;

	private TextView timeLabel;
	private Chronometer timeText;
	private TextView speedLabel;
	private TextView speedText;
	private TextView speedUnits;
	private TextView distanceLabel;
	private TextView distanceText;
	private TextView distanceUnits;
	private ImageButton startStop;
	private Button shareButton;

	private Timer locationStopTimer;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		font = Typeface.createFromAsset(getAssets(), "fonts/led_real.ttf");
		Resources res = getResources();
		play = res.getDrawable(android.R.drawable.ic_media_play);
		play.setColorFilter(res.getColor(R.color.control), PorterDuff.Mode.MULTIPLY);
		pause = res.getDrawable(android.R.drawable.ic_media_pause);
		pause.setColorFilter(res.getColor(R.color.control), PorterDuff.Mode.MULTIPLY);
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
	public void onMapReady(GoogleMap map)
	{
		tracker = new Tracker(this, map);
		tracker.startLocating();
		startStop.setOnClickListener(new StartStopClick());
	}

	public void updateSpeed(double speed)
	{
		speed *= 2.23694;
		DecimalFormat df = new DecimalFormat("#0.0");
		speedText.setText(df.format(speed));
	}

	public void updateDistance(double distance)
	{
		distance *= 0.000621371;
		DecimalFormat df = new DecimalFormat("##0.00");
		distanceText.setText(df.format(distance));
	}

	public void shareButtonClick(View view)
	{
		File gpxFile = new File(getFilesDir(), "track.gpx");
		Uri gpxUri = FileProvider.getUriForFile(this, "net.emilymaier.movebot.fileprovider", gpxFile);
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_STREAM, gpxUri);
		sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		sendIntent.setType("application/xml");
		startActivity(sendIntent);
	}
}
