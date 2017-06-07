/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.fuelgauge.anomaly;

import android.content.Context;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.utils.AsyncLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Loader to compute which apps are anomaly and return a anomaly list. It will return
 * an empty list if there is no anomaly.
 */
public class AnomalyLoader extends AsyncLoader<List<Anomaly>> {
    private BatteryStatsHelper mBatteryStatsHelper;
    private String mPackageName;
    private UserManager mUserManager;
    @VisibleForTesting
    AnomalyUtils mAnomalyUtils;
    @VisibleForTesting
    AnomalyDetectionPolicy mPolicy;

    /**
     * Create {@link AnomalyLoader} that runs anomaly check for all apps.
     */
    public AnomalyLoader(Context context, BatteryStatsHelper batteryStatsHelper) {
        this(context, batteryStatsHelper, null, new AnomalyDetectionPolicy(context));

    }

    /**
     * Create {@link AnomalyLoader} with {@code packageName}, so this loader will only
     * detect anomalies related to {@code packageName}, or check all apps if {@code packageName}
     * is {@code null}.
     *
     * This constructor will create {@link BatteryStatsHelper} in background thread.
     *
     * @param context
     * @param packageName if set, only finds anomalies for this package. If {@code null},
     *                    detects all anomalies of this type.
     */
    public AnomalyLoader(Context context, String packageName) {
        this(context, null, packageName, new AnomalyDetectionPolicy(context));
    }

    @VisibleForTesting
    AnomalyLoader(Context context, BatteryStatsHelper batteryStatsHelper,
            String packageName, AnomalyDetectionPolicy policy) {
        super(context);
        mBatteryStatsHelper = batteryStatsHelper;
        mPackageName = packageName;
        mAnomalyUtils = AnomalyUtils.getInstance(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mPolicy = policy;
    }

    @Override
    protected void onDiscardResult(List<Anomaly> result) {
    }

    @Override
    public List<Anomaly> loadInBackground() {
        if (mBatteryStatsHelper == null) {
            mBatteryStatsHelper = new BatteryStatsHelper(getContext());
            mBatteryStatsHelper.create((Bundle) null);
            mBatteryStatsHelper.refreshStats(BatteryStats.STATS_SINCE_CHARGED,
                    mUserManager.getUserProfiles());
        }

        final List<Anomaly> anomalies = new ArrayList<>();
        for (@Anomaly.AnomalyType int type : Anomaly.ANOMALY_TYPE_LIST) {
            if (mPolicy.isAnomalyDetectorEnabled(type)) {
                anomalies.addAll(mAnomalyUtils.getAnomalyDetector(type).detectAnomalies(
                        mBatteryStatsHelper, mPackageName));
            }
        }

        return anomalies;
    }

}
