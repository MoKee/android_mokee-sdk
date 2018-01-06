/*
 * Copyright (C) 2015-2017 The MoKee Open Source Project
 *               2017 The LineageOS Project
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
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Range;

import com.android.server.SystemService;

import mokee.app.MKContextConstants;
import mokee.hardware.IMKHardwareService;
import mokee.hardware.MKHardwareManager;
import mokee.hardware.DisplayMode;
import mokee.hardware.IThermalListenerCallback;
import mokee.hardware.ThermalListenerCallback;
import mokee.hardware.HSIC;
import mokee.hardware.TouchscreenGesture;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mokee.hardware.AdaptiveBacklight;
import org.mokee.hardware.AutoContrast;
import org.mokee.hardware.ColorBalance;
import org.mokee.hardware.ColorEnhancement;
import org.mokee.hardware.DisplayColorCalibration;
import org.mokee.hardware.DisplayGammaCalibration;
import org.mokee.hardware.DisplayModeControl;
import org.mokee.hardware.HighTouchSensitivity;
import org.mokee.hardware.KeyDisabler;
import org.mokee.hardware.LongTermOrbits;
import org.mokee.hardware.PersistentStorage;
import org.mokee.hardware.PictureAdjustment;
import org.mokee.hardware.SerialNumber;
import org.mokee.hardware.SunlightEnhancement;
import org.mokee.hardware.ThermalMonitor;
import org.mokee.hardware.ThermalUpdateCallback;
import org.mokee.hardware.TouchscreenGestures;
import org.mokee.hardware.TouchscreenHovering;
import org.mokee.hardware.UniqueDeviceId;
import org.mokee.hardware.VibratorHW;

/** @hide */
public class MKHardwareService extends MKSystemService implements ThermalUpdateCallback {

    private static final boolean DEBUG = true;
    private static final String TAG = MKHardwareService.class.getSimpleName();

    private final Context mContext;
    private final MKHardwareInterface mMkHwImpl;
    private int mCurrentThermalState = ThermalListenerCallback.State.STATE_UNKNOWN;
    private RemoteCallbackList<IThermalListenerCallback> mRemoteCallbackList;

    private final ArrayMap<String, String> mDisplayModeMappings =
            new ArrayMap<String, String>();
    private final boolean mFilterDisplayModes;

    private interface MKHardwareInterface {
        public int getSupportedFeatures();
        public boolean get(int feature);
        public boolean set(int feature, boolean enable);

        public int[] getDisplayColorCalibration();
        public boolean setDisplayColorCalibration(int[] rgb);

        public int getNumGammaControls();
        public int[] getDisplayGammaCalibration(int idx);
        public boolean setDisplayGammaCalibration(int idx, int[] rgb);

        public int[] getVibratorIntensity();
        public boolean setVibratorIntensity(int intensity);

        public String getLtoSource();
        public String getLtoDestination();
        public long getLtoDownloadInterval();

        public String getSerialNumber();
        public String getUniqueDeviceId();

        public boolean requireAdaptiveBacklightForSunlightEnhancement();
        public boolean isSunlightEnhancementSelfManaged();

        public DisplayMode[] getDisplayModes();
        public DisplayMode getCurrentDisplayMode();
        public DisplayMode getDefaultDisplayMode();
        public boolean setDisplayMode(DisplayMode mode, boolean makeDefault);

        public boolean writePersistentBytes(String key, byte[] value);
        public byte[] readPersistentBytes(String key);

        public int getColorBalanceMin();
        public int getColorBalanceMax();
        public int getColorBalance();
        public boolean setColorBalance(int value);

        public HSIC getPictureAdjustment();
        public HSIC getDefaultPictureAdjustment();
        public boolean setPictureAdjustment(HSIC hsic);
        public List<Range<Float>> getPictureAdjustmentRanges();

        public TouchscreenGesture[] getTouchscreenGestures();
        public boolean setTouchscreenGestureEnabled(TouchscreenGesture gesture, boolean state);
    }

    private class LegacyMKHardware implements MKHardwareInterface {

        private int mSupportedFeatures = 0;

        public LegacyMKHardware() {
            if (AdaptiveBacklight.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_ADAPTIVE_BACKLIGHT;
            if (ColorEnhancement.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_COLOR_ENHANCEMENT;
            if (DisplayColorCalibration.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_DISPLAY_COLOR_CALIBRATION;
            if (DisplayGammaCalibration.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_DISPLAY_GAMMA_CALIBRATION;
            if (HighTouchSensitivity.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_HIGH_TOUCH_SENSITIVITY;
            if (KeyDisabler.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_KEY_DISABLE;
            if (LongTermOrbits.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_LONG_TERM_ORBITS;
            if (SerialNumber.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_SERIAL_NUMBER;
            if (SunlightEnhancement.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT;
            if (VibratorHW.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_VIBRATOR;
            if (TouchscreenHovering.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_TOUCH_HOVERING;
            if (AutoContrast.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_AUTO_CONTRAST;
            if (DisplayModeControl.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_DISPLAY_MODES;
            if (PersistentStorage.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_PERSISTENT_STORAGE;
            if (ThermalMonitor.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_THERMAL_MONITOR;
            if (UniqueDeviceId.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_UNIQUE_DEVICE_ID;
            if (ColorBalance.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_COLOR_BALANCE;
            if (PictureAdjustment.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_PICTURE_ADJUSTMENT;
            if (TouchscreenGestures.isSupported())
                mSupportedFeatures |= MKHardwareManager.FEATURE_TOUCHSCREEN_GESTURES;
        }

        public int getSupportedFeatures() {
            return mSupportedFeatures;
        }

        public boolean get(int feature) {
            switch(feature) {
                case MKHardwareManager.FEATURE_ADAPTIVE_BACKLIGHT:
                    return AdaptiveBacklight.isEnabled();
                case MKHardwareManager.FEATURE_COLOR_ENHANCEMENT:
                    return ColorEnhancement.isEnabled();
                case MKHardwareManager.FEATURE_HIGH_TOUCH_SENSITIVITY:
                    return HighTouchSensitivity.isEnabled();
                case MKHardwareManager.FEATURE_KEY_DISABLE:
                    return KeyDisabler.isActive();
                case MKHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT:
                    return SunlightEnhancement.isEnabled();
                case MKHardwareManager.FEATURE_TOUCH_HOVERING:
                    return TouchscreenHovering.isEnabled();
                case MKHardwareManager.FEATURE_AUTO_CONTRAST:
                    return AutoContrast.isEnabled();
                case MKHardwareManager.FEATURE_THERMAL_MONITOR:
                    return ThermalMonitor.isEnabled();
                default:
                    Log.e(TAG, "feature " + feature + " is not a boolean feature");
                    return false;
            }
        }

        public boolean set(int feature, boolean enable) {
            switch(feature) {
                case MKHardwareManager.FEATURE_ADAPTIVE_BACKLIGHT:
                    return AdaptiveBacklight.setEnabled(enable);
                case MKHardwareManager.FEATURE_COLOR_ENHANCEMENT:
                    return ColorEnhancement.setEnabled(enable);
                case MKHardwareManager.FEATURE_HIGH_TOUCH_SENSITIVITY:
                    return HighTouchSensitivity.setEnabled(enable);
                case MKHardwareManager.FEATURE_KEY_DISABLE:
                    return KeyDisabler.setActive(enable);
                case MKHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT:
                    return SunlightEnhancement.setEnabled(enable);
                case MKHardwareManager.FEATURE_TOUCH_HOVERING:
                    return TouchscreenHovering.setEnabled(enable);
                case MKHardwareManager.FEATURE_AUTO_CONTRAST:
                    return AutoContrast.setEnabled(enable);
                default:
                    Log.e(TAG, "feature " + feature + " is not a boolean feature");
                    return false;
            }
        }

        private int[] splitStringToInt(String input, String delimiter) {
            if (input == null || delimiter == null) {
                return null;
            }
            String strArray[] = input.split(delimiter);
            try {
                int intArray[] = new int[strArray.length];
                for(int i = 0; i < strArray.length; i++) {
                    intArray[i] = Integer.parseInt(strArray[i]);
                }
                return intArray;
            } catch (NumberFormatException e) {
                /* ignore */
            }
            return null;
        }

        private String rgbToString(int[] rgb) {
            StringBuilder builder = new StringBuilder();
            builder.append(rgb[MKHardwareManager.COLOR_CALIBRATION_RED_INDEX]);
            builder.append(" ");
            builder.append(rgb[MKHardwareManager.COLOR_CALIBRATION_GREEN_INDEX]);
            builder.append(" ");
            builder.append(rgb[MKHardwareManager.COLOR_CALIBRATION_BLUE_INDEX]);
            return builder.toString();
        }

        public int[] getDisplayColorCalibration() {
            int[] rgb = splitStringToInt(DisplayColorCalibration.getCurColors(), " ");
            if (rgb == null || rgb.length != 3) {
                Log.e(TAG, "Invalid color calibration string");
                return null;
            }
            int[] currentCalibration = new int[6];
            currentCalibration[MKHardwareManager.COLOR_CALIBRATION_RED_INDEX] = rgb[0];
            currentCalibration[MKHardwareManager.COLOR_CALIBRATION_GREEN_INDEX] = rgb[1];
            currentCalibration[MKHardwareManager.COLOR_CALIBRATION_BLUE_INDEX] = rgb[2];
            currentCalibration[MKHardwareManager.COLOR_CALIBRATION_DEFAULT_INDEX] =
                DisplayColorCalibration.getDefValue();
            currentCalibration[MKHardwareManager.COLOR_CALIBRATION_MIN_INDEX] =
                DisplayColorCalibration.getMinValue();
            currentCalibration[MKHardwareManager.COLOR_CALIBRATION_MAX_INDEX] =
                DisplayColorCalibration.getMaxValue();
            return currentCalibration;
        }

        public boolean setDisplayColorCalibration(int[] rgb) {
            return DisplayColorCalibration.setColors(rgbToString(rgb));
        }

        public int getNumGammaControls() {
            return DisplayGammaCalibration.getNumberOfControls();
        }

        public int[] getDisplayGammaCalibration(int idx) {
            int[] rgb = splitStringToInt(DisplayGammaCalibration.getCurGamma(idx), " ");
            if (rgb == null || rgb.length != 3) {
                Log.e(TAG, "Invalid gamma calibration string");
                return null;
            }
            int[] currentCalibration = new int[5];
            currentCalibration[MKHardwareManager.GAMMA_CALIBRATION_RED_INDEX] = rgb[0];
            currentCalibration[MKHardwareManager.GAMMA_CALIBRATION_GREEN_INDEX] = rgb[1];
            currentCalibration[MKHardwareManager.GAMMA_CALIBRATION_BLUE_INDEX] = rgb[2];
            currentCalibration[MKHardwareManager.GAMMA_CALIBRATION_MIN_INDEX] =
                DisplayGammaCalibration.getMinValue(idx);
            currentCalibration[MKHardwareManager.GAMMA_CALIBRATION_MAX_INDEX] =
                DisplayGammaCalibration.getMaxValue(idx);
            return currentCalibration;
        }

        public boolean setDisplayGammaCalibration(int idx, int[] rgb) {
            return DisplayGammaCalibration.setGamma(idx, rgbToString(rgb));
        }

        public int[] getVibratorIntensity() {
            int[] vibrator = new int[5];
            vibrator[MKHardwareManager.VIBRATOR_INTENSITY_INDEX] = VibratorHW.getCurIntensity();
            vibrator[MKHardwareManager.VIBRATOR_DEFAULT_INDEX] = VibratorHW.getDefaultIntensity();
            vibrator[MKHardwareManager.VIBRATOR_MIN_INDEX] = VibratorHW.getMinIntensity();
            vibrator[MKHardwareManager.VIBRATOR_MAX_INDEX] = VibratorHW.getMaxIntensity();
            vibrator[MKHardwareManager.VIBRATOR_WARNING_INDEX] = VibratorHW.getWarningThreshold();
            return vibrator;
        }

        public boolean setVibratorIntensity(int intensity) {
            return VibratorHW.setIntensity(intensity);
        }

        public String getLtoSource() {
            return LongTermOrbits.getSourceLocation();
        }

        public String getLtoDestination() {
            File file = LongTermOrbits.getDestinationLocation();
            return file.getAbsolutePath();
        }

        public long getLtoDownloadInterval() {
            return LongTermOrbits.getDownloadInterval();
        }

        public String getSerialNumber() {
            return SerialNumber.getSerialNumber();
        }

        public String getUniqueDeviceId() {
            return UniqueDeviceId.getUniqueDeviceId();
        }

        public boolean requireAdaptiveBacklightForSunlightEnhancement() {
            return SunlightEnhancement.isAdaptiveBacklightRequired();
        }

        public boolean isSunlightEnhancementSelfManaged() {
            return SunlightEnhancement.isSelfManaged();
        }

        public DisplayMode[] getDisplayModes() {
            return DisplayModeControl.getAvailableModes();
        }

        public DisplayMode getCurrentDisplayMode() {
            return DisplayModeControl.getCurrentMode();
        }

        public DisplayMode getDefaultDisplayMode() {
            return DisplayModeControl.getDefaultMode();
        }

        public boolean setDisplayMode(DisplayMode mode, boolean makeDefault) {
            return DisplayModeControl.setMode(mode, makeDefault);
        }

        public boolean writePersistentBytes(String key, byte[] value) {
            return PersistentStorage.set(key, value);
        }

        public byte[] readPersistentBytes(String key) {
            return PersistentStorage.get(key);
        }

        public int getColorBalanceMin() {
            return ColorBalance.getMinValue();
        }

        public int getColorBalanceMax() {
            return ColorBalance.getMaxValue();
        }

        public int getColorBalance() {
            return ColorBalance.getValue();
        }

        public boolean setColorBalance(int value) {
            return ColorBalance.setValue(value);
        }

        public HSIC getPictureAdjustment() { return PictureAdjustment.getHSIC(); }

        public HSIC getDefaultPictureAdjustment() { return PictureAdjustment.getDefaultHSIC(); }

        public boolean setPictureAdjustment(HSIC hsic) { return PictureAdjustment.setHSIC(hsic); }

        public List<Range<Float>> getPictureAdjustmentRanges() {
            return Arrays.asList(
                    PictureAdjustment.getHueRange(),
                    PictureAdjustment.getSaturationRange(),
                    PictureAdjustment.getIntensityRange(),
                    PictureAdjustment.getContrastRange(),
                    PictureAdjustment.getSaturationThresholdRange());
        }

        public TouchscreenGesture[] getTouchscreenGestures() {
            return TouchscreenGestures.getAvailableGestures();
        }

        public boolean setTouchscreenGestureEnabled(TouchscreenGesture gesture, boolean state) {
            return TouchscreenGestures.setGestureEnabled(gesture, state);
        }
    }

    private MKHardwareInterface getImpl(Context context) {
        return new LegacyMKHardware();
    }

    public MKHardwareService(Context context) {
        super(context);
        mContext = context;
        mMkHwImpl = getImpl(context);
        publishBinderService(MKContextConstants.MK_HARDWARE_SERVICE, mService);

        final String[] mappings = mContext.getResources().getStringArray(
                org.mokee.platform.internal.R.array.config_displayModeMappings);
        if (mappings != null && mappings.length > 0) {
            for (String mapping : mappings) {
                String[] split = mapping.split(":");
                if (split.length == 2) {
                    mDisplayModeMappings.put(split[0], split[1]);
                }
            }
        }
        mFilterDisplayModes = mContext.getResources().getBoolean(
                org.mokee.platform.internal.R.bool.config_filterDisplayModes);
    }

    @Override
    public String getFeatureDeclaration() {
        return MKContextConstants.Features.HARDWARE_ABSTRACTION;
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_BOOT_COMPLETED) {
            Intent intent = new Intent(mokee.content.Intent.ACTION_INITIALIZE_MK_HARDWARE);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL,
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS);
        }
    }

    @Override
    public void onStart() {
        if (ThermalMonitor.isSupported()) {
            ThermalMonitor.initialize(this);
            mRemoteCallbackList = new RemoteCallbackList<IThermalListenerCallback>();
        }
    }

    @Override
    public void setThermalState(int state) {
        mCurrentThermalState = state;
        int i = mRemoteCallbackList.beginBroadcast();
        while (i > 0) {
            i--;
            try {
                mRemoteCallbackList.getBroadcastItem(i).onThermalChanged(state);
            } catch (RemoteException e) {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mRemoteCallbackList.finishBroadcast();
    }

    private DisplayMode remapDisplayMode(DisplayMode in) {
        if (in == null) {
            return null;
        }
        if (mDisplayModeMappings.containsKey(in.name)) {
            return new DisplayMode(in.id, mDisplayModeMappings.get(in.name));
        }
        if (!mFilterDisplayModes) {
            return in;
        }
        return null;
    }

    private final IBinder mService = new IMKHardwareService.Stub() {

        private boolean isSupported(int feature) {
            return (getSupportedFeatures() & feature) == feature;
        }

        @Override
        public int getSupportedFeatures() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            return mMkHwImpl.getSupportedFeatures();
        }

        @Override
        public boolean get(int feature) {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(feature)) {
                Log.e(TAG, "feature " + feature + " is not supported");
                return false;
            }
            return mMkHwImpl.get(feature);
        }

        @Override
        public boolean set(int feature, boolean enable) {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(feature)) {
                Log.e(TAG, "feature " + feature + " is not supported");
                return false;
            }
            return mMkHwImpl.set(feature, enable);
        }

        @Override
        public int[] getDisplayColorCalibration() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_DISPLAY_COLOR_CALIBRATION)) {
                Log.e(TAG, "Display color calibration is not supported");
                return null;
            }
            return mMkHwImpl.getDisplayColorCalibration();
        }

        @Override
        public boolean setDisplayColorCalibration(int[] rgb) {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_DISPLAY_COLOR_CALIBRATION)) {
                Log.e(TAG, "Display color calibration is not supported");
                return false;
            }
            if (rgb.length < 3) {
                Log.e(TAG, "Invalid color calibration");
                return false;
            }
            return mMkHwImpl.setDisplayColorCalibration(rgb);
        }

        @Override
        public int getNumGammaControls() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_DISPLAY_GAMMA_CALIBRATION)) {
                Log.e(TAG, "Display gamma calibration is not supported");
                return 0;
            }
            return mMkHwImpl.getNumGammaControls();
        }

        @Override
        public int[] getDisplayGammaCalibration(int idx) {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_DISPLAY_GAMMA_CALIBRATION)) {
                Log.e(TAG, "Display gamma calibration is not supported");
                return null;
            }
            return mMkHwImpl.getDisplayGammaCalibration(idx);
        }

        @Override
        public boolean setDisplayGammaCalibration(int idx, int[] rgb) {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_DISPLAY_GAMMA_CALIBRATION)) {
                Log.e(TAG, "Display gamma calibration is not supported");
                return false;
            }
            return mMkHwImpl.setDisplayGammaCalibration(idx, rgb);
        }

        @Override
        public int[] getVibratorIntensity() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_VIBRATOR)) {
                Log.e(TAG, "Vibrator is not supported");
                return null;
            }
            return mMkHwImpl.getVibratorIntensity();
        }

        @Override
        public boolean setVibratorIntensity(int intensity) {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_VIBRATOR)) {
                Log.e(TAG, "Vibrator is not supported");
                return false;
            }
            return mMkHwImpl.setVibratorIntensity(intensity);
        }

        @Override
        public String getLtoSource() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_LONG_TERM_ORBITS)) {
                Log.e(TAG, "Long term orbits is not supported");
                return null;
            }
            return mMkHwImpl.getLtoSource();
        }

        @Override
        public String getLtoDestination() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_LONG_TERM_ORBITS)) {
                Log.e(TAG, "Long term orbits is not supported");
                return null;
            }
            return mMkHwImpl.getLtoDestination();
        }

        @Override
        public long getLtoDownloadInterval() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_LONG_TERM_ORBITS)) {
                Log.e(TAG, "Long term orbits is not supported");
                return 0;
            }
            return mMkHwImpl.getLtoDownloadInterval();
        }

        @Override
        public String getSerialNumber() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_SERIAL_NUMBER)) {
                Log.e(TAG, "Serial number is not supported");
                return null;
            }
            return mMkHwImpl.getSerialNumber();
        }

        @Override
        public String getUniqueDeviceId() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_UNIQUE_DEVICE_ID)) {
                Log.e(TAG, "Unique device ID is not supported");
                return null;
            }
            return mMkHwImpl.getUniqueDeviceId();
        }

        @Override
        public boolean requireAdaptiveBacklightForSunlightEnhancement() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT)) {
                Log.e(TAG, "Sunlight enhancement is not supported");
                return false;
            }
            return mMkHwImpl.requireAdaptiveBacklightForSunlightEnhancement();
        }

        @Override
        public boolean isSunlightEnhancementSelfManaged() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_SUNLIGHT_ENHANCEMENT)) {
                Log.e(TAG, "Sunlight enhancement is not supported");
                return false;
            }
            return mMkHwImpl.isSunlightEnhancementSelfManaged();
        }

        @Override
        public DisplayMode[] getDisplayModes() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_DISPLAY_MODES)) {
                Log.e(TAG, "Display modes are not supported");
                return null;
            }
            final DisplayMode[] modes = mMkHwImpl.getDisplayModes();
            if (modes == null) {
                return null;
            }
            final ArrayList<DisplayMode> remapped = new ArrayList<DisplayMode>();
            for (DisplayMode mode : modes) {
                DisplayMode r = remapDisplayMode(mode);
                if (r != null) {
                    remapped.add(r);
                }
            }
            return remapped.toArray(new DisplayMode[remapped.size()]);
        }

        @Override
        public DisplayMode getCurrentDisplayMode() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_DISPLAY_MODES)) {
                Log.e(TAG, "Display modes are not supported");
                return null;
            }
            return remapDisplayMode(mMkHwImpl.getCurrentDisplayMode());
        }

        @Override
        public DisplayMode getDefaultDisplayMode() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_DISPLAY_MODES)) {
                Log.e(TAG, "Display modes are not supported");
                return null;
            }
            return remapDisplayMode(mMkHwImpl.getDefaultDisplayMode());
        }

        @Override
        public boolean setDisplayMode(DisplayMode mode, boolean makeDefault) {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_DISPLAY_MODES)) {
                Log.e(TAG, "Display modes are not supported");
                return false;
            }
            return mMkHwImpl.setDisplayMode(mode, makeDefault);
        }

        @Override
        public boolean writePersistentBytes(String key, byte[] value) {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.MANAGE_PERSISTENT_STORAGE, null);
            if (key == null || key.length() == 0 || key.length() > 64) {
                Log.e(TAG, "Invalid key: " + key);
                return false;
            }
            // A null value is delete
            if (value != null && (value.length > 4096 || value.length == 0)) {
                Log.e(TAG, "Invalid value: " + (value != null ? Arrays.toString(value) : null));
                return false;
            }
            if (!isSupported(MKHardwareManager.FEATURE_PERSISTENT_STORAGE)) {
                Log.e(TAG, "Persistent storage is not supported");
                return false;
            }
            return mMkHwImpl.writePersistentBytes(key, value);
        }

        @Override
        public byte[] readPersistentBytes(String key) {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.MANAGE_PERSISTENT_STORAGE, null);
            if (key == null || key.length() == 0 || key.length() > 64) {
                Log.e(TAG, "Invalid key: " + key);
                return null;
            }
            if (!isSupported(MKHardwareManager.FEATURE_PERSISTENT_STORAGE)) {
                Log.e(TAG, "Persistent storage is not supported");
                return null;
            }
            return mMkHwImpl.readPersistentBytes(key);
        }

        @Override
        public int getThermalState() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (isSupported(MKHardwareManager.FEATURE_THERMAL_MONITOR)) {
                return mCurrentThermalState;
            }
            return ThermalListenerCallback.State.STATE_UNKNOWN;
        }

        @Override
        public boolean registerThermalListener(IThermalListenerCallback callback) {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (isSupported(MKHardwareManager.FEATURE_THERMAL_MONITOR)) {
                return mRemoteCallbackList.register(callback);
            }
            return false;
        }

        @Override
        public boolean unRegisterThermalListener(IThermalListenerCallback callback) {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (isSupported(MKHardwareManager.FEATURE_THERMAL_MONITOR)) {
                return mRemoteCallbackList.unregister(callback);
            }
            return false;
        }

        @Override
        public int getColorBalanceMin() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (isSupported(MKHardwareManager.FEATURE_COLOR_BALANCE)) {
                return mMkHwImpl.getColorBalanceMin();
            }
            return 0;
        }

        @Override
        public int getColorBalanceMax() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (isSupported(MKHardwareManager.FEATURE_COLOR_BALANCE)) {
                return mMkHwImpl.getColorBalanceMax();
            }
            return 0;
        }

        @Override
        public int getColorBalance() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (isSupported(MKHardwareManager.FEATURE_COLOR_BALANCE)) {
                return mMkHwImpl.getColorBalance();
            }
            return 0;
        }

        @Override
        public boolean setColorBalance(int value) {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (isSupported(MKHardwareManager.FEATURE_COLOR_BALANCE)) {
                return mMkHwImpl.setColorBalance(value);
            }
            return false;
        }

        @Override
        public HSIC getPictureAdjustment() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (isSupported(MKHardwareManager.FEATURE_PICTURE_ADJUSTMENT)) {
                return mMkHwImpl.getPictureAdjustment();
            }
            return new HSIC(0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        @Override
        public HSIC getDefaultPictureAdjustment() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (isSupported(MKHardwareManager.FEATURE_PICTURE_ADJUSTMENT)) {
                return mMkHwImpl.getDefaultPictureAdjustment();
            }
            return new HSIC(0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        @Override
        public boolean setPictureAdjustment(HSIC hsic) {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (isSupported(MKHardwareManager.FEATURE_PICTURE_ADJUSTMENT) && hsic != null) {
                return mMkHwImpl.setPictureAdjustment(hsic);
            }
            return false;
        }

        @Override
        public float[] getPictureAdjustmentRanges() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (isSupported(MKHardwareManager.FEATURE_PICTURE_ADJUSTMENT)) {
                final List<Range<Float>> r = mMkHwImpl.getPictureAdjustmentRanges();
                return new float[] {
                        r.get(0).getLower(), r.get(0).getUpper(),
                        r.get(1).getLower(), r.get(1).getUpper(),
                        r.get(2).getLower(), r.get(2).getUpper(),
                        r.get(3).getLower(), r.get(3).getUpper(),
                        r.get(4).getUpper(), r.get(4).getUpper() };
            }
            return new float[10];
        }

        @Override
        public TouchscreenGesture[] getTouchscreenGestures() {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_TOUCHSCREEN_GESTURES)) {
                Log.e(TAG, "Touchscreen gestures are not supported");
                return null;
            }
            return mMkHwImpl.getTouchscreenGestures();
        }

        @Override
        public boolean setTouchscreenGestureEnabled(TouchscreenGesture gesture, boolean state) {
            mContext.enforceCallingOrSelfPermission(
                    mokee.platform.Manifest.permission.HARDWARE_ABSTRACTION_ACCESS, null);
            if (!isSupported(MKHardwareManager.FEATURE_TOUCHSCREEN_GESTURES)) {
                Log.e(TAG, "Touchscreen gestures are not supported");
                return false;
            }
            return mMkHwImpl.setTouchscreenGestureEnabled(gesture, state);
        }
    };
}
