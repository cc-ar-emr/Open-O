//CHECKSTYLE:OFF
package org.oscarehr.util;

public class PDFGenerationException extends Exception {
    public PDFGenerationException() {
        super();
    }

    public PDFGenerationException(String message) {
        super(message);
    }

    public PDFGenerationException(Throwable cause) {
        super(cause);
    }

    public PDFGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
