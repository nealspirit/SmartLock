package com.smartlock.android.com.smartlock.android.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public  class TimeUtil {

    static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");

    public static String getCurrentTime(Date date) {
        return simpleDateFormat.format(date);
    }

    public static int getCurrentHour(Date date){
        String hour = simpleDateFormat.format(date).split(":")[0];
        return Integer.parseInt(hour);
    }

    public static int getCurrentMin(Date date){
        String min = simpleDateFormat.format(date).split(":")[1];
        return Integer.parseInt(min);
    }
}
