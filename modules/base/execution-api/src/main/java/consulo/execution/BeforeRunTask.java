package consulo.execution;

import consulo.util.jdom.JDOMUtil;
import consulo.util.xml.serializer.SkipDefaultsSerializationFilter;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Transient;
import org.jdom.Element;

import java.util.Objects;

/**
 * The state of one task run before a configuration launches. Subclasses are state POJOs (XmlSerializer beans, JSON-ready);
 * the {@link BeforeRunFragment} owning a task is found by the task's class.
 */
public abstract class BeforeRunTask {
    private boolean myEnabled;

    @Attribute("enabled")
    public boolean isEnabled() {
        return myEnabled;
    }

    public void setEnabled(boolean enabled) {
        myEnabled = enabled;
    }

    /**
     * How many items the task aggregates (e.g. artifacts to build).
     */
    @Transient
    public int getItemsCount() {
        return 1;
    }

    public Element writeState(String elementName) {
        Element element = new Element(elementName);
        XmlSerializer.serializeInto(this, element, new SkipDefaultsSerializationFilter());
        return element;
    }

    @SuppressWarnings("unchecked")
    public <T extends BeforeRunTask> T copy() {
        return (T) Objects.requireNonNull(XmlSerializer.deserialize(writeState("task"), getClass()));
    }

    public boolean isSameState(BeforeRunTask other) {
        return getClass() == other.getClass() && JDOMUtil.areElementsEqual(writeState("task"), other.writeState("task"));
    }
}
