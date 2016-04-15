package com.zhonglushu.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ArrayAdapter;
import com.zhonglushu.example.hovertab.fragment.ObservableListFragment;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangyq on 2016/4/11.
 */
public class Test1Fragment extends ObservableListFragment{

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case 1:
                    pullDownRefreshComplete();
                    break;
                case 2:
                    setPullUpHasNoMore(true);
                    pullUpRefreshComplete();
                    break;
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        List<String> list = new ArrayList<String>();
        for(int i = 0; i < 50; i++){
            list.add("item" + i);
        }
        ArrayAdapter mAdapter = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, list);
        setListAdapter(mAdapter);
    }

    @Override
    public void pullDownRefresh() {
        mHandler.sendEmptyMessageDelayed(1, 5000);
    }

    @Override
    public void pullUpRefresh() {
        mHandler.sendEmptyMessageDelayed(2, 5000);
    }
}
