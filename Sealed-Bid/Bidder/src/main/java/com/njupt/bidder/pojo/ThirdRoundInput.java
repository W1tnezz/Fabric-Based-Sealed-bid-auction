package com.njupt.bidder.pojo;

import lombok.Data;

import java.util.List;

@Data
public class ThirdRoundInput {
    private String identity;
    private int rank;
    private List<ThirdRoundCompareRes> allProofs;
}
