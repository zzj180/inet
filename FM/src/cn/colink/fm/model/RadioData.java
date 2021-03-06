package cn.colink.fm.model;

import java.util.Arrays;

public class RadioData {
    public static final int PACKET_SIZE = 6;
    public int  curFreq;
    public int  curBand;
    public int  curFavDown;
    public int[] FF = new int[PACKET_SIZE];
    
    public void clearData(){
        curFreq = 0;
        curBand = 0;
        curFavDown = 0;
        Arrays.fill(FF, 0);
    }
}
