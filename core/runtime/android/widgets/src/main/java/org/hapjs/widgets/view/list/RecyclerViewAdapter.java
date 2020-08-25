/*
 * Copyright (C) 2020, hapjs.org. All rights reserved.
 */
package org.hapjs.widgets.view.list;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import org.hapjs.component.Component;

public interface RecyclerViewAdapter {

    RecyclerView getActualRecyclerView();

    void setScrollPage(boolean scrollPage);

    void setComponent(Component component);

    void resumeRequestLayout();

    void requestLayout();

    View getMoveableView();

    void setDirty(boolean dirty);

    RecyclerView.State getState();
}
