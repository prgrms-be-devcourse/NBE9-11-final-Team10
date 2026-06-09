package com.team10.backend.domain.account.util;

import java.util.concurrent.ThreadLocalRandom;

public class AccountNumberGenerator {

    public static String generate() {
        long number = ThreadLocalRandom.current()
                .nextLong(100_000_000_000L,
                        999_999_999_999L);

        return String.valueOf(number);
    }
}
