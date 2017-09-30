/**
 * Copyright (C) 2017 The LineageOS Project
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

package org.mokee.internal.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.UserHandle;
import android.util.Slog;

import mokee.providers.MKSettings;

import org.mokee.internal.notification.LedValues;
import org.mokee.internal.notification.LightsCapabilities;

public final class MKBatteryLights {
    private final String TAG = "MKBatteryLights";
    private final boolean DEBUG = false;

    // Battery light capabilities.
    private final boolean mMultiColorLed;
    private final boolean mUseSegmentedBatteryLed;

    // Battery light intended operational state.
    private boolean mLightEnabled = false; // Disable until observer is started
    private boolean mLedPulseEnabled;
    private int mBatteryLowARGB;
    private int mBatteryMediumARGB;
    private int mBatteryFullARGB;

    private final Context mContext;

    public interface LedUpdater {
        public void update();
    }
    private final LedUpdater mLedUpdater;

    public MKBatteryLights(Context context, LedUpdater ledUpdater) {
        mContext = context;
        mLedUpdater = ledUpdater;

        // Does the device support changing battery LED colors?
        mMultiColorLed = LightsCapabilities.supports(
                mContext, LightsCapabilities.LIGHTS_RGB_BATTERY_LED);

        // Does the device have segmented battery LED support? In this case, we send the level
        // in the alpha channel of the color and let the HAL sort it out.
        mUseSegmentedBatteryLed = LightsCapabilities.supports(
                mContext, LightsCapabilities.LIGHTS_SEGMENTED_BATTERY_LED);

        SettingsObserver observer = new SettingsObserver(new Handler());
        observer.observe();
    }

    public void calcLights(LedValues ledValues, int level, int status, boolean low) {
        if (DEBUG) {
            Slog.i(TAG, "calcLights input: ledValues={ " + ledValues + " } level="
                    + level + " status=" + status + " low=" + low);
        }
        // The only meaningful ledValues values received by frameworks BatteryService
        // are the pulse times (for low battery). Explicitly set enabled state and
        // color to ensure that we arrive at a deterministic outcome.
        ledValues.setEnabled(false);
        ledValues.setColor(0);

        if (!mLightEnabled) {
            return;
        }

        ledValues.setBrightness(mUseSegmentedBatteryLed ?
                level : LedValues.LIGHT_BRIGHTNESS_MAXIMUM);

        if (low) {
            if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                // Battery is charging and low.
                ledValues.setColor(mBatteryLowARGB);
                ledValues.setSolid();
            } else if (mLedPulseEnabled) {
                // Battery is low, not charging and pulse is enabled
                // (pulsing values are set by frameworks BatteryService).
                ledValues.setColor(mBatteryLowARGB);
            }
        } else if (status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL) {
            if (status == BatteryManager.BATTERY_STATUS_FULL || level >= 90) {
                // Battery is full or charging and nearly full.
                ledValues.setColor(mBatteryFullARGB);
                ledValues.setSolid();
            } else {
                // Battery is charging and not nearly full.
                ledValues.setColor(mBatteryMediumARGB);
                ledValues.setSolid();
            }
        }

        // If a color was set, enable light.
        if (ledValues.getColor() != 0) {
            ledValues.setEnabled(true);
        }
        if (DEBUG) {
            Slog.i(TAG, "calcLights output: ledValues={ " + ledValues + " }");
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();

            // Battery light enabled
            resolver.registerContentObserver(MKSettings.System.getUriFor(
                    MKSettings.System.BATTERY_LIGHT_ENABLED), false, this,
                   UserHandle.USER_ALL);

            // Low battery pulse
            resolver.registerContentObserver(MKSettings.System.getUriFor(
                    MKSettings.System.BATTERY_LIGHT_PULSE), false, this,
                UserHandle.USER_ALL);

            // Light colors
            if (mMultiColorLed) {
                resolver.registerContentObserver(MKSettings.System.getUriFor(
                        MKSettings.System.BATTERY_LIGHT_LOW_COLOR), false, this,
                        UserHandle.USER_ALL);
                resolver.registerContentObserver(MKSettings.System.getUriFor(
                        MKSettings.System.BATTERY_LIGHT_MEDIUM_COLOR), false, this,
                        UserHandle.USER_ALL);
                resolver.registerContentObserver(MKSettings.System.getUriFor(
                        MKSettings.System.BATTERY_LIGHT_FULL_COLOR), false, this,
                        UserHandle.USER_ALL);
            }

            update();
        }

        @Override
        public void onChange(boolean selfChange) {
            update();
        }

        private void update() {
            ContentResolver resolver = mContext.getContentResolver();
            Resources res = mContext.getResources();

            // Battery light enabled
            mLightEnabled = MKSettings.System.getInt(resolver,
                    MKSettings.System.BATTERY_LIGHT_ENABLED, 1) != 0;

            // Low battery pulse
            mLedPulseEnabled = MKSettings.System.getInt(resolver,
                        MKSettings.System.BATTERY_LIGHT_PULSE, 1) != 0;

            // Light colors
            mBatteryLowARGB = MKSettings.System.getInt(resolver,
                    MKSettings.System.BATTERY_LIGHT_LOW_COLOR, res.getInteger(
                    com.android.internal.R.integer.config_notificationsBatteryLowARGB));
            mBatteryMediumARGB = MKSettings.System.getInt(resolver,
                    MKSettings.System.BATTERY_LIGHT_MEDIUM_COLOR, res.getInteger(
                    com.android.internal.R.integer.config_notificationsBatteryMediumARGB));
            mBatteryFullARGB = MKSettings.System.getInt(resolver,
                    MKSettings.System.BATTERY_LIGHT_FULL_COLOR, res.getInteger(
                    com.android.internal.R.integer.config_notificationsBatteryFullARGB));

            mLedUpdater.update();
        }
    }
}
