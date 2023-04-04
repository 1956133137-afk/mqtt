package com.yannuo.dgcanteen.util;

import android.text.format.Time;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {

    public static int CurrentTimeSection(){
        int result = 0;
        if (isCurrentInTimeScope(7,30,11,30)){
            result = 1;
        }else if (isCurrentInTimeScope(11,30,18,30)){
            result = 2;
        } else if (isCurrentInTimeScope(18, 30, 19, 30)) {
            result = 3;
        }else {
            result = 0;
        }
        return result;
    }

//    判断时间区间
    public static boolean isCurrentInTimeScope(int beginHour,int beginMin,int endHour,int endMin){
        boolean result = false;
        final long aDayInMillis = 1000 * 60 * 60 * 24;
        final long currentTimeMillis = System.currentTimeMillis();

        Time now = new Time();
        now.set(currentTimeMillis);

        Time startTime = new Time();
        startTime.set(currentTimeMillis);
        startTime.hour = beginHour;
        startTime.minute = beginMin;

        Time endTime = new Time();
        endTime.set(currentTimeMillis);
        endTime.hour = endHour;
        endTime.minute = endMin;

        if (!startTime.before(endTime)) {
            // 跨天的特殊情况
            startTime.set(startTime.toMillis(true) - aDayInMillis);
            result = !now.before(startTime) && !now.after(endTime); // startTime <= now <= endTime
            Time startTimeInThisDay = new Time();
            startTimeInThisDay.set(startTime.toMillis(true) + aDayInMillis);
            if (!now.before(startTimeInThisDay)) {
                result = true;
            }
        } else {
            // 普通情况
            result = !now.before(startTime) && !now.after(endTime); // startTime <= now <= endTime
        }
        return result;
    }

}
