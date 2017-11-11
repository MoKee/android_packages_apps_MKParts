/**
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

package org.mokee.mkparts.fingerprint;

import android.app.Activity;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.Fingerprint;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mokee.fingerprint.FingerprintShortcutManager;
import mokee.fingerprint.FingerprintShortcutManager.Target;
import mokee.fingerprint.FingerprintShortcutManager.ComponentTarget;
import mokee.fingerprint.FingerprintShortcutManager.ShortcutTarget;

import org.mokee.mkparts.R;

public class FingerprintShortcutEditor extends Activity {

    static final String EXTRA_FINGERPRINT = "fingerprint";

    private static final int FLAG_SHORTCUT_ALL =
            ShortcutQuery.FLAG_MATCH_DYNAMIC
                    | ShortcutQuery.FLAG_MATCH_PINNED
                    | ShortcutQuery.FLAG_MATCH_MANIFEST;

    private UserHandle mUser = new UserHandle(UserHandle.myUserId());
    private Fingerprint mFingerprint;

    private LauncherApps mLauncherApps;
    private FingerprintShortcutManager mShortcutManager;
    private LayoutInflater mInflater;

    private Adapter mAdapter;

    private final List<Item> mItems = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFingerprint = getIntent().getParcelableExtra(
                EXTRA_FINGERPRINT);

        getActionBar().setSubtitle(mFingerprint.getName());

        setContentView(R.layout.fingerprint_shortcut_layout);

        mLauncherApps = (LauncherApps) getSystemService(
                LAUNCHER_APPS_SERVICE);
        mShortcutManager = new FingerprintShortcutManager(this);
        mInflater = LayoutInflater.from(this);

        mAdapter = new Adapter();

        final RecyclerView targetsView = (RecyclerView) findViewById(
                R.id.targets);
        targetsView.setLayoutManager(new LinearLayoutManager(this));
        targetsView.setAdapter(mAdapter);

        loadActivities();
    }

    private void onItemClick(int position) {
        mShortcutManager.addShortcut(mFingerprint.getFingerId(),
                mItems.get(position).target);

        setResult(RESULT_OK);
        finish();
    }

    private void loadActivities() {
        final int density = getResources().getDisplayMetrics().densityDpi;

        for (LauncherActivityInfo activity : getActivities()) {
            mItems.add(new Item(
                    new ComponentTarget(activity),
                    activity.getLabel(), activity.getIcon(density)));

            for (ShortcutInfo shortcut : getShortcuts(activity)) {
                final CharSequence longLabel = shortcut.getLongLabel();
                final CharSequence shortLabel = shortcut.getShortLabel();

                final CharSequence label = !TextUtils.isEmpty(longLabel)
                        ? longLabel : shortLabel;

                final Drawable icon = mLauncherApps.getShortcutIconDrawable(
                        shortcut, density);

                mItems.add(new Item(
                        new ShortcutTarget(shortcut),
                        label, icon));
            }
        }

        mAdapter.notifyDataSetChanged();
    }

    private List<LauncherActivityInfo> getActivities() {
        return mLauncherApps.getActivityList(null, mUser);
    }

    private List<ShortcutInfo> getShortcuts(LauncherActivityInfo activity) {
        return mLauncherApps.getShortcuts(
                new ShortcutQuery()
                        .setActivity(activity.getComponentName())
                        .setQueryFlags(FLAG_SHORTCUT_ALL),
                mUser);
    }

    private class Adapter extends RecyclerView.Adapter<BaseViewHolder> {

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public int getItemViewType(int position) {
            final Target target = mItems.get(position).target;
            if (target instanceof ComponentTarget) {
                return 1;
            } else if (target instanceof ShortcutTarget) {
                return 2;
            }
            return 0;
        }

        @Override
        public BaseViewHolder onCreateViewHolder(ViewGroup parent,
                int viewType) {
            if (viewType == 1) {
                return new ActivityViewHolder(parent);
            } else if (viewType == 2) {
                return new ShortcutViewHolder(parent);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(BaseViewHolder holder, int position) {
            if (holder != null) {
                final Item item = mItems.get(position);
                holder.bind(item);
            }
        }

    }

    private class BaseViewHolder extends RecyclerView.ViewHolder {
        final ImageView iconView;
        final TextView nameView;

        private BaseViewHolder(View itemView) {
            super(itemView);
            iconView = (ImageView) itemView.findViewById(R.id.icon);
            nameView = (TextView) itemView.findViewById(R.id.name);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClick(getLayoutPosition());
                }
            });
        }

        void bind(Item item) {
            iconView.setImageDrawable(item.icon);
            nameView.setText(item.label);
        }
    }

    private class ActivityViewHolder extends BaseViewHolder {
        private ActivityViewHolder(ViewGroup root) {
            super(mInflater.inflate(
                    R.layout.fingerprint_shortcut_item_activity,
                    root, false));
        }
    }

    private class ShortcutViewHolder extends BaseViewHolder {
        private ShortcutViewHolder(ViewGroup root) {
            super(mInflater.inflate(
                    R.layout.fingerprint_shortcut_item_shortcut,
                    root, false));
        }
    }

    private class Item {
        final Target target;
        final CharSequence label;
        final Drawable icon;
        private Item(Target target, CharSequence label, Drawable icon) {
            this.target = target;
            this.label = label;
            this.icon = icon;
        }
    }

}
