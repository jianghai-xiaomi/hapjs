/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */

package org.hapjs.runtime.sandbox;

import android.os.ParcelFileDescriptor;

import org.hapjs.runtime.sandbox.ILogProvider;

interface ISandbox {
    ParcelFileDescriptor[] createChannel(in ParcelFileDescriptor[] readSide);
    void setLogProvider(ILogProvider logProvider);
}