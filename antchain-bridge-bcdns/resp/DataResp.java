package org.bcdns.credential.dto.resp;

import java.io.Serializable;

public class DataResp<T> extends Resp implements Serializable {
    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
