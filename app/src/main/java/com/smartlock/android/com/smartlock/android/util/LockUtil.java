package com.smartlock.android.com.smartlock.android.util;

import com.smartlock.android.domain.LockInfo;

import java.util.List;

public class LockUtil {

    public static LockInfo findLockWithlatitude(List<LockInfo> lockList,double latitude, double longitude){
        for(LockInfo lock : lockList){
            if (lock.getLatitude() == latitude){
                if (lock.getLongitude() == longitude){
                    return lock;
                }
            }
        }

        return null;
    }
}
