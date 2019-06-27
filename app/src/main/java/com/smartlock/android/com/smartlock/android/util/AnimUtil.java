package com.smartlock.android.com.smartlock.android.util;

import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.smartlock.android.R;

public class AnimUtil {
    public static final int SLIDE_IN_BOTTOM = 0;
    public static final int SLIDE_OUT_BOTTOM = 1;

    public static Animation getAnimation(Context context,int animationID){

        switch (animationID){
            case SLIDE_IN_BOTTOM:
                return AnimationUtils.loadAnimation(context, R.anim.lockinfoview_slide_in_bottom);
            case SLIDE_OUT_BOTTOM:
                return AnimationUtils.loadAnimation(context, R.anim.lockinfoview_slide_out_bottom);
            default:
                break;
        }

        return null;
    }
}
