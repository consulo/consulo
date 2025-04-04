package consulo.credentialStorage.impl.internal.kdbx;

import org.jdom.Element;

public final class ProtectedValueUtil {
    private ProtectedValueUtil() {
    }

    public static boolean isValueProtected(Element valueElement) {
        String attr = valueElement.getAttributeValue(KdbxAttributeNames.PROTECTED);
        return "true".equalsIgnoreCase(attr);
    }
}
