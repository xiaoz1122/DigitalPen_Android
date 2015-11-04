package com.smart.pen.core.utils;

/**
 * Created by Xiaoz on 15/9/28.
 */
public class FFMergePictureUtils {

    public native void setVideoRate(int rate);
    public native float getTimeDifference();
    public native int start(String out_file,int width,int height);
    public native int end();
    public native int appendImage(byte[] picData,int length);
    public native int appendAudio(byte[] audioData,int length);
}