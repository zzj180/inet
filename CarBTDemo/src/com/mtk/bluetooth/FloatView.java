package com.mtk.bluetooth;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.mtk.bluetooth.util.Utils;
import android.util.Log;
import android.graphics.Rect;

/**
 * 
 * @author zzj
 */
public class FloatView extends ImageView {

    private Context c;
    private float mTouchX;
    private float mTouchY;
    private float x;
    private float y;
    private int startX;
    private int startY;
    private int imgId = R.drawable.answer;
    private int controlledSpace = 48;
    private int screenWidth;
    private int screenHeight;
    boolean isShow = false;
    private OnClickListener mClickListener;

    private WindowManager windowManager;

    private WindowManager.LayoutParams windowManagerParams = new WindowManager.LayoutParams();

    public FloatView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatView(Context c) {
        super(c);
        initView(c);
    }

    // 初始化窗体
    public void initView(Context c) {
        this.c = c;
        windowManager = (WindowManager) c.getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        this.setImageResource(imgId);
        windowManagerParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        windowManagerParams.format = PixelFormat.RGBA_8888; // 背景透明
        windowManagerParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        // 调整悬浮窗口至左上角，便于调整坐标
        windowManagerParams.gravity = Gravity.LEFT | Gravity.TOP;
        // 以屏幕左上角为原点，设置x、y初始值
        windowManagerParams.x = 0;
        windowManagerParams.y = screenHeight >> 1;
        // 设置悬浮窗口长宽数据
        windowManagerParams.width = 96;
        windowManagerParams.height = 96;

    }

    public void setImgResource(int id) {
        imgId = id;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
      
            Rect frame = new Rect();  
             getWindowVisibleDisplayFrame(frame);  
             int statusBarWidth= frame.left;
            x = event.getRawX() - statusBarWidth;
            y = event.getRawY();
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN: {         
            mTouchX = event.getX();
            mTouchY = event.getY();
            startX = (int) event.getRawX()- statusBarWidth;
            startY = (int) event.getRawY();
            break;

        }
        case MotionEvent.ACTION_MOVE: {
            updateViewPosition();
            break;
        }
        case MotionEvent.ACTION_UP: {

            if (Math.abs(x - startX) < controlledSpace
                    && Math.abs(y - startY) < controlledSpace) {
                if (mClickListener != null) {
                    mClickListener.onClick(this);
                }
            }

            if (x <= screenWidth / 2) {
                x = 0;
            } else {
                x = screenWidth;
            }

            updateViewPosition();
     //       saveViewPostion();
            break;
        }
        }

        return true;
    }

    // 隐藏该窗体
    public void hide() {
        if (isShow) {
            windowManager.removeView(this);
            isShow = false;
        }
    }

    // 显示该窗体
    public void show() {
        if (isShow == false) {
     //       getViewPostion();
            windowManager.addView(this, windowManagerParams);
            isShow = true;
        }
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        this.mClickListener = l;
    }

    private void updateViewPosition() {
        // 更新浮动窗口位置参数
        windowManagerParams.x = (int) (x - mTouchX);
        windowManagerParams.y = (int) (y - mTouchY);
        windowManager.updateViewLayout(this, windowManagerParams); // 刷新显示
    }


    private void saveViewPostion() {
       Utils.setInputViewX(c, windowManagerParams.x);
        Utils.setInputViewY(c, windowManagerParams.y);
    }

    private void getViewPostion() {
        int savedX = Utils.getInputViewX(c, 0);
        int savedY = Utils.getInputViewY(c, screenHeight >> 1);
        windowManagerParams.x = savedX;
        windowManagerParams.y = savedY;
    }
}