package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Unique;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class UserFaceData {
    @Id
    private Long id;
    @Unique()
    private String custId;//智慧食堂用户唯一标识（建行平台）
    @Unique()
    private String userId;//用户iD
    private String campusId;//园区iD
    @Unique()
    private String eigenvalue;// 人脸特征值
    private String way;// 人脸录入方式：1、后台导入，2、h5录入，3、终端设备录入
    private String faceImgUrl; //本地脸库人脸图片路径
    private String version;// 版本号
    private String createTime;// 初次录入人脸时间
    private String updateTime;// 数据更新时间（提取时间）
    @Generated(hash = 1158237083)
    public UserFaceData(Long id, String custId, String userId, String campusId,
            String eigenvalue, String way, String faceImgUrl, String version,
            String createTime, String updateTime) {
        this.id = id;
        this.custId = custId;
        this.userId = userId;
        this.campusId = campusId;
        this.eigenvalue = eigenvalue;
        this.way = way;
        this.faceImgUrl = faceImgUrl;
        this.version = version;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }
    @Generated(hash = 1295092164)
    public UserFaceData() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getCustId() {
        return this.custId;
    }
    public void setCustId(String custId) {
        this.custId = custId;
    }
    public String getUserId() {
        return this.userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getCampusId() {
        return this.campusId;
    }
    public void setCampusId(String campusId) {
        this.campusId = campusId;
    }
    public String getEigenvalue() {
        return this.eigenvalue;
    }
    public void setEigenvalue(String eigenvalue) {
        this.eigenvalue = eigenvalue;
    }
    public String getWay() {
        return this.way;
    }
    public void setWay(String way) {
        this.way = way;
    }
    public String getFaceImgUrl() {
        return this.faceImgUrl;
    }
    public void setFaceImgUrl(String faceImgUrl) {
        this.faceImgUrl = faceImgUrl;
    }
    public String getVersion() {
        return this.version;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    public String getCreateTime() {
        return this.createTime;
    }
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
    public String getUpdateTime() {
        return this.updateTime;
    }
    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}
