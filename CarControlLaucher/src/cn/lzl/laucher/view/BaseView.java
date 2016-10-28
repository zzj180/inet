package cn.lzl.laucher.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public abstract class BaseView extends View {

    protected Context mContext;

    public BaseView(Context context) {
        this(context,null);
    }
    public BaseView(Context context, AttributeSet attrs) {
        this(context,null,0);
    }
    
    public BaseView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        initialize(attrs,defStyleAttr);
    }

    protected abstract void initialize(AttributeSet attrs, int defStyleAttr);

}
