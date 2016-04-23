package com.zhonglushu.example.hovertab.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;
import com.zhonglushu.example.hovertab.HoverTabActivity;
import com.zhonglushu.example.hovertab.R;
import com.zhonglushu.example.hovertab.observable.ScrollUtils;

/**
 * Created by zhonglushu on 2015/10/29.
 * 实现下拉刷新的类
 */
public class CustomPullDownRefreshLinearLayout extends LinearLayout{

    static final float FRICTION = 3.0f;
    private HoverTabActivity activity = null;
    private boolean mIsBeingDragged = false;
    private float mLastMotionX, mLastMotionY;
    private float mInitialMotionX, mInitialMotionY;
    private int mTouchSlop;
    private float mDownScrollY = 0.0f;
    private State mState = State.RESET;
    private ObservableRefreshView mRefreshView = null;
    public static final int SMOOTH_SCROLL_DURATION_MS = 400;
    private SmoothScrollRunnable mCurrentSmoothScrollRunnable;
    private Interpolator mScrollAnimationInterpolator;
    private OnRefreshListener mOnRefreshListener;
    private FrameLayout mTabLayout;
    private LinearLayout mHeaderLayout;
    private FrameLayout mHeaderContainer;
    private int mHeadHeight = 0;
    private boolean mIntercept = false;

    public void setActivity(HoverTabActivity activity) {
        this.activity = activity;
    }

    public CustomPullDownRefreshLinearLayout(Context context) {
        this(context, null);
    }

    public CustomPullDownRefreshLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public CustomPullDownRefreshLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ViewConfiguration config = ViewConfiguration.get(context);
        mTouchSlop = config.getScaledTouchSlop();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mRefreshView = (ObservableRefreshView) this.findViewById(R.id.com_zhonglushu_example_hovertab_refresh);
        mHeaderLayout = (LinearLayout) this.findViewById(R.id.com_zhonglushu_example_hovertab_header);
        mTabLayout = (FrameLayout) this.findViewById(R.id.com_zhonglushu_example_hovertab_tab);
        mHeaderContainer = (FrameLayout) this.findViewById(R.id.com_zhonglushu_example_hovertab_header_container);
    }

    public void setHoverHeaderView(View view){
        if(mHeaderContainer != null) {
            mHeaderContainer.addView(view);
            mHeadHeight += measureViewHeight(view);
        }
    }

    public void setHoverTabView(View view){
        if(mTabLayout != null) {
            mTabLayout.addView(view);
            mHeadHeight += measureViewHeight(view);
        }
    }

    private int measureViewHeight(View view){
        int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
        return view.getMeasuredHeight();
    }

    public int getmHeadHeight() {
        return mHeadHeight;
    }

    public void setmOnRefreshListener(OnRefreshListener mOnRefreshListener) {
        this.mOnRefreshListener = mOnRefreshListener;
    }

    @Override
    public final boolean onInterceptTouchEvent(MotionEvent event) {

        final int action = event.getAction();

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mIsBeingDragged = false;
            return false;
        }

        /*if (action != MotionEvent.ACTION_DOWN && mIsBeingDragged) {
            return true;
        }*/

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                if (activity.isReadyForPullStart()||isShowHeaderRefreshView()) {
                    final float y = event.getY(), x = event.getX();
                    final float diff, oppositeDiff, absDiff;

                    // We need to use the correct values, based on scroll
                    // direction
                    diff = y - mLastMotionY;
                    oppositeDiff = x - mLastMotionX;
                    absDiff = Math.abs(diff);
                    if (absDiff > mTouchSlop && absDiff > Math.abs(oppositeDiff)) {
                        //Log.i("zhonglushu", "diff = " + diff + ", activity.isReadyForPullStart() = " + activity.isReadyForPullStart() + ", isShowHeaderRefreshView() = " + isShowHeaderRefreshView());
                        if ((diff >= 1f && activity.isReadyForPullStart())||(isShowHeaderRefreshView() && diff < -1f)) {
                            mLastMotionY = y;
                            mLastMotionX = x;
                            mIsBeingDragged = true;
                        }
                    }
                }
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                if (activity.isReadyForPullStart()||isShowHeaderRefreshView()) {
                    mLastMotionY = mInitialMotionY = event.getY();
                    mLastMotionX = mInitialMotionX = event.getX();
                    Log.i("zhonglushu", "onInterceptTouchEvent MotionEvent.ACTION_DOWN mInitialMotionY = " + mInitialMotionY + ", mLastMotionY = " + mLastMotionY);
                    mIsBeingDragged = false;
                    mIntercept = false;
                    mDownScrollY = this.getScrollY();
                }
                break;
            }
        }

        return mIsBeingDragged;
    }

    @Override
    public final boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                //Log.i("zhonglushu", "event.getY() = " + event.getY());
                if (mIsBeingDragged) {
                    //Log.i("zhonglushu", "mIntercept = " + mIntercept + ", activity.isReadyForPullStart() = " + activity.isReadyForPullStart() + ", sthis.getScrollY() = " + this.getScrollY());
                    if(!mIntercept && activity.isReadyForPullStart() && Math.abs(this.getScrollY()) < 10 && isShowHeaderRefreshView()){
                        float y = event.getY();
                        float diff = y - mLastMotionY;
                        if(diff < -1f){
                            final MotionEvent ev = MotionEvent.obtainNoHistory(event);
                            if (onInterceptTouchEvent(ev)) {
                                mIntercept = true;

                                //滚动到scrollY == 0的位置
                                scrollTo(0, 0);

                                // If the parent wants to intercept ACTION_MOVE events,
                                // we pass ACTION_DOWN event to the parent
                                // as if these touch events just have began now.
                                ev.setAction(MotionEvent.ACTION_DOWN);

                                // Return this onTouchEvent() first and set ACTION_DOWN event for parent
                                // to the queue, to keep events sequence.
                                post(new Runnable() {
                                    @Override
                                    public void run() {
                                        dispatchTouchEvent(ev);
                                    }
                                });
                                return false;
                            }
                        }
                    }
                    if(activity.isReadyForPullStart() || isShowHeaderRefreshView()){
                        mLastMotionY = event.getY();
                        mLastMotionX = event.getX();
                        pullEvent();
                        return true;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                if (activity.isReadyForPullStart()||isShowHeaderRefreshView()) {
                    Log.i("zhonglushu", "onTouchEvent MotionEvent.ACTION_DOWN mInitialMotionY = " + mInitialMotionY + ", mLastMotionY = " + mLastMotionY);
                    mLastMotionY = mInitialMotionY = event.getY();
                    mLastMotionX = mInitialMotionX = event.getX();
                    mDownScrollY = this.getScrollY();
                    return true;
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                //Log.i("zhonglushu", "MotionEvent.ACTION_UP");
                mIntercept = false;
                mDownScrollY = 0.0f;
                mInitialMotionY = 0.0f;
                mLastMotionY = 0.0f;
                if (mIsBeingDragged) {
                    mIsBeingDragged = false;

                    if (mState == State.RELEASE_TO_REFRESH
                            && null != mOnRefreshListener) {
                        setState(State.REFRESHING, true);
                        return true;
                    }

                    // If we're already refreshing, just scroll back to the top
                    if (isRefreshing()) {
                        smoothScrollTo(0);
                        return true;
                    }

                    // If we haven't returned by here, then we're not in a state
                    // to pull, so just reset
                    setState(State.RESET);

                    return true;
                }
                break;
            }
        }

        return false;
    }

    /**
     * Smooth Scroll to position using the default duration of
     * {@value #SMOOTH_SCROLL_DURATION_MS} ms.
     *
     * @param scrollValue - Position to scroll to
     */
    protected final void smoothScrollTo(int scrollValue) {
        smoothScrollTo(scrollValue, SMOOTH_SCROLL_DURATION_MS);
    }

    /**
     * Smooth Scroll to position using the specific duration
     *
     * @param scrollValue - Position to scroll to
     * @param duration - Duration of animation in milliseconds
     */
    private final void smoothScrollTo(int scrollValue, long duration) {
        smoothScrollTo(scrollValue, duration, 0, null);
    }

    /**
     * Smooth Scroll to position using the default duration of
     * {@value #SMOOTH_SCROLL_DURATION_MS} ms.
     *
     * @param scrollValue - Position to scroll to
     * @param listener - Listener for scroll
     */
    protected final void smoothScrollTo(int scrollValue, OnSmoothScrollFinishedListener listener) {
        smoothScrollTo(scrollValue,SMOOTH_SCROLL_DURATION_MS , 0, listener);
    }

    private final void smoothScrollTo(int newScrollValue, long duration, long delayMillis,
                                      OnSmoothScrollFinishedListener listener) {
        if (null != mCurrentSmoothScrollRunnable) {
            mCurrentSmoothScrollRunnable.stop();
        }

        final int oldScrollValue;
        oldScrollValue = getScrollY();

        if (oldScrollValue != newScrollValue) {
            if (null == mScrollAnimationInterpolator) {
                // Default interpolator is a Decelerate Interpolator
                mScrollAnimationInterpolator = new DecelerateInterpolator();
            }
            mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(oldScrollValue, newScrollValue, duration, listener);

            if (delayMillis > 0) {
                postDelayed(mCurrentSmoothScrollRunnable, delayMillis);
            } else {
                post(mCurrentSmoothScrollRunnable);
            }
        }
    }

    /**
     * Actions a Pull Event
     *
     * @return true if the Event has been handled, false if there has been no
     *         change
     */
    private void pullEvent() {
        final int newScrollValue;
        final int itemDimension;
        final float initialMotionValue, lastMotionValue;

        initialMotionValue = mInitialMotionY;
        lastMotionValue = mLastMotionY;
        //Log.i("zhonglushu", "pullEvent() mLastMotionY = " + mLastMotionY);
        float deltaY = initialMotionValue - lastMotionValue;
        /*if(isRefreshing() && deltaY > 1.0f){
            newScrollValue = Math.min(0, Math.round(mDownScrollY + deltaY / FRICTION));
        }else{
            newScrollValue = Math.round(Math.min(deltaY, 0) / FRICTION);
        }*/
        Log.i("zhonglushu", "initialMotionValue = " + initialMotionValue + ", lastMotionValue = " + lastMotionValue + ", mDownScrollY = " + mDownScrollY);
        newScrollValue = Math.round(mDownScrollY + deltaY / FRICTION);//Math.min(0, );
        itemDimension = getRefreshFooterHeight();

        setHeaderScroll(newScrollValue);

        if (newScrollValue != 0 && !isRefreshing()) {
            if (mState != State.PULL_TO_REFRESH && itemDimension >= Math.abs(newScrollValue)) {
                setState(State.PULL_TO_REFRESH);
            } else if (mState == State.PULL_TO_REFRESH && itemDimension < Math.abs(newScrollValue)) {
                setState(State.RELEASE_TO_REFRESH);
            }
        }
    }

    public boolean isShowHeaderRefreshView(){
        if(isRefreshing() && this.getScrollY() < 0){
            return true;
        }
        return false;
    }

    /**
     * Helper method which just calls scrollTo() in the correct scrolling
     * direction.
     *
     * @param value - New Scroll value
     */
    protected final void setHeaderScroll(int value) {
        // Clamp value to with pull scroll range
        final int maximumPullScroll = getMaximumPullScroll();
        value = Math.min(maximumPullScroll, Math.max(-maximumPullScroll, value));

        if (value < 0) {
            mRefreshView.setVisibility(View.VISIBLE);
        }
        scrollTo(0, value);
    }

    /**
     * Updates the View State when the mode has been set. This does not do any
     * checking that the mode is different to current state so always updates.
     */
    protected void updateUIForMode() {
        // Hide Loading Views
        refreshLoadingViewsSize();
    }

    @Override
    protected final void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // We need to update the header/footer when our size changes
        refreshLoadingViewsSize();

        // Update the Refreshable View layout
        //refreshRefreshableViewSize(w, h);

        /**
         * As we're currently in a Layout Pass, we need to schedule another one
         * to layout any changes we've made here
         */
        post(new Runnable() {
            @Override
            public void run() {
                requestLayout();
            }
        });
    }

    /**
     * Re-measure the Loading Views height, and adjust internal padding as
     * necessary
     */
    protected final void refreshLoadingViewsSize() {
        final int maximumPullScroll = this.getMaximumPullScroll();

        int pLeft = getPaddingLeft();
        int pTop = getPaddingTop();
        int pRight = getPaddingRight();
        int pBottom = getPaddingBottom();
        ViewGroup.LayoutParams lp = (ViewGroup.LayoutParams) mRefreshView.getLayoutParams();
        lp.height = maximumPullScroll;
        mRefreshView.requestLayout();
        pTop = -maximumPullScroll;
        setPadding(pLeft, pTop, pRight, pBottom);
    }

    private int getMaximumPullScroll() {
        return Math.round(this.getHeight() / FRICTION);
    }

    private void setState(State state, final boolean... params) {
        mState = state;
        switch (mState) {
            case RESET:
                onReset();
                break;
            case PULL_TO_REFRESH:
                onPullToRefresh();
                break;
            case RELEASE_TO_REFRESH:
                onReleaseToRefresh();
                break;
            case REFRESHING:
            case MANUAL_REFRESHING:
                onRefreshing(params[0]);
                break;
            case OVERSCROLLING:
                // NO-OP
                break;
        }

        // Call OnPullEventListener
        /*if (null != mOnPullEventListener) {
            mOnPullEventListener.onPullEvent(this, mState, mCurrentMode);
        }*/
    }

    protected  void onReset(){
        mIsBeingDragged = false;
        smoothScrollTo(0, new OnSmoothScrollFinishedListener() {

            @Override
            public void onSmoothScrollFinished() {
                // Always reset both layouts, just in case...
                mRefreshView.reset();
                //mFooterLayout.reset();
            }
        });
    }

    /**
     * Called when the UI has been to be updated to be in the
     * {@link State#PULL_TO_REFRESH} state.
     */
    protected void onPullToRefresh() {
        mRefreshView.setPullToRefreshText();
    }

    /**
     * Called when the UI has been to be updated to be in the
     * {@link State#RELEASE_TO_REFRESH} state.
     */
    protected void onReleaseToRefresh() {
        mRefreshView.setReleaseToRefreshText();
    }

    /**
     * Called when the UI has been to be updated to be in the
     * {@link State#REFRESHING} or {@link State#MANUAL_REFRESHING} state.
     *
     * @param doScroll - Whether the UI should scroll for this event.
     */
    protected void onRefreshing(final boolean doScroll) {
        mRefreshView.setRefreshingText();
        mRefreshView.startRotationAnim();

        if (doScroll) {
            // Call Refresh Listener when the Scroll has finished
            OnSmoothScrollFinishedListener listener = new OnSmoothScrollFinishedListener() {
                @Override
                public void onSmoothScrollFinished() {
                    callRefreshListener();
                }
            };
            smoothScrollTo(-getRefreshFooterHeight(), listener);
        } else {
            // We're not scrolling, so just call Refresh Listener now
            callRefreshListener();
        }
    }

    public void onRefreshComplete() {
        if (isRefreshing()) {
            setState(State.RESET);
        }
    }

    private void callRefreshListener() {
        if (null != mOnRefreshListener) {
            mOnRefreshListener.onRefresh();
        }
    }

    public int getTabHeight() {
        return mTabLayout == null? 0 : mTabLayout.getHeight();
    }

    public int getHeaderHeight() {
        return mHeaderLayout == null? 0 : mHeaderLayout.getHeight();
    }

    public int getRefreshHeight() {
        return mRefreshView == null? 0 : mRefreshView.getHeight();
    }

    public int getRefreshFooterHeight() {
        return (int)(mRefreshView.getHeight()/FRICTION);
    }

    public final boolean isRefreshing() {
        return mState == State.REFRESHING || mState == State.MANUAL_REFRESHING;
    }

    public void translateTab(int scrollY, boolean animated) {
        ViewPropertyAnimator.animate(mHeaderLayout).cancel();
        // Tabs will move between the top of the screen to the bottom of the image.
        float translationY = ScrollUtils.getFloat(-scrollY, -(getHeaderHeight() - getTabHeight()), 0);
        if (animated) {
            // Animation will be invoked only when the current tab is changed.
            ViewPropertyAnimator.animate(mHeaderLayout)
                    .translationY(translationY)
                    .setDuration(200)
                    .start();
        } else {
            // When Fragments' scroll, translate tabs immediately (without animation).
            ViewHelper.setTranslationY(mHeaderLayout, translationY);
        }
    }

    final class SmoothScrollRunnable implements Runnable {
        private final Interpolator mInterpolator;
        private final int mScrollToY;
        private final int mScrollFromY;
        private final long mDuration;
        private OnSmoothScrollFinishedListener mListener;

        private boolean mContinueRunning = true;
        private long mStartTime = -1;
        private int mCurrentY = -1;

        public SmoothScrollRunnable(int fromY, int toY, long duration, OnSmoothScrollFinishedListener listener) {
            mScrollFromY = fromY;
            mScrollToY = toY;
            mInterpolator = mScrollAnimationInterpolator;
            mDuration = duration;
            mListener = listener;
        }

        @Override
        public void run() {

            /**
             * Only set mStartTime if this is the first time we're starting,
             * else actually calculate the Y delta
             */
            if (mStartTime == -1) {
                mStartTime = System.currentTimeMillis();
            } else {

                /**
                 * We do do all calculations in long to reduce software float
                 * calculations. We use 1000 as it gives us good accuracy and
                 * small rounding errors
                 */
                long normalizedTime = (1000 * (System.currentTimeMillis() - mStartTime)) / mDuration;
                normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

                final int deltaY = Math.round((mScrollFromY - mScrollToY)
                        * mInterpolator.getInterpolation(normalizedTime / 1000f));
                mCurrentY = mScrollFromY - deltaY;
                setHeaderScroll(mCurrentY);
            }

            // If we're not at the target Y, keep going...
            if (mContinueRunning && mScrollToY != mCurrentY) {
                ViewCompat.postOnAnimation(CustomPullDownRefreshLinearLayout.this, this);
            } else {
                if (null != mListener) {
                    mListener.onSmoothScrollFinished();
                }
            }
        }

        public void stop() {
            mContinueRunning = false;
            removeCallbacks(this);
        }
    }

    /**
     * Simple Listener to listen for any callbacks to Refresh.
     *
     * @author Chris Banes
     */
    public static interface OnRefreshListener {

        /**
         * onRefresh will be called for both a Pull from start, and Pull from
         * end
         */
        public void onRefresh();

    }

    static interface OnSmoothScrollFinishedListener {
        void onSmoothScrollFinished();
    }

    public static enum State {

        /**
         * When the UI is in a state which means that user is not interacting
         * with the Pull-to-Refresh function.
         */
        RESET(0x0),

        /**
         * When the UI is being pulled by the user, but has not been pulled far
         * enough so that it refreshes when released.
         */
        PULL_TO_REFRESH(0x1),

        /**
         * When the UI is being pulled by the user, and <strong>has</strong>
         * been pulled far enough so that it will refresh when released.
         */
        RELEASE_TO_REFRESH(0x2),

        /**
         * When the UI is currently refreshing, caused by a pull gesture.
         */
        REFRESHING(0x8),

        /**
         * When the UI is currently refreshing, caused by a call to
         */
        MANUAL_REFRESHING(0x9),

        /**
         * When the UI is currently overscrolling, caused by a fling on the
         * Refreshable View.
         */
        OVERSCROLLING(0x10);

        /**
         * Maps an int to a specific state. This is needed when saving state.
         *
         * @param stateInt - int to map a State to
         * @return State that stateInt maps to
         */
        static State mapIntToValue(final int stateInt) {
            for (State value : State.values()) {
                if (stateInt == value.getIntValue()) {
                    return value;
                }
            }

            // If not, return default
            return RESET;
        }

        private int mIntValue;

        State(int intValue) {
            mIntValue = intValue;
        }

        int getIntValue() {
            return mIntValue;
        }
    }
}
