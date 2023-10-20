package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

public class GenerateBlobRespDto {

    /**
     * blob :
     * hash :
     * platformSignData : {"publicKey":"","signBlob":""}
     */
    private String blob;
    private String hash;
    private PlatformSignDataEntity platformSignData;

    public void setBlob(String blob) {
        this.blob = blob;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setPlatformSignData(PlatformSignDataEntity platformSignData) {
        this.platformSignData = platformSignData;
    }

    public String getBlob() {
        return blob;
    }

    public String getHash() {
        return hash;
    }

    public PlatformSignDataEntity getPlatformSignData() {
        return platformSignData;
    }

    public static class PlatformSignDataEntity {
        /**
         * publicKey :
         * signBlob :
         */
        private String publicKey;
        private String signBlob;

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public void setSignBlob(String signBlob) {
            this.signBlob = signBlob;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public String getSignBlob() {
            return signBlob;
        }
    }
}