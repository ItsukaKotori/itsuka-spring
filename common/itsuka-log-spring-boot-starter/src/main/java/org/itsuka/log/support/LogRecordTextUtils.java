package org.itsuka.log.support;

/**
 * Text utilities shared by operation log stores and the aspect.
 */
public final class LogRecordTextUtils {

    private static final String ELLIPSIS = "...";

    private LogRecordTextUtils() {
    }

    /**
     * Truncates the given text to at most {@code maxLength} code points.
     *
     * <p>When truncation is needed the last three characters are replaced by {@code ...};
     * for lengths below four the text is cut without an ellipsis. Truncation always lands on a
     * code-point boundary so a surrogate pair (e.g. an emoji) is never split.</p>
     *
     * @param value     the text to truncate, may be {@code null}
     * @param maxLength the maximum number of code points, non-positive values yield an empty string
     * @return the truncated text, or {@code null} when {@code value} is {@code null}
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (maxLength <= 0) {
            return "";
        }
        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxLength) {
            return value;
        }
        if (maxLength < 4) {
            return value.substring(0, value.offsetByCodePoints(0, maxLength));
        }
        return value.substring(0, value.offsetByCodePoints(0, maxLength - ELLIPSIS.length())) + ELLIPSIS;
    }
}
