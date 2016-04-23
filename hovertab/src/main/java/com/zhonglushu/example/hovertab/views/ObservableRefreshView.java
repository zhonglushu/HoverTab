package com.zhonglushu.example.hovertab.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.nineoldandroids.animation.ObjectAnimator;
import com.zhonglushu.example.hovertab.R;

/**
 * Created by zhonglushu on 2015/10/15.
 * ObservableListView的footer和header
 */
public class ObservableRefreshView extends FrameLayout {

    private View mFooterView = null;
    private ImageView mRotationImage = null;
    private TextView mRefreshText = null;
    private ObjectAnimator mAnim = null;
    //是header还是footer
    private boolean isFooter = true;

    public ObservableRefreshView(Context context) {
        this(context, null);
    }

    public ObservableRefreshView(Context context, boolean isFooter) {
        this(context, null);
        this.isFooter = isFooter;
    }

    public ObservableRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ObservableRefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        handleAttributes(context, attrs);
        init(context);
    }

    private void handleAttributes(Context context, AttributeSet attrs){
        if(attrs != null){
            // Styleables from XML
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ObservableRefreshView);
            if(a.hasValue(R.styleable.ObservableRefreshView_customRefreshStyle)){
                String value = a.getString(R.styleable.ObservableRefreshView_customRefreshStyle);
                if(value != null && "header".equals(value)){
                    isFooter = false;
                }
            }
            a.recycle();
        }
    }

    private void init(Context context){
        mFooterView = LayoutInflater.from(this.getContext()).inflate(R.layout.observablelistview_footer_view, null, false);
        mRotationImage = (ImageView) mFooterView.findViewById(R.id.pull_to_refresh_image);
        mRefreshText = (TextView) mFooterView.findViewById(R.id.pull_to_refresh_text);
        LayoutParams mParams = null;
        if(isFooter){
            mParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            mParams.gravity = Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM;
            mRefreshText.setText(R.string.triangel_pull_to_refresh_from_bottom_pull_label);
        }else{
            mParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            mParams.gravity = Gravity.CENTER_HORIZONTAL|Gravity.TOP;
            mRefreshText.setText(R.string.triangel_pull_to_refresh_pull_label);
        }
        this.addView(mFooterView, mParams);
    }

    public void startRotationAnim(){
        if(mAnim == null)
            mAnim = ObjectAnimator.ofFloat(mRotationImage, "rotation", 0f, 360f);
        mAnim.setInterpolator(new LinearInterpolator());
        mAnim.setDuration(1000);
        mAnim.setRepeatCount(-1);
        mAnim.setRepeatMode(ObjectAnimator.RESTART);
        mAnim.start();
    }

    public void stopRotationAnim(){
        if(mAnim != null)
            mAnim.cancel();
    }

    public void setHasNoMoreText(){
        mRotationImage.setVisibility(View.GONE);
        mRefreshText.setText(R.string.pull_to_refresh_no_more);
    }

    public void setPullToRefreshText(){
        mRotationImage.setVisibility(View.GONE);
        if(isFooter){
            mRefreshText.setText(R.string.triangel_pull_to_refresh_from_bottom_pull_label);
        }else{
            mRefreshText.setText(R.string.triangel_pull_to_refresh_pull_label);
        }
    }

    public void setReleaseToRefreshText(){
        mRotationImage.setVisibility(View.GONE);
        mRefreshText.setText(R.string.triangel_pull_to_refresh_release_label);
    }

    public void setRefreshingText(){
        mRotationImage.setVisibility(View.VISIBLE);
        mRefreshText.setText(R.string.triangel_pull_to_refresh_from_bottom_refreshing_label);
    }

    public final void reset() {
        if (null != mRefreshText) {
            mRefreshText.setText(R.string.pull_to_refresh_has_finish);
        }
        mRotationImage.setVisibility(View.VISIBLE);
        this.stopRotationAnim();
    }

    public void setmFooterViewText(String reason){
        mRotationImage.setVisibility(View.GONE);
        mRefreshText.setText(reason);
    }
}
