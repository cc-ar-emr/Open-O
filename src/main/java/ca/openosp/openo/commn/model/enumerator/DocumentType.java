//CHECKSTYLE:OFF
package ca.openosp.openo.commn.model.enumerator;

public enum DocumentType {
    EFORM("E", "eForm"),
    DOC("D", "doc"),
    LAB("L", "lab"),
    FORM("F", "form"),
    HRM("H", "hrm");

    private final String name;
    private final String type;

    DocumentType(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Resolves a DocumentType from its single-letter type code.
     *
     * @param type String the type code (e.g. "D", "L")
     * @return DocumentType the matching type, or null if unknown
     */
    public static DocumentType fromType(String type) {
        for (DocumentType documentType : values()) {
            if (documentType.type.equals(type)) {
                return documentType;
            }
        }
        return null;
    }
}