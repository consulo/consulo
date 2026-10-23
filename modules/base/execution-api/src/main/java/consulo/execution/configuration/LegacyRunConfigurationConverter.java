package consulo.execution.configuration;

import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.XmlSerializer;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LegacyRunConfigurationConverter {
    private static final String ENVS = "envs";
    private static final String ENV = "env";
    private static final String OPTION = "option";
    private static final String ENV_VARIABLES = "ENV_VARIABLES";
    private static final String EXTENSION = "extension";
    private static final String EXTENSION_NAME = "name";
    private static final String EXTENSION_ROOT = "EXTENSION";
    private static final String EXTENSION_ID = "ID";

    private final Element myElement;
    private @Nullable Object myOptions;
    private final List<Object> myFragmentStates = new ArrayList<>();
    private final Set<Element> myConvertedExtensions = new HashSet<>();

    public LegacyRunConfigurationConverter(Element element) {
        myElement = element;
    }

    public Element getElement() {
        return myElement;
    }

    public <T> T readXml(Class<T> legacyStateClass) {
        T state = newInstance(legacyStateClass);
        XmlSerializer.deserializeInto(state, myElement);
        return state;
    }

    public Map<String, String> readEnvs() {
        Map<String, String> envs = new LinkedHashMap<>();

        Element envsElement = myElement.getChild(ENVS);
        readEnvChildren(envsElement != null ? envsElement : myElement, envs);

        for (Element option : myElement.getChildren(OPTION)) {
            if (ENV_VARIABLES.equals(option.getAttributeValue("name"))) {
                for (String pair : StringUtil.split(StringUtil.notNullize(option.getAttributeValue("value")), ";")) {
                    int index = pair.indexOf('=');
                    if (index > 0) {
                        envs.put(pair.substring(0, index), pair.substring(index + 1));
                    }
                }
            }
        }
        return envs;
    }

    public @Nullable String readModule() {
        Element module = myElement.getChild("module");
        return module != null ? StringUtil.nullize(module.getAttributeValue("name"), true) : null;
    }

    /**
     * The settings element an old run configuration extension wrote ({@code <extension name="id">} or
     * {@code <EXTENSION ID="id">}), marked as converted; {@code null} when there is none.
     */
    public @Nullable Element findExtension(String serializationId) {
        for (Element child : myElement.getChildren()) {
            if (serializationId.equals(getExtensionId(child))) {
                myConvertedExtensions.add(child);
                return child;
            }
        }
        return null;
    }

    public void setOptions(Object options) {
        myOptions = options;
    }

    public void addFragment(Object fragmentState) {
        myFragmentStates.add(fragmentState);
    }

    @Nullable Object getOptions() {
        return myOptions;
    }

    List<Object> getFragmentStates() {
        return Collections.unmodifiableList(myFragmentStates);
    }

    /**
     * Extension settings no fragment converted, kept until the plugin owning them is loaded.
     */
    List<Element> getUnconvertedExtensions() {
        List<Element> result = new ArrayList<>();
        for (Element child : myElement.getChildren()) {
            if (getExtensionId(child) != null && !myConvertedExtensions.contains(child)) {
                result.add(child);
            }
        }
        return result;
    }

    private static @Nullable String getExtensionId(Element element) {
        if (EXTENSION.equals(element.getName())) {
            return element.getAttributeValue(EXTENSION_NAME);
        }
        if (EXTENSION_ROOT.equals(element.getName())) {
            return element.getAttributeValue(EXTENSION_ID);
        }
        return null;
    }

    static <T> T newInstance(Class<T> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Cannot create " + type.getName(), e);
        }
    }

    private static void readEnvChildren(Element parent, Map<String, String> envs) {
        for (Element env : parent.getChildren(ENV)) {
            String name = env.getAttributeValue("name");
            if (name != null) {
                envs.put(name, StringUtil.notNullize(env.getAttributeValue("value")));
            }
        }
    }
}
