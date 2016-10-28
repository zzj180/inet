package cn.lzl.soundeffect.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import cn.lzl.soundeffect.R;

/**
 * Created by lzl on 2015/7/14.
 */
public class VolumnSelectedView extends View {

    private Bitmap mImg;
    private Paint mPaint;
    private Rect mRect;
    private int mRectHeight;
    private int mRectWidth;
    private final int mCount = 15;
    private int mCurrentCount = 0;
    private int divider = 2;

    public VolumnSelectedView(Context context) {
        this(context, null);
    }

    public VolumnSelectedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VolumnSelectedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context,attrs,defStyleAttr);
    }

    private void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.volumnSelectedView,defStyleAttr,0);
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++){
            int attr = a.getIndex(i);
            switch (attr){
                case R.styleable.volumnSelectedView_volumnBg:
                    mImg = BitmapFactory.decodeResource(getResources(),a.getResourceId(attr,0));
                    break;
                case R.styleable.volumnSelectedView_rectHeight:
                    mRectHeight = a.getDimensionPixelSize(attr,(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            9, getResources().getDisplayMetrics()));
                    break;
                case R.styleable.volumnSelectedView_rectWidth:
                    mRectWidth = a.getDimensionPixelSize(attr,(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            20, getResources().getDisplayMetrics()));
                    break;
            }
        }

        a.recycle();
        mPaint = new Paint();
        mRect = new Rect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth((float) 2.0);
        mPaint.setStyle(Paint.Style.FILL);
        mRect.left = getPaddingLeft();
        mRect.top = getPaddingTop();
        mRect.bottom = getHeight()- getPaddingBottom();
        mRect.right = getWidth() - getPaddingRight();
        canvas.drawBitmap(mImg, null, mRect, mPaint);
        divider = (getHeight() - mRectHeight * mCount) / (mCount + 1);
        mRect.left = getWidth() / 2 - mRectWidth / 2;
        mRect.right = getWidth() / 2 + mRectWidth / 2;
        mRect.top = 0;
        mRect.bottom = 4;
        for (int i = 0; i < mCount; i++){
            if(mCurrentCount == i){
                mPaint.setColor(Color.YELLOW);
            }else{
                mPaint.setColor(Color.WHITE);
            }
            mRect.top = divider + mRect.bottom;
            mRect.bottom = mRect.top + mRectHeight;
            canvas.drawRect(mRect,mPaint);
        }
    }
    
    public void setVolumnValue(int value){
        if(Math.abs(value) > 7)return;
        if(value >= 0){
            mCurrentCount = 7 - value;
        }else{
            mCurrentCount = 7 + Math.abs(value);
        }
        invalidate();
    }
    
    public void setCurrentCount(int index){
        if(index >= mCount || index < 0){
            return;
        }
        mCurrentCount = index;
        invalidate();
    }
    
    public void setIndex(int index){
        if(index >= mCount || index < 0){
            return;
        }
        mCurrentCount = (mCount - 1) - index;
        invalidate();
    }
    
    public int getCurrentCount(){
        return mCurrentCount;
    }
}
