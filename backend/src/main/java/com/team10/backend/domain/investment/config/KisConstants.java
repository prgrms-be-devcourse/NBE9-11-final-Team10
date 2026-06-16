package com.team10.backend.domain.investment.config;

public final class KisConstants {

    private KisConstants() {
    }

    public static final String BASE_URL = "https://openapi.koreainvestment.com:9443";
    public static final String WEB_SOCKET_URL = "ws://ops.koreainvestment.com:21000";

    public static final class Header {

        public static final String APP_KEY = "appkey";
        public static final String APP_SECRET = "appsecret";
        public static final String SECRET_KEY = "secretkey";
        public static final String TR_ID = "tr_id";
        public static final String AUTHORIZATION = "authorization";
        public static final String CONTENT_TYPE = "content-type";
        public static final String CUST_TYPE = "custtype";

        private Header() {
        }

    }

    public static final class Auth {

        public static final String TOKEN_ISSUE_PATH = "/oauth2/tokenP";
        public static final String TOKEN_REVOKE_PATH = "/oauth2/revokeP";
        public static final String WEBSOCKET_APPROVAL_PATH = "/oauth2/Approval";

        private Auth() {
        }

    }

    public static final class MarketHoliday {

        public static final String MARKET_HOLIDAY_PATH = "/uapi/domestic-stock/v1/quotations/chk-holiday";
        public static final String MARKET_HOLIDAY_TR_ID = "CTCA0903R";

        private MarketHoliday() {

        }

    }

}
