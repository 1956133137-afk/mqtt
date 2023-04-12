package com.yannuo.dgcanteen.util;

import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;

import com.yannuo.dgcanteen.dao.MealTable;
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TimeUtil {

    private static List<MealTable> mealTables = null;
    public static int CurrentTimeSection(){
        int result = 0;
        if (mealTables != null){
            for (MealTable u : mealTables){
                if (isCurrentInTimeScope(u)){
                    result = u.getMealId();
                    break;
                }
            }
        }else {
            mealTables = DishesDBHelper.getInstance().queryAllMeals();
        }
        return result;
    }

//    判断时间区间
    private static boolean isCurrentInTimeScope(MealTable data){
        boolean result = false;
        final long aDayInMillis = 1000 * 60 * 60 * 24;
        final long currentTimeMillis = System.currentTimeMillis();

        Time now = new Time();
        now.set(currentTimeMillis);

        Time startTime = new Time();
        startTime.set(currentTimeMillis);
        startTime.hour = data.getStartTime().getHours();
        startTime.minute = data.getStartTime().getMinutes();

        Time endTime = new Time();
        endTime.set(currentTimeMillis);
        endTime.hour = data.getEndTime().getHours();
        endTime.minute = data.getEndTime().getMinutes();

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

    //判断时间戳间隔
    public static long timestamp(long time){
        long diff = time - System.currentTimeMillis();
        long days = diff / (1000 * 60 * 60 * 24);
        long hours = (diff - days * (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (diff - days * (1000 * 60 * 60 * 24) - hours * (1000 * 60 * 60)) / (1000 * 60);
        return days * 24 + hours;
    }

    //转时间戳
    public static String DateToTimestamp() throws ParseException {
        Calendar calendar = Calendar.getInstance();
        String year = String.valueOf(calendar.get(Calendar.YEAR));
        Date time = new SimpleDateFormat("yyyyMMddHHmmss").parse(year + "0101000000");
        String result = DateFormat.format("yyyyMMdd",System.currentTimeMillis()).toString() +
                String.format("%11s",(System.currentTimeMillis() - time.getTime())).replace(" ","0");
        return result;
    }


}
