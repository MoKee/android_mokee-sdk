/*
 * Copyright (C) 2017 The MoKee Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mokee.internal.preference.deviceinfo;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;

import mokee.preference.SelfRemovingPreference;

import org.mokee.platform.internal.R;

public class MKVersionPreference extends SelfRemovingPreference
        implements Preference.OnPreferenceClickListener {

    private static final String TAG = "MKVersionPreference";

    private static final String KEY_MK_VERSION_PROP = "ro.mk.version";

    private static final String PLATLOGO_PACKAGE_NAME = "android";
    private static final String PLATLOGO_ACTIVITY_CLASS = com.android.internal.app.PlatLogoActivity.class.getName();

    private long[] mHits = new long[3];

    public MKVersionPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MKVersionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MKVersionPreference(Context context) {
        super(context);
    }

    @Override
    public void onAttached() {
        super.onAttached();

        setOnPreferenceClickListener(this);
        setTitle(R.string.mk_version);
        setSummary(SystemProperties.get(KEY_MK_VERSION_PROP,
                getContext().getResources().getString(R.string.unknown)));
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
        if (mHits[0] >= (SystemClock.uptimeMillis() - 500)) {
            launchLogoActivity();
        }
        return true; // handled
    }

    private void launchLogoActivity() {
        final Intent intent = new Intent(Intent.ACTION_MAIN)
                .setClassName(PLATLOGO_PACKAGE_NAME, PLATLOGO_ACTIVITY_CLASS);
        try {
            getContext().startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Unable to start activity " + intent.toString());
        }
    }
}
