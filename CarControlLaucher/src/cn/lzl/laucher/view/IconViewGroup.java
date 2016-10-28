package cn.lzl.laucher.view;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;


public class IconViewGroup extends FrameLayout {


    protected Activity mActivity;

    protected Context mContext;

    protected View mView;

    protected LayoutInflater mInflater;

    protected ScaleAnimation scaleAnimation;

    public IconViewGroup(Context context) {
        this(context,null);
    }
    
    public IconViewGroup(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }
    
    public IconViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        scaleAnimation = new ScaleAnimation(1.0f, 1.1f,
                1.0f, 1.1f, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnimation.setDuration(500);
    }

    public void setContentView(int layoutId) {
        mContext = getContext();
        mActivity = (Activity) mContext;
        mInflater = LayoutInflater.from(mContext);
        mView = mInflater.inflate(layoutId, null);
        addView(mView);
    }

    public View fid(int id) {
        return findViewById(id);
    }

}
