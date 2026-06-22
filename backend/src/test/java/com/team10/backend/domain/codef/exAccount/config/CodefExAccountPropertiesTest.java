package com.team10.backend.domain.codef.exAccount.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CodefExAccountPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CodefExAccountConfig.class)
            .withPropertyValues(
                    "codef.account-inquiry.service-type=DEMO",
                    "codef.account-inquiry.client-id=account-client-id",
                    "codef.account-inquiry.client-secret=account-client-secret",
                    "codef.account-inquiry.public-key=account-public-key",
                    "codef.account-inquiry.account-list-path=/account-list",
                    "codef.account-inquiry.bank-transaction-path=/transaction-list",
                    "codef.one-won-transfer.client-id=transfer-client-id",
                    "codef.one-won-transfer.client-secret=transfer-client-secret",
                    "codef.one-won-transfer.public-key=transfer-public-key"
            );

    @Test
    void bindsOnlyAccountInquiryProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CodefExAccountProperties.class);

            CodefExAccountProperties properties = context.getBean(CodefExAccountProperties.class);
            assertThat(properties.serviceType()).isEqualTo("DEMO");
            assertThat(properties.clientId()).isEqualTo("account-client-id");
            assertThat(properties.clientSecret()).isEqualTo("account-client-secret");
            assertThat(properties.publicKey()).isEqualTo("account-public-key");
            assertThat(properties.accountListPath()).isEqualTo("/account-list");
            assertThat(properties.bankTransactionPath()).isEqualTo("/transaction-list");
        });
    }
}
