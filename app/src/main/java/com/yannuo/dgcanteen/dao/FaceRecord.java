package com.yannuo.dgcanteen.dao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Unique;
import org.greenrobot.greendao.annotation.Generated;

/**
 * 暂存等待入库的人员
 */

@Entity
public class FaceRecord {
    @Id(autoincrement = true)
    private Long id;
    @Unique()
    private String custId;//智慧食堂用户唯一标识（建行平台）
    @NotNull
    private String image;// 人脸图片路径
    private Boolean tryd = false ; //人脸照片下载失败跳过下载
    @Generated(hash = 1018409517)
    public FaceRecord(Long id, String custId, @NotNull String image, Boolean tryd) {
        this.id = id;
        this.custId = custId;
        this.image = image;
        this.tryd = tryd;
    }
    @Generated(hash = 1052449182)
    public FaceRecord() {
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
    public String getImage() {
        return this.image;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public Boolean getTryd() {
        return this.tryd;
    }
    public void setTryd(Boolean tryd) {
        this.tryd = tryd;
    }
}
