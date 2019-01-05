/**
 * Copyright (C) 2014-2019 The MoKee Open Source project
 * Copyright (C) 2017-2018 The LineageOS project
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

package org.mokee.internal.statusbar;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.UserHandle;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import mokee.providers.MKSettings;

import java.text.DecimalFormat;
import java.util.HashMap;

import org.mokee.platform.internal.R;

public class NetworkTraffic extends TextView {
    private static final String TAG = "NetworkTraffic";

    private static final int MODE_DISABLED = 0;
    private static final int MODE_UPSTREAM_ONLY = 1;
    private static final int MODE_DOWNSTREAM_ONLY = 2;
    private static final int MODE_UPSTREAM_AND_DOWNSTREAM = 3;

    private static final int MESSAGE_TYPE_PERIODIC_REFRESH = 0;
    private static final int MESSAGE_TYPE_UPDATE_VIEW = 1;

    private static final int REFRESH_INTERVAL = 2000;

    // Thresholds themselves are always defined in kbps
    private static final long AUTOHIDE_THRESHOLD_MEGABYTES = 80;

    private static final int KILOBYTE = 1024;
    private int KB = KILOBYTE;
    private int MB = KB * KB;
    private int GB = MB * KB;

    private static DecimalFormat decimalFormat = new DecimalFormat("##0.#");
    static {
        decimalFormat.setMaximumIntegerDigits(4);
        decimalFormat.setMaximumFractionDigits(1);
    }

    private int mMode = MODE_DISABLED;
    private boolean mNetworkTrafficIsVisible;
    private long mTxKbps;
    private long mRxKbps;
    private long mLastUpdateTime;
    private int mTextSizeSingle;
    private int mTextSizeMulti;
    private boolean mAutoHide;
    private long mAutoHideThreshold;
    private int mDarkModeFillColor;
    private int mLightModeFillColor;
    private int mIconTint = Color.WHITE;
    private SettingsObserver mObserver;
    private Drawable mDrawable;
    private HashMap<String, IfaceTrafficStats> mActiveIfaceStats;
    private boolean mIsStatsDirty;

    public NetworkTraffic(Context context) {
        this(context, null);
    }

    public NetworkTraffic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NetworkTraffic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final Resources resources = getResources();
        mTextSizeSingle = resources.getDimensionPixelSize(R.dimen.net_traffic_single_text_size);
        mTextSizeMulti = resources.getDimensionPixelSize(R.dimen.net_traffic_multi_text_size);

        mNetworkTrafficIsVisible = false;

        mObserver = new SettingsObserver(mTrafficHandler);

        /* Prepare for extreme case: WiFi + Mobile + Bluetooth + Ethernet */
        mActiveIfaceStats = new HashMap<>(4);
    }

    private MKStatusBarItem.DarkReceiver mDarkReceiver =
            new MKStatusBarItem.DarkReceiver() {
        public void onDarkChanged(Rect area, float darkIntensity, int tint) {
            mIconTint = tint;
            setTextColor(mIconTint);
            updateTrafficDrawableColor();
        }
        public void setFillColors(int darkColor, int lightColor) {
            mDarkModeFillColor = darkColor;
            mLightModeFillColor = lightColor;
        }
    };

    private MKStatusBarItem.VisibilityReceiver mVisibilityReceiver =
            new MKStatusBarItem.VisibilityReceiver() {
        public void onVisibilityChanged(boolean isVisible) {
            if (mNetworkTrafficIsVisible != isVisible) {
                mNetworkTrafficIsVisible = isVisible;
                updateViewState();
            }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        MKStatusBarItem.Manager manager =
                MKStatusBarItem.findManager((View) this);
        manager.addDarkReceiver(mDarkReceiver);
        manager.addVisibilityReceiver(mVisibilityReceiver);

        mContext.registerReceiver(mIntentReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mObserver.observe();
        mIsStatsDirty = true;
        updateSettings();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mContext.unregisterReceiver(mIntentReceiver);
        mObserver.unobserve();
    }

    private Handler mTrafficHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long now = SystemClock.elapsedRealtime();
            long timeDelta = now - mLastUpdateTime;

            if (mIsStatsDirty) {
                if (refreshActiveIfaces()) {
                    mIsStatsDirty = false;
                } else {
                    return;
                }
            }

            if (msg.what == MESSAGE_TYPE_PERIODIC_REFRESH
                    && timeDelta >= REFRESH_INTERVAL * 0.95f) {
                // Update counters
                mLastUpdateTime = now;
                long txBytes = diffAndUpdateTxBytes();
                long rxBytes = diffAndUpdateRxBytes();
                mTxKbps = (long) (txBytes * 8f / (timeDelta / 1000f) / 1000f);
                mRxKbps = (long) (rxBytes * 8f / (timeDelta / 1000f) / 1000f);
            }

            final boolean enabled = mMode != MODE_DISABLED && mActiveIfaceStats.size() != 0;
            final boolean showUpstream =
                    mMode == MODE_UPSTREAM_ONLY || mMode == MODE_UPSTREAM_AND_DOWNSTREAM;
            final boolean showDownstream =
                    mMode == MODE_DOWNSTREAM_ONLY || mMode == MODE_UPSTREAM_AND_DOWNSTREAM;
            final boolean shouldHide = mAutoHide && (!showUpstream || mTxKbps < mAutoHideThreshold)
                    && (!showDownstream || mRxKbps < mAutoHideThreshold);

            if (!enabled || shouldHide) {
                setText("");
                setVisibility(GONE);
            } else {
                // Get information for uplink ready so the line return can be added
                StringBuilder output = new StringBuilder();
                if (showUpstream) {
                    output.append(formatOutput(mTxKbps));
                }

                // Ensure text size is where it needs to be
                int textSize;
                if (showUpstream && showDownstream) {
                    output.append("\n");
                    textSize = mTextSizeMulti;
                } else {
                    textSize = mTextSizeSingle;
                }

                // Add information for downlink if it's called for
                if (showDownstream) {
                    output.append(formatOutput(mRxKbps));
                }

                // Update view if there's anything new to show
                if (!output.toString().contentEquals(getText())) {
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) textSize);
                    setText(output.toString());
                }
                setVisibility(VISIBLE);
            }

            // Schedule periodic refresh
            mTrafficHandler.removeMessages(MESSAGE_TYPE_PERIODIC_REFRESH);
            if (enabled && mNetworkTrafficIsVisible) {
                mTrafficHandler.sendEmptyMessageDelayed(MESSAGE_TYPE_PERIODIC_REFRESH,
                        REFRESH_INTERVAL);
            }
        }

        private String formatOutput(long kbps) {
            final String value;
            final String unit;

            long kilobyte = kbps / 8;
            if (kilobyte < KB) {
                value = decimalFormat.format(kilobyte);
                unit = mContext.getString(R.string.bytespersecond_short);
            } else if (kilobyte < MB) {
                value = decimalFormat.format(kilobyte / KB);
                unit = mContext.getString(R.string.kilobytespersecond_short);
            } else if (kilobyte < GB) {
                value = decimalFormat.format(kilobyte / MB);
                unit = mContext.getString(R.string.megabytespersecond_short);
            } else {
                value = decimalFormat.format(kilobyte / GB);
                unit = mContext.getString(R.string.gigabytespersecond_short);
            }

            return value + " " + unit;
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                mIsStatsDirty = true;
                updateViewState();
            }
        }
    };

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(MKSettings.Secure.getUriFor(
                    MKSettings.Secure.NETWORK_TRAFFIC_MODE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(MKSettings.Secure.getUriFor(
                    MKSettings.Secure.NETWORK_TRAFFIC_AUTOHIDE),
                    false, this, UserHandle.USER_ALL);
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    private long diffAndUpdateTxBytes() {
        long txBytesDelta = 0;
        for (String iface : mActiveIfaceStats.keySet()) {
            IfaceTrafficStats stats = mActiveIfaceStats.get(iface);
            long txBytes = TrafficStats.getTxBytes(iface);

            txBytesDelta += txBytes - stats.mTxBytes;
            stats.mTxBytes = txBytes;
        }
        return txBytesDelta;
    }

    private long diffAndUpdateRxBytes() {
        long rxBytesDelta = 0;
        for (String iface : mActiveIfaceStats.keySet()) {
            IfaceTrafficStats stats = mActiveIfaceStats.get(iface);
            long rxBytes = TrafficStats.getRxBytes(iface);

            rxBytesDelta += rxBytes - stats.mRxBytes;
            stats.mRxBytes = rxBytes;
        }
        return rxBytesDelta;
    }

    private boolean refreshActiveIfaces() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        mActiveIfaceStats.clear();

        Network[] networks = cm.getAllNetworks();
        for (Network network : networks) {
            NetworkInfo networkInfo = cm.getNetworkInfo(network);
            if (networkInfo == null) {
                return false;
            }

            if (networkInfo.getType() != ConnectivityManager.TYPE_VPN) {
                LinkProperties properties = cm.getLinkProperties(network);
                IfaceTrafficStats stats;
                String iface;

                /* This is likely to be null when switching data SIM */
                if (properties == null) {
                    return false;
                }

                iface = properties.getInterfaceName();
                if (iface == null) return false;
                stats = new IfaceTrafficStats();
                stats.mRxBytes = TrafficStats.getRxBytes(iface);
                stats.mTxBytes = TrafficStats.getTxBytes(iface);
                mActiveIfaceStats.put(iface, stats);
            }
        }
        return true;
    }

    private void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        mMode = MKSettings.Secure.getIntForUser(resolver,
                MKSettings.Secure.NETWORK_TRAFFIC_MODE, 3, UserHandle.USER_CURRENT);
        mAutoHide = MKSettings.Secure.getIntForUser(resolver,
                MKSettings.Secure.NETWORK_TRAFFIC_AUTOHIDE, 0, UserHandle.USER_CURRENT) == 1;
        mAutoHideThreshold = AUTOHIDE_THRESHOLD_MEGABYTES;

        if (mMode != MODE_DISABLED) {
            updateTrafficDrawable();
        }
        updateViewState();
    }

    private void updateViewState() {
        mTrafficHandler.sendEmptyMessage(MESSAGE_TYPE_UPDATE_VIEW);
    }

    private void clearHandlerCallbacks() {
        mTrafficHandler.removeMessages(MESSAGE_TYPE_PERIODIC_REFRESH);
        mTrafficHandler.removeMessages(MESSAGE_TYPE_UPDATE_VIEW);
    }

    private void updateTrafficDrawable() {
        final int drawableResId;
        if (mMode == MODE_UPSTREAM_AND_DOWNSTREAM) {
            drawableResId = R.drawable.stat_sys_network_traffic_updown;
        } else if (mMode == MODE_UPSTREAM_ONLY) {
            drawableResId = R.drawable.stat_sys_network_traffic_up;
        } else if (mMode == MODE_DOWNSTREAM_ONLY) {
            drawableResId = R.drawable.stat_sys_network_traffic_down;
        } else {
            drawableResId = 0;
        }
        mDrawable = drawableResId != 0 ? getResources().getDrawable(drawableResId) : null;
        setCompoundDrawablesWithIntrinsicBounds(null, null, mDrawable, null);
        updateTrafficDrawableColor();
    }

    private void updateTrafficDrawableColor() {
        if (mDrawable != null) {
            mDrawable.setColorFilter(mIconTint, PorterDuff.Mode.MULTIPLY);
        }
    }

    private static class IfaceTrafficStats {
        public long mTxBytes;
        public long mRxBytes;
    }
}
