package org.itsuka.log.support;

import org.itsuka.core.domain.LoginUser;
import org.itsuka.log.domain.LogOperator;
import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.spi.LogOperatorProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

/**
 * {@link LogOperatorProvider} resolving the operator from the Spring Security context.
 *
 * <p>The principal is inspected when an authenticated user is present:</p>
 * <ul>
 *   <li>{@link LoginUser}: id from {@code String.valueOf(loginUser.getId())}, name from
 *       {@code nickname} falling back to {@code username};</li>
 *   <li>any other principal: both id and name from {@code authentication.getName()};</li>
 *   <li>no authentication (or a failure while reading it): the configured defaults.</li>
 * </ul>
 *
 * <p>This provider never throws.</p>
 */
public class SecurityLogOperatorProvider implements LogOperatorProvider {

    private final DefaultLogOperatorProvider defaultProvider;

    /**
     * Creates a provider backed by the given properties.
     *
     * @param properties the operation log properties
     */
    public SecurityLogOperatorProvider(LogRecordProperties properties) {
        this.defaultProvider = new DefaultLogOperatorProvider(properties);
    }

    @Override
    public LogOperator getOperator() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return defaultProvider.getOperator();
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof LoginUser loginUser) {
                String name = loginUser.getNickname();
                if (!StringUtils.hasText(name)) {
                    name = loginUser.getUsername();
                }
                return new LogOperator(String.valueOf(loginUser.getId()), name);
            }
            String name = authentication.getName();
            return new LogOperator(name, name);
        } catch (RuntimeException ex) {
            return defaultProvider.getOperator();
        }
    }
}
