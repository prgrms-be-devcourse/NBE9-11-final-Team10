package com.team10.backend.domain.account.util;

import java.util.concurrent.ThreadLocalRandom;

public class AccountNumberGenerator {

    private static final long MIN_ACCOUNT_NUMBER = 100_000_000_000L;
    private static final long MAX_ACCOUNT_NUMBER = 999_999_999_999L;

    public static String generate() {
        long number = ThreadLocalRandom.current()
                .nextLong(MIN_ACCOUNT_NUMBER, MAX_ACCOUNT_NUMBER);

        return String.valueOf(number);
    }
}
