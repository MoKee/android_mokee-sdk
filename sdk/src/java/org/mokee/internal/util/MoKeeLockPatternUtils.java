/*
 * Copyright (C) 2015 The CyanogenMod Project
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
package org.mokee.internal.util;

import android.content.Context;

import com.android.internal.widget.LockPatternUtils;

import mokee.providers.MoKeeSettings;

public class MoKeeLockPatternUtils extends LockPatternUtils {
    public MoKeeLockPatternUtils(Context context) {
        super(context);
    }

    public boolean shouldPassToSecurityView(int userId) {
        return getBoolean(MoKeeSettings.Secure.LOCK_PASS_TO_SECURITY_VIEW, false, userId);
    }

    public void setPassToSecurityView(boolean enabled, int userId) {
        setBoolean(MoKeeSettings.Secure.LOCK_PASS_TO_SECURITY_VIEW, enabled, userId);
    }
}
