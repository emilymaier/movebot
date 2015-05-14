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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.pm.PackageManager;
import android.os.Handler;

import java.util.UUID;

/**
 * Manages the heart rate monitor.
 */
public class HeartMonitor implements BluetoothAdapter.LeScanCallback
{
	private class HeartThread extends Thread
	{
		public volatile boolean running = true;

		@Override
		public void run()
		{
			if(!running)
			{
				return;
			}
			bluetoothGatt.readCharacteristic(characteristic);
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
	private BluetoothAdapter adapter;
	private BluetoothGatt bluetoothGatt;
	private BluetoothGattCharacteristic characteristic;
	private HeartThread loop;

	public HeartMonitor(MoveBotActivity act, BluetoothAdapter adapter)
	{
		this.act = act;
		this.adapter = adapter;
	}

	@SuppressWarnings("deprecation")
	public void start()
	{
		if(!act.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
		{
			return;
		}
		this.adapter.startLeScan(new UUID[]{UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")}, this);
		final HeartMonitor t = this;
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run()
			{
				t.adapter.stopLeScan(t);
			}
		}, 10000);
	}

	public void stop()
	{
		if(loop != null)
		{
			loop.running = false;
			loop = null;
		}
		if(bluetoothGatt != null)
		{
			bluetoothGatt.close();
			bluetoothGatt = null;
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
	{
		adapter.stopLeScan(this);
		bluetoothGatt = device.connectGatt(act, false, new BluetoothGattCallback() {
			@Override
			public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
			{
				if(newState == BluetoothProfile.STATE_CONNECTED)
				{
					bluetoothGatt.discoverServices();
				}
			}

			@Override
			public void onServicesDiscovered(BluetoothGatt gatt, int status)
			{
				if(status == BluetoothGatt.GATT_SUCCESS)
				{
					for(BluetoothGattService service : bluetoothGatt.getServices())
					{
						characteristic = service.getCharacteristic(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"));
					}
					loop = new HeartThread();
					loop.start();
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
					act.updateHeart(characteristic.getIntValue(format, 1));
				}
			}
		});
	}
}
