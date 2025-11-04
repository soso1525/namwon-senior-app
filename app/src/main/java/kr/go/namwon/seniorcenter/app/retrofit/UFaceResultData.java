package kr.go.namwon.seniorcenter.app.retrofit;

public class UFaceResultData {
    private String code;
    private String msg;
    private EzResponse ezResponse;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public EzResponse getezResponse() {
        return ezResponse;
    }

    public void setezResponse(EzResponse ezResponse) {
        this.ezResponse = ezResponse;
    }

    public UFaceResultData() {
        this.code = "";
        this.msg = "";
        this.ezResponse = new EzResponse();
    }

    public UFaceResultData(String code, String msg, EzResponse ezResponse) {
        this.code = code;
        this.msg = msg;
        this.ezResponse = ezResponse;
    }

    public static class EzResponse {
        private String resp_code;
        private String resp_msg;
        private String resp_score;
        private String resp_age;
        private String resp_gender;
        private String resp_threshold;
        private String resp_cust_no;
        private String resp_chnl_dv;

        // Default constructor
        public EzResponse() {
            this.resp_code = "";
            this.resp_msg = "";
            this.resp_score = "";
            this.resp_age = "";
            this.resp_gender = "";
            this.resp_threshold = "";
            this.resp_cust_no = "";
            this.resp_chnl_dv = "";
        }

        public EzResponse(String resp_code, String resp_msg, String resp_score, String resp_age,
                          String resp_gender, String resp_threshold, String resp_cust_no, String resp_chnl_dv) {
            this.resp_code = resp_code;
            this.resp_msg = resp_msg;
            this.resp_score = resp_score;
            this.resp_age = resp_age;
            this.resp_gender = resp_gender;
            this.resp_threshold = resp_threshold;
            this.resp_cust_no = resp_cust_no;
            this.resp_chnl_dv = resp_chnl_dv;
        }

        public String getResp_code() {
            return resp_code;
        }

        public void setResp_code(String resp_code) {
            this.resp_code = resp_code;
        }

        public String getResp_msg() {
            return resp_msg;
        }

        public void setResp_msg(String resp_msg) {
            this.resp_msg = resp_msg;
        }

        public String getResp_score() {
            return resp_score;
        }

        public void setResp_score(String resp_score) {
            this.resp_score = resp_score;
        }

        public String getResp_age() {
            return resp_age;
        }

        public void setResp_age(String resp_age) {
            this.resp_age = resp_age;
        }

        public String getResp_gender() {
            return resp_gender;
        }

        public void setResp_gender(String resp_gender) {
            this.resp_gender = resp_gender;
        }

        public String getResp_threshold() {
            return resp_threshold;
        }

        public void setResp_threshold(String resp_threshold) {
            this.resp_threshold = resp_threshold;
        }

        public String getResp_cust_no() {
            return resp_cust_no;
        }

        public void setResp_cust_no(String resp_cust_no) {
            this.resp_cust_no = resp_cust_no;
        }

        public String getResp_chnl_dv() {
            return resp_chnl_dv;
        }

        public void setResp_chnl_dv(String resp_chnl_dv) {
            this.resp_chnl_dv = resp_chnl_dv;
        }
    }
}