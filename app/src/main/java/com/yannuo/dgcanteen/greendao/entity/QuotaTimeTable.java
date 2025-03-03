package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class QuotaTimeTable {
    @Id(autoincrement = true)
    private Long id;

    private String startTime;

    private String endTime;

    private String quotaAmount;

    @Generated(hash = 1671565689)
    public QuotaTimeTable(Long id, String startTime, String endTime,
            String quotaAmount) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.quotaAmount = quotaAmount;
    }

    @Generated(hash = 51586196)
    public QuotaTimeTable() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStartTime() {
        return this.startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return this.endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getQuotaAmount() {
        return this.quotaAmount;
    }

    public void setQuotaAmount(String quotaAmount) {
        this.quotaAmount = quotaAmount;
    }
}
