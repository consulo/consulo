package consulo.credentialStorage.impl.internal.kdbx;

import consulo.credentialStorage.impl.internal.SharedLogger;
import consulo.util.collection.Lists;
import org.jdom.Element;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Predicate;

public class KdbxGroup {
    private final Element element;
    private final KeePassDatabase database;
    private volatile KdbxGroup parent;

    private final Map<String, KdbxGroup> groups = new HashMap<>();
    // Lazy-initialized list of entries (using a lock-free copy-on-write list if available)
    private List<KdbxEntry> entries = Lists.newLockFreeCopyOnWriteList(new ArrayList<>());

    // locationChanged property managed via getter/setter
    private long locationChanged;

    // Date formatter used for time formatting
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public KdbxGroup(Element element, KeePassDatabase database, KdbxGroup parent) {
        this.element = element;
        this.database = database;
        this.parent = parent;
        // Initialize entries from existing child elements
        List<Element> entryElements = element.getChildren(KdbxDbElementNames.ENTRY);
        List<KdbxEntry> entryList = new ArrayList<>();
        for (Element e : entryElements) {
            entryList.add(new KdbxEntry(e, database, this));
        }
        this.entries = Lists.newLockFreeCopyOnWriteList(entryList);
    }

    public synchronized String getName() {
        String name = element.getChildText(KdbxDbElementNames.NAME);
        return name != null ? name : "Unnamed";
    }

    public synchronized void setName(String value) {
        Element nameElement = getOrCreateChild(element, KdbxDbElementNames.NAME);
        if (nameElement.getText().equals(value)) {
            return;
        }
        nameElement.setText(value);
        database.setDirty(true);
    }

    public synchronized KdbxGroup getGroup(String name) {
        KdbxGroup result = groups.get(name);
        if (result != null) {
            return result;
        }
        // Search among child elements for a group with matching name.
        List<?> contents = element.getContent();
        for (Object obj : contents) {
            if (obj instanceof Element) {
                Element child = (Element) obj;
                String childName = child.getChildText(KdbxDbElementNames.NAME);
                if (name.equals(childName)) {
                    result = new KdbxGroup(child, database, this);
                    groups.put(name, result);
                    return result;
                }
            }
        }
        return null;
    }

    public synchronized void removeGroup(String name) {
        KdbxGroup group = getGroup(name);
        if (group != null) {
            removeGroup(group);
        }
    }

    private synchronized void removeGroup(KdbxGroup group) {
        KdbxGroup removedGroup = groups.remove(group.getName());
        SharedLogger.LOG.assertTrue(group == removedGroup);
        element.getContent().remove(group.element);
        group.parent = null;
        database.setDirty(true);
    }

    public synchronized KdbxGroup getOrCreateGroup(String name) {
        KdbxGroup group = getGroup(name);
        return (group != null) ? group : createGroup(name);
    }

    private KdbxGroup createGroup(String name) {
        KdbxGroup result = KdbxGroup.createGroup(database, this);
        result.setName(name);
        if (result.equals(database.getRootGroup())) {
            throw new IllegalStateException("Cannot set root group as child of another group");
        }
        groups.put(result.getName(), result);
        result.parent = this;
        long now = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC);
        result.setLocationChanged(now);
        element.addContent(result.element);
        database.setDirty(true);
        return result;
    }

    public synchronized KdbxEntry getEntry(Predicate<KdbxEntry> matcher) {
        for (KdbxEntry entry : entries) {
            if (matcher.test(entry)) {
                return entry;
            }
        }
        return null;
    }

    public synchronized KdbxEntry addEntry(KdbxEntry entry) {
        if (entry.getGroup() != null) {
            entry.getGroup().removeEntry(entry);
        }
        entries.add(entry);
        entry.setGroup(this);
        database.setDirty(true);
        element.addContent(entry.getEntryElement());
        return entry;
    }

    private synchronized KdbxEntry removeEntry(KdbxEntry entry) {
        if (entries.remove(entry)) {
            entry.setGroup(null);
            element.getContent().remove(entry.getEntryElement());
            database.setDirty(true);
        }
        return entry;
    }

    public synchronized KdbxEntry getEntry(String title, String userName) {
        return getEntry(entry -> entry.getTitle().equals(title) &&
            (entry.getUserName() != null ? entry.getUserName().equals(userName) : userName == null));
    }

    public synchronized KdbxEntry createEntry(String title, String userName) {
        KdbxEntry entry = database.createEntry(title);
        entry.setUserName(userName);
        addEntry(entry);
        return entry;
    }

    public synchronized KdbxEntry removeEntry(String title, String userName) {
        KdbxEntry entry = getEntry(title, userName);
        return (entry != null) ? removeEntry(entry) : null;
    }

    public long getLocationChanged() {
        Element timesElem = element.getChild("Times");
        if (timesElem != null) {
            Element locChanged = timesElem.getChild("LocationChanged");
            if (locChanged != null) {
                String text = locChanged.getText();
                Long parsed = parseTime(text);
                return (parsed != null) ? parsed : 0;
            }
        }
        return 0;
    }

    public Element getElement() {
        return element;
    }

    public void setLocationChanged(long value) {
        Element timesElem = getOrCreateChild(element, "Times");
        Element locChanged = getOrCreateChild(timesElem, "LocationChanged");
        String formatted = Instant.ofEpochMilli(value * 1000L).atZone(ZoneOffset.UTC).format(dateFormatter);
        locChanged.setText(formatted);
    }

    // Static helper method to create a new group.
    public static KdbxGroup createGroup(KeePassDatabase db, KdbxGroup parent) {
        Element element = new Element(KdbxDbElementNames.GROUP);
        ensureElements(element, mandatoryGroupElements);
        return new KdbxGroup(element, db, parent);
    }

    // Definition of mandatory group elements.
    private static final Map<String[], ValueCreator> mandatoryGroupElements = new LinkedHashMap<>();

    static {
        mandatoryGroupElements.put(KeePassDatabase.UUID_ELEMENT_NAME, new UuidValueCreator());
        mandatoryGroupElements.put(new String[]{"Notes"}, new ConstantValueCreator(""));
        mandatoryGroupElements.put(KeePassDatabase.ICON_ELEMENT_NAME, new ConstantValueCreator("0"));
        mandatoryGroupElements.put(KeePassDatabase.CREATION_TIME_ELEMENT_NAME, new DateValueCreator());
        mandatoryGroupElements.put(KeePassDatabase.LAST_MODIFICATION_TIME_ELEMENT_NAME, new DateValueCreator());
        mandatoryGroupElements.put(KeePassDatabase.LAST_ACCESS_TIME_ELEMENT_NAME, new DateValueCreator());
        mandatoryGroupElements.put(KeePassDatabase.EXPIRY_TIME_ELEMENT_NAME , new DateValueCreator());
        mandatoryGroupElements.put(KeePassDatabase.EXPIRES_ELEMENT_NAME, new ConstantValueCreator("False"));
        mandatoryGroupElements.put(KeePassDatabase.USAGE_COUNT_ELEMENT_NAME, new ConstantValueCreator("0"));
        mandatoryGroupElements.put(KeePassDatabase.LOCATION_CHANGED, new DateValueCreator());
    }

    private static Long parseTime(String value) {
        try {
            return ZonedDateTime.parse(value).toEpochSecond();
        }
        catch (DateTimeParseException e) {
            return 0L;
        }
    }

    // Helper method: get or create a child element with the given name.
    private static Element getOrCreateChild(Element parent, String childName) {
        Element child = parent.getChild(childName);
        if (child == null) {
            child = new Element(childName);
            parent.addContent(child);
        }
        return child;
    }

    // Ensures that all mandatory elements exist on the given element.
    private static void ensureElements(Element element, Map<String[], ValueCreator> childElements) {
        for (Map.Entry<String[], ValueCreator> entry : childElements.entrySet()) {
            String[] elementPath = entry.getKey();
            ValueCreator valueCreator = entry.getValue();
            Element result = findElement(element, elementPath);
            if (result == null) {
                Element currentElement = element;
                for (String elementName : elementPath) {
                    currentElement = getOrCreateChild(currentElement, elementName);
                }
                currentElement.setText(valueCreator.getValue());
            }
        }
    }

    private static Element findElement(Element element, String[] elementPath) {
        Element result = element;
        for (String elementName : elementPath) {
            result = result.getChild(elementName);
            if (result == null) {
                return null;
            }
        }
        return result;
    }
}
