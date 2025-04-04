package consulo.credentialStorage.impl.internal.kdbx;

import consulo.util.lang.StringUtil;
import org.jdom.Element;
import org.jdom.Text;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class KdbxEntry {
    // The underlying XML element representing the entry.
    final Element entryElement;
    // The KeePass database that owns this entry.
    private final KeePassDatabase database;
    // The group this entry belongs to (volatile as in Kotlin).
    volatile KdbxGroup group;

    public KdbxEntry(Element entryElement, KeePassDatabase database, KdbxGroup group) {
        this.entryElement = entryElement;
        this.database = database;
        this.group = group;
    }

    public Element getEntryElement() {
        return entryElement;
    }

    public void setGroup(KdbxGroup group) {
        this.group = group;
    }

    public KdbxGroup getGroup() {
        return group;
    }

    public String getTitle() {
        return getProperty(KdbxEntryElementNames.TITLE);
    }

    public void setTitle(String value) {
        setProperty(this.entryElement, value, KdbxEntryElementNames.TITLE);
    }

    public String getUserName() {
        return getProperty(KdbxEntryElementNames.USER_NAME);
    }

    public void setUserName(String value) {
        setProperty(this.entryElement, value, KdbxEntryElementNames.USER_NAME);
    }

    // Synchronized getter for a property.
    private synchronized String getProperty(String propertyName) {
        Element propElem = getPropertyElement(this.entryElement, propertyName);
        if (propElem == null) {
            return null;
        }
        Element valueElem = propElem.getChild(KdbxEntryElementNames.VALUE);
        if (valueElem == null) {
            return null;
        }
        String value = StringUtil.nullize(valueElem.getText());
        if (isValueProtected(valueElem)) {
            throw new UnsupportedOperationException(propertyName + " protection is not supported");
        }
        else {
            return value;
        }
    }

    // Synchronized setter for a property.
    private synchronized Element setProperty(Element entryElem, String value, String propertyName) {
        String normalizedValue = StringUtil.nullize(value);
        Element propElem = getPropertyElement(entryElem, propertyName);
        if (propElem == null) {
            if (normalizedValue == null) {
                return null;
            }
            propElem = createPropertyElement(entryElem, propertyName);
        }
        Element valueElem = getOrCreateChild(propElem, KdbxEntryElementNames.VALUE);

        if (Objects.equals(StringUtil.nullize(valueElem.getText()), normalizedValue)) {
            return null;
        }

        valueElem.setText(value);
        if (entryElem == this.entryElement) {
            touch();
        }
        return valueElem;
    }

    // Synchronized getter for the password property.
    public synchronized SecureString getPassword() {
        Element propElem = getPropertyElement(this.entryElement, KdbxEntryElementNames.PASSWORD);
        if (propElem == null) {
            return null;
        }
        Element valueElem = propElem.getChild(KdbxEntryElementNames.VALUE);
        if (valueElem == null) {
            return null;
        }
        List content = valueElem.getContent();
        if (content.isEmpty()) {
            return null;
        }
        Object first = content.get(0);
        if (first instanceof SecureString) {
            return (SecureString) first;
        }
        // If value was not originally protected, protect it.
        valueElem.setAttribute(KdbxAttributeNames.PROTECTED, "True");
        String plainValue;
        if (first instanceof Text) {
            plainValue = ((Text) first).getText();
        }
        else {
            plainValue = first.toString();
        }
        
        UnsavedProtectedValue result = new UnsavedProtectedValue(database.protectValue(plainValue));
        valueElem.setContent(result);
        return result;
    }

    // Synchronized setter for the password property.
    public synchronized void setPassword(SecureString value) {
        if (value == null) {
            // Remove the property from the XML.
            List<Element> stringElements = this.entryElement.getChildren(KdbxEntryElementNames.STRING);
            Iterator<Element> iterator = stringElements.iterator();
            while (iterator.hasNext()) {
                Element elem = iterator.next();
                if (KdbxEntryElementNames.PASSWORD.equals(elem.getChildText(KdbxEntryElementNames.KEY))) {
                    iterator.remove();
                    touch();
                }
            }
            return;
        }
        Element propElem = getOrCreatePropertyElement(KdbxEntryElementNames.PASSWORD);
        Element valueElem = getOrCreateChild(propElem, KdbxEntryElementNames.VALUE);
        valueElem.setAttribute(KdbxAttributeNames.PROTECTED, "True");
        Object oldValue = valueElem.getContent().isEmpty() ? null : valueElem.getContent().get(0);
        if (oldValue == value) {
            return;
        }
        // Cast the value to StringProtectedByStreamCipher.
        valueElem.setContent(new UnsavedProtectedValue((StringProtectedByStreamCipher) value));
        touch();
    }

    // Helper method: returns the property element or creates one if it doesn't exist.
    private Element getOrCreatePropertyElement(String name) {
        Element elem = getPropertyElement(this.entryElement, name);
        if (elem == null) {
            elem = createPropertyElement(this.entryElement, name);
        }
        return elem;
    }

    // Synchronized method to update the modification time.
    private synchronized void touch() {
        Element timesElem = getOrCreateChild(this.entryElement, "Times");
        Element lastModTime = getOrCreateChild(timesElem, "LastModificationTime");
        lastModTime.setText(formattedNow());
        database.setDirty(true);
    }

    // --- Helper static methods ---

    // Returns the first child element of 'element' among those with name KdbxEntryElementNames.STRING whose child 'Key' equals name.
    private static Element getPropertyElement(Element element, String name) {
        List<Element> children = element.getChildren(KdbxEntryElementNames.STRING);
        for (Element child : children) {
            String keyText = child.getChildText(KdbxEntryElementNames.KEY);
            if (name.equals(keyText)) {
                return child;
            }
        }
        return null;
    }

    // Creates a new property element with a child key element set to propertyName.
    private static Element createPropertyElement(Element parentElement, String propertyName) {
        Element propertyElement = new Element(KdbxEntryElementNames.STRING);
        Element keyElement = new Element(KdbxEntryElementNames.KEY);
        keyElement.setText(propertyName);
        propertyElement.addContent(keyElement);
        parentElement.addContent(propertyElement);
        return propertyElement;
    }

    // Returns the child element with the specified name, creating it if it doesn't exist.
    private static Element getOrCreateChild(Element parent, String childName) {
        Element child = parent.getChild(childName);
        if (child == null) {
            child = new Element(childName);
            parent.addContent(child);
        }
        return child;
    }

    // Returns the current time formatted as "yyyy-MM-dd'T'HH:mm:ss'Z'".
    private static String formattedNow() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        return LocalDateTime.now(ZoneOffset.UTC).format(formatter);
    }

    // Checks if the given value element is marked as protected.
    private static boolean isValueProtected(Element valueElement) {
        String attr = valueElement.getAttributeValue(KdbxAttributeNames.PROTECTED);
        return attr != null && attr.equalsIgnoreCase("true");
    }
}
