package com.smartlock.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import com.smartlock.android.com.smartlock.android.util.AnimUtil;

public class LockInfoView extends CardView {
    private boolean CARDVIEW_MODE_FLAG = false;
    private float lastY;

    public LockInfoView(@NonNull Context context) {
        super(context);
    }

    public LockInfoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LockInfoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        TranslateAnimation translateAnimation;
        float Y = event.getY();

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                if (Y > lastY){
                    CARDVIEW_MODE_FLAG = true;
                }else {
                    CARDVIEW_MODE_FLAG = false;
                }

                break;
            case MotionEvent.ACTION_UP:
                if (CARDVIEW_MODE_FLAG){
                    Animation animation = AnimUtil.getAnimation(getContext(),AnimUtil.SLIDE_OUT_BOTTOM);
                    this.startAnimation(animation);
                    this.setVisibility(GONE);

                    if (MainActivity.overlay != null) {
                        MainActivity.overlay.removeFromMap();
                    }
                }
                break;
            default:
                break;
        }
        lastY = Y;

        return true;
    }
}
