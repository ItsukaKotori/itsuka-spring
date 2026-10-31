package org.itsuka.log.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.itsuka.log.domain.LogOperator;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.core.domain.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityLogOperatorProviderTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsLoginUser() {
        LoginUser user = new LoginUser();
        user.setId(42L);
        user.setUsername("itsuka");
        user.setNickname("Itsuka");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, List.of()));

        LogOperator operator = new SecurityLogOperatorProvider(new LogRecordProperties()).getOperator();
        assertThat(operator.id()).isEqualTo("42");
        assertThat(operator.name()).isEqualTo("Itsuka");
    }

    @Test
    void fallsBackWhenNoAuthentication() {
        LogOperator operator = new SecurityLogOperatorProvider(new LogRecordProperties()).getOperator();
        assertThat(operator.id()).isEqualTo("system");
    }
}
