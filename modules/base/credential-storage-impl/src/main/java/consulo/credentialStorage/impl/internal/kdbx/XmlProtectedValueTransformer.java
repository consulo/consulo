package consulo.credentialStorage.impl.internal.kdbx;

import org.bouncycastle.crypto.SkippingStreamCipher;
import org.jdom.Element;

import java.util.Base64;
import java.util.List;

public class XmlProtectedValueTransformer {
    private final SkippingStreamCipher streamCipher;
    private int position = 0;

    public XmlProtectedValueTransformer(SkippingStreamCipher streamCipher) {
        this.streamCipher = streamCipher;
    }

    public void processEntries(Element parentElement) {
        // Process all child elements in order.
        List<?> contentList = parentElement.getContent();
        for (Object obj : contentList) {
            if (!(obj instanceof Element)) {
                continue;
            }
            Element element = (Element) obj;
            if (element.getName().equals(KdbxDbElementNames.GROUP)) {
                processEntries(element);
            }
            else if (element.getName().equals(KdbxDbElementNames.ENTRY)) {
                List<Element> containers = element.getChildren(KdbxEntryElementNames.STRING);
                for (Element container : containers) {
                    Element valueElement = container.getChild(KdbxEntryElementNames.VALUE);
                    if (valueElement == null) {
                        continue;
                    }

                    if (ProtectedValueUtil.isValueProtected(valueElement)) {
                        byte[] value = Base64.getDecoder().decode(valueElement.getText());
                        valueElement.setContent(new ProtectedValue(value, position, streamCipher));
                        position += value.length;
                    }
                }
            }
        }
    }
}
