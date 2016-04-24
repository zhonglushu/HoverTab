# HoverTab
悬浮的标题栏，并支持上拉、下拉刷新，结合PullToRefresh和Android-ObservableScrollView开源项目

    运行效果如下，可以是listview或者recyclerview, 头部图片部分是可以滚动的viewpager：
![](https://github.com/zhonglushu/HoverTab/raw/master/effect_picture1.png)

activity中的代码主要是设置一些header、adapter
```Java
        //headerview
        View adView = inflater.inflate(R.layout.header_layout, null);
        ViewPager viewPager = (ViewPager) adView.findViewById(R.id.header_pager);
        AdPagerAdapter pagerAdapter = new AdPagerAdapter();
        viewPager.setAdapter(pagerAdapter);
        //设置上图效果中的图片部分，可以是任意的布局
        setHoverHeaderView(adView);

        //tabview
        View tabview = inflater.inflate(R.layout.tab_layout, null);
        tab1 = tabview.findViewById(R.id.tab1);
        tab2 = tabview.findViewById(R.id.tab2);
        tab3 = tabview.findViewById(R.id.tab3);
        tab1.setOnClickListener(mListener);
        tab2.setOnClickListener(mListener);
        tab3.setOnClickListener(mListener);
        tab1.setSelected(true);
        //设置上图效果中的Tab部分，可以是自定义的tab布局也可以是support中的TabLayout控件（可以自己结合viewpager写逻辑），
        支持任意的布局,
        setHoverTabView(tabview);

        //设置viewpager的adapter
        setmPagerAdapter(new LocalPagerAdapter(this.getSupportFragmentManager()));

        //自己写tab和viewpager的逻辑，有利于适应不同的需求
        setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    tab1.setSelected(true);
                    tab2.setSelected(false);
                    tab3.setSelected(false);
                } else if (position == 1) {
                    tab1.setSelected(false);
                    tab2.setSelected(true);
                    tab3.setSelected(false);
                } else if (position == 2) {
                    tab1.setSelected(false);
                    tab2.setSelected(false);
                    tab3.setSelected(true);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        //启动activity时设置自动刷新auto refresh
        setManualRefreshing();
```

Fragment中的代码比较简单

```Java
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final List<String> list = new ArrayList<String>();
        ...
        LocalAdapter mAdapter = new LocalAdapter(list);
        //设置listview或者recyclerview的adapter
        setListAdapter(mAdapter);
    }

    //(header)下拉刷新调用的函数
    @Override
    public void pullDownRefresh() {
        
    }
    
    //(footer)上拉刷新调用的函数
    @Override
    public void pullUpRefresh() {
        
    }
```
