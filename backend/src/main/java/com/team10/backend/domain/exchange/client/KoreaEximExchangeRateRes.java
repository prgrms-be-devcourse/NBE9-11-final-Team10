package com.team10.backend.domain.exchange.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KoreaEximExchangeRateRes(
        Integer result,

        @JsonProperty("cur_unit")
        String curUnit,

        @JsonProperty("ttb")
        String ttb,

        @JsonProperty("tts")
        String tts,

        @JsonProperty("deal_bas_r")
        String dealBasR,

        @JsonProperty("cur_nm")
        String curName
) {
}
