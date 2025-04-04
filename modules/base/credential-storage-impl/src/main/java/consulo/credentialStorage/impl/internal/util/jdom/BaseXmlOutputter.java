package consulo.credentialStorage.impl.internal.util.jdom;

import org.jdom.DocType;

import java.io.IOException;
import java.io.Writer;

public abstract class BaseXmlOutputter {
    protected final String lineSeparator;

    public BaseXmlOutputter(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public static boolean doesNameSuggestSensitiveInformation(String name) {
        if (name.contains("password")) {
            String lowerName = name.toLowerCase();
            boolean isRemember = lowerName.contains("remember") ||
                lowerName.contains("keep") ||
                lowerName.contains("use") ||
                lowerName.contains("save") ||
                lowerName.contains("stored");
            return !isRemember;
        }
        return false;
    }

    /**
     * Handles printing the DOCTYPE declaration if one exists.
     *
     * @param out     the Writer to use
     * @param docType the DocType whose declaration to write
     * @throws IOException if an I/O error occurs
     */
    protected void printDocType(Writer out, DocType docType) throws IOException {
        String publicID = docType.getPublicID();
        String systemID = docType.getSystemID();
        String internalSubset = docType.getInternalSubset();
        boolean hasPublic = false;

        out.write("<!DOCTYPE ");
        out.write(docType.getElementName());
        if (publicID != null) {
            out.write(" PUBLIC \"");
            out.write(publicID);
            out.write('"');
            hasPublic = true;
        }
        if (systemID != null) {
            if (!hasPublic) {
                out.write(" SYSTEM");
            }
            out.write(" \"");
            out.write(systemID);
            out.write("\"");
        }
        if (internalSubset != null && !internalSubset.isEmpty()) {
            out.write(" [");
            out.write(lineSeparator);
            out.write(internalSubset);
            out.write("]");
        }
        out.write(">");
    }
}
