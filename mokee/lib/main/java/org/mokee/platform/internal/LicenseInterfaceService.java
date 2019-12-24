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
import android.mokee.utils.MoKeeUtils;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.mokee.center.model.DonationInfo;
import com.mokee.utils.DonationUtils;
import com.mokee.security.LicenseUtils;

import mokee.app.MKContextConstants;
import mokee.license.ILicenseInterface;
import mokee.license.LicenseConstants;

public class LicenseInterfaceService extends MKSystemService {

    private static final String TAG = "MKLicenseInterfaceService";

    private static final String CHANNEL_NAME = "LicenseInterface";
    private static final int NOTIFICATION_ID = 2019;

    private static final String INTENT_SETTINGS = "android.settings.SYSTEM_UPDATE_SETTINGS";

    private Context mContext;
    private NotificationManager mNotificationManager = null;
    private DonationInfo mDonationInfo = new DonationInfo();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(LicenseConstants.ACTION_LICENSE_CHANGED, action)) {
                LicenseUtils.copyLicenseFile(context, intent.getData(), getLicensePath());
                DonationUtils.updateDonationInfo(mContext, mDonationInfo, getLicensePath(), LicenseConstants.LICENSE_PUB_KEY);
                if (mDonationInfo.isAdvanced()) {
                    cancelNotificationForFeatureInternal();
                    mContext.unregisterReceiver(mIntentReceiver);
                }
            }
        }
    };
    private Runnable mToastRunnable = new Runnable() {
        @Override
        public void run() {
            String message = mContext.getString(R.string.license_notification_content, LicenseConstants.DONATION_ADVANCED);
            Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
        }
    };
    /* Service */
    private final IBinder mService = new ILicenseInterface.Stub() {

        @Override
        public void licenseVerification() {
            if (!mDonationInfo.isAdvanced()) {
                mHandler.post(mToastRunnable);
            }
        }
    };

    public LicenseInterfaceService(Context context) {
        super(context);
        mContext = context;
        publishBinderService(MKContextConstants.MK_LICENSE_INTERFACE, mService);
    }

    public static String getLicensePath() {
        return String.join("/", Environment.getDataSystemDirectory().getAbsolutePath(), "mokee.lic");
    }

    /* Public methods implementation */

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_BOOT_COMPLETED) {
            DonationUtils.updateDonationInfo(mContext, mDonationInfo, getLicensePath(), LicenseConstants.LICENSE_PUB_KEY);
            if (!mDonationInfo.isAdvanced()) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(LicenseConstants.ACTION_LICENSE_CHANGED);
                try {
                    filter.addDataType("application/octet-stream");
                } catch (IntentFilter.MalformedMimeTypeException e) {
                    e.printStackTrace();
                }
                mContext.registerReceiver(mIntentReceiver, filter);

                if (!MoKeeUtils.isPremiumVersion()) return;
                postNotificationForFeatureInternal();
            }
        }
    }

    @Override
    public String getFeatureDeclaration() {
        return MKContextConstants.Features.LICENSE;
    }

    @Override
    public void onStart() {
        mNotificationManager = mContext.getSystemService(NotificationManager.class);
    }

    private void postNotificationForFeatureInternal() {
        String title = mContext.getString(R.string.license_notification_title);
        String message = mContext.getString(R.string.license_notification_content, LicenseConstants.DONATION_ADVANCED);

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
}
