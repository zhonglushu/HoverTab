package com.zhonglushu.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.zhonglushu.example.hovertab.fragment.ObservableRecyclerViewFragment;
import com.zhonglushu.example.hovertab.views.ObservableRecyclerView;

import java.util.ArrayList;

/**
 * Created by huangyq on 2016/4/11.
 */
public class Test2Fragment extends ObservableRecyclerViewFragment {

    private ArrayList<String> list = null;
    private ObservableRecyclerView.BaseRecyclerAdapter mAdapter = null;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case 1:
                    pullDownRefreshComplete();
                    break;
                case 2:
                    setPullUpNetworkError(true, "糟糕，网络出错了！");
                    list.add("add");
                    list.add("add");
                    list.add("add");
                    if(mAdapter != null){
                        mAdapter.notifyDataSetChanged();
                    }
                    pullUpRefreshComplete();
                    break;
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setLayoutManager(new GridLayoutManager(this.getActivity(), 2));
        list = new ArrayList<String>();
        for(int i = 0; i < 50; i++){
            list.add("item" + i);
        }
        mAdapter = new LocalAdapter();
        mAdapter.addDatas(list);
        setRecyclerAdapter(mAdapter);
        setOnItemClickListener(new ObservableRecyclerView.OnItemClickListener<String>(){

            @Override
            public void onItemClick(int position, String data) {
                Toast.makeText(Test2Fragment.this.getActivity(), "" + data, Toast.LENGTH_SHORT).show();
            }
        });
    }

    class LocalAdapter extends ObservableRecyclerView.BaseRecyclerAdapter<String>{

        @Override
        public RecyclerView.ViewHolder onCreate(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item, null, false);
            return new MyHolder(view);
        }

        @Override
        public void onBind(RecyclerView.ViewHolder viewHolder, int RealPosition, String data) {
            if(viewHolder instanceof MyHolder) {
                ((MyHolder) viewHolder).text.setText(data);
            }
        }
    }

    class MyHolder extends ObservableRecyclerView.Holder {
        TextView text;
        public MyHolder(View itemView) {
            super(itemView);
            text = (TextView) itemView.findViewById(R.id.text1);
        }
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
