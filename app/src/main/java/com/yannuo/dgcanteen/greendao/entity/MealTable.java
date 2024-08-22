package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;

import java.util.Date;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class MealTable {
    @Id(autoincrement = true)
    private Long id;
    @NotNull
    private int mealId;  //餐别ID
    @NotNull
    private String mealName;  //餐别名称：早餐/午餐/晚餐/下午茶/夜宵等
    @NotNull
//    private String windowId;  //档口ID
    private Date startTime;  //餐别每日开始时间
    private Date endTime;  //餐别每日结束时间

    
    public MealTable(Long id, int mealId, @NotNull String mealName,
            @NotNull String windowId, Date startTime, Date endTime) {
        this.id = id;
        this.mealId = mealId;
        this.mealName = mealName;
//        this.windowId = windowId;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    @Generated(hash = 1028783363)
    public MealTable() {
    }
    @Generated(hash = 284262457)
    public MealTable(Long id, int mealId, @NotNull String mealName,
            @NotNull Date startTime, Date endTime) {
        this.id = id;
        this.mealId = mealId;
        this.mealName = mealName;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public int getMealId() {
        return this.mealId;
    }
    public void setMealId(int mealId) {
        this.mealId = mealId;
    }
    public String getMealName() {
        return this.mealName;
    }
    public void setMealName(String mealName) {
        this.mealName = mealName;
    }
//    public String getWindowId() {
//        return this.windowId;
//    }
//    public void setWindowId(String windowId) {
//        this.windowId = windowId;
//    }
    public Date getStartTime() {
        return this.startTime;
    }
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
    public Date getEndTime() {
        return this.endTime;
    }
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}
