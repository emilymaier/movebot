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

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Fragment for the heart rate monitor.
 */
public class HeartFragment extends Fragment implements View.OnClickListener, BluetoothAdapter.LeScanCallback
{
	private class NoBluetoothDialogFragment extends DialogFragment
	{
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState)
		{
			return new AlertDialog.Builder(act)
				.setMessage("Bluetooth not found on this device.")
				.setPositiveButton("Okay", null)
				.create();
		}
	}

	private class BluetoothDisabledDialogFragment extends DialogFragment
	{
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState)
		{
			return new AlertDialog.Builder(act)
				.setMessage("Bluetooth is disabled on this device.")
				.setPositiveButton("Okay", null)
				.create();
		}
	}

	private class NoLeDialogFragment extends DialogFragment
	{
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState)
		{
			return new AlertDialog.Builder(act)
				.setMessage("Bluetooth Low Energy not found on this device.")
				.setPositiveButton("Okay", null)
				.create();
		}
	}

	/**
	 * A single Bluetooth LE device that provides the HRM service.
	 */
	private class HeartDevice
	{
		public BluetoothGatt bluetoothGatt;
		public BluetoothGattCharacteristic characteristic;

		@Override
		public String toString()
		{
			return bluetoothGatt.getDevice().getName();
		}
	}

	/**
	 * Thread to get HR info from the active device.
	 */
	private class HeartThread extends Thread
	{
		public volatile boolean running = true;
		public HeartDevice device;

		@Override
		public void run()
		{
			if(!running)
			{
				return;
			}
			device.bluetoothGatt.readCharacteristic(device.characteristic);
			try
			{
				Thread.sleep(1000);
			}
			catch(InterruptedException e)
			{
			}
		}
	}

	private MoveBotActivity act;
	private Button heartScan;
	private ListView heartList;
	private ArrayList<HeartDevice> heartDevices;
	private ArrayAdapter<HeartDevice> heartListAdapter;

	private HeartThread heartThread;

	public HeartFragment()
	{
		super();
	}

	public HeartFragment(MoveBotActivity act)
	{
		this.act = act;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.main_heart, container, false);
		heartScan = (Button) rootView.findViewById(R.id.heartScan);
		heartList = (ListView) rootView.findViewById(R.id.heartList);
		heartDevices = new ArrayList<>();
		heartListAdapter = new ArrayAdapter<>(act, android.R.layout.simple_list_item_1, heartDevices);
		heartList.setAdapter(heartListAdapter);
		heartList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				Log.d("HeartFragment", "Selecting device");
				if(heartThread != null)
				{
					heartThread.running = false;
					heartThread = null;
				}
				heartThread = new HeartThread();
				heartThread.device = heartDevices.get(position);
				heartThread.start();
			}
		});
		heartScan.setOnClickListener(this);
		return rootView;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onClick(View view)
	{
		Log.d("HeartFragment", "Scan button clicked");
		final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(bluetoothAdapter == null)
		{
			new NoBluetoothDialogFragment().show(act.getSupportFragmentManager(), "bluetooth");
			return;
		}
		if(!bluetoothAdapter.isEnabled())
		{
			new BluetoothDisabledDialogFragment().show(act.getSupportFragmentManager(), "disabled");
			return;
		}
		if(!act.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
		{
			new NoLeDialogFragment().show(act.getSupportFragmentManager(), "le");
			return;
		}
		Log.d("HeartFragment", "Starting scan");
		heartScan.setClickable(false);
		heartScan.setText("Scanning...");
		heartDevices.clear();
		heartListAdapter.notifyDataSetChanged();
		bluetoothAdapter.startLeScan(new UUID[]{UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")}, this);
		final HeartFragment t = this;
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run()
			{
				Log.d("HeartFragment", "Stopping scan");
				bluetoothAdapter.stopLeScan(t);
				heartScan.setText("Scan");
				heartScan.setClickable(true);
			}
		}, 5000);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
	{
		Log.d("HeartFragment", "Bluetooth LE device found");
		BluetoothGatt bluetoothGatt = device.connectGatt(act, false, new BluetoothGattCallback() {
			@Override
			public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
			{
				if(newState == BluetoothProfile.STATE_CONNECTED)
				{
					Log.d("HeartFragment", "Connected to LE device");
					gatt.discoverServices();
				}
			}

			@Override
			public void onServicesDiscovered(BluetoothGatt gatt, int status)
			{
				if(status == BluetoothGatt.GATT_SUCCESS)
				{
					BluetoothGattCharacteristic characteristic = null;
					for(BluetoothGattService service : gatt.getServices())
					{
						characteristic = service.getCharacteristic(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"));
						if(characteristic != null)
						{
							break;
						}
					}
					if(characteristic != null)
					{
						Log.d("HeartFragment", "Found device with HRM characteristic");
						HeartDevice device = new HeartDevice();
						device.bluetoothGatt = gatt;
						device.characteristic = characteristic;
						heartDevices.add(device);
						act.runOnUiThread(new Runnable() {
							@Override
							public void run()
							{
								heartListAdapter.notifyDataSetChanged();
							}
						});
					}
					else
					{
						Log.d("HeartFragment", "Device does not have HRM characteristic");
						gatt.disconnect();
					}
				}
				else
				{
					Log.w("HeartFragment", "Failed to discover device services");
					gatt.disconnect();
				}
			}

			@Override
			public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
			{
				if(status == BluetoothGatt.GATT_SUCCESS)
				{
					int flag = characteristic.getProperties();
					int format = -1;
					if((flag & 0x01) != 0)
					{
						format = BluetoothGattCharacteristic.FORMAT_UINT16;
					}
					else
					{
						format = BluetoothGattCharacteristic.FORMAT_UINT8;
					}
					final int heartRate = characteristic.getIntValue(format, 1);
					act.runOnUiThread(new Runnable() {
						@Override
						public void run()
						{
							act.updateHeart(heartRate);
						}
					});
				}
			}
		});
	}
}
