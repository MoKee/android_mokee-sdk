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
import android.content.Context;
import android.os.IBinder;

import com.mokee.os.Build;
import com.mokee.security.License;
import com.mokee.security.LicenseInfo;

import java.io.File;
import java.util.Arrays;

import mokee.app.MKContextConstants;
import mokee.license.DonationInfo;
import mokee.license.ILicenseInterface;
import mokee.license.LicenseInterface;

public class LicenseInterfaceService extends MKSystemService {
    private static final String TAG = "MKLicenseInterfaceService";

    private static final String CHANNEL_NAME = "LicenseInterface";
    private static final int NOTIFCATION_ID = 89;

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
        updateLicenseInfo();
        if (!mDonationInfo.isAdvanced()) {
            postNotificationForFeatureInternal();
        }
    }

    /* Public methods implementation */

    private boolean postNotificationForFeatureInternal() {

        String title = mContext.getString(R.string.license_notification_title);
        String message = mContext.getString(R.string.license_notification_content);

        Notification.Builder notification = new Notification.Builder(mContext, CHANNEL_NAME)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setAutoCancel(false)
                .setOngoing(true)
                .setColor(mContext.getColor(R.color.color_error))
                .setSmallIcon(R.drawable.ic_warning);

        createNotificationChannelIfNeeded();
        mNotificationManager.notify(NOTIFCATION_ID, notification.build());
        return true;
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

    private void updateLicenseInfo() {
        mDonationInfo.setPaid(getTotalAmountPaid(mContext).intValue());
        mDonationInfo.setBasic(mDonationInfo.getPaid() >= LicenseInterface.DONATION_BASIC);
        mDonationInfo.setAdvanced(mDonationInfo.getPaid() >= LicenseInterface.DONATION_ADVANCED);
    }

    private Float getTotalAmountPaid(Context context) {
        if (new File(LicenseInterface.LICENSE_FILE).exists()) {
            try {
                LicenseInfo licenseInfo = License.readLicense(LicenseInterface.LICENSE_FILE, LicenseInterface.LICENSE_PUB_KEY);
                String unique_ids = Build.getUniqueIDS(context);
                if (Arrays.asList(unique_ids.split(",")).contains(licenseInfo.getUniqueID())
                        && licenseInfo.getPackageName().equals(context.getPackageName())) {
                    return licenseInfo.getPrice();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0f;
    }

    /* Service */

    private final IBinder mService = new ILicenseInterface.Stub() {

    };

}
