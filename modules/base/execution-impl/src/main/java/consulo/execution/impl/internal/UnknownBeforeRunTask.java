package consulo.execution.impl.internal;

import consulo.execution.BeforeRunTask;
import org.jdom.Element;

/**
 * A task whose {@link consulo.execution.BeforeRunFragment} is not loaded; its element is written back unchanged.
 */
public final class UnknownBeforeRunTask extends BeforeRunTask {
    private final String myId;
    private final Element myElement;

    public UnknownBeforeRunTask(String id, Element element) {
        myId = id;
        myElement = element.clone();
        setEnabled(!"false".equals(element.getAttributeValue("enabled")));
    }

    public String getId() {
        return myId;
    }

    public Element getElement() {
        return myElement.clone();
    }

    @Override
    public Element writeState(String elementName) {
        return getElement();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BeforeRunTask> T copy() {
        return (T) new UnknownBeforeRunTask(myId, myElement);
    }
}
