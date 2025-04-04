package consulo.credentialStorage.impl.internal.kdbx;

import consulo.credentialStorage.impl.internal.util.jdom.JbXmlOutputter;
import org.bouncycastle.crypto.SkippingStreamCipher;
import org.jdom.Element;

import java.io.IOException;
import java.io.Writer;

public class ProtectedXmlWriter extends JbXmlOutputter {
    private final SkippingStreamCipher streamCipher;

    public ProtectedXmlWriter(SkippingStreamCipher streamCipher) {
        super("\n", null, null, null, false, null);

        this.streamCipher = streamCipher;
    }

    @Override
    public boolean writeContent(Writer out, Element element, int level, boolean substituteMacro) throws IOException {
        // Assume KdbxEntryElementNames.VALUE is a constant holding the element name for a protected value.
        if (!element.getName().equals(KdbxEntryElementNames.VALUE)) {
            return super.writeContent(out, element, level, substituteMacro);
        }

        Object firstContent = element.getContent().isEmpty() ? null : element.getContent().get(0);
        if (firstContent instanceof SecureString) {
            ProtectedValue protectedValue;
            if (firstContent instanceof ProtectedValue) {
                ProtectedValue pv = (ProtectedValue) firstContent;
                pv.setNewStreamCipher(streamCipher);
                protectedValue = pv;
            }
            else {
                // Must be an UnsavedProtectedValue.
                UnsavedProtectedValue upv = (UnsavedProtectedValue) firstContent;
                // Assume secureString has a method getAsByteArray().
                byte[] bytes = upv.getSecureString().getAsByteArray();
                synchronized (streamCipher) {
                    int pos = (int) streamCipher.getPosition();
                    streamCipher.processBytes(bytes, 0, bytes.length, bytes, 0);
                    protectedValue = new ProtectedValue(bytes, pos, streamCipher);
                }
                element.setContent(protectedValue);
            }
            out.write('>');
            out.write(escapeElementEntities(protectedValue.encodeToBase64()));
            return true;
        }
        return super.writeContent(out, element, level, substituteMacro);
    }
}
