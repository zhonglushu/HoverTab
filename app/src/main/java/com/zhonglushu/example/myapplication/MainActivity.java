package com.zhonglushu.example.myapplication;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;

import com.zhonglushu.example.hovertab.HoverTabActivity;
import com.zhonglushu.example.hovertab.fragment.ObservableBaseFragment;

public class MainActivity extends HoverTabActivity {

    private View tab1;
    private View tab2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View tabview = inflater.inflate(R.layout.tab_layout, null);
        tab1 = tabview.findViewById(R.id.tab1);
        tab2 = tabview.findViewById(R.id.tab2);
        tab1.setOnClickListener(mListener);
        tab2.setOnClickListener(mListener);
        tab1.setSelected(true);
        setHoverTabView(tabview);

        setmPagerAdapter(new LocalPagerAdapter(this.getSupportFragmentManager()));

        View view = inflater.inflate(R.layout.header_layout, null);
        setHoverHeaderView(view);

        //invalidateHeaderView();
    }

    private View.OnClickListener mListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            switch(v.getId()){
                case R.id.tab1:
                    setViewPagerCurrentItem(0);
                    break;
                case R.id.tab2:
                    setViewPagerCurrentItem(1);
                    break;
            }
        }
    };

    @Override
    public void onHoverPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onHoverPageSelected(int position) {
        if(position == 0){
            tab1.setSelected(true);
            tab2.setSelected(false);
        }else if(position == 1){
            tab1.setSelected(false);
            tab2.setSelected(true);
        }
    }

    @Override
    public void onHoverPageScrollStateChanged(int state) {

    }

    private class LocalPagerAdapter extends HoverTabFragmentStatePagerAdapter{

        public LocalPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getTabCount() {
            return 2;
        }

        @Override
        public ObservableBaseFragment createTab(int position) {
            ObservableBaseFragment f;
            if(position == 0){
                f = new Test1Fragment();
            }else{
                f = new Test2Fragment();
            }
            return f;
        }
    }
}
