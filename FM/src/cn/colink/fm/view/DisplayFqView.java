package cn.colink.fm.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;

import cn.colink.fm.R;

public class DisplayFqView extends BaseView {
    
    public static final String TAG = "DisplayFqView";
    
    private ImageView mFqChannelImg;
    private ImageView mFqUnitImg;
    private TextView mDisplayFqTv;

    public DisplayFqView(Context context) {
        super(context);
        setUpViews();
    }

    public DisplayFqView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUpViews();
    }
    
    private void setUpViews() {
        setContentView(R.layout.display_fq);
        mFqChannelImg = (ImageView)findViewById(R.id.mFqChannelImg);
        mFqUnitImg = (ImageView)findViewById(R.id.mFqUnitImg);
        mDisplayFqTv = (TextView)findViewById(R.id.mDisplayFqTv);
    }
    
    private DecimalFormat df2  = new DecimalFormat("###.00");
    public void showFq(float fq,boolean isMin){
        mDisplayFqTv.setText(String.valueOf(df2.format(fq)));
    }
    
    public void showFq(int fq){
        mDisplayFqTv.setText(String.valueOf(fq));
    }

    public void changeBand(int i) {
        mFqChannelImg.setImageDrawable(getResources().getDrawable(i));
    }

    public void changeUnit(int unit) {
        mFqUnitImg.setImageDrawable(getResources().getDrawable(unit));
    }
    
    public void startAnimator(){
    }
    
    public void stopAnimator(){
    }
    
}
