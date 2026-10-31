package org.itsuka.log.support;

import java.util.Set;

/**
 * Shared predicate deciding which method arguments must be skipped when serializing a parameter
 * snapshot.
 *
 * <p>Servlet request/response and multipart arguments are skipped. The checks are done by class
 * name (walking the class and interface hierarchy) rather than {@code instanceof} against
 * compile-time servlet types, so the default serializer stays safe in applications that do not
 * have the servlet or Spring Web API on the classpath.</p>
 */
public final class LogArgumentFilter {

    private static final Set<String> IGNORED_TYPE_NAMES = Set.of(
            "jakarta.servlet.http.HttpServletRequest",
            "jakarta.servlet.http.HttpServletResponse",
            "org.springframework.web.multipart.MultipartFile");

    private LogArgumentFilter() {
    }

    /**
     * Returns {@code true} when the argument is {@code null} or of an ignored servlet/multipart
     * type.
     *
     * @param arg the method argument, may be {@code null}
     * @return {@code true} to skip the argument
     */
    public static boolean isIgnored(Object arg) {
        if (arg == null) {
            return true;
        }
        return isIgnoredType(arg.getClass());
    }

    private static boolean isIgnoredType(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (IGNORED_TYPE_NAMES.contains(type.getName())) {
            return true;
        }
        if (isIgnoredType(type.getSuperclass())) {
            return true;
        }
        for (Class<?> itf : type.getInterfaces()) {
            if (isIgnoredType(itf)) {
                return true;
            }
        }
        return false;
    }
}
