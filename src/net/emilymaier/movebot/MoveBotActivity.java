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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ListView;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The only activity for MoveBot. Runs the entire app.
 */
public class MoveBotActivity extends FragmentActivity implements OnMapReadyCallback
{
	/**
	 * FragmentPagerAdapter for the activity pages. Returns the fragments in
	 * the main activity.
	 */
	private class MainPagerAdapter extends FragmentPagerAdapter
	{
		public MainPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public int getCount()
		{
			return 3;
		}

		@Override
		public Fragment getItem(int position)
		{
			switch(position)
			{
				case 0:
					return runsFragment;
				case 1:
					return controlFragment;
				case 2:
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
					return "Runs";
				case 1:
					return "Control";
				case 2:
					return "Map";
				default:
					return null;
			}
		}
	}

	/**
	 * Fragment for the list of run sessions.
	 */
	private class RunsFragment extends Fragment
	{
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View rootView = inflater.inflate(R.layout.main_runs, container, false);
			runsList = (ListView) rootView.findViewById(R.id.runsList);
			runsListAdapter = new RunsListAdapter(getActivity());
			runsList.setAdapter(runsListAdapter);
			return rootView;
		}
	}

	/**
	 * ListView adapter for the runs fragment.
	 */
	private class RunsListAdapter extends ArrayAdapter<Run>
	{
		private Context context;

		public RunsListAdapter(Context context)
		{
			super(context, R.layout.main_runs_item, runs);
			this.context = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			Run currentRun = runs.get(position);
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.main_runs_item, parent, false);
			TextView runItemTime = (TextView) rowView.findViewById(R.id.runItemTime);
			runItemTime.setText(DateFormat.getDateTimeInstance().format(new Date(currentRun.getStartTime())));
			TextView runItemDistance = (TextView) rowView.findViewById(R.id.runItemDistance);
			DecimalFormat df = new DecimalFormat("##0.00");
			runItemDistance.setText(df.format(currentRun.getDistance() * 0.000621371));
			TextView runItemSpeed = (TextView) rowView.findViewById(R.id.runItemSpeed);
			df = new DecimalFormat("#0.0");
			runItemSpeed.setText(df.format(currentRun.getAverageSpeed() * 2.23694));
			Button runItemShare = (Button) rowView.findViewById(R.id.runItemShare);
			runItemShare.setTag(position);
			runItemShare.setOnClickListener(new View.OnClickListener () {
				@Override
				public void onClick(View view)
				{
					shareGpx(runs.get((int) view.getTag()));
				}
			});
			return rowView;
		}
	}

	/**
	 * Fragment for the app controls. Contains the statistics about the
	 * current run, the play/pause button, and the share button.
	 */
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
			gpsInfo = (TextView) rootView.findViewById(R.id.gpsInfo);
			gpsInfo.setTypeface(font);
			shareButton = (Button) rootView.findViewById(R.id.shareButton);
			return rootView;
		}
	}

	/**
	 * Listener for the start/stop button. Begins and ends a running
	 * session.
	 */
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
					runs.add(0, tracker.stopTracking());
					runsListAdapter.notifyDataSetChanged();
					startStop.setBackground(play);
					shareButton.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	/**
	 * TimerTask that updates the GPS status line on the controls page.
	 */
	private class GpsInfoTask extends TimerTask
	{
		@Override
		public void run()
		{
			runOnUiThread(new Runnable() {
				@Override
				public void run()
				{
					Resources res = getResources();
					if(SystemClock.elapsedRealtime() - lastGpsTime > 5000)
					{
						gpsInfo.setText("Acquiring GPS");
						gpsInfo.setTextColor(res.getColor(R.color.red));
					}
					else if(lastGpsAccuracy > 10.0)
					{
						gpsInfo.setText("GPS Fair");
						gpsInfo.setTextColor(res.getColor(R.color.yellow));
					}
					else
					{
						gpsInfo.setText("GPS Good");
						gpsInfo.setTextColor(res.getColor(R.color.green));
					}
					gpsInfoTimer = new Timer();
					gpsInfoTimer.schedule(new GpsInfoTask(), 2 * 1000);
				}
			});
		}
	}

	private ViewPager pager;
	private FragmentPagerAdapter adapter;
	private RunsFragment runsFragment;
	private ControlFragment controlFragment;
	private SupportMapFragment mapFragment;

	private Tracker tracker;
	private ArrayList<Run> runs;

	private RunsListAdapter runsListAdapter;
	private Typeface font;
	private Drawable play;
	private Drawable pause;

	private ListView runsList;
	private TextView timeLabel;
	private Chronometer timeText;
	private TextView speedLabel;
	private TextView speedText;
	private TextView speedUnits;
	private TextView distanceLabel;
	private TextView distanceText;
	private TextView distanceUnits;
	private ImageButton startStop;
	private TextView gpsInfo;
	private Button shareButton;

	private long lastGpsTime = 0;
	private double lastGpsAccuracy = 10000.0;
	private Timer gpsInfoTimer;

	@Override
	@SuppressWarnings({"deprecation", "unchecked"})
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		runs = new ArrayList<>();
		try
		{
			FileInputStream fis = openFileInput("runs.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			runs = (ArrayList<Run>) ois.readObject();
			ois.close();
			fis.close();
		}
		catch(FileNotFoundException e)
		{
		}
		catch(IOException e)
		{
			throw new RuntimeException("IOException", e);
		}
		catch(ClassNotFoundException e)
		{
			throw new RuntimeException("ClassNotFoundException", e);
		}

		font = Typeface.createFromAsset(getAssets(), "fonts/led_real.ttf");
		Resources res = getResources();
		play = res.getDrawable(android.R.drawable.ic_media_play);
		play.setColorFilter(res.getColor(R.color.control), PorterDuff.Mode.MULTIPLY);
		pause = res.getDrawable(android.R.drawable.ic_media_pause);
		pause.setColorFilter(res.getColor(R.color.control), PorterDuff.Mode.MULTIPLY);
		pager = (ViewPager) findViewById(R.id.pager);
		adapter = new MainPagerAdapter(getSupportFragmentManager());
		runsFragment = new RunsFragment();
		controlFragment = new ControlFragment();
		mapFragment = SupportMapFragment.newInstance();
		pager.setAdapter(adapter);
		pager.setCurrentItem(1);
		gpsInfoTimer = new Timer();
		gpsInfoTimer.schedule(new GpsInfoTask(), 2 * 1000);
		mapFragment.getMapAsync(this);
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		synchronized(tracker)
		{
			if(!tracker.tracking)
			{
				tracker.stopLocating();
			}
		}
		try
		{
			FileOutputStream fos = openFileOutput("runs.ser", Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(runs);
			oos.close();
			fos.close();
		}
		catch(IOException e)
		{
			throw new RuntimeException("IOException", e);
		}
	}

	@Override
	protected void onRestart()
	{
		super.onRestart();
		tracker.startLocating();
	}

	@Override
	public void onMapReady(GoogleMap map)
	{
		tracker = new Tracker(this, map);
		tracker.startLocating();
		startStop.setOnClickListener(new StartStopClick());
	}

	/**
	 * Called by the Tracker to update the statistics for the running
	 * session.
	 * @param speed the current speed
	 * @param distance the total distance traveled
	 */
	public void updateStats(double speed, double distance)
	{
		speed *= 2.23694;
		DecimalFormat df = new DecimalFormat("#0.0");
		speedText.setText(df.format(speed));
		distance *= 0.000621371;
		df = new DecimalFormat("##0.00");
		distanceText.setText(df.format(distance));
	}

	/**
	 * Called by the Tracker to update the GPS accuracy and timing info.
	 * @param accuracy the accuracy of the latest GPS location
	 */
	public void updateGps(double accuracy)
	{
		lastGpsTime = SystemClock.elapsedRealtime();
		lastGpsAccuracy = accuracy;
	}

	/**
	 * Generate a run's .gpx file and share it.
	 * @param run the run session to generate the .gpx from
	 */
	private void shareGpx(Run run)
	{
		try
		{
			run.generateGpx(this);
		}
		catch(IOException e)
		{
			throw new RuntimeException("IOException", e);
		}
		File gpxFile = new File(getFilesDir(), "track.gpx");
		Uri gpxUri = FileProvider.getUriForFile(this, "net.emilymaier.movebot.fileprovider", gpxFile);
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_STREAM, gpxUri);
		sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		sendIntent.setType("application/xml");
		startActivity(sendIntent);
	}

	/**
	 * Listener for the share button. Shares the .gpx of the latest run.
	 * @param view the share button
	 */
	public void shareButtonClick(View view)
	{
		shareGpx(runs.get(0));
	}
}
