package cn.lzl.laucher.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import cn.lzl.laucher.R;

public class AppIconView extends IconViewGroup {


    private ImageView mFunIcon;
    private TextView mFunTv;
    private LinearLayout mFunIconll;
    private int mId;
    
    public AppIconView(Context context) {
        this(context,null);
    }
    
    public AppIconView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public AppIconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setUpViews();
    }

    private void setUpViews() {
        setContentView(R.layout.activity_main_app_icon);
        mFunIcon = (ImageView)findViewById(R.id.mFunIcon);
        mFunTv = (TextView)findViewById(R.id.mFunTv);
        mFunIconll = (LinearLayout)findViewById(R.id.mFunIconll);
        
//        mFunIcon.setOnClickListener(new OnClickListener() {
//            
//            @Override
//            public void onClick(View v) {
//                if(mClickListener != null)
//                    mClickListener.click(mId);
//            }
//        });
        
        mFunIconll.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if(mClickListener != null)
                    mClickListener.click(mId);
            }
        });
    }
    
    public void setAppIcon(Drawable drawable){
        mFunIcon.setImageDrawable(drawable);
    }
    
    public void setAppIcon(Bitmap bitmap){
        mFunIcon.setImageBitmap(bitmap);
    }
    
    public void setAppTv(String text){
        mFunTv.setText(text);
    }
    
    public void setId(int id){
        this.mId = id;
    }
    
    public void setImgClickListener(ImgClickListener listener){
        this.mClickListener = listener;
    }
    public ImgClickListener mClickListener;
    public interface ImgClickListener{
        public void click(int id);
    }
}
