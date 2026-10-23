/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.impl.internal.configuration;

import consulo.component.persist.StateJsonSerializer;
import consulo.execution.BeforeRunFragment;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.project.Project;
import consulo.util.xml.serializer.SkipDefaultsSerializationFilter;
import consulo.util.xml.serializer.XmlSerializer;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a shared configuration between its XML state ({@code format="2"}) and the JSON of a {@code runConfigurations/<name>.json}
 * file. Options, fragments and before-run tasks are bound by their own POJO classes; a configuration with anything else (runner
 * settings, legacy state) stays XML. Parts whose plugin is not loaded are carried as raw JSON in a {@code json} attribute and
 * written back unchanged.
 *
 * @author VISTALL
 * @since 2026-10-23
 */
final class RunConfigurationJsonConverter {
    static final String JSON_ATTR = "json";

    private static final String FORMAT = "format";
    private static final String FORMAT_VERSION = "2";
    private static final String OPTIONS = "options";
    private static final String FRAGMENT = "fragment";
    private static final String FRAGMENT_ID = "id";
    private static final String SELECTED_OPTIONS = "selectedOptions";
    private static final String SELECTED_OPTION = "option";
    private static final String SELECTED_OPTION_NAME = "name";
    private static final String SELECTED_OPTION_VISIBLE = "visible";
    private static final String LOG_FILE = "log_file";
    private static final String PREDEFINED_LOG_FILE = "predefined_log_file";
    private static final String OUTPUT_FILE = "output_file";
    private static final String SHOW_CONSOLE_ON_STD_OUT = "show_console_on_std_out";
    private static final String SHOW_CONSOLE_ON_STD_ERR = "show_console_on_std_err";

    private static final String KEY_NAME = "name";
    private static final String KEY_TYPE = "type";
    private static final String KEY_FACTORY_NAME = "factoryName";
    private static final String KEY_FOLDER_NAME = "folderName";
    private static final String KEY_EDIT_BEFORE_RUN = "editBeforeRun";
    private static final String KEY_SINGLETON = "singleton";
    private static final String KEY_TEMPORARY = "temporary";
    private static final String KEY_SHOW_CONSOLE_ON_STD_OUT = "showConsoleOnStdOut";
    private static final String KEY_SHOW_CONSOLE_ON_STD_ERR = "showConsoleOnStdErr";
    private static final String KEY_LOG_FILES = "logFiles";
    private static final String KEY_PREDEFINED_LOG_FILES = "predefinedLogFiles";
    private static final String KEY_OUTPUT_FILE = "outputFile";
    private static final String KEY_OPTIONS = "options";
    private static final String KEY_FRAGMENTS = "fragments";
    private static final String KEY_SELECTED_OPTIONS = "selectedOptions";
    private static final String KEY_BEFORE_RUN = "beforeRun";
    private static final String KEY_ID = "id";
    private static final String KEY_STATE = "state";

    private final RunManagerImpl myManager;
    private final StateJsonSerializer mySerializer;

    RunConfigurationJsonConverter(RunManagerImpl manager, StateJsonSerializer serializer) {
        myManager = manager;
        mySerializer = serializer;
    }

    byte @Nullable [] toJson(Element configuration) throws IOException {
        String raw = configuration.getAttributeValue(JSON_ATTR);
        if (raw != null) {
            return raw.getBytes(StandardCharsets.UTF_8);
        }

        if (!FORMAT_VERSION.equals(configuration.getAttributeValue(FORMAT))) {
            return null;
        }

        Class<?> optionsClass = findOptionsClass(configuration.getAttributeValue(RunnerAndConfigurationSettingsImpl.CONFIGURATION_TYPE_ATTRIBUTE),
            configuration.getAttributeValue(RunnerAndConfigurationSettingsImpl.FACTORY_NAME_ATTRIBUTE));
        if (optionsClass == null) {
            return null;
        }

        Map<String, Object> json = new LinkedHashMap<>();
        for (Attribute attribute : configuration.getAttributes()) {
            String value = attribute.getValue();
            switch (attribute.getName()) {
                case RunnerAndConfigurationSettingsImpl.TEMPLATE_FLAG_ATTRIBUTE -> {
                    if (Boolean.parseBoolean(value)) {
                        return null;
                    }
                }
                case FORMAT -> {
                }
                case RunnerAndConfigurationSettingsImpl.NAME_ATTR -> json.put(KEY_NAME, value);
                case RunnerAndConfigurationSettingsImpl.CONFIGURATION_TYPE_ATTRIBUTE -> json.put(KEY_TYPE, value);
                case RunnerAndConfigurationSettingsImpl.FACTORY_NAME_ATTRIBUTE -> json.put(KEY_FACTORY_NAME, value);
                case RunnerAndConfigurationSettingsImpl.FOLDER_NAME -> json.put(KEY_FOLDER_NAME, value);
                case RunnerAndConfigurationSettingsImpl.EDIT_BEFORE_RUN -> json.put(KEY_EDIT_BEFORE_RUN, Boolean.parseBoolean(value));
                case RunnerAndConfigurationSettingsImpl.SINGLETON -> json.put(KEY_SINGLETON, Boolean.parseBoolean(value));
                case RunnerAndConfigurationSettingsImpl.TEMPORARY_ATTRIBUTE -> json.put(KEY_TEMPORARY, Boolean.parseBoolean(value));
                case SHOW_CONSOLE_ON_STD_OUT -> json.put(KEY_SHOW_CONSOLE_ON_STD_OUT, Boolean.parseBoolean(value));
                case SHOW_CONSOLE_ON_STD_ERR -> json.put(KEY_SHOW_CONSOLE_ON_STD_ERR, Boolean.parseBoolean(value));
                default -> {
                    return null;
                }
            }
        }

        List<Object> logFiles = new ArrayList<>();
        List<Object> predefinedLogFiles = new ArrayList<>();
        Map<String, Object> fragments = new LinkedHashMap<>();
        Map<String, Object> selectedOptions = new LinkedHashMap<>();
        List<Object> beforeRun = new ArrayList<>();
        for (Element child : configuration.getChildren()) {
            switch (child.getName()) {
                case LOG_FILE -> {
                    Map<String, Object> attributes = attributesToJson(child);
                    if (attributes == null) {
                        return null;
                    }
                    logFiles.add(attributes);
                }
                case PREDEFINED_LOG_FILE -> {
                    Map<String, Object> attributes = attributesToJson(child);
                    if (attributes == null) {
                        return null;
                    }
                    predefinedLogFiles.add(attributes);
                }
                case OUTPUT_FILE -> {
                    Map<String, Object> attributes = attributesToJson(child);
                    if (attributes == null) {
                        return null;
                    }
                    json.put(KEY_OUTPUT_FILE, attributes);
                }
                case OPTIONS -> {
                    Object options = stateToJson(child, optionsClass);
                    if (options == null) {
                        return null;
                    }
                    json.put(KEY_OPTIONS, options);
                }
                case FRAGMENT -> {
                    String id = child.getAttributeValue(FRAGMENT_ID);
                    if (id == null) {
                        return null;
                    }

                    Object state = fragmentToJson(id, child);
                    if (state == null) {
                        return null;
                    }
                    fragments.put(id, state);
                }
                case SELECTED_OPTIONS -> {
                    for (Element option : child.getChildren(SELECTED_OPTION)) {
                        String name = option.getAttributeValue(SELECTED_OPTION_NAME);
                        if (name != null) {
                            selectedOptions.put(name, Boolean.parseBoolean(option.getAttributeValue(SELECTED_OPTION_VISIBLE)));
                        }
                    }
                }
                case RunManagerImpl.METHOD -> {
                    if (!RunManagerImpl.METHOD_VERSION.equals(child.getAttributeValue(RunManagerImpl.METHOD_VERSION_ATTR))) {
                        return null;
                    }

                    for (Element taskElement : child.getChildren()) {
                        Object task = taskToJson(taskElement);
                        if (task == null) {
                            return null;
                        }
                        beforeRun.add(task);
                    }
                }
                default -> {
                    return null;
                }
            }
        }

        putIfNotEmpty(json, KEY_LOG_FILES, logFiles);
        putIfNotEmpty(json, KEY_PREDEFINED_LOG_FILES, predefinedLogFiles);
        if (!fragments.isEmpty()) {
            json.put(KEY_FRAGMENTS, fragments);
        }
        if (!selectedOptions.isEmpty()) {
            json.put(KEY_SELECTED_OPTIONS, selectedOptions);
        }
        putIfNotEmpty(json, KEY_BEFORE_RUN, beforeRun);
        return mySerializer.writeTree(json);
    }

    Element fromJson(byte[] content) throws IOException {
        Map<String, Object> json = asMap(mySerializer.readTree(content));
        String name = asString(json.get(KEY_NAME));
        String type = asString(json.get(KEY_TYPE));
        String factoryName = asString(json.get(KEY_FACTORY_NAME));

        Element configuration = new Element(RunManagerImpl.CONFIGURATION);
        configuration.setAttribute(RunnerAndConfigurationSettingsImpl.TEMPLATE_FLAG_ATTRIBUTE, String.valueOf(false));
        setAttribute(configuration, RunnerAndConfigurationSettingsImpl.NAME_ATTR, name);
        setAttribute(configuration, RunnerAndConfigurationSettingsImpl.CONFIGURATION_TYPE_ATTRIBUTE, type);
        setAttribute(configuration, RunnerAndConfigurationSettingsImpl.FACTORY_NAME_ATTRIBUTE, factoryName);

        Class<?> optionsClass = findOptionsClass(type, factoryName);
        if (optionsClass == null) {
            return configuration.setAttribute(JSON_ATTR, new String(content, StandardCharsets.UTF_8));
        }

        setAttribute(configuration, RunnerAndConfigurationSettingsImpl.FOLDER_NAME, asString(json.get(KEY_FOLDER_NAME)));
        setBooleanAttribute(configuration, RunnerAndConfigurationSettingsImpl.EDIT_BEFORE_RUN, json.get(KEY_EDIT_BEFORE_RUN));
        setBooleanAttribute(configuration, RunnerAndConfigurationSettingsImpl.SINGLETON, json.get(KEY_SINGLETON));
        setBooleanAttribute(configuration, RunnerAndConfigurationSettingsImpl.TEMPORARY_ATTRIBUTE, json.get(KEY_TEMPORARY));
        setBooleanAttribute(configuration, SHOW_CONSOLE_ON_STD_OUT, json.get(KEY_SHOW_CONSOLE_ON_STD_OUT));
        setBooleanAttribute(configuration, SHOW_CONSOLE_ON_STD_ERR, json.get(KEY_SHOW_CONSOLE_ON_STD_ERR));
        configuration.setAttribute(FORMAT, FORMAT_VERSION);

        for (Object logFile : asList(json.get(KEY_LOG_FILES))) {
            configuration.addContent(attributesFromJson(new Element(LOG_FILE), logFile));
        }
        for (Object logFile : asList(json.get(KEY_PREDEFINED_LOG_FILES))) {
            configuration.addContent(attributesFromJson(new Element(PREDEFINED_LOG_FILE), logFile));
        }
        Object outputFile = json.get(KEY_OUTPUT_FILE);
        if (outputFile != null) {
            configuration.addContent(attributesFromJson(new Element(OUTPUT_FILE), outputFile));
        }

        Object options = json.get(KEY_OPTIONS);
        if (options != null) {
            Element optionsElement = stateFromJson(new Element(OPTIONS), optionsClass, options);
            if (optionsElement.hasAttributes() || optionsElement.getContentSize() > 0) {
                configuration.addContent(optionsElement);
            }
        }

        for (Map.Entry<String, Object> entry : asMap(json.get(KEY_FRAGMENTS)).entrySet()) {
            configuration.addContent(fragmentFromJson(entry.getKey(), entry.getValue()));
        }

        Map<String, Object> selectedOptions = asMap(json.get(KEY_SELECTED_OPTIONS));
        if (!selectedOptions.isEmpty()) {
            Element selectedOptionsElement = new Element(SELECTED_OPTIONS);
            for (Map.Entry<String, Object> entry : selectedOptions.entrySet()) {
                selectedOptionsElement.addContent(new Element(SELECTED_OPTION)
                    .setAttribute(SELECTED_OPTION_NAME, entry.getKey())
                    .setAttribute(SELECTED_OPTION_VISIBLE, String.valueOf(Boolean.TRUE.equals(entry.getValue()))));
            }
            configuration.addContent(selectedOptionsElement);
        }

        Element method = new Element(RunManagerImpl.METHOD).setAttribute(RunManagerImpl.METHOD_VERSION_ATTR, RunManagerImpl.METHOD_VERSION);
        for (Object task : asList(json.get(KEY_BEFORE_RUN))) {
            Element taskElement = taskFromJson(task);
            if (taskElement != null) {
                method.addContent(taskElement);
            }
        }
        configuration.addContent(method);
        return configuration;
    }

    private @Nullable Class<?> findOptionsClass(@Nullable String type, @Nullable String factoryName) {
        if (type == null) {
            return null;
        }

        ConfigurationFactory factory = myManager.getFactory(type, factoryName);
        if (factory == null) {
            return null;
        }

        RunConfiguration template = myManager.getConfigurationTemplate(factory).getConfiguration();
        if (!(template instanceof RunConfigurationBase base)) {
            return null;
        }

        Class<?> optionsClass = base.getOptions().getClass();
        return mySerializer.isJsonCapable(optionsClass) ? optionsClass : null;
    }

    private @Nullable Object fragmentToJson(String id, Element element) throws IOException {
        String raw = element.getAttributeValue(JSON_ATTR);
        if (raw != null) {
            return mySerializer.readTree(raw.getBytes(StandardCharsets.UTF_8));
        }

        Class<?> stateClass = findFragmentStateClass(id);
        return stateClass == null ? null : stateToJson(element, stateClass);
    }

    private Element fragmentFromJson(String id, Object state) throws IOException {
        Element element = new Element(FRAGMENT).setAttribute(FRAGMENT_ID, id);
        Class<?> stateClass = findFragmentStateClass(id);
        if (stateClass == null) {
            return element.setAttribute(JSON_ATTR, new String(mySerializer.writeTree(state), StandardCharsets.UTF_8));
        }
        return stateFromJson(element, stateClass, state);
    }

    private @Nullable Class<?> findFragmentStateClass(String id) {
        RunConfigurationFragment<?> fragment = myManager.getProject().getApplication().getExtensionPoint(RunConfigurationFragment.class)
            .findFirstSafe(it -> id.equals(it.getId()) && it.getStateClass() != null);
        Class<?> stateClass = fragment == null ? null : fragment.getStateClass();
        return stateClass != null && mySerializer.isJsonCapable(stateClass) ? stateClass : null;
    }

    private @Nullable Object taskToJson(Element element) throws IOException {
        String id = element.getAttributeValue(RunManagerImpl.TASK_ID_ATTR);
        if (id == null || !RunManagerImpl.TASK_ELEMENT.equals(element.getName())) {
            return null;
        }

        Object state;
        String raw = element.getAttributeValue(JSON_ATTR);
        if (raw != null) {
            state = mySerializer.readTree(raw.getBytes(StandardCharsets.UTF_8));
        }
        else {
            Class<?> stateClass = findTaskStateClass(id);
            if (stateClass == null) {
                return null;
            }
            state = stateToJson(element, stateClass);
            if (state == null) {
                return null;
            }
        }

        Map<String, Object> task = new LinkedHashMap<>();
        task.put(KEY_ID, id);
        task.put(KEY_STATE, state);
        return task;
    }

    private @Nullable Element taskFromJson(Object json) throws IOException {
        Map<String, Object> task = asMap(json);
        String id = asString(task.get(KEY_ID));
        if (id == null) {
            return null;
        }

        Object state = task.get(KEY_STATE);
        Element element = new Element(RunManagerImpl.TASK_ELEMENT).setAttribute(RunManagerImpl.TASK_ID_ATTR, id);
        Class<?> stateClass = findTaskStateClass(id);
        if (stateClass == null) {
            if (Boolean.FALSE.equals(asMap(state).get("enabled"))) {
                element.setAttribute("enabled", String.valueOf(false));
            }
            return element.setAttribute(JSON_ATTR, new String(mySerializer.writeTree(state == null ? Map.of() : state), StandardCharsets.UTF_8));
        }
        return stateFromJson(element, stateClass, state == null ? Map.of() : state);
    }

    private @Nullable Class<?> findTaskStateClass(String id) {
        Project project = myManager.getProject();
        BeforeRunFragment<?> fragment = BeforeRunFragment.find(project, id);
        Class<?> stateClass = fragment == null ? null : fragment.getStateClass();
        return stateClass != null && mySerializer.isJsonCapable(stateClass) ? stateClass : null;
    }

    private @Nullable Object stateToJson(Element element, Class<?> stateClass) throws IOException {
        Object state = XmlSerializer.deserialize(element, stateClass);
        if (state == null) {
            return null;
        }
        return mySerializer.readTree(mySerializer.serialize(state));
    }

    private Element stateFromJson(Element element, Class<?> stateClass, Object json) throws IOException {
        Object state = mySerializer.deserialize(stateClass, mySerializer.writeTree(json));
        if (state == null) {
            return element.setAttribute(JSON_ATTR, new String(mySerializer.writeTree(json), StandardCharsets.UTF_8));
        }
        XmlSerializer.serializeInto(state, element, new SkipDefaultsSerializationFilter());
        return element;
    }

    private static @Nullable Map<String, Object> attributesToJson(Element element) {
        if (element.getContentSize() > 0) {
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Attribute attribute : element.getAttributes()) {
            result.put(attribute.getName(), attribute.getValue());
        }
        return result;
    }

    private static Element attributesFromJson(Element element, Object json) {
        for (Map.Entry<String, Object> entry : asMap(json).entrySet()) {
            if (entry.getValue() != null) {
                element.setAttribute(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return element;
    }

    private static void putIfNotEmpty(Map<String, Object> json, String key, List<Object> values) {
        if (!values.isEmpty()) {
            json.put(key, values);
        }
    }

    private static void setAttribute(Element element, String name, @Nullable String value) {
        if (value != null) {
            element.setAttribute(name, value);
        }
    }

    private static void setBooleanAttribute(Element element, String name, @Nullable Object value) {
        if (value instanceof Boolean bool) {
            element.setAttribute(name, String.valueOf(bool));
        }
    }

    private static @Nullable String asString(@Nullable Object value) {
        return value instanceof String string ? string : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(@Nullable Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(@Nullable Object value) {
        return value instanceof List<?> list ? (List<Object>) list : List.of();
    }
}
