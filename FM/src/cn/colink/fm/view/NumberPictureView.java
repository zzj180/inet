package cn.colink.fm.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import cn.colink.fm.R;
import cn.colink.fm.utils.Trace;

public class NumberPictureView extends LinearLayout {
    
    final Context mContext;
    
    final int[] drawableId = new int[]{
            R.drawable.num_0,
            R.drawable.num_1,
            R.drawable.num_2,
            R.drawable.num_3,
            R.drawable.num_4,
            R.drawable.num_5,
            R.drawable.num_6,
            R.drawable.num_7,
            R.drawable.num_8,
            R.drawable.num_9
    };
    
    final int[] minDrawableId = new int[]{
            R.drawable.min_num0,
            R.drawable.min_num1,
            R.drawable.min_num2,
            R.drawable.min_num3,
            R.drawable.min_num4,
            R.drawable.min_num5,
            R.drawable.min_num6,
            R.drawable.min_num7,
            R.drawable.min_num8,
            R.drawable.min_num9
    };
    
    public NumberPictureView(Context context) {
        super(context);
        this.mContext = context;
    }
    
    public NumberPictureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }
    
    
    public void setFqDisplay(float fq,boolean isMin){
        char c[] = String.valueOf(fq).toCharArray();
        int id[];
        int dotId;
        if(isMin){
            id = minDrawableId;
            dotId = R.drawable.min_numdot;
        }else{
            id = drawableId;
            dotId = R.drawable.dot;
        }
        for (char d : c) {
            if(Character.isDigit(d)){
                addView(createView(id[Integer.parseInt(String.valueOf(d))]), createLayoutParams(isMin));
            }else {
                addView(createView(dotId), createLayoutParams(isMin));
            }
        }
    }
    
    private ImageView createView(int drawableId){
        ImageView numImg = new ImageView(mContext);
        numImg.setImageDrawable(getResources().getDrawable(drawableId));
        return numImg;
    }
    
    private LinearLayout.LayoutParams createLayoutParams(boolean isMin){
        int width;
        int height;
        if(isMin){
             width = (int) getResources().getDimension(R.dimen.minNumWidth);
            height = (int)getResources().getDimension(R.dimen.minNumHeight);
        }else{
            width = (int) getResources().getDimension(R.dimen.numWidth);
            height = (int)getResources().getDimension(R.dimen.numHeight);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
        return lp;
    }
}
