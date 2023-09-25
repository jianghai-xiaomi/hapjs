/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.runtime.sandbox;

import android.os.Handler;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.executors.Future;
import org.hapjs.logging.RuntimeLogManager;

public class SandboxLogHelper {
    private static final long SLOW_MESSAGE_THRESHOLD = 100;
    private static final long HEART_BEAT_THRESHOLD = 1000;

    public static class PositiveChannelStatHelper {
        private ChannelSender mChannel;
        private Handler mHandler;
        private boolean mHeartBeatWaiting;

        public PositiveChannelStatHelper(ChannelSender channel, Handler handler) {
            mChannel = channel;
            mHandler = handler;
        }

        public void scheduleHeartBeat(String pkg) {
            if (mHeartBeatWaiting) {
                return;
            }

            mHeartBeatWaiting = true;
            long start = System.currentTimeMillis();
            Future future = Executors.scheduled().executeWithDelay(() -> {
                RuntimeLogManager.getDefault().recordSandboxMessageSlow(pkg, SandboxIpcMethods.HEART_BEAT, 0,
                        System.currentTimeMillis() - start);
            }, HEART_BEAT_THRESHOLD);

            mHandler.post(() -> {
                mChannel.invokeSync(SandboxIpcMethods.HEART_BEAT, boolean.class);
                mHeartBeatWaiting = false;
                future.cancel(false);
                if (System.currentTimeMillis() - start > HEART_BEAT_THRESHOLD) {
                    RuntimeLogManager.getDefault().recordSandboxMessageSlow(pkg, SandboxIpcMethods.HEART_BEAT, 0,
                            System.currentTimeMillis() - start);
                }
            });
        }
    }

    public static void onChannelReceive(String pkg, String method, int dataSize, long startStamp) {
        long now = System.currentTimeMillis();
        long timeCost = now - startStamp;
        if (timeCost > SLOW_MESSAGE_THRESHOLD) {
            RuntimeLogManager.getDefault().recordSandboxMessageSlow(pkg, method, dataSize, timeCost);
        }
    }
}
