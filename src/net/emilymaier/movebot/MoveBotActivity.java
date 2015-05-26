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
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ListView;
import android.widget.TextView;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The only activity for MoveBot. Runs the entire app.
 */
public class MoveBotActivity extends AppCompatActivity implements OnMapReadyCallback
{
	/**
	 * MainPagerAdapter for the activity pages. Returns the fragments in the
	 * main activity.
	 */
	private class MainPagerAdapter extends FragmentPagerAdapter
	{
		private boolean developerMode = false;

		public MainPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public int getCount()
		{
			if(developerMode)
			{
				return 5;
			}
			return 4;
		}

		public Fragment getItem(int position)
		{
			switch(position)
			{
				case 0:
					return runsFragment;
				case 1:
					return heartFragment;
				case 2:
					return developerFragment;
				case 3:
					return controlFragment;
				case 4:
					return mapFragment;
				default:
					return null;
			}
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			if(developerMode)
			{
				switch(position)
				{
					case 0:
						return "Runs";
					case 1:
						return "HRM";
					case 2:
						return "Developer";
					case 3:
						return "Control";
					case 4:
						return "Map";
					default:
						return null;
				}
			}
			switch(position)
			{
				case 0:
					return "Runs";
				case 1:
					return "HRM";
				case 2:
					return "Control";
				case 3:
					return "Map";
				default:
					return null;
			}
		}

		@Override
		public int getItemPosition(Object object)
		{
			return POSITION_NONE;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position)
		{
			if(!developerMode)
			{
				switch(position)
				{
					case 0:
						return super.instantiateItem(container, 0);
					case 1:
						return super.instantiateItem(container, 1);
					case 2:
						return super.instantiateItem(container, 3);
					case 3:
						return super.instantiateItem(container, 4);
				}
			}
			return super.instantiateItem(container, position);
		}

		@Override
		public boolean isViewFromObject(View view, Object object)
		{
			return ((Fragment) object).getView() == view;
		}

		public void setDeveloperMode(ViewPager pager, boolean developerMode)
		{
			this.developerMode = developerMode;
			notifyDataSetChanged();
		}
	}

	/**
	 * Fragment for the list of run sessions.
	 */
	private class RunsFragment extends Fragment
	{
		public RunsFragment()
		{
			super();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View rootView = inflater.inflate(R.layout.main_runs, container, false);
			runsList = (ListView) rootView.findViewById(R.id.runsList);
			runsListAdapter = new RunsListAdapter(getActivity());
			runsList.setAdapter(runsListAdapter);
			runsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id)
				{
					Button runItemShare = (Button) view.findViewById(R.id.runItemShare);
					Button runItemDelete = (Button) view.findViewById(R.id.runItemDelete);
					if(runItemShare.getVisibility() == View.GONE)
					{
						runItemShare.setVisibility(View.VISIBLE);
						runItemDelete.setVisibility(View.VISIBLE);
					}
					else
					{
						runItemShare.setVisibility(View.GONE);
						runItemDelete.setVisibility(View.GONE);
					}
				}
			});
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
			TextView runItemStart = (TextView) rowView.findViewById(R.id.runItemStart);
			runItemStart.setText(DateFormat.getDateTimeInstance().format(new Date(currentRun.getStartTime())));
			TextView runItemTime = (TextView) rowView.findViewById(R.id.runItemTime);
			runItemTime.setText(Units.duration(currentRun.getTotalTime()));
			TextView runItemDistance = (TextView) rowView.findViewById(R.id.runItemDistance);
			runItemDistance.setText(Units.distance(currentRun.getDistance()));
			TextView runItemDistanceUnits = (TextView) rowView.findViewById(R.id.runItemDistanceUnits);
			runItemDistanceUnits.setText(" " + Units.distanceUnits());
			TextView runItemSpeed = (TextView) rowView.findViewById(R.id.runItemSpeed);
			runItemSpeed.setText(Units.speed(currentRun.getAverageSpeed()));
			TextView runItemSpeedUnits = (TextView) rowView.findViewById(R.id.runItemSpeedUnits);
			runItemSpeedUnits.setText(" " + Units.speedUnits());
			TextView runItemPace = (TextView) rowView.findViewById(R.id.runItemPace);
			runItemPace.setText(Units.pace(currentRun.getAverageSpeed()));
			TextView runItemPaceUnits = (TextView) rowView.findViewById(R.id.runItemPaceUnits);
			runItemPaceUnits.setText(" " + Units.paceUnits());
			Button runItemShare = (Button) rowView.findViewById(R.id.runItemShare);
			runItemShare.setTag(position);
			runItemShare.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view)
				{
					shareGpx(runs.get((int) view.getTag()));
				}
			});
			Button runItemDelete = (Button) rowView.findViewById(R.id.runItemDelete);
			runItemDelete.setTag(position);
			runItemDelete.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View view)
				{
					DialogFragment frag = new DialogFragment() {
						@Override
						public Dialog onCreateDialog(Bundle savedInstanceState)
						{
							return new AlertDialog.Builder(context)
								.setTitle("Confirm Delete")
								.setMessage("Really delete this activity?")
								.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int id)
									{
										runs.remove((int) view.getTag());
										runsListAdapter.notifyDataSetChanged();
									}
								})
								.setNegativeButton("No", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int id)
									{
									}
								})
								.create();
						}
					};
					frag.show(getSupportFragmentManager(), "delete");
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
		public ControlFragment()
		{
			super();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View rootView = inflater.inflate(R.layout.main_control, container, false);
			TextView timeLabel = (TextView) rootView.findViewById(R.id.timeLabel);
			timeLabel.setTypeface(font);
			timeText = (Chronometer) rootView.findViewById(R.id.timeText);
			timeText.setTypeface(font);
			TextView speedLabel = (TextView) rootView.findViewById(R.id.speedLabel);
			speedLabel.setTypeface(font);
			speedText = (TextView) rootView.findViewById(R.id.speedText);
			speedText.setTypeface(font);
			speedUnits = (TextView) rootView.findViewById(R.id.speedUnits);
			speedUnits.setTypeface(font);
			TextView distanceLabel = (TextView) rootView.findViewById(R.id.distanceLabel);
			distanceLabel.setTypeface(font);
			distanceText = (TextView) rootView.findViewById(R.id.distanceText);
			distanceText.setTypeface(font);
			distanceUnits = (TextView) rootView.findViewById(R.id.distanceUnits);
			distanceUnits.setTypeface(font);
			TextView paceLabel = (TextView) rootView.findViewById(R.id.paceLabel);
			paceLabel.setTypeface(font);
			paceText = (TextView) rootView.findViewById(R.id.paceText);
			paceText.setTypeface(font);
			paceUnits = (TextView) rootView.findViewById(R.id.paceUnits);
			paceUnits.setTypeface(font);
			TextView hrLabel = (TextView) rootView.findViewById(R.id.hrLabel);
			hrLabel.setTypeface(font);
			hrText = (TextView) rootView.findViewById(R.id.hrText);
			hrText.setTypeface(font);
			TextView hrUnits = (TextView) rootView.findViewById(R.id.hrUnits);
			hrUnits.setTypeface(font);
			startStop = (Button) rootView.findViewById(R.id.startStop);
			startStop.setTypeface(font);
			pauseResume = (Button) rootView.findViewById(R.id.pauseResume);
			pauseResume.setTypeface(font);
			gpsInfo = (TextView) rootView.findViewById(R.id.gpsInfo);
			gpsInfo.setTypeface(font);
			shareButton = (Button) rootView.findViewById(R.id.shareButton);
			updateUnits();
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
			Resources res = getResources();
			synchronized(tracker)
			{
				if(!tracker.tracking)
				{
					shareButton.setVisibility(View.INVISIBLE);
					tracker.startTracking();
					timeText.setBase(SystemClock.elapsedRealtime());
					timeText.start();
					startStop.setText("Stop");
					startStop.setTextColor(res.getColor(R.color.red));
					pauseResume.setText("Pause");
					pauseResume.setVisibility(View.VISIBLE);
				}
				else
				{
					timeText.stop();
					runs.add(0, tracker.stopTracking());
					if(runsListAdapter != null)
					{
						runsListAdapter.notifyDataSetChanged();
					}
					startStop.setText("Start");
					startStop.setTextColor(res.getColor(R.color.green));
					pauseResume.setVisibility(View.GONE);
					shareButton.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	/**
	 * Listener for the pause/resume button. Pauses and resumes a running
	 * session.
	 */
	private class PauseResumeClick implements View.OnClickListener
	{
		@Override
		public void onClick(View view)
		{
			synchronized(tracker)
			{
				if(!tracker.paused)
				{
					pauseResume.setText("Resume");
					tracker.pauseTracking();
					timeText.stop();
					chronoStopTime = timeText.getBase() - SystemClock.elapsedRealtime();
				}
				else
				{
					pauseResume.setText("Pause");
					timeText.setBase(SystemClock.elapsedRealtime() + chronoStopTime);
					timeText.start();
					tracker.resumeTracking();
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
	private MainPagerAdapter adapter;
	private RunsFragment runsFragment;
	private HeartFragment heartFragment;
	private DeveloperFragment developerFragment;
	private ControlFragment controlFragment;
	private SupportMapFragment mapFragment;

	private Tracker tracker;
	private ArrayList<Run> runs;

	private RunsListAdapter runsListAdapter;
	private Typeface font;

	private ListView runsList;
	private Chronometer timeText;
	private TextView speedText;
	private TextView speedUnits;
	private TextView distanceText;
	private TextView distanceUnits;
	private TextView paceText;
	private TextView paceUnits;
	private TextView hrText;
	private Button startStop;
	private Button pauseResume;
	private TextView gpsInfo;
	private Button shareButton;

	private long lastGpsTime = 0;
	private double lastGpsAccuracy = 10000.0;
	private Timer gpsInfoTimer;
	private long chronoStopTime = 0;

	@Override
	@SuppressWarnings({"deprecation", "unchecked"})
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		Units.initialize(this);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

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
		pager = (ViewPager) findViewById(R.id.pager);
		adapter = new MainPagerAdapter(getSupportFragmentManager());
		runsFragment = new RunsFragment();
		heartFragment = new HeartFragment(this);
		developerFragment = new DeveloperFragment();
		controlFragment = new ControlFragment();
		mapFragment = SupportMapFragment.newInstance();
		pager.setAdapter(adapter);
		updateDeveloperMode();
		gpsInfoTimer = new Timer();
		gpsInfoTimer.schedule(new GpsInfoTask(), 2 * 1000);
		mapFragment.getMapAsync(this);

		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(bluetoothAdapter != null)
		{
			if(!bluetoothAdapter.isEnabled())
			{
				Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(intent, 1);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case R.id.actionAbout:
				Intent intent = new Intent(this, AboutActivity.class);
				startActivity(intent);
				return true;
			case R.id.action_settings:
				Intent intent2 = new Intent(this, SettingsActivity.class);
				startActivityForResult(intent2, 0);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(requestCode == 0)
		{
			updateUnits();
			updateDeveloperMode();
		}
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
		tracker = new Tracker(this, map, developerFragment);
		tracker.startLocating();
		startStop.setOnClickListener(new StartStopClick());
		pauseResume.setOnClickListener(new PauseResumeClick());
	}

	/**
	 * Update the units being used in the app.
	 */
	private void updateUnits()
	{
		if(runsList != null)
		{
			runsList.invalidateViews();
		}
		speedUnits.setText(Units.speedUnits());
		distanceUnits.setText(Units.distanceUnits());
		paceUnits.setText(Units.paceUnits());
	}

	/**
	 * Update the status of developer mode.
	 */
	private void updateDeveloperMode()
	{
		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("developer", false))
		{
			adapter.setDeveloperMode(pager, true);
			pager.setCurrentItem(3);
		}
		else
		{
			adapter.setDeveloperMode(pager, false);
			pager.setCurrentItem(2);
		}
	}

	/**
	 * Called by the Tracker to update the statistics for the running
	 * session.
	 * @param speed the current speed
	 * @param distance the total distance traveled
	 */
	public void updateStats(double speed, double distance)
	{
		speedText.setText(Units.speed(speed));
		distanceText.setText(Units.distance(distance));
		paceText.setText(Units.pace(speed));
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
	 * Called by the HeartMonitor to update the heart rate.
	 * @param heartRate the current heart rate
	 */
	public void updateHeart(int heartRate)
	{
		hrText.setText(String.valueOf(heartRate));
		tracker.updateHeartRate(heartRate);
	}

	/**
	 * Generate a run's .gpx file and share it.
	 * @param run the run session to generate the .gpx from
	 */
	private void shareGpx(Run run)
	{
		String filename;
		try
		{
			filename = run.generateGpx(this);
		}
		catch(IOException e)
		{
			throw new RuntimeException("IOException", e);
		}
		File gpxFile = new File(getFilesDir(), filename);
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
