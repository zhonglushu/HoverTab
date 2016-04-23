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

package com.zhonglushu.example.hovertab.views;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import com.zhonglushu.example.hovertab.R;
import com.zhonglushu.example.hovertab.Utils.Mode;
import com.zhonglushu.example.hovertab.Utils.OnPullUpRefreshListener;
import com.zhonglushu.example.hovertab.observable.ObservableScrollViewCallbacks;
import com.zhonglushu.example.hovertab.observable.ScrollState;
import com.zhonglushu.example.hovertab.observable.Scrollable;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView that its scroll position can be observed.
 * Before using this, please consider to use the RecyclerView.OnScrollListener
 * provided by the support library officially.
 */
public class ObservableRecyclerView extends RecyclerView implements Scrollable {

    private static int recyclerViewLibraryVersion = 22;

    // Fields that should be saved onSaveInstanceState
    private int mPrevFirstVisiblePosition;
    private int mPrevFirstVisibleChildHeight = -1;
    private int mPrevScrolledChildrenHeight;
    private int mPrevScrollY;
    private int mScrollY;
    private SparseIntArray mChildrenHeights;

    // Fields that don't need to be saved onSaveInstanceState
    private ObservableScrollViewCallbacks mCallbacks;
    private List<ObservableScrollViewCallbacks> mCallbackCollection;
    private ScrollState mScrollState;
    private boolean mFirstScroll;
    private boolean mDragging;
    private boolean mIntercepted;
    private MotionEvent mPrevMoveEvent;
    private ViewGroup mTouchInterceptionViewGroup;
    private OnPullUpRefreshListener mOnRefreshListener = null;
    private Mode mCurrentMode = Mode.getDefault();
    private boolean isRefreshing = false;
    private boolean hasNoMore = false;
    private boolean isNetworkError = false;
    private String networkErrorStr = null;
    private View mHeaderView;
    private ObservableRefreshView mFooterView;
    private OnItemClickListener mOnItemClickListener;
    private float mInitTouchX;
    private float mInitTouchY;
    private int mTouchSlop;

    public ObservableRecyclerView(Context context) {
        super(context);
        init();
    }

    public ObservableRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ObservableRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        mPrevFirstVisiblePosition = ss.prevFirstVisiblePosition;
        mPrevFirstVisibleChildHeight = ss.prevFirstVisibleChildHeight;
        mPrevScrolledChildrenHeight = ss.prevScrolledChildrenHeight;
        mPrevScrollY = ss.prevScrollY;
        mScrollY = ss.scrollY;
        mChildrenHeights = ss.childrenHeights;
        super.onRestoreInstanceState(ss.getSuperState());
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.prevFirstVisiblePosition = mPrevFirstVisiblePosition;
        ss.prevFirstVisibleChildHeight = mPrevFirstVisibleChildHeight;
        ss.prevScrolledChildrenHeight = mPrevScrolledChildrenHeight;
        ss.prevScrollY = mPrevScrollY;
        ss.scrollY = mScrollY;
        ss.childrenHeights = mChildrenHeights;
        return ss;
    }

    public void setOnItemClickListener(OnItemClickListener li) {
        mOnItemClickListener = li;
    }

    public void setHeaderView(View headerView) {
        mHeaderView = headerView;
        //getAdapter().notifyItemInserted(0);
    }

    public void setmFooterView(ObservableRefreshView mFooterView) {
        this.mFooterView = mFooterView;
    }

    public View getmHeaderView() {
        return mHeaderView;
    }

    public ObservableRefreshView getmFooterView() {
        return mFooterView;
    }

    public OnItemClickListener getmOnItemClickListener() {
        return mOnItemClickListener;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (hasNoCallbacks()) {
            return;
        }
        if (getChildCount() > 0) {
            int firstVisiblePosition = getChildAdapterPosition(getChildAt(0));
            int lastVisiblePosition = getChildAdapterPosition(getChildAt(getChildCount() - 1));
            for (int i = firstVisiblePosition, j = 0; i <= lastVisiblePosition; i++, j++) {
                int childHeight = 0;
                View child = getChildAt(j);
                if (child != null) {
                    if (mChildrenHeights.indexOfKey(i) < 0 || (child.getHeight() != mChildrenHeights.get(i))) {
                        childHeight = child.getHeight();
                    }
                }
                mChildrenHeights.put(i, childHeight);
            }

            View firstVisibleChild = getChildAt(0);
            if (firstVisibleChild != null) {
                if (mPrevFirstVisiblePosition < firstVisiblePosition) {
                    // scroll down
                    int skippedChildrenHeight = 0;
                    if (firstVisiblePosition - mPrevFirstVisiblePosition != 1) {
                        for (int i = firstVisiblePosition - 1; i > mPrevFirstVisiblePosition; i--) {
                            if (0 < mChildrenHeights.indexOfKey(i)) {
                                skippedChildrenHeight += mChildrenHeights.get(i);
                            } else {
                                // Approximate each item's height to the first visible child.
                                // It may be incorrect, but without this, scrollY will be broken
                                // when scrolling from the bottom.
                                skippedChildrenHeight += firstVisibleChild.getHeight();
                            }
                        }
                    }
                    mPrevScrolledChildrenHeight += mPrevFirstVisibleChildHeight + skippedChildrenHeight;
                    mPrevFirstVisibleChildHeight = firstVisibleChild.getHeight();
                } else if (firstVisiblePosition < mPrevFirstVisiblePosition) {
                    // scroll up
                    int skippedChildrenHeight = 0;
                    if (mPrevFirstVisiblePosition - firstVisiblePosition != 1) {
                        for (int i = mPrevFirstVisiblePosition - 1; i > firstVisiblePosition; i--) {
                            if (0 < mChildrenHeights.indexOfKey(i)) {
                                skippedChildrenHeight += mChildrenHeights.get(i);
                            } else {
                                // Approximate each item's height to the first visible child.
                                // It may be incorrect, but without this, scrollY will be broken
                                // when scrolling from the bottom.
                                skippedChildrenHeight += firstVisibleChild.getHeight();
                            }
                        }
                    }
                    mPrevScrolledChildrenHeight -= firstVisibleChild.getHeight() + skippedChildrenHeight;
                    mPrevFirstVisibleChildHeight = firstVisibleChild.getHeight();
                } else if (firstVisiblePosition == 0) {
                    mPrevFirstVisibleChildHeight = firstVisibleChild.getHeight();
                    mPrevScrolledChildrenHeight = 0;
                }
                if (mPrevFirstVisibleChildHeight < 0) {
                    mPrevFirstVisibleChildHeight = 0;
                }
                mScrollY = mPrevScrolledChildrenHeight - firstVisibleChild.getTop() + getPaddingTop();
                mPrevFirstVisiblePosition = firstVisiblePosition;

                dispatchOnScrollChanged(mScrollY, mFirstScroll, mDragging);
                if (mFirstScroll) {
                    mFirstScroll = false;
                }

                if (mPrevScrollY < mScrollY) {
                    //down
                    mScrollState = ScrollState.UP;
                } else if (mScrollY < mPrevScrollY) {
                    //up
                    mScrollState = ScrollState.DOWN;
                } else {
                    mScrollState = ScrollState.STOP;
                }
                mPrevScrollY = mScrollY;
            }

            if(!isRefreshing && !hasNoMore && !isNetworkError) {
                int[] positions = getLastestVisiblePosition();
                for(int i = 0; i < positions.length; i++){
                    if (positions[i] == getAdapter().getItemCount() - 1) {
                        if (mCurrentMode == Mode.PULL_FROM_END) {
                            isRefreshing = true;
                            mFooterView.setRefreshingText();
                            if(mOnRefreshListener != null){
                                mOnRefreshListener.onRefresh();
                                mFooterView.startRotationAnim();
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    public void setmOnRefreshListener(OnPullUpRefreshListener mOnRefreshListener) {
        this.mOnRefreshListener = mOnRefreshListener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (hasNoCallbacks()) {
            return super.onInterceptTouchEvent(ev);
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // Whether or not motion events are consumed by children,
                // flag initializations which are related to ACTION_DOWN events should be executed.
                // Because if the ACTION_DOWN is consumed by children and only ACTION_MOVEs are
                // passed to parent (this view), the flags will be invalid.
                // Also, applications might implement initialization codes to onDownMotionEvent,
                // so call it here.
                mFirstScroll = mDragging = true;
                mInitTouchX = ev.getX();
                mInitTouchY = ev.getY();
                dispatchOnDownMotionEvent();
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    public Mode getmCurrentMode() {
        return mCurrentMode;
    }

    public void setmCurrentMode(Mode mCurrentMode) {
        this.mCurrentMode = mCurrentMode;
        if(this.mCurrentMode == Mode.DISABLED){
            mFooterView.setmFooterViewText("");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (hasNoCallbacks()) {
            return super.onTouchEvent(ev);
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float deltaX = ev.getX() - mInitTouchX;
                float deltaY = ev.getY() - mInitTouchY;
                if(!isRefreshing && !hasNoMore && isNetworkError && Math.abs(deltaY) > Math.abs(deltaX) && deltaY < -mTouchSlop*10) {
                    int[] positions = getLastestVisiblePosition();
                    for(int i = 0; i < positions.length; i++){
                        if (positions[i] == getAdapter().getItemCount() - 1) {
                            if (mCurrentMode == Mode.PULL_FROM_END) {
                                isNetworkError = false;
                                isRefreshing = true;
                                mFooterView.setRefreshingText();
                                if(mOnRefreshListener != null){
                                    mOnRefreshListener.onRefresh();
                                    mFooterView.startRotationAnim();
                                }
                                break;
                            }
                        }
                    }
                }
                mInitTouchY = 0.0f;
                mInitTouchX = 0.0f;
                mIntercepted = false;
                mDragging = false;
                dispatchOnUpOrCancelMotionEvent(mScrollState);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mPrevMoveEvent == null) {
                    mPrevMoveEvent = ev;
                }
                float diffY = ev.getY() - mPrevMoveEvent.getY();
                mPrevMoveEvent = MotionEvent.obtainNoHistory(ev);
                if (getCurrentScrollY() - diffY <= 0) {
                    // Can't scroll anymore.

                    if (mIntercepted) {
                        // Already dispatched ACTION_DOWN event to parents, so stop here.
                        return false;
                    }

                    // Apps can set the interception target other than the direct parent.
                    final ViewGroup parent;
                    if (mTouchInterceptionViewGroup == null) {
                        parent = (ViewGroup) getParent();
                    } else {
                        parent = mTouchInterceptionViewGroup;
                    }

                    // Get offset to parents. If the parent is not the direct parent,
                    // we should aggregate offsets from all of the parents.
                    float offsetX = 0;
                    float offsetY = 0;
                    for (View v = this; v != null && v != parent; v = (View) v.getParent()) {
                        offsetX += v.getLeft() - v.getScrollX();
                        offsetY += v.getTop() - v.getScrollY();
                    }
                    final MotionEvent event = MotionEvent.obtainNoHistory(ev);
                    event.offsetLocation(offsetX, offsetY);

                    if (parent.onInterceptTouchEvent(event)) {
                        mIntercepted = true;

                        // If the parent wants to intercept ACTION_MOVE events,
                        // we pass ACTION_DOWN event to the parent
                        // as if these touch events just have began now.
                        event.setAction(MotionEvent.ACTION_DOWN);

                        // Return this onTouchEvent() first and set ACTION_DOWN event for parent
                        // to the queue, to keep events sequence.
                        post(new Runnable() {
                            @Override
                            public void run() {
                                parent.dispatchTouchEvent(event);
                            }
                        });
                        return false;
                    }
                    // Even when this can't be scrolled anymore,
                    // simply returning false here may cause subView's click,
                    // so delegate it to super.
                    return super.onTouchEvent(ev);
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    public void setScrollViewCallbacks(ObservableScrollViewCallbacks listener) {
        mCallbacks = listener;
    }

    @Override
    public void addScrollViewCallbacks(ObservableScrollViewCallbacks listener) {
        if (mCallbackCollection == null) {
            mCallbackCollection = new ArrayList<>();
        }
        mCallbackCollection.add(listener);
    }

    @Override
    public void removeScrollViewCallbacks(ObservableScrollViewCallbacks listener) {
        if (mCallbackCollection != null) {
            mCallbackCollection.remove(listener);
        }
    }

    @Override
    public void clearScrollViewCallbacks() {
        if (mCallbackCollection != null) {
            mCallbackCollection.clear();
        }
    }

    @Override
    public void setTouchInterceptionViewGroup(ViewGroup viewGroup) {
        mTouchInterceptionViewGroup = viewGroup;
    }

    @Override
    public boolean isReadyForPullStart() {
        LinearLayoutManager lm= (LinearLayoutManager) getLayoutManager();
        if (lm.findFirstVisibleItemPosition() <= 1) {
            final View firstVisibleChild = this.getChildAt(0);
            if (firstVisibleChild != null) {
                return lm.findViewByPosition(lm.findFirstVisibleItemPosition()).getTop() + 10 >= this.getTop();
            }
        }
        return false;
    }

    private int[] getLastestVisiblePosition(){
        LayoutManager lm= getLayoutManager();
        if(lm instanceof LinearLayoutManager){
            return new int[]{((LinearLayoutManager) lm).findLastVisibleItemPosition()};
        }else if(lm instanceof GridLayoutManager){
            return new int[]{((GridLayoutManager) lm).findLastVisibleItemPosition()};
        }else if(lm instanceof StaggeredGridLayoutManager){
            int[] lastItem = new int[((StaggeredGridLayoutManager) lm).getSpanCount()];
            ((StaggeredGridLayoutManager) lm).findLastVisibleItemPositions(lastItem);
            return lastItem;
        }
        return null;
    }

    @Override
    public void scrollVerticallyTo(int y) {
        View firstVisibleChild = getChildAt(0);
        if (firstVisibleChild != null) {
            int baseHeight = firstVisibleChild.getHeight();
            int position = y / baseHeight;
            scrollVerticallyToPosition(position);
        }
    }

    /**
     * <p>Same as {@linkplain #scrollToPosition(int)} but it scrolls to the position not only make
     * the position visible.</p>
     * <p>It depends on {@code LayoutManager} how {@linkplain #scrollToPosition(int)} works,
     * and currently we know that {@linkplain LinearLayoutManager#scrollToPosition(int)} just
     * make the position visible.</p>
     * <p>In LinearLayoutManager, scrollToPositionWithOffset() is provided for scrolling to the position.
     * This method checks which LayoutManager is set,
     * and handles which method should be called for scrolling.</p>
     * <p>Other know classes (StaggeredGridLayoutManager and GridLayoutManager) are not tested.</p>
     *
     * @param position Position to scroll.
     */
    public void scrollVerticallyToPosition(int position) {
        LayoutManager lm = getLayoutManager();

        if (lm != null && lm instanceof LinearLayoutManager) {
            ((LinearLayoutManager) lm).scrollToPositionWithOffset(position, 0);
        } else {
            scrollToPosition(position);
        }
    }

    @Override
    public int getCurrentScrollY() {
        return mScrollY;
    }

    @SuppressWarnings("deprecation")
    public int getChildAdapterPosition(View child) {
        if (22 <= recyclerViewLibraryVersion) {
            return super.getChildAdapterPosition(child);
        }
        return getChildPosition(child);
    }

    private void init() {
        ViewConfiguration config = ViewConfiguration.get(getContext());
        mTouchSlop = config.getScaledTouchSlop();
        mChildrenHeights = new SparseIntArray();
        checkLibraryVersion();
        networkErrorStr = this.getContext().getResources().getString(R.string.network_error);
    }

    private void checkLibraryVersion() {
        try {
            super.getChildAdapterPosition(null);
        } catch (NoSuchMethodError e) {
            recyclerViewLibraryVersion = 21;
        }
    }

    private void dispatchOnDownMotionEvent() {
        if (mCallbacks != null) {
            mCallbacks.onDownMotionEvent();
        }
        if (mCallbackCollection != null) {
            for (int i = 0; i < mCallbackCollection.size(); i++) {
                ObservableScrollViewCallbacks callbacks = mCallbackCollection.get(i);
                callbacks.onDownMotionEvent();
            }
        }
    }

    private void dispatchOnScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
        if (mCallbacks != null) {
            mCallbacks.onScrollChanged(scrollY, firstScroll, dragging);
        }
        if (mCallbackCollection != null) {
            for (int i = 0; i < mCallbackCollection.size(); i++) {
                ObservableScrollViewCallbacks callbacks = mCallbackCollection.get(i);
                callbacks.onScrollChanged(scrollY, firstScroll, dragging);
            }
        }
    }

    private void dispatchOnUpOrCancelMotionEvent(ScrollState scrollState) {
        if (mCallbacks != null) {
            mCallbacks.onUpOrCancelMotionEvent(scrollState);
        }
        if (mCallbackCollection != null) {
            for (int i = 0; i < mCallbackCollection.size(); i++) {
                ObservableScrollViewCallbacks callbacks = mCallbackCollection.get(i);
                callbacks.onUpOrCancelMotionEvent(scrollState);
            }
        }
    }

    private boolean hasNoCallbacks() {
        return mCallbacks == null && mCallbackCollection == null;
    }

    //刷新完成
    public void onRefreshComplete(){
        if(isRefreshing){
            isRefreshing = false;
            if(isNetworkError){
                mFooterView.setmFooterViewText(this.networkErrorStr);
            }
            if(hasNoMore) {
                mFooterView.setHasNoMoreText();
            }
            mFooterView.stopRotationAnim();
        }
    }

    public void setHasNoMore(boolean hasNoMore) {
        this.hasNoMore = hasNoMore;
    }

    public void setNetworkError(boolean isNetworkError, String networkErrorStr) {
        this.isNetworkError = isNetworkError;
        if(networkErrorStr != null && !TextUtils.isEmpty(networkErrorStr))
            this.networkErrorStr = networkErrorStr;
    }

    public void setSelectionFromTop(int position, int offset){
        LayoutManager lm= getLayoutManager();
        if(lm instanceof LinearLayoutManager){
            ((LinearLayoutManager) lm).scrollToPositionWithOffset(position, offset);
        }else if(lm instanceof GridLayoutManager){
            ((GridLayoutManager) lm).scrollToPositionWithOffset(position, offset);
        }else if(lm instanceof StaggeredGridLayoutManager){
            ((StaggeredGridLayoutManager) lm).scrollToPositionWithOffset(position, offset);
        }
    }

    /**
     * This saved state class is a Parcelable and should not extend
     * {@link View.BaseSavedState} nor {@link android.view.AbsSavedState}
     * because its super class AbsSavedState's constructor
     * {@link android.view.AbsSavedState#AbsSavedState(Parcel)} currently passes null
     * as a class loader to read its superstate from Parcelable.
     * This causes {@link android.os.BadParcelableException} when restoring saved states.
     * <p/>
     * The super class "RecyclerView" is a part of the support library,
     * and restoring its saved state requires the class loader that loaded the RecyclerView.
     * It seems that the class loader is not required when restoring from RecyclerView itself,
     * but it is required when restoring from RecyclerView's subclasses.
     */
    static class SavedState implements Parcelable {
        public static final SavedState EMPTY_STATE = new SavedState() {
        };

        int prevFirstVisiblePosition;
        int prevFirstVisibleChildHeight = -1;
        int prevScrolledChildrenHeight;
        int prevScrollY;
        int scrollY;
        SparseIntArray childrenHeights;

        // This keeps the parent(RecyclerView)'s state
        Parcelable superState;

        /**
         * Called by EMPTY_STATE instantiation.
         */
        private SavedState() {
            superState = null;
        }

        /**
         * Called by onSaveInstanceState.
         */
        SavedState(Parcelable superState) {
            this.superState = superState != EMPTY_STATE ? superState : null;
        }

        /**
         * Called by CREATOR.
         */
        private SavedState(Parcel in) {
            // Parcel 'in' has its parent(RecyclerView)'s saved state.
            // To restore it, class loader that loaded RecyclerView is required.
            Parcelable superState = in.readParcelable(RecyclerView.class.getClassLoader());
            this.superState = superState != null ? superState : EMPTY_STATE;

            prevFirstVisiblePosition = in.readInt();
            prevFirstVisibleChildHeight = in.readInt();
            prevScrolledChildrenHeight = in.readInt();
            prevScrollY = in.readInt();
            scrollY = in.readInt();
            childrenHeights = new SparseIntArray();
            final int numOfChildren = in.readInt();
            if (0 < numOfChildren) {
                for (int i = 0; i < numOfChildren; i++) {
                    final int key = in.readInt();
                    final int value = in.readInt();
                    childrenHeights.put(key, value);
                }
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeParcelable(superState, flags);

            out.writeInt(prevFirstVisiblePosition);
            out.writeInt(prevFirstVisibleChildHeight);
            out.writeInt(prevScrolledChildrenHeight);
            out.writeInt(prevScrollY);
            out.writeInt(scrollY);
            final int numOfChildren = childrenHeights == null ? 0 : childrenHeights.size();
            out.writeInt(numOfChildren);
            if (0 < numOfChildren) {
                for (int i = 0; i < numOfChildren; i++) {
                    out.writeInt(childrenHeights.keyAt(i));
                    out.writeInt(childrenHeights.valueAt(i));
                }
            }
        }

        public Parcelable getSuperState() {
            return superState;
        }

        public static final Creator<SavedState> CREATOR
            = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public static abstract class BaseRecyclerAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public static final int TYPE_HEADER = 0;
        public static final int TYPE_NORMAL = 1;
        public static final int TYPE_FOOTER = 2;
        private ObservableRecyclerView mRecyclerView = null;

        private ArrayList<T> mDatas = new ArrayList<>();

        public void setmRecyclerView(ObservableRecyclerView mRecyclerView) {
            this.mRecyclerView = mRecyclerView;
        }

        public void addDatas(ArrayList<T> datas) {
            mDatas.addAll(datas);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0 && mRecyclerView.getmHeaderView() != null) return TYPE_HEADER;
            if (position == getItemCount() - 1 && mRecyclerView.getmFooterView() != null) return TYPE_FOOTER;
            return TYPE_NORMAL;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, final int viewType) {
            if (mRecyclerView.getmHeaderView() != null && viewType == TYPE_HEADER) return new Holder(mRecyclerView.getmHeaderView());
            if (mRecyclerView.getmFooterView() != null && viewType == TYPE_FOOTER) return new Holder(mRecyclerView.getmFooterView());
            return onCreate(parent, viewType);
        }


        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            int type = getItemViewType(position);
            if (type == TYPE_HEADER || type == TYPE_FOOTER) return;

            final int pos = getRealPosition(viewHolder);
            final T data = mDatas.get(pos);
            onBind(viewHolder, pos, data);

            if (mRecyclerView.getmOnItemClickListener() != null) {
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mRecyclerView.getmOnItemClickListener().onItemClick(pos, data);
                    }
                });
            }
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);

            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if (manager instanceof GridLayoutManager) {
                final GridLayoutManager gridManager = ((GridLayoutManager) manager);
                gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        int type = getItemViewType(position);
                        return (type == TYPE_HEADER || type == TYPE_FOOTER)
                                ? gridManager.getSpanCount() : 1;
                    }
                });
            }
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            if (lp != null
                    && lp instanceof StaggeredGridLayoutManager.LayoutParams) {
                StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
                int position = holder.getLayoutPosition();
                if ((mRecyclerView.getmHeaderView() != null && position == 0) || (mRecyclerView.getmHeaderView() != null && position == getItemCount() - 1)) {
                    p.setFullSpan(true);
                } else {
                    p.setFullSpan(false);
                }
            }
        }

        public int getRealPosition(RecyclerView.ViewHolder holder) {
            int position = holder.getLayoutPosition();
            return mRecyclerView.getmHeaderView() == null ? position : position - 1;
        }

        @Override
        public int getItemCount() {
            int size = mDatas.size();
            if (mRecyclerView.getmHeaderView() != null) {
                size += 1;
            }
            if (mRecyclerView.getmHeaderView() != null) {
                size += 1;
            }
            return size;
        }

        public abstract RecyclerView.ViewHolder onCreate(ViewGroup parent, final int viewType);

        public abstract void onBind(RecyclerView.ViewHolder viewHolder, int RealPosition, T data);
    }

    public static class Holder extends RecyclerView.ViewHolder {
        public Holder(View itemView) {
            super(itemView);
        }
    }

    public interface OnItemClickListener<T> {
        void onItemClick(int position, T data);
    }
}
