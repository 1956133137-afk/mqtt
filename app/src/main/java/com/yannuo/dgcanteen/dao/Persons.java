package com.yannuo.dgcanteen.dao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Index;
import org.greenrobot.greendao.annotation.Unique;

@Entity
public class Persons {
    @Id(autoincrement = true)
    private Long id;

//    private String campusId;  //园区ID（建行平台）
    @Index
    private String cardId;  //卡号
    @Unique()
    private String custId;//智慧食堂用户唯一标识（建行平台）
//    private String deptId;//所属分组Id
    private String grade;//年级
    private String personName;//人员姓名
    private String personNumber;// 学号/工号
//    private String phone;//
//    private String sex;//性别
    private String userClass;// 班级
    private String image;// 人脸图片路径
    private String messageId ; //消息ID
//    private Boolean update = false ; //人脸照片是否下载成功
    @Generated(hash = 1172284047)
    public Persons(Long id, String cardId, String custId, String grade,
            String personName, String personNumber, String userClass, String image,
            String messageId) {
        this.id = id;
        this.cardId = cardId;
        this.custId = custId;
        this.grade = grade;
        this.personName = personName;
        this.personNumber = personNumber;
        this.userClass = userClass;
        this.image = image;
        this.messageId = messageId;
    }
    @Generated(hash = 1519000671)
    public Persons() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getCardId() {
        return this.cardId;
    }
    public void setCardId(String cardId) {
        this.cardId = cardId;
    }
    public String getCustId() {
        return this.custId;
    }
    public void setCustId(String custId) {
        this.custId = custId;
    }
    public String getGrade() {
        return this.grade;
    }
    public void setGrade(String grade) {
        this.grade = grade;
    }
    public String getPersonName() {
        return this.personName;
    }
    public void setPersonName(String personName) {
        this.personName = personName;
    }
    public String getPersonNumber() {
        return this.personNumber;
    }
    public void setPersonNumber(String personNumber) {
        this.personNumber = personNumber;
    }
    public String getUserClass() {
        return this.userClass;
    }
    public void setUserClass(String userClass) {
        this.userClass = userClass;
    }
    public String getImage() {
        return this.image;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public String getMessageId() {
        return this.messageId;
    }
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }


}
