package com.yannuo.dgcanteen.model;

public class MqttAddFaceCallback {

    private String msg;
    private String key = "add_user";
    private Integer code;
    private String messageId;
    private DataBeanZ data;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public DataBeanZ getData() {
        return data;
    }

    public void setData(DataBeanZ data) {
        this.data = data;
    }

    public static class DataBeanZ {
        private String name;
        private String number;

        public DataBeanZ(String name, String number, String deviceNum) {
            this.name = name;
            this.number = number;
            this.deviceNum = deviceNum;
        }

        private String deviceNum;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getDeviceNum() {
            return deviceNum;
        }

        public void setDeviceNum(String deviceNum) {
            this.deviceNum = deviceNum;
        }
    }
}
