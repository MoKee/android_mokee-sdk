/*
 * Copyright (C) 2018 The LineageOS Project
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

package org.mokee.platform.internal;

import android.content.Context;
import android.os.SystemProperties;

import mokee.app.MoKeeContextConstants;
import mokee.providers.MoKeeSettings;

/** @hide */
public class MoKeeSettingsService extends MoKeeSystemService {

    private static final String TAG = MoKeeSettingsService.class.getSimpleName();

    private final Context mContext;

    public MoKeeSettingsService(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public String getFeatureDeclaration() {
        return MoKeeContextConstants.Features.SETTINGS;
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_BOOT_COMPLETED) {
            // Load custom hostname
            String hostname = MoKeeSettings.Secure.getString(mContext.getContentResolver(),
                    MoKeeSettings.Secure.DEVICE_HOSTNAME);
            if (hostname != null) {
                SystemProperties.set("net.hostname", hostname);
            }
        }
    }

    @Override
    public void onStart() {
    }
}
