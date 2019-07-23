/**
 * Copyright (c) 2015, The MoKee Open Source Project
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

package mokee.app;

import android.annotation.SdkConstant;

/**
 * @hide
 * TODO: We need to somehow make these managers accessible via getSystemService
 */
public final class MKContextConstants {

    /**
     * @hide
     */
    private MKContextConstants() {
        // Empty constructor
    }

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link mokee.app.ProfileManager} for informing the user of
     * background events.
     *
     * @see android.content.Context#getSystemService
     * @see mokee.app.ProfileManager
     *
     * @hide
     */
    public static final String MK_PROFILE_SERVICE = "profile";

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link mokee.hardware.MKHardwareManager} to manage the extended
     * hardware features of the device.
     *
     * @see android.content.Context#getSystemService
     * @see mokee.hardware.MKHardwareManager
     *
     * @hide
     */
    public static final String MK_HARDWARE_SERVICE = "mkhardware";

    /**
     * Control device power profile and characteristics.
     *
     * @hide
     */
    public static final String MK_PERFORMANCE_SERVICE = "mkperformance";

    /**
     * Manages composed icons
     *
     * @hide
     */
    public static final String MK_ICON_CACHE_SERVICE = "mkiconcache";

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link mokee.weather.MKWeatherManager} to manage the weather service
     * settings and request weather updates
     *
     * @see android.content.Context#getSystemService
     * @see mokee.weather.MKWeatherManager
     *
     * @hide
     */
    public static final String MK_WEATHER_SERVICE = "mkweather";

    /**
     * Manages license info
     *
     * @hide
     */
    public static final String MK_LICENSE_INTERFACE = "mklicense";

    /**
     * Manages display color adjustments
     *
     * @hide
     */
    public static final String MK_LIVEDISPLAY_SERVICE = "mklivedisplay";


    /**
     * Manages enhanced audio functionality
     *
     * @hide
     */
    public static final String MK_AUDIO_SERVICE = "mkaudio";

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link mokee.app.StyleInterface} interact with system style.
     *
     * @see android.content.Context#getSystemService
     * @see mokee.app.StyleInterface
     *
     * @hide
     */
    public static final String MK_STYLE_INTERFACE = "mkstyle";

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link mokee.trust.TrustInterface} to access the Trust interface.
     *
     * @see android.content.Context#getSystemService
     * @see mokee.trust.TrustInterface
     *
     * @hide
     */
    public static final String MK_TRUST_INTERFACE = "mktrust";

    /**
     * Features supported by the MoKee SDK.
     */
    public static class Features {
        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the hardware abstraction
         * framework service utilized by the mokee sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String HARDWARE_ABSTRACTION = "org.mokee.hardware";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the mk profiles service
         * utilized by the mokee sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String PROFILES = "org.mokee.profiles";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the mk performance service
         * utilized by the mokee sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String PERFORMANCE = "org.mokee.performance";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the mk weather weather
         * service utilized by the mokee sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String WEATHER_SERVICES = "org.mokee.weather";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the mk License service
         * utilized by the mokee sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String LICENSE = "org.mokee.license";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the LiveDisplay service
         * utilized by the mokee sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String LIVEDISPLAY = "org.mokee.livedisplay";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the MK audio extensions
         * utilized by the mokee sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String AUDIO = "org.mokee.audio";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the MK style service
         * utilized by the mokee sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String STYLES = "org.mokee.style";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the MK trust service
         * utilized by the mokee sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String TRUST = "org.mokee.trust";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the MK settings service
         * utilized by the mokee sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String SETTINGS = "org.mokee.settings";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the MK
         * fingerprint in screen utilized by the mokee sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String FOD = "vendor.mokee.biometrics.fingerprint.inscreen";
    }
}
