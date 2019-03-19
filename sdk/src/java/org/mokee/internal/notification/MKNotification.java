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

public class MKNotification {

    ///////////////////////////////////////////////////
    // MoKee-specific Notification bundle extras
    ///////////////////////////////////////////////////

    /**
     * Used by light picker in Settings to force
     * notification lights on when screen is on.
     */
    public static final String EXTRA_FORCE_SHOW_LIGHTS = "mokee.forceShowLights";

    /**
     * Used by light picker in Settings to force
     * a specific light brightness.
     */
    public static final String EXTRA_FORCE_LIGHT_BRIGHTNESS = "mokee.forceLightBrightness";

    /**
     * Used by light picker in Settings to force
     * a specific light color.
     */
    public static final String EXTRA_FORCE_COLOR = "mokee.forceColor";

    /**
     * Used by light picker in Settings to force
     * a specific light on duration.
     *
     * Value must be greater than or equal to 0.
     */
    public static final String EXTRA_FORCE_LIGHT_ON_MS = "mokee.forceLightOnMs";

    /**
     * Used by light picker in Settings to force
     * a specific light off duration.
     *
     * Value must be greater than or equal to 0.
     */
    public static final String EXTRA_FORCE_LIGHT_OFF_MS = "mokee.forceLightOffMs";
}
