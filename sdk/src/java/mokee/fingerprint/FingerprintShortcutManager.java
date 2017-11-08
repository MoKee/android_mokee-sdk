/*
 * Copyright (C) 2017-2018 The MoKee Open Source Project
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
 * limitations under the License
 */

package mokee.fingerprint;

import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.SparseArray;

import java.util.Collections;
import java.util.List;

import mokee.providers.MKSettings;

public class FingerprintShortcutManager {

    private static final String TAG = "FingerprintShortcutManager";

    private static final int FLAG_SHORTCUT_ALL =
            ShortcutQuery.FLAG_MATCH_DYNAMIC
                    | ShortcutQuery.FLAG_MATCH_PINNED
                    | ShortcutQuery.FLAG_MATCH_MANIFEST;

    private final Context mContext;
    private final PackageManager mPackageManager;
    private final LauncherApps mLauncherApps;

    private final int mDensity;

    public FingerprintShortcutManager(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mLauncherApps = (LauncherApps) context.getSystemService(
                Context.LAUNCHER_APPS_SERVICE);

        this.mDensity = mContext.getResources().getDisplayMetrics().densityDpi;
    }

    public void startShortcutAsUser(int fingerId, UserHandle user) {
        final SparseArray<Target> shortcuts = getShortcuts();
        final Target target = shortcuts.get(fingerId);
        if (target != null) {
            target.startAsUser(user);
        }
    }

    public SparseArray<Target> getShortcuts() {
        final SparseArray<Target> shortcuts = new SparseArray<>();

        final String shortcutMappings = MKSettings.System.getString(
                mContext.getContentResolver(),
                MKSettings.System.FINGERPRINT_SHORTCUTS);

        if (TextUtils.isEmpty(shortcutMappings)) {
            return shortcuts;
        }

        for (String line : shortcutMappings.split(";")) {
            final String[] mapping = line.split(":");
            final int fingerId = Integer.parseInt(mapping[0]);
            switch (mapping[1]) {
                case "cmp":
                    shortcuts.put(fingerId, new ComponentTargetImpl(mapping[2]));
                    break;
                case "shortcut":
                    final String[] shortcut = mapping[2].split("/");
                    shortcuts.put(fingerId, new ShortcutTargetImpl(shortcut[0], shortcut[1]));
                    break;
            }
        }

        return shortcuts;
    }

    public void addShortcut(int fingerId, Target target) {
        final SparseArray<Target> shortcuts = getShortcuts();
        shortcuts.put(fingerId, target);
        persist(shortcuts);
    }

    public void removeShortcut(int fingerId) {
        final SparseArray<Target> shortcuts = getShortcuts();
        shortcuts.delete(fingerId);
        persist(shortcuts);
    }

    private void persist(SparseArray<Target> shortcuts) {
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < shortcuts.size(); i++) {
            if (i > 0) builder.append(";");
            builder.append(shortcuts.keyAt(i));
            builder.append(":");
            builder.append(shortcuts.valueAt(i).toString());
        }

        MKSettings.System.putString(mContext.getContentResolver(),
                MKSettings.System.FINGERPRINT_SHORTCUTS, builder.toString());
    }

    private ActivityInfo getActivityInfo(ComponentName component) {
        try {
            if (component == null) {
                return null;
            }

            return mPackageManager.getActivityInfo(component, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private Drawable getActivityIcon(ActivityInfo activity) {
        if (activity == null) {
            return null;
        }

        return activity.loadIcon(mPackageManager);
    }

    private CharSequence getActivityLabel(ActivityInfo activity) {
        if (activity == null) {
            return null;
        }

        return activity.loadLabel(mPackageManager);
    }

    private ShortcutInfo getShortcutInfo(String packageName, String shortcutId) {
        final ShortcutQuery query = new ShortcutQuery()
                .setPackage(packageName)
                .setShortcutIds(Collections.singletonList(shortcutId))
                .setQueryFlags(FLAG_SHORTCUT_ALL);

        final UserHandle userHandle = new UserHandle(UserHandle.myUserId());

        final List<ShortcutInfo> shortcuts =  mLauncherApps.getShortcuts(
                query, userHandle);
        if (shortcuts.isEmpty()) {
            return null;
        }

        return shortcuts.get(0);
    }

    private Drawable getShortcutIcon(ShortcutInfo shortcut) {
        if (shortcut == null) {
            return null;
        }

        return mLauncherApps.getShortcutIconDrawable(shortcut, mDensity);
    }

    private CharSequence getShortcutLabel(ShortcutInfo shortcut) {
        if (shortcut == null) {
            return null;
        }

        final ActivityInfo activity = getActivityInfo(shortcut.getActivity());
        final CharSequence activityLabel = getActivityLabel(activity);
        if (activityLabel == null) {
            return null;
        }

        final CharSequence longLabel = shortcut.getLongLabel();
        final CharSequence shortLabel = shortcut.getShortLabel();

        final CharSequence shortcutLabel =
                !TextUtils.isEmpty(longLabel) ? longLabel : shortLabel;

        if (TextUtils.isEmpty(shortcutLabel)) {
            return null;
        }

        return activityLabel + " " + shortcutLabel;
    }

    public static class Target {

        void startAsUser(UserHandle user) {
        }

        public Drawable getIcon() {
            return null;
        }

        public CharSequence getLabel() {
            return null;
        }

    }

    public static class ComponentTarget extends Target {

        public final ComponentName component;

        private ComponentTarget(ComponentName component) {
            this.component = component;
        }

        public ComponentTarget(LauncherActivityInfo info) {
            this(info.getComponentName());
        }

        @Override
        public String toString() {
            return "cmp:" + component.flattenToShortString();
        }

    }

    public static class ShortcutTarget extends Target {

        public final String packageName;
        public final String shortcutId;

        private ShortcutTarget(String packageName, String shortcutId) {
            this.packageName = packageName;
            this.shortcutId = shortcutId;
        }

        public ShortcutTarget(ShortcutInfo info) {
            this(info.getPackage(), info.getId());
        }

        @Override
        public String toString() {
            return "shortcut:" + packageName + "/" + shortcutId;
        }

    }

    public class ComponentTargetImpl extends ComponentTarget {

        private ActivityInfo mActivity;

        private ComponentTargetImpl(String component) {
            super(ComponentName.unflattenFromString(component));
        }

        @Override
        void startAsUser(UserHandle user) {
            mLauncherApps.startMainActivity(component,
                    user, null, null);
        }

        @Override
        public Drawable getIcon() {
            if (mActivity == null) {
                mActivity = getActivityInfo(component);
            }
            return getActivityIcon(mActivity);
        }

        @Override
        public CharSequence getLabel() {
            if (mActivity == null) {
                mActivity = getActivityInfo(component);
            }
            return getActivityLabel(mActivity);
        }

    }

    public class ShortcutTargetImpl extends ShortcutTarget {

        private ShortcutInfo mShortcut;

        private ShortcutTargetImpl(String packageName, String shortcutId) {
            super(packageName, shortcutId);
        }

        @Override
        void startAsUser(UserHandle user) {
            mLauncherApps.startShortcut(packageName, shortcutId,
                    null, null, user);
        }

        @Override
        public Drawable getIcon() {
            if (mShortcut == null) {
                mShortcut = getShortcutInfo(packageName, shortcutId);
            }
            return getShortcutIcon(mShortcut);
        }

        @Override
        public CharSequence getLabel() {
            if (mShortcut == null) {
                mShortcut = getShortcutInfo(packageName, shortcutId);
            }
            return getShortcutLabel(mShortcut);
        }

    }

}
