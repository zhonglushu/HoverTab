package com.zhonglushu.example.myapplication;

import android.support.v4.view.PagerAdapter;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

public abstract class AbstractViewPagerAdapter extends PagerAdapter {
    protected SparseArray<View> mViews;

    public AbstractViewPagerAdapter() {
        mViews = new SparseArray<View>();
    }

    protected ViewGroup mContainer;

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        mContainer = container;
        View view = mViews.get(position);
        if (view == null) {
            view = newView(position);
            mViews.put(position, view);
        }
        container.addView(view);
        return view;
    }

    public abstract View newView(int position);

    public void notifyUpdateView(int position) {
        View view = updateView(mViews.get(position), position);
        mViews.put(position, view);
        notifyDataSetChanged();
    }

    public View updateView(View view, int position) {
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(mViews.get(position));
    }

}