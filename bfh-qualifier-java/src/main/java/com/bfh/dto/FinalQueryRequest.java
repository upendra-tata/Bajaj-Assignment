package com.bfh.dto;

import lombok.Getter;

@Getter
public class FinalQueryRequest {

    private String finalQuery;

    public FinalQueryRequest() {
    }

    public FinalQueryRequest(String finalQuery) {
        this.finalQuery = finalQuery;
    }

    public void setFinalQuery(String finalQuery) {
        this.finalQuery = finalQuery;
    }
}
