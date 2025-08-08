package org.oscarehr.util;

public class SqlEscapeUtil {
    /**
     * Escapes single quotes for safe inclusion in SQL literal strings.
     * For example: O'Reilly ➜ O''Reilly
     */
    public static String escapeSql(String input) {
        return (input == null) ? null : input.replace("'", "''");
    }
}
