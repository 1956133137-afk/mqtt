package com.yannuo.dgcanteen.util;

import android.text.format.Time;

import com.yannuo.dgcanteen.greendao.entity.MealTable;
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TimeUtil {

    public static List<MealTable> mealTables = null;

    public static int CurrentTimeSection() {
        int result = 0;
        mealTables = DishesDBHelper.getInstance().queryAllMeals();
        if (mealTables != null && mealTables.size() > 0) {
            for (MealTable u : mealTables) {
                if (isCurrentInTimeScope(u)) {
                    result = u.getMealId();
                    break;
                }
            }
        }
        return result;
    }

    //    判断时间区间
    private static boolean isCurrentInTimeScope(MealTable data) {
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
    public static long timestamp(long time) {
        long diff = time - System.currentTimeMillis();
        long days = diff / (1000 * 60 * 60 * 24);
        long hours = (diff - days * (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (diff - days * (1000 * 60 * 60 * 24) - hours * (1000 * 60 * 60)) / (1000 * 60);
        return days * 24 * 60 + hours * 60 + minutes;
    }

    //时间格式化
    public static String timeFormat(String format, long currentTime) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.SIMPLIFIED_CHINESE);
        return sdf.format(currentTime);
    }

    public static String dateFormat(String time) {
        StringBuilder timeStr = new StringBuilder();
        timeStr.append(time.substring(0, 4)).append("-")
                .append(time.substring(4, 6)).append("-")
                .append(time.substring(6, 8)).append(" ")
                .append(time.substring(8, 10)).append(":")
                .append(time.substring(10, 12)).append(":")
                .append(time.substring(12));
        return new String(timeStr);
    }

    public static String formatDate(String time) {
        StringBuilder timeStr = new StringBuilder();
        timeStr.append(time.substring(0, 4)).append("-")
                .append(time.substring(4, 6)).append("-")
                .append(time.substring(6, 8));
        return new String(timeStr);
    }
}
