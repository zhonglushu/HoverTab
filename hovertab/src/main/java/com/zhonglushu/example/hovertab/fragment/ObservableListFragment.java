package com.zhonglushu.example.hovertab.fragment;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import com.zhonglushu.example.hovertab.HoverTabActivity;
import com.zhonglushu.example.hovertab.R;
import com.zhonglushu.example.hovertab.observable.ScrollUtils;
import com.zhonglushu.example.hovertab.views.ObservableListView;

/**
 * Created by huangyq on 2016/4/11.
 */
public abstract class ObservableListFragment extends ObservableBaseFragment<ObservableListView>{

    private ObservableListView listView = null;

    public void setListAdapter(ListAdapter adapter){
        listView.setAdapter(adapter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.com_zhonglushu_example_hovertab_listview, container, false);

        listView = (ObservableListView) view.findViewById(R.id.scroll);
        listView.setmCurrentMode(ObservableListView.Mode.PULL_FROM_END);
        listView.setmOnRefreshListener(new ObservableListView.OnRefreshListener() {

            @Override
            public void onRefresh() {
                pullUpRefresh();
            }
        });
        // Set padding view for ListView. This is the flexible space.
        /*View paddingView = new View(getActivity());
        int flexibleSpaceImageHeight = getResources().getDimensionPixelSize(R.dimen.person_info_activity_flexible_space_height);
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
                flexibleSpaceImageHeight);
        paddingView.setLayoutParams(lp);
        // This is required to disable header's list selector effect
        paddingView.setClickable(true);
        listView.addHeaderView(paddingView);*/
        listView.setTouchInterceptionViewGroup((ViewGroup) getActivity().findViewById(R.id.com_zhonglushu_example_hovertab_custom));

        // Scroll to the specified offset after layout
        Bundle args = getArguments();
        if (args != null) {
            boolean hasScroll = args.containsKey(ObservableBaseFragment.ARG_SCROLL_Y);
            final boolean hasHeaderHeight = args.containsKey(ObservableBaseFragment.ARG_HEADER_HEIGHT);
            if(hasScroll || hasHeaderHeight){
                final int scrollY = args.getInt(ObservableBaseFragment.ARG_SCROLL_Y, 0);
                final int headerHeight = args.getInt(ObservableBaseFragment.ARG_HEADER_HEIGHT, 0);
                ScrollUtils.addOnGlobalLayoutListener(listView, new Runnable() {
                    @Override
                    public void run() {
                    /*int offset = scrollY % flexibleSpaceImageHeight;
                    listView.setSelectionFromTop(0, -offset);*/
                        if (hasHeaderHeight) {
                            setHeaderView(headerHeight);
                        }
                    }
                });
                if(hasScroll)
                    updateFlexibleSpace(scrollY, view);
            }
        } else {
            updateFlexibleSpace(0, view);
        }
        listView.setScrollViewCallbacks(this);
        updateFlexibleSpace(0, view);
        return view;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setScrollY(int scrollY, int threshold) {
        View view = getView();
        if (view == null) {
            return;
        }
        ObservableListView listView = (ObservableListView) view.findViewById(R.id.scroll);
        if (listView == null) {
            return;
        }
        View firstVisibleChild = listView.getChildAt(0);
        if (firstVisibleChild != null) {
            int offset = scrollY;
            int position = 0;
            if (threshold < scrollY) {
                int baseHeight = firstVisibleChild.getHeight();
                position = scrollY / baseHeight;
                offset = scrollY % baseHeight;
            }
            listView.setSelectionFromTop(position, -offset);
        }
    }

    @Override
    public void updateFlexibleSpace(int scrollY, View view) {
        // Also pass this event to parent Activity
        HoverTabActivity parentActivity = (HoverTabActivity) getActivity();
        if (parentActivity != null) {
            parentActivity.onScrollChanged(scrollY, (ObservableListView) view.findViewById(R.id.scroll));
        }
    }

    @Override
    public void setHeaderView(int height) {
        if(listView != null){
            View paddingView = new View(getActivity());
            AbsListView.LayoutParams lp = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, height);
            paddingView.setLayoutParams(lp);
            // This is required to disable header's list selector effect
            paddingView.setClickable(true);
            listView.addHeaderView(paddingView);
        }
    }

    //下拉刷新完成
    public void pullDownRefreshComplete(){
        onRefreshComplete();
    }

    //上拉刷新完成
    public void pullUpRefreshComplete(){
        if(listView != null){
            listView.onRefreshComplete();
        }
    }

    //下拉刷新，设置没有更多
    public void setPullUpHasNoMore(boolean b){
        if(listView != null){
            listView.setHasNoMore(b);
        }
    }

    //下拉刷新，设置网络出错
    public void setPullUpNetworkError(boolean isNetworkError, String networkErrorStr) {
        if(listView != null){
            listView.setNetworkError(isNetworkError, networkErrorStr);
        }
    }
}