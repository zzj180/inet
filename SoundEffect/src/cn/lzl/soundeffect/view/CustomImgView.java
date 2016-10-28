package cn.lzl.soundeffect.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import cn.lzl.soundeffect.R;
import cn.lzl.soundeffect.utils.Trace;

/**
 * Created by lzl on 2015/7/9.
 */
@SuppressLint("DrawAllocation")
public class CustomImgView extends View{

    private String iconDescText;
    private int iconDescTextSize;
    private int iconDescTextColor;
    private Bitmap img;
    private int imgScaleType;
    private Rect rect;
    private Paint mPaint;
    private Rect mTextBound;
    private int mWidth;
    private int mHeight;
    private int mIndex = 7;
    private final int MAX_INDEX = 14;
    private final int MIN_INDEX = 0;
    private final int MIDDLE_INDEX = 7;

    public CustomImgView(Context context) {
        this(context, null);
    }

    public CustomImgView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomImgView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews(context,attrs,defStyleAttr);
    }

    private void initViews(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.CustomImgView,defStyleAttr,0);
        int at = a.getIndexCount();
        for (int i = 0; i < at; i++){
            int attr = a.getIndex(i);
            switch (attr){
                case R.styleable.CustomImgView_image:
                    img = BitmapFactory.decodeResource(getResources(),a.getResourceId(attr,R.drawable.ic_launcher));
                    break;
                case R.styleable.CustomImgView_imageScaleType:
                    imgScaleType = a.getInt(attr,0);
                    break;
                case R.styleable.CustomImgView_iconDescText:
                    iconDescText = "+7";
                    break;
                case R.styleable.CustomImgView_iconDescTextSize:
                    iconDescTextSize = a.getDimensionPixelSize(attr,(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                            16,getResources().getDisplayMetrics()));
                    break;
                case R.styleable.CustomImgView_iconDescTextColor:
                    iconDescTextColor = a.getColor(attr, Color.BLACK);
                    break;
            }
        }

        a.recycle();

        rect = new Rect();
        mPaint = new Paint();
        mTextBound = new Rect();

        mPaint.setTextSize(iconDescTextSize);
        mPaint.getTextBounds(iconDescText,0,iconDescText.length(),mTextBound);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specMode = MeasureSpec.getMode(widthMeasureSpec);
        int specSize = MeasureSpec.getSize(widthMeasureSpec);

        if (specMode == MeasureSpec.EXACTLY)// match_parent , accurate
        {
            mWidth = specSize;
        } else
        {
            // 由图片决定的宽
            int desireByImg = getPaddingLeft() + getPaddingRight() + img.getWidth();
            // 由字体决定的宽
            int desireByTitle = getPaddingLeft() + getPaddingRight() + mTextBound.width();

            if (specMode == MeasureSpec.AT_MOST)// wrap_content
            {
                int desire = Math.max(desireByImg, desireByTitle);
                mWidth = Math.min(desire, specSize);
            }
        }

        specMode = MeasureSpec.getMode(heightMeasureSpec);
        specSize = MeasureSpec.getSize(heightMeasureSpec);

        if(specMode == MeasureSpec.EXACTLY){
            mHeight = specSize;
        }else{
            int desire = getPaddingTop()+getPaddingBottom()+img.getHeight()+mTextBound.height();
            if(specMode == MeasureSpec.AT_MOST){
                mHeight = Math.min(desire, specSize);
            }
        }

        setMeasuredDimension(mWidth,mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        rect.left = getPaddingLeft();
        rect.right = mWidth - getPaddingRight();
        rect.top = getPaddingTop();
        rect.bottom = mHeight - getPaddingBottom();

        mPaint.setColor(iconDescTextColor);
        mPaint.setStyle(Paint.Style.FILL);

        if(mTextBound.width() > mWidth){
            Trace.i("rect.top : " + rect.top);
            TextPaint paint = new TextPaint(mPaint);
            String msg = TextUtils.ellipsize(iconDescText,paint,(float)mWidth - getPaddingLeft()-getPaddingRight(),
                    TextUtils.TruncateAt.END).toString();
            canvas.drawText(msg, getPaddingLeft(),mTextBound.height() + 5, mPaint);
        }
        else
        {
            mPaint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(iconDescText,mWidth / 2 - mTextBound.width() / 2,mTextBound.height() + 5,mPaint);
        }

        //取消使用掉的快
        rect.top += mTextBound.height();

        if (imgScaleType == 0)
        {
            canvas.drawBitmap(img, null, rect, mPaint);
        } else
        {
            //计算居中的矩形范围
            rect.left = mWidth / 2 - img.getWidth() / 2;
            rect.right = mWidth / 2 + img.getWidth() / 2;
            rect.top = (mHeight - mTextBound.height()) / 2 - img.getHeight() / 2 + 10;
            rect.bottom = (mHeight - mTextBound.height()) / 2 + img.getHeight() / 2;
            canvas.drawBitmap(img, null, rect, mPaint);
        }
    }
    
    public void addIndex(){
        if(mIndex >= 7)return;
        mIndex += 1;
        onChangeText();
        invalidate();
    }
    
    public void subIndex(){
        if(mIndex <= -7)return;
        mIndex -= 1;
        onChangeText();
        invalidate();
    }
    
    public void setIndex(int index){
        if(index < MIN_INDEX || index > MAX_INDEX){
            return;
        }
        mIndex = index - MIDDLE_INDEX;
        onChangeText();
        invalidate();
    }
    
    
    public void onChangeText(){
        if(mIndex > 0){
            iconDescText = "+" + mIndex;
        }else if(mIndex < 0){
            iconDescText = "-" + Math.abs(mIndex);
        }else{
            iconDescText = "00";
        }
    }
    
    public int getIndex(){
        return mIndex;
    }
    
}
