package com.zhonglushu.example.hovertab.fragment;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.zhonglushu.example.hovertab.HoverTabActivity;
import com.zhonglushu.example.hovertab.R;
import com.zhonglushu.example.hovertab.Utils.Mode;
import com.zhonglushu.example.hovertab.Utils.OnPullUpRefreshListener;
import com.zhonglushu.example.hovertab.observable.ScrollUtils;
import com.zhonglushu.example.hovertab.views.ObservableListView;
import com.zhonglushu.example.hovertab.views.ObservableRecyclerView;
import com.zhonglushu.example.hovertab.views.ObservableRefreshView;

/**
 * Created by zhonglushu on 2016/4/18.
 */
public abstract class ObservableRecyclerViewFragment extends ObservableBaseFragment<ObservableRecyclerView> {

    private ObservableRecyclerView recyclerView = null;
    private int headerHeight = 0;

    public void setRecyclerAdapter(ObservableRecyclerView.BaseRecyclerAdapter adapter){
        if(headerHeight > 0){
            View paddingView = new View(getActivity());
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, headerHeight);
            paddingView.setLayoutParams(lp);
            // This is required to disable header's list selector effect
            paddingView.setClickable(true);
            recyclerView.setHeaderView(paddingView);
        }
        ObservableRefreshView mRefreshView = new ObservableRefreshView(this.getContext(), true);
        recyclerView.setmFooterView(mRefreshView);
        adapter.setmRecyclerView(recyclerView);
        recyclerView.setAdapter(adapter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.com_zhonglushu_example_hovertab_recyclerview, container, false);

        recyclerView = (ObservableRecyclerView) view.findViewById(R.id.scroll);
        recyclerView.setmCurrentMode(Mode.PULL_FROM_END);
        recyclerView.setmOnRefreshListener(new OnPullUpRefreshListener() {

            @Override
            public void onRefresh() {
                pullUpRefresh();
            }
        });
        recyclerView.setTouchInterceptionViewGroup((ViewGroup) getActivity().findViewById(R.id.com_zhonglushu_example_hovertab_custom));

        // Scroll to the specified offset after layout
        Bundle args = getArguments();
        if (args != null) {
            final boolean hasScroll = args.containsKey(ObservableBaseFragment.ARG_SCROLL_Y);
            final boolean hasHeaderHeight = args.containsKey(ObservableBaseFragment.ARG_HEADER_HEIGHT);
            int scrollY = 0;
            if(hasScroll) {
                scrollY = args.getInt(ObservableBaseFragment.ARG_SCROLL_Y, 0);
            }
            if(hasHeaderHeight){
                headerHeight = args.getInt(ObservableBaseFragment.ARG_HEADER_HEIGHT, 0);
            }
            final int finalHeaderHeight = headerHeight;
            final int finalScrollY = scrollY;
            ScrollUtils.addOnGlobalLayoutListener(recyclerView, new Runnable() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    if (hasHeaderHeight) {
                        setHeaderView(finalHeaderHeight);
                    }
                    if(hasScroll){
                        setSelectionFromTop(finalScrollY, finalHeaderHeight);
                    }else{
                        int offset = finalScrollY % finalHeaderHeight;
                        recyclerView.setSelectionFromTop(0, -offset);
                    }
                }
            });
            if(hasScroll) {
                updateFlexibleSpace(scrollY, view);
            }else{
                updateFlexibleSpace(0, view);
            }
        } else {
            updateFlexibleSpace(0, view);
        }
        recyclerView.setScrollViewCallbacks(this);
        //updateFlexibleSpace(0, view);
        return view;
    }

    public void setLayoutManager(RecyclerView.LayoutManager lm){
        if(recyclerView != null){
            recyclerView.setLayoutManager(lm);
        }
    }

    @Override
    public void setScrollY(int scrollY, int threshold) {
        View view = getView();
        if (view == null) {
            return;
        }
        ObservableRecyclerView recyclerView = (ObservableRecyclerView) view.findViewById(R.id.scroll);
        if (recyclerView == null) {
            return;
        }
        if(recyclerView.getChildCount() > 0){
            setSelectionFromTop(scrollY, threshold);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setSelectionFromTop(int scrollY, int threshold){
        View firstVisibleChild = recyclerView.getChildAt(1);
        if (firstVisibleChild != null) {
            int offset = scrollY;
            int position = 0;
            if (threshold < scrollY) {
                int baseHeight = firstVisibleChild.getHeight();
                position = scrollY / baseHeight;
                offset = scrollY % baseHeight;
            }
            recyclerView.setSelectionFromTop(position, -offset);
        }
    }

    @Override
    public void updateFlexibleSpace(int scrollY, View view) {
        // Also pass this event to parent Activity
        HoverTabActivity parentActivity = (HoverTabActivity) getActivity();
        if (parentActivity != null) {
            parentActivity.onScrollChanged(scrollY, (ObservableRecyclerView) view.findViewById(R.id.scroll));
        }
    }

    @Override
    public void setHeaderView(int height) {
        if(recyclerView != null){
            View paddingView = new View(getActivity());
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(ViewGroup.MarginLayoutParams.MATCH_PARENT, height);
            paddingView.setLayoutParams(lp);
            // This is required to disable header's list selector effect
            paddingView.setClickable(true);
            recyclerView.setHeaderView(paddingView);
        }
    }

    public void pullDownRefreshComplete() {
        onRefreshComplete();
    }

    public void pullUpRefreshComplete() {
        if(recyclerView != null)
            recyclerView.onRefreshComplete();
    }

    //下拉刷新，设置没有更多
    public void setPullUpHasNoMore(boolean b){
        if(recyclerView != null){
            recyclerView.setHasNoMore(b);
        }
    }

    //下拉刷新，设置网络出错
    public void setPullUpNetworkError(boolean isNetworkError, String networkErrorStr) {
        if(recyclerView != null){
            recyclerView.setNetworkError(isNetworkError, networkErrorStr);
        }
    }

    public void setCurrentMode(Mode mode){
        if(recyclerView != null)
            recyclerView.setmCurrentMode(mode);
    }

    public void setOnItemClickListener(ObservableRecyclerView.OnItemClickListener listener) {
        if(recyclerView != null)
            recyclerView.setOnItemClickListener(listener);
    }
}
