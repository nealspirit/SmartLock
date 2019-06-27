package com.smartlock.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.Animation;

import com.smartlock.android.com.smartlock.android.util.AnimUtil;

public class LockInfoView extends CardView {
    private int mLastYIntercept;

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
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercepted = false;
        int y = (int) ev.getY();

        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                intercepted = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (y < mLastYIntercept){
                    intercepted = true;
                }else {
                    intercepted = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                intercepted = false;
                break;
            default:
                break;
        }
        mLastYIntercept = y;

        return intercepted;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_UP:
                Animation animation = AnimUtil.getAnimation(getContext(),AnimUtil.SLIDE_OUT_BOTTOM);
                this.startAnimation(animation);
                this.setVisibility(GONE);

                if (MainActivity.overlay != null) {
                    MainActivity.overlay.removeFromMap();
                }
                break;
        }

        return true;
    }
}
