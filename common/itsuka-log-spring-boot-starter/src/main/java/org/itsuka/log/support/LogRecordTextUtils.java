package org.itsuka.log.support;

/**
 * Text utilities shared by operation log stores and the aspect.
 */
public final class LogRecordTextUtils {

    private static final String ELLIPSIS = "...";

    private LogRecordTextUtils() {
    }

    /**
     * Truncates the given text to at most {@code maxLength} characters.
     *
     * <p>When truncation is needed the last three characters are replaced by {@code ...};
     * for lengths below four the text is cut without an ellipsis.</p>
     *
     * @param value     the text to truncate, may be {@code null}
     * @param maxLength the maximum length, non-positive values yield an empty string
     * @return the truncated text, or {@code null} when {@code value} is {@code null}
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (maxLength <= 0) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        if (maxLength < 4) {
            return value.substring(0, maxLength);
        }
        return value.substring(0, maxLength - ELLIPSIS.length()) + ELLIPSIS;
    }
}
