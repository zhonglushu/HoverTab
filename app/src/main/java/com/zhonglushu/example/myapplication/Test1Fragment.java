package com.zhonglushu.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
        final List<String> list = new ArrayList<String>();
        for(int i = 0; i < 50; i++){
            list.add("item" + i);
        }
        LocalAdapter mAdapter = new LocalAdapter(list);
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

    private class LocalAdapter extends BaseAdapter{

        private List<String> mList = null;

        public LocalAdapter(List<String> mList) {
            this.mList = mList;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = LayoutInflater.from(Test1Fragment.this.getActivity()).inflate(R.layout.list_item, null);
            TextView textView = (TextView) view.findViewById(R.id.text1);
            textView.setText(mList.get(position));
            view.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Toast.makeText(Test1Fragment.this.getActivity(), "" + mList.get(position), Toast.LENGTH_SHORT).show();
                }
            });
            return view;
        }
    }
}
