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
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Displays the about screen.
 */
public class AboutActivity extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		try
		{
			TextView aboutVersion = (TextView) findViewById(R.id.aboutVersion);
			aboutVersion.setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
			TextView aboutVersionCode = (TextView) findViewById(R.id.aboutVersionCode);
			aboutVersionCode.setText(String.valueOf(getPackageManager().getPackageInfo(getPackageName(), 0).versionCode));
		}
		catch(NameNotFoundException e)
		{
		}
	}
}
