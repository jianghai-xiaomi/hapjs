/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */
package org.hapjs.render.jsruntime;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.hapjs.common.executors.Executors;
import org.hapjs.common.utils.ProcessUtils;
import org.hapjs.logging.LogProvider;
import org.hapjs.runtime.ProviderManager;
import org.hapjs.runtime.Runtime;
import org.hapjs.runtime.sandbox.ISandbox;
import org.hapjs.runtime.sandbox.ILogProvider;

public class SandboxProcessLauncher {
    private static final String TAG = "SandboxProcessLauncher";

    private static class SingletonHolder {
        private static SandboxProcessLauncher sInstance = new SandboxProcessLauncher();
    }

    public static SandboxProcessLauncher getInstance() {
        return SingletonHolder.sInstance;
    }

    private ISandbox mSandbox;
    private ParcelFileDescriptor[] mPositiveChannelDescriptors;
    private ParcelFileDescriptor[] mPassiveChannelDescriptors;

    private SandboxProcessLauncher() {
    }

    public void preBindSandboxProcess() {
        Executors.io().execute(() -> ensureBind());
    }

    public synchronized ParcelFileDescriptor[][] getChannelDescriptor() {
        ensureBind();
        ParcelFileDescriptor[][] channels = new ParcelFileDescriptor[][] {mPositiveChannelDescriptors, mPassiveChannelDescriptors};
        mPositiveChannelDescriptors = null;
        mPassiveChannelDescriptors = null;
        return channels;
    }

    private synchronized void ensureBind() {
        if (mSandbox != null) {
            ensureChannel(mSandbox);
            return;
        }

        bindService();
    }

    private void bindService() {
        CountDownLatch bindingLatch = new CountDownLatch(1);

        String currentProcessName = ProcessUtils.getCurrentProcessName();
        String sandboxName = "org.hapjs.runtime.sandbox.SandboxService$Sandbox"
                + currentProcessName.charAt(currentProcessName.length() - 1);

        Context context = Runtime.getInstance().getContext();
        Intent intent = new Intent();
        intent.setClassName(context.getPackageName(), sandboxName);

        context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ISandbox iSandbox = ISandbox.Stub.asInterface(service);
                mSandbox = iSandbox;
                try {
                    iSandbox.setLogProvider(new SandboxLogProvider());
                    iSandbox.asBinder().linkToDeath(() -> {
                        Log.e(TAG, "sandbox process has died. kill app process as well.");
                        Process.killProcess(Process.myPid());
                    }, 0);
                } catch (RemoteException e) {
                    Log.e(TAG, "failed to setLogProvider or linkToDeath", e);
                }

                ensureChannel(iSandbox);

                bindingLatch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        }, Context.BIND_AUTO_CREATE);

        try {
            bindingLatch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "exception while mBindingLatch.await", e);
        }
    }

    private void ensureChannel(ISandbox iSandbox) {
        if (mPositiveChannelDescriptors != null) {
            return;
        }

        try {
            ParcelFileDescriptor[] forwardChannel = ParcelFileDescriptor.createReliablePipe();
            ParcelFileDescriptor[] backwardChannel = ParcelFileDescriptor.createReliablePipe();
            ParcelFileDescriptor[] readSides = iSandbox.createChannel(new ParcelFileDescriptor[] {
                    forwardChannel[0], backwardChannel[0]
            });
            mPositiveChannelDescriptors = new ParcelFileDescriptor[] {
                    readSides[1], forwardChannel[1]
            };
            mPassiveChannelDescriptors = new ParcelFileDescriptor[] {
                    readSides[0], backwardChannel[1]
            };
        } catch (RemoteException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class SandboxLogProvider extends ILogProvider.Stub {
        @Override
        public void logCountEvent(String appPackage, String category, String key) {
            LogProvider provider = ProviderManager.getDefault().getProvider(LogProvider.NAME);
            if (provider != null) {
                provider.logCountEvent(appPackage, category, key);
            }
        }

        @Override
        public void logCountEventWithParams(String appPackage, String category, String key, Map params) {
            LogProvider provider = ProviderManager.getDefault().getProvider(LogProvider.NAME);
            if (provider != null) {
                provider.logCountEvent(appPackage, category, key, params);
            }
        }

        @Override
        public void logCalculateEvent(String appPackage, String category, String key, long value) {
            LogProvider provider = ProviderManager.getDefault().getProvider(LogProvider.NAME);
            if (provider != null) {
                provider.logCalculateEvent(appPackage, category, key, value);
            }
        }

        @Override
        public void logCalculateEventWithParams(String appPackage, String category, String key, long value, Map params) {
            LogProvider provider = ProviderManager.getDefault().getProvider(LogProvider.NAME);
            if (provider != null) {
                provider.logCalculateEvent(appPackage, category, key, value, params);
            }
        }

        @Override
        public void logNumericPropertyEvent(String appPackage, String category, String key, long value) {
            LogProvider provider = ProviderManager.getDefault().getProvider(LogProvider.NAME);
            if (provider != null) {
                provider.logNumericPropertyEvent(appPackage, category, key, value);
            }
        }

        @Override
        public void logNumericPropertyEventWithParams(String appPackage, String category, String key, long value, Map params) {
            LogProvider provider = ProviderManager.getDefault().getProvider(LogProvider.NAME);
            if (provider != null) {
                provider.logNumericPropertyEvent(appPackage, category, key, value);
            }
        }

        @Override
        public void logStringPropertyEvent(String appPackage, String category, String key, String value) {
            LogProvider provider = ProviderManager.getDefault().getProvider(LogProvider.NAME);
            if (provider != null) {
                provider.logStringPropertyEvent(appPackage, category, key, value);
            }
        }

        @Override
        public void logStringPropertyEventWithParams(String appPackage, String category, String key, String value, Map params) {
            LogProvider provider = ProviderManager.getDefault().getProvider(LogProvider.NAME);
            if (provider != null) {
                provider.logStringPropertyEvent(appPackage, category, key, value, params);
            }
        }
    }
}
