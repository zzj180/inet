package cn.lzl.soundeffect.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

import cn.lzl.soundeffect.R;

/**
 * Created by lzl on 2015/7/15.
 */
public class CarSelectedView extends View{

    private Bitmap mImg;
    private int mColumnIndex = 7;
    private int mRowIndex = 7;
    private Paint mPaint;
    private int headerDistance = 17;
    private int divider = 11;
    private float columnDivider = 11.45f;
    private int columnDistance = 13;
    private Rect mRect;
    private LinePosition rowPosition;
    private LinePosition colunmPosion;
    private final int MAX_INDEX = 14;
    private final int MIN_INDEX = 0;

    public CarSelectedView(Context context) {
        this(context, null);
    }

    public CarSelectedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarSelectedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context,attrs,defStyleAttr);
    }

    private void initView(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.carSelectedView,defStyleAttr,0);
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++){
            int attr = a.getIndex(i);
            switch (attr){
                case R.styleable.carSelectedView_carBg:
                    mImg = BitmapFactory.decodeResource(getResources(),a.getResourceId(attr,0));
                    break;
            }
        }

        a.recycle();
        mPaint = new Paint();
        mRect = new Rect();
        rowPosition = new LinePosition();
        colunmPosion = new LinePosition();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        setRowPostion(mRowIndex);
        setColumnPostion(mColumnIndex);
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.YELLOW);
        mPaint.setStrokeWidth((float) 3.0);
        mPaint.setStyle(Paint.Style.FILL);
        mRect.left = getPaddingLeft();
        mRect.top = getPaddingTop();
        mRect.bottom = getHeight()- getPaddingBottom();
        mRect.right = getWidth() - getPaddingRight();
        canvas.drawBitmap(mImg, null, mRect, mPaint);
        
        canvas.drawLine(rowPosition.startX,rowPosition.startY,rowPosition.stopX,rowPosition.stopY,mPaint);
        canvas.drawLine(colunmPosion.startX,colunmPosion.startY,colunmPosion.stopX,colunmPosion.stopY,mPaint);
    }

    private void setRowPostion(int index){
        rowPosition.startX = divider;
        rowPosition.stopX = getWidth() - divider;
        rowPosition.startY = headerDistance + index * divider;
        rowPosition.stopY = headerDistance + index * divider;
    }

    private void setColumnPostion(int index){
        colunmPosion.startX = columnDistance + index * columnDivider;
        colunmPosion.stopX = columnDistance + index * columnDivider;
        colunmPosion.startY = columnDistance;
        colunmPosion.stopY = getHeight() - columnDistance;
    }

    public int getColumnIndex() {
        return mColumnIndex;
    }

    public void setColumnIndex(int columnIndex) {
        if(columnIndex > MAX_INDEX || columnIndex < MIN_INDEX){
            return;
        }
        this.mColumnIndex = columnIndex;
        invalidate();
    }

    public int getRowIndex() {
        return mRowIndex;
    }

    public void setRowIndex(int rowIndex) {
        if(rowIndex > MAX_INDEX || rowIndex < MIN_INDEX ){
            return;
        }
        this.mRowIndex = MAX_INDEX - rowIndex;
        invalidate();
    }

    class LinePosition{
        float startX;
        float startY;
        float stopX;
        float stopY;
    }
}
