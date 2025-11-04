package kr.go.namwon.seniorcenter.app.retrofit;

public class UFaceDataUntactRequest {
    private String uuid;
    private String custNo;
    private String osType;
    private String untactType;
    private String verifyType;
    private String image;
    private String idImage;
    private String chnlDv;

    public UFaceDataUntactRequest() {
        this.uuid = "";
        this.custNo = "";
        this.osType = "";
        this.untactType = "";
        this.verifyType = "";
        this.image = "";
        this.idImage = "";
        this.chnlDv = "";
    }

    public UFaceDataUntactRequest(String uuid, String custNo, String osType, String untactType, String verifyType,
                                  String image, String idImage, String chnlDv) {
        this.uuid = uuid;
        this.custNo = custNo;
        this.osType = osType;
        this.untactType = untactType;
        this.verifyType = verifyType;
        this.image = image;
        this.idImage = idImage;
        this.chnlDv = chnlDv;
    }
}