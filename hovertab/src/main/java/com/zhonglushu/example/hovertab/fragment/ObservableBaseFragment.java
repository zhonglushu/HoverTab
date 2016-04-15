/*
 * Copyright 2014 Soichiro Kashima
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

package com.zhonglushu.example.hovertab.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import com.zhonglushu.example.hovertab.R;
import com.zhonglushu.example.hovertab.observable.ObservableScrollViewCallbacks;
import com.zhonglushu.example.hovertab.observable.ScrollState;
import com.zhonglushu.example.hovertab.observable.Scrollable;

public abstract class ObservableBaseFragment<S extends Scrollable> extends Fragment
        implements ObservableScrollViewCallbacks {

    public static final String ARG_SCROLL_Y = "ARG_SCROLL_Y";
    public static final String ARG_HEADER_HEIGHT = "ARG_HEADER_HEIGHT";

    protected OnRefreshCompleteListener onRefreshCompleteListener = null;

    public void setOnRefreshCompleteListener(OnRefreshCompleteListener onRefreshCompleteListener) {
        this.onRefreshCompleteListener = onRefreshCompleteListener;
    }

    public void onRefreshComplete(){
        if(onRefreshCompleteListener != null)
            onRefreshCompleteListener.onRefreshComplete();
    }

    public void setArguments(int scrollY, int height) {
        if (0 <= scrollY) {
            Bundle args = new Bundle();
            args.putInt(ARG_SCROLL_Y, scrollY);
            args.putInt(ARG_HEADER_HEIGHT, height);
            setArguments(args);
        }
    }

    public void setScrollY(int scrollY, int threshold) {
        View view = getView();
        if (view == null) {
            return;
        }
        Scrollable scrollView = (Scrollable) view.findViewById(R.id.scroll);
        if (scrollView == null) {
            return;
        }
        scrollView.scrollVerticallyTo(scrollY);
    }

    public void updateFlexibleSpace(int scrollY) {
        updateFlexibleSpace(scrollY, getView());
    }

    public abstract void updateFlexibleSpace(int scrollY, View view);

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
        if (getView() == null) {
            return;
        }
        updateFlexibleSpace(scrollY, getView());
    }

    @Override
    public final void onDownMotionEvent() {
        // We don't use this callback in this pattern.
    }

    @Override
    public final void onUpOrCancelMotionEvent(ScrollState scrollState) {
        // We don't use this callback in this pattern.
    }

    protected S getScrollable() {
        View view = getView();
        return view == null ? null : (S) view.findViewById(R.id.scroll);
    }

    //下拉刷新
    public abstract void pullDownRefresh();

    //上拉刷新
    public abstract void pullUpRefresh();

    //set header view
    public abstract void setHeaderView(int height);

    public static interface OnRefreshCompleteListener{
        public void onRefreshComplete();
    }
}