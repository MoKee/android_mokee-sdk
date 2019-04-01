/*
 * Copyright (C) 2019 The MoKee Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mokee.platform.internal;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.text.TextUtils;

import com.mokee.os.Build;
import com.mokee.security.License;

import java.util.Locale;

import mokee.app.MKContextConstants;
import mokee.license.DonationInfo;
import mokee.license.ILicenseInterface;
import mokee.license.LicenseInterface;
import mokee.providers.MKSettings;

public class LicenseInterfaceService extends MKSystemService {
    private static final String TAG = "MKLicenseInterfaceService";

    private static final String CHANNEL_NAME = "LicenseInterface";
    private static final int NOTIFICATION_ID = 2019;

    private static final String INTENT_SETTINGS = "android.settings.SYSTEM_UPDATE_SETTINGS";

    private Context mContext;
    private NotificationManager mNotificationManager = null;
    private DonationInfo mDonationInfo = new DonationInfo();

    public LicenseInterfaceService(Context context) {
        super(context);
        mContext = context;
        publishBinderService(MKContextConstants.MK_LICENSE_INTERFACE, mService);
    }

    @Override
    public String getFeatureDeclaration() {
        return MKContextConstants.Features.LICENSE;
    }

    @Override
    public void onStart() {
        mNotificationManager = mContext.getSystemService(NotificationManager.class);
        updateLicenseInfoInternal();
        if (!LicenseInterface.isPremiumVersion()) return;
        if (!mDonationInfo.isAdvanced()) {
            postNotificationForFeatureInternal();
            IntentFilter filter = new IntentFilter();
            filter.addAction(LicenseInterface.ACTION_LICENSE_CHANGED);
            mContext.registerReceiver(mIntentReceiver, filter);
        }
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(LicenseInterface.ACTION_LICENSE_CHANGED)) {
                String deviceLicenseKey = intent.getStringExtra("data");
                MKSettings.Secure.putString(mContext.getContentResolver(),
                        MKSettings.Secure.DEVICE_LICENSE_KEY, deviceLicenseKey);
                updateLicenseInfoInternal();
                if (mDonationInfo.isAdvanced()) {
                    cancelNotificationForFeatureInternal();
                    mContext.unregisterReceiver(mIntentReceiver);
                }
            }
        }
    };

    /* Public methods implementation */

    private void postNotificationForFeatureInternal() {

        String title = mContext.getString(R.string.license_notification_title);
        String message = mContext.getString(R.string.license_notification_content, LicenseInterface.DONATION_ADVANCED);

        Intent mainIntent = new Intent(INTENT_SETTINGS);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pMainIntent = PendingIntent.getActivity(mContext, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder notification = new Notification.Builder(mContext, CHANNEL_NAME)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(pMainIntent)
                .setColor(mContext.getColor(R.color.color_error))
                .setSmallIcon(R.drawable.ic_warning);

        createNotificationChannelIfNeeded();
        mNotificationManager.notify(NOTIFICATION_ID, notification.build());
    }

    private void cancelNotificationForFeatureInternal() {
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    private void createNotificationChannelIfNeeded() {
        NotificationChannel channel = mNotificationManager.getNotificationChannel(CHANNEL_NAME);
        if (channel != null) {
            return;
        }

        String name = mContext.getString(R.string.license_notification_channel);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel licenseChannel = new NotificationChannel(CHANNEL_NAME,
                name, importance);
        licenseChannel.setBlockableSystem(false);
        mNotificationManager.createNotificationChannel(licenseChannel);
    }

    private void updateLicenseInfoInternal() {
        mDonationInfo.setPaid(getTotalAmountPaid().intValue());
        mDonationInfo.setBasic(mDonationInfo.getPaid() >= LicenseInterface.DONATION_BASIC);
        mDonationInfo.setAdvanced(mDonationInfo.getPaid() >= LicenseInterface.DONATION_ADVANCED);
    }

    private Float getTotalAmountPaid() {
        String deviceLicenseKey = MKSettings.Secure.getString(mContext.getContentResolver(),
                MKSettings.Secure.DEVICE_LICENSE_KEY);
        if (!TextUtils.isEmpty(deviceLicenseKey)) {
            try {
                return License.loadLicenseFromContent(deviceLicenseKey, LicenseInterface.LICENSE_PUB_KEY).getPrice();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0f;
    }

    /* Service */

    private final IBinder mService = new ILicenseInterface.Stub() {

        @Override
        public void updateLicenseInfo() {
            updateLicenseInfoInternal();
            if (mDonationInfo.isAdvanced()
                    && TextUtils.equals(Build.RELEASE_TYPE.toLowerCase(Locale.ENGLISH), "premium")) {
                cancelNotificationForFeatureInternal();
            }
        }
    };

}
