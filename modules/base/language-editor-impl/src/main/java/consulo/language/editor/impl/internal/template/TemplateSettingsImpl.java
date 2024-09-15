/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.editor.impl.internal.template;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.ProcessCanceledException;
import consulo.component.extension.ExtensionPoint;
import consulo.component.persist.*;
import consulo.component.persist.scheme.BaseSchemeProcessor;
import consulo.component.persist.scheme.SchemeManager;
import consulo.component.persist.scheme.SchemeManagerFactory;
import consulo.component.util.PluginExceptionUtil;
import consulo.component.util.localize.AbstractBundle;
import consulo.container.classloader.PluginClassLoader;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.language.editor.internal.TemplateConstants;
import consulo.language.editor.template.DefaultLiveTemplatesProvider;
import consulo.language.editor.template.LiveTemplateContributor;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateSettings;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.collection.MultiMap;
import consulo.util.collection.SmartList;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.Converter;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.util.xml.serializer.annotation.OptionTag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Singleton
@State(name = "TemplateSettingsImpl", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml", deprecated = true),
    @Storage(file = StoragePathMacros.APP_CONFIG + "/templates.xml")}, additionalExportFile = TemplateSettingsImpl.TEMPLATES_DIR_PATH)
@ServiceImpl
public class TemplateSettingsImpl implements PersistentStateComponent<TemplateSettingsImpl.State>, TemplateSettings {
    private static final Logger LOG = Logger.getInstance(TemplateSettingsImpl.class);

    public static final String USER_GROUP_NAME = "user";
    private static final String TEMPLATE_SET = "templateSet";
    private static final String GROUP = "group";
    private static final String TEMPLATE = "template";

    public static final char SPACE_CHAR = TemplateConstants.SPACE_CHAR;
    public static final char TAB_CHAR = TemplateConstants.TAB_CHAR;
    public static final char ENTER_CHAR = TemplateConstants.ENTER_CHAR;
    public static final char DEFAULT_CHAR = TemplateConstants.DEFAULT_CHAR;
    public static final char CUSTOM_CHAR = TemplateConstants.CUSTOM_CHAR;

    private static final String SPACE = "SPACE";
    private static final String TAB = "TAB";
    private static final String ENTER = "ENTER";
    private static final String CUSTOM = "CUSTOM";

    private static final String NAME = "name";
    private static final String VALUE = "value";
    private static final String DESCRIPTION = "description";
    private static final String SHORTCUT = "shortcut";

    private static final String VARIABLE = "variable";
    private static final String EXPRESSION = "expression";
    private static final String DEFAULT_VALUE = "defaultValue";
    private static final String ALWAYS_STOP_AT = "alwaysStopAt";

    private static final String CONTEXT = "context";
    private static final String TO_REFORMAT = "toReformat";
    private static final String TO_SHORTEN_FQ_NAMES = "toShortenFQNames";
    private static final String USE_STATIC_IMPORT = "useStaticImport";

    private static final String DEACTIVATED = "deactivated";

    private static final String RESOURCE_BUNDLE = "resource-bundle";
    private static final String KEY = "key";
    private static final String ID = "id";

    static final String TEMPLATES_DIR_PATH = StoragePathMacros.ROOT_CONFIG + "/templates";

    private final MultiMap<String, TemplateImpl> myTemplates = MultiMap.createLinked();

    private final Map<String, Template> myTemplatesById = new LinkedHashMap<>();
    private final Map<TemplateKey, TemplateImpl> myDefaultTemplates = new LinkedHashMap<>();

    private int myMaxKeyLength = 0;
    private final SchemeManager<TemplateGroup, TemplateGroup> mySchemeManager;

    private State myState = new State();

    static final class ShortcutConverter extends Converter<Character> {
        @Nullable
        @Override
        public Character fromString(@Nonnull String shortcut) {
            return TAB.equals(shortcut) ? TAB_CHAR : ENTER.equals(shortcut) ? ENTER_CHAR : CUSTOM.equals(shortcut) ? CUSTOM_CHAR : SPACE_CHAR;
        }

        @Nonnull
        @Override
        public String toString(@Nonnull Character shortcut) {
            return shortcut == TAB_CHAR ? TAB : shortcut == ENTER_CHAR ? ENTER : shortcut == CUSTOM_CHAR ? CUSTOM : SPACE;
        }
    }

    final static class State {
        @OptionTag(nameAttribute = "", valueAttribute = "shortcut", converter = ShortcutConverter.class)
        public char defaultShortcut = TAB_CHAR;

        public List<TemplateSettingsImpl.TemplateKey> deletedKeys = new SmartList<>();
    }

    public static class TemplateKey {
        private String groupName;
        private String key;

        @SuppressWarnings("UnusedDeclaration")
        public TemplateKey() {
        }

        private TemplateKey(String groupName, String key) {
            this.groupName = groupName;
            this.key = key;
        }

        public static TemplateKey keyOf(TemplateImpl template) {
            return new TemplateKey(template.getGroupName(), template.getKey());
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TemplateKey that = (TemplateKey) o;
            return Comparing.equal(groupName, that.groupName) && Comparing.equal(key, that.key);
        }

        public int hashCode() {
            int result = groupName != null ? groupName.hashCode() : 0;
            result = 31 * result + (key != null ? key.hashCode() : 0);
            return result;
        }

        public String getGroupName() {
            return groupName;
        }

        @SuppressWarnings("UnusedDeclaration")
        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return getKey() + "@" + getGroupName();
        }
    }

    private TemplateKey myLastSelectedTemplate;

    @Inject
    public TemplateSettingsImpl(Application application, SchemeManagerFactory schemeManagerFactory) {
        mySchemeManager = schemeManagerFactory.createSchemeManager(TEMPLATES_DIR_PATH, new BaseSchemeProcessor<TemplateGroup, TemplateGroup>() {
            @Override
            @Nullable
            public TemplateGroup readScheme(@Nonnull final Document schemeContent) throws InvalidDataException {
                return readTemplateFile(schemeContent, schemeContent.getRootElement().getAttributeValue("group"), false, false, getClass().getClassLoader());
            }


            @Override
            public boolean shouldBeSaved(@Nonnull final TemplateGroup template) {
                for (TemplateImpl t : template.getElements()) {
                    if (differsFromDefault(t)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Element writeScheme(@Nonnull TemplateGroup template) {
                Element templateSetElement = new Element(TEMPLATE_SET);
                templateSetElement.setAttribute(GROUP, template.getName());

                for (TemplateImpl t : template.getElements()) {
                    if (differsFromDefault(t)) {
                        saveTemplate(t, templateSetElement);
                    }
                }

                return templateSetElement;
            }

            @Override
            public void initScheme(@Nonnull final TemplateGroup scheme) {
                for (TemplateImpl template : scheme.getElements()) {
                    addTemplateImpl(template);
                }
            }

            @Override
            public void onSchemeAdded(@Nonnull final TemplateGroup scheme) {
                for (TemplateImpl template : scheme.getElements()) {
                    addTemplateImpl(template);
                }
            }

            @Override
            public void onSchemeDeleted(@Nonnull final TemplateGroup scheme) {
                for (TemplateImpl template : scheme.getElements()) {
                    removeTemplate(template);
                }
            }

            @Nonnull
            @Override
            public String getName(@Nonnull TemplateGroup immutableElement) {
                return immutableElement.getName();
            }
        }, RoamingType.DEFAULT);

        for (TemplateGroup group : mySchemeManager.loadSchemes()) {
            for (TemplateImpl template : group.getElements()) {
                addTemplateImpl(template);
            }
        }

        loadDefaultLiveTemplates(application);
    }

    public static TemplateSettingsImpl getInstanceImpl() {
        return (TemplateSettingsImpl) TemplateSettings.getInstance();
    }

    private boolean differsFromDefault(TemplateImpl t) {
        TemplateImpl def = getDefaultTemplate(t);
        return def == null || !t.equals(def) || !t.contextsEqual(def);
    }

    @Nullable
    public TemplateImpl getDefaultTemplate(TemplateImpl t) {
        return myDefaultTemplates.get(TemplateKey.keyOf(t));
    }

    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        myState = state;

        applyNewDeletedTemplates();
    }

    void applyNewDeletedTemplates() {
        for (TemplateKey templateKey : myState.deletedKeys) {
            if (templateKey.groupName == null) {
                for (TemplateImpl template : new ArrayList<>(myTemplates.get(templateKey.key))) {
                    removeTemplate(template);
                }
            }
            else {
                TemplateImpl toDelete = getTemplate(templateKey.key, templateKey.groupName);
                if (toDelete != null) {
                    removeTemplate(toDelete);
                }
            }
        }
    }

    @Nullable
    public String getLastSelectedTemplateKey() {
        return myLastSelectedTemplate != null ? myLastSelectedTemplate.key : null;
    }

    @Nullable
    public String getLastSelectedTemplateGroup() {
        return myLastSelectedTemplate != null ? myLastSelectedTemplate.groupName : null;
    }

    public void setLastSelectedTemplate(@Nullable String group, @Nullable String key) {
        myLastSelectedTemplate = group == null ? null : new TemplateKey(group, key);
    }

    @Nonnull
    @Override
    public Collection<? extends Template> getTemplates() {
        return Collections.unmodifiableCollection(myTemplates.values());
    }

    @Override
    public char getDefaultShortcutChar() {
        return myState.defaultShortcut;
    }

    public void setDefaultShortcutChar(char defaultShortcutChar) {
        myState.defaultShortcut = defaultShortcutChar;
    }

    public Collection<TemplateImpl> getTemplates(@NonNls String key) {
        return myTemplates.get(key);
    }

    @Nullable
    public TemplateImpl getTemplate(@NonNls String key, String group) {
        final Collection<TemplateImpl> templates = myTemplates.get(key);
        for (TemplateImpl template : templates) {
            if (template.getGroupName().equals(group)) {
                return template;
            }
        }
        return null;
    }

    public Template getTemplateById(@Nonnull String id) {
        return myTemplatesById.get(id);
    }

    public int getMaxKeyLength() {
        return myMaxKeyLength;
    }

    public void addTemplate(Template template) {
        clearPreviouslyRegistered(template);
        addTemplateImpl(template);

        TemplateImpl templateImpl = (TemplateImpl) template;
        String groupName = templateImpl.getGroupName();
        TemplateGroup group = mySchemeManager.findSchemeByName(groupName);
        if (group == null) {
            group = new TemplateGroup(groupName);
            mySchemeManager.addNewScheme(group, true);
        }
        group.addElement(templateImpl);
    }

    private void clearPreviouslyRegistered(final Template template) {
        TemplateImpl existing = getTemplate(template.getKey(), ((TemplateImpl) template).getGroupName());
        if (existing != null) {
            LOG.info("Template with key " + template.getKey() + " and id " + template.getId() + " already registered");
            TemplateGroup group = mySchemeManager.findSchemeByName(existing.getGroupName());
            if (group != null) {
                group.removeElement(existing);
                if (group.isEmpty()) {
                    mySchemeManager.removeScheme(group);
                }
            }
            myTemplates.remove(template.getKey(), existing);
        }
    }

    private void addTemplateImpl(@Nonnull Template template) {
        TemplateImpl templateImpl = (TemplateImpl) template;
        if (getTemplate(templateImpl.getKey(), templateImpl.getGroupName()) == null) {
            myTemplates.putValue(template.getKey(), templateImpl);
        }

        myMaxKeyLength = Math.max(myMaxKeyLength, template.getKey().length());
        myState.deletedKeys.remove(TemplateKey.keyOf((TemplateImpl) template));
    }

    private void addTemplateById(Template template) {
        if (!myTemplatesById.containsKey(template.getId())) {
            final String id = template.getId();
            if (id != null) {
                myTemplatesById.put(id, template);
            }
        }
    }

    public void removeTemplate(@Nonnull Template template) {
        myTemplates.remove(template.getKey(), (TemplateImpl) template);

        TemplateGroup group = mySchemeManager.findSchemeByName(((TemplateImpl) template).getGroupName());
        if (group != null) {
            group.removeElement((TemplateImpl) template);
            if (group.isEmpty()) {
                mySchemeManager.removeScheme(group);
            }
        }
    }

    private TemplateImpl addTemplate(String key,
                                     String string,
                                     String group,
                                     String description,
                                     @Nullable String shortcut,
                                     boolean isDefault,
                                     final String id) {
        TemplateImpl template = new TemplateImpl(key, string, group);
        template.setId(id);
        template.setDescription(description);
        if (TAB.equals(shortcut)) {
            template.setShortcutChar(TAB_CHAR);
        }
        else if (ENTER.equals(shortcut)) {
            template.setShortcutChar(ENTER_CHAR);
        }
        else if (SPACE.equals(shortcut)) {
            template.setShortcutChar(SPACE_CHAR);
        }
        else {
            template.setShortcutChar(DEFAULT_CHAR);
        }
        if (isDefault) {
            myDefaultTemplates.put(TemplateKey.keyOf(template), template);
        }
        return template;
    }

    private void loadDefaultLiveTemplates(Application application) {
        ExtensionPoint<TemplateContextType> templateContextTypes = application.getExtensionPoint(TemplateContextType.class);

        application.getExtensionPoint(LiveTemplateContributor.class).forEachExtensionSafe(contributor -> {
            String groupId = contributor.groupId();
            LocalizeValue groupName = contributor.groupName();

            TemplateGroup result = new TemplateGroup(groupName.get());   // TODO [VISTALL] support localize key

            Map<String, TemplateImpl> created = new LinkedHashMap<>();

            contributor.contribute((id, abbreviation, value, description) -> {
                LiveTemplateContributorBuilder builder = new LiveTemplateContributorBuilder(groupId,
                    groupName,
                    id,
                    abbreviation,
                    value,
                    description) {
                    @Override
                    public void close() {
                        TemplateImpl template = registerTemplate(this, groupId, groupName);

                        for (Map.Entry<Class<? extends TemplateContextType>, Boolean> entry : myStrictContextTypes.entrySet()) {
                            Class<? extends TemplateContextType> strictContext = entry.getKey();
                            Boolean state = entry.getValue();

                            TemplateContextType extension = templateContextTypes.findExtension(strictContext);
                            if (extension == null) {
                                PluginExceptionUtil.logPluginError(LOG, "Can't find " + strictContext + " extension for live template " + id,
                                    null,
                                    contributor.getClass()
                                );
                                return;
                            }

                            template.getTemplateContext().setEnabled(extension, state);
                        }

                        for (Map.Entry<Class<? extends TemplateContextType>, Boolean> entry : myContextTypes.entrySet()) {
                            Class<? extends TemplateContextType> contextTypeClass = entry.getKey();
                            Boolean state = entry.getValue();

                            templateContextTypes.forEachExtensionSafe(it -> {
                                if (contextTypeClass.isInstance(it)) {
                                    template.getTemplateContext().setEnabled(it, state);
                                }
                            });
                        }

                        TemplateImpl existing = getTemplate(template.getKey(), template.getGroupName());
                        boolean defaultTemplateModified = myState.deletedKeys.contains(TemplateKey.keyOf(template))
                            || myTemplatesById.containsKey(template.getId())
                            || existing != null;

                        if (!defaultTemplateModified) {
                            created.put(template.getKey(), template);
                        }

                        if (existing != null) {
                            existing.getTemplateContext().setDefaultContext(template.getTemplateContext());
                        }
                    }
                };
                return builder;
            });

            TemplateGroup existingScheme = mySchemeManager.findSchemeByName(result.getName());
            if (existingScheme != null) {
                result = existingScheme;
            }

            for (TemplateImpl template : created.values()) {
                clearPreviouslyRegistered(template);

                addTemplateImpl(template);

                addTemplateById(template);

                result.addElement(template);
            }

            if (existingScheme == null && !result.isEmpty()) {
                mySchemeManager.addNewScheme(result, false);
            }
        });

        application.getExtensionPoint(DefaultLiveTemplatesProvider.class).forEachExtensionSafe(provider -> {
            try {
                for (String defTemplate : provider.getDefaultLiveTemplateFiles()) {
                    readDefTemplate(provider, defTemplate, true);
                }

                String[] hidden = provider.getHiddenLiveTemplateFiles();
                if (hidden != null) {
                    for (String s : hidden) {
                        readDefTemplate(provider, s, false);
                    }
                }
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Exception e) {
                LOG.error(e);
            }
        });
    }

    private TemplateImpl registerTemplate(LiveTemplateContributorBuilder builder, String groupId, LocalizeValue groupName) {
        String groupNameText = groupName.getValue();   // TODO [VISTALL] support localize key
        TemplateImpl template = new TemplateImpl(builder.getAbbreviation(), builder.getValue(), groupNameText);
        template.setId(builder.getId());
        template.setDescription(builder.getDescription().get());  // TODO [VISTALL] support localize key
        template.setShortcutChar(builder.myShortcut);

        for (LiveTemplateContributorBuilder.Variable variable : builder.getVariables()) {
            template.addVariable(variable.name(), variable.expression(), variable.defaultValue(), variable.alwaysStopAt());
        }

        myDefaultTemplates.put(TemplateKey.keyOf(template), template);

        return template;
    }

    private void readDefTemplate(DefaultLiveTemplatesProvider provider, String defTemplate, boolean registerTemplate) throws JDOMException, InvalidDataException, IOException {
        String xmlFilePath = defTemplate;
        if (!xmlFilePath.endsWith(".xml")) {
            xmlFilePath = xmlFilePath + ".xml";
        }

        InputStream inputStream;
        try {
            inputStream = provider.getClass().getResourceAsStream(xmlFilePath);
        }
        catch (Exception e) {
            LOG.warn(e);
            return;
        }

        if (inputStream == null) {
            PluginDescriptor plugin = PluginManager.getPlugin(provider.getClass());
            LOG.error("Template by path " + xmlFilePath + " not found in plugin: " + plugin.getPluginClassLoader());
            return;
        }

        TemplateGroup group = readTemplateFile(JDOMUtil.loadDocument(inputStream), defTemplate, true, registerTemplate, provider.getClass().getClassLoader());
        if (group != null && group.getReplace() != null) {
            Collection<TemplateImpl> templates = myTemplates.get(group.getReplace());
            for (TemplateImpl template : templates) {
                removeTemplate(template);
            }
        }
    }

    private static String getDefaultTemplateName(String defTemplate) {
        return defTemplate.substring(defTemplate.lastIndexOf('/') + 1);
    }

    @Nullable
    private TemplateGroup readTemplateFile(Document document, @NonNls String path, boolean isDefault, boolean registerTemplate, ClassLoader classLoader) throws InvalidDataException {
        if (document == null) {
            throw new InvalidDataException();
        }
        Element root = document.getRootElement();
        if (root == null || !TEMPLATE_SET.equals(root.getName())) {
            throw new InvalidDataException();
        }

        String groupName = root.getAttributeValue(GROUP);
        if (StringUtil.isEmpty(groupName)) {
            groupName = path.substring(path.lastIndexOf("/") + 1);
            LOG.warn("Group attribute is empty. Path '" + path + "'. Plugin: " + ((PluginClassLoader) classLoader).getPluginId());
        }

        TemplateGroup result = new TemplateGroup(groupName, root.getAttributeValue("REPLACE"));

        Map<String, TemplateImpl> created = new LinkedHashMap<>();

        for (final Element element : root.getChildren(TEMPLATE)) {
            TemplateImpl template = readTemplateFromElement(isDefault, groupName, element, classLoader);
            TemplateImpl existing = getTemplate(template.getKey(), template.getGroupName());
            boolean defaultTemplateModified = isDefault && (myState.deletedKeys.contains(TemplateKey.keyOf(template)) || myTemplatesById.containsKey(template.getId()) || existing != null);

            if (!defaultTemplateModified) {
                created.put(template.getKey(), template);
            }
            if (isDefault && existing != null) {
                existing.getTemplateContext().setDefaultContext(template.getTemplateContext());
            }
        }

        if (registerTemplate) {
            TemplateGroup existingScheme = mySchemeManager.findSchemeByName(result.getName());
            if (existingScheme != null) {
                result = existingScheme;
            }
        }

        for (TemplateImpl template : created.values()) {
            if (registerTemplate) {
                clearPreviouslyRegistered(template);
                addTemplateImpl(template);
            }
            addTemplateById(template);

            result.addElement(template);
        }

        if (registerTemplate) {
            TemplateGroup existingScheme = mySchemeManager.findSchemeByName(result.getName());
            if (existingScheme == null && !result.isEmpty()) {
                mySchemeManager.addNewScheme(result, false);
            }
        }

        return result.isEmpty() ? null : result;

    }

    private TemplateImpl readTemplateFromElement(final boolean isDefault, final String groupName, final Element element, ClassLoader classLoader) throws InvalidDataException {
        String name = element.getAttributeValue(NAME);
        String value = element.getAttributeValue(VALUE);
        String description;
        String resourceBundle = element.getAttributeValue(RESOURCE_BUNDLE);
        String key = element.getAttributeValue(KEY);
        String id = element.getAttributeValue(ID);
        if (resourceBundle != null && key != null) {
            if (classLoader == null) {
                classLoader = getClass().getClassLoader();
            }
            ResourceBundle bundle;
            try {
                bundle = AbstractBundle.getResourceBundle(resourceBundle, classLoader);
                description = bundle.getString(key);
            }
            catch (MissingResourceException e) {
                description = resourceBundle + "@" + key;
            }
        }
        else {
            description = element.getAttributeValue(DESCRIPTION);
        }
        String shortcut = element.getAttributeValue(SHORTCUT);
        TemplateImpl template = addTemplate(name, value, groupName, description, shortcut, isDefault, id);

        template.setToReformat(Boolean.parseBoolean(element.getAttributeValue(TO_REFORMAT)));
        template.setToShortenLongNames(Boolean.parseBoolean(element.getAttributeValue(TO_SHORTEN_FQ_NAMES)));
        template.setDeactivated(Boolean.parseBoolean(element.getAttributeValue(DEACTIVATED)));

        String useStaticImport = element.getAttributeValue(USE_STATIC_IMPORT);
        if (useStaticImport != null) {
            template.setValue(TemplateImpl.Property.USE_STATIC_IMPORT_IF_POSSIBLE, Boolean.parseBoolean(useStaticImport));
        }

        for (final Element e : element.getChildren(VARIABLE)) {
            String variableName = e.getAttributeValue(NAME);
            String expression = e.getAttributeValue(EXPRESSION);
            String defaultValue = e.getAttributeValue(DEFAULT_VALUE);
            boolean isAlwaysStopAt = Boolean.parseBoolean(e.getAttributeValue(ALWAYS_STOP_AT));
            template.addVariable(variableName, expression, defaultValue, isAlwaysStopAt);
        }

        Element context = element.getChild(CONTEXT);
        if (context != null) {
            template.getTemplateContext().readTemplateContext(context);
        }

        return template;
    }

    private void saveTemplate(TemplateImpl template, Element templateSetElement) {
        Element element = new Element(TEMPLATE);
        final String id = template.getId();
        if (id != null) {
            element.setAttribute(ID, id);
        }
        element.setAttribute(NAME, template.getKey());
        element.setAttribute(VALUE, template.getString());
        if (template.getShortcutChar() == TAB_CHAR) {
            element.setAttribute(SHORTCUT, TAB);
        }
        else if (template.getShortcutChar() == ENTER_CHAR) {
            element.setAttribute(SHORTCUT, ENTER);
        }
        else if (template.getShortcutChar() == SPACE_CHAR) {
            element.setAttribute(SHORTCUT, SPACE);
        }
        if (template.getDescription() != null) {
            element.setAttribute(DESCRIPTION, template.getDescription());
        }
        element.setAttribute(TO_REFORMAT, Boolean.toString(template.isToReformat()));
        element.setAttribute(TO_SHORTEN_FQ_NAMES, Boolean.toString(template.isToShortenLongNames()));
        if (template.getValue(Template.Property.USE_STATIC_IMPORT_IF_POSSIBLE) != Template.getDefaultValue(Template.Property.USE_STATIC_IMPORT_IF_POSSIBLE)) {
            element.setAttribute(USE_STATIC_IMPORT, Boolean.toString(template.getValue(Template.Property.USE_STATIC_IMPORT_IF_POSSIBLE)));
        }
        if (template.isDeactivated()) {
            element.setAttribute(DEACTIVATED, Boolean.toString(true));
        }

        for (int i = 0; i < template.getVariableCount(); i++) {
            Element variableElement = new Element(VARIABLE);
            variableElement.setAttribute(NAME, template.getVariableNameAt(i));
            variableElement.setAttribute(EXPRESSION, template.getExpressionStringAt(i));
            variableElement.setAttribute(DEFAULT_VALUE, template.getDefaultValueStringAt(i));
            variableElement.setAttribute(ALWAYS_STOP_AT, Boolean.toString(template.isAlwaysStopAt(i)));
            element.addContent(variableElement);
        }

        try {
            Element contextElement = new Element(CONTEXT);
            TemplateImpl def = getDefaultTemplate(template);
            template.getTemplateContext().writeTemplateContext(contextElement, def == null ? null : def.getTemplateContext());
            element.addContent(contextElement);
        }
        catch (WriteExternalException ignore) {
        }
        templateSetElement.addContent(element);
    }

    public void setTemplates(@Nonnull List<TemplateGroup> newGroups) {
        myTemplates.clear();
        myState.deletedKeys.clear();
        for (TemplateImpl template : myDefaultTemplates.values()) {
            myState.deletedKeys.add(TemplateKey.keyOf(template));
        }
        mySchemeManager.clearAllSchemes();
        myMaxKeyLength = 0;
        for (TemplateGroup group : newGroups) {
            if (!group.isEmpty()) {
                mySchemeManager.addNewScheme(group, true);
                for (TemplateImpl template : group.getElements()) {
                    clearPreviouslyRegistered(template);
                    addTemplateImpl(template);
                }
            }
        }
    }

    public SchemeManager<TemplateGroup, TemplateGroup> getSchemeManager() {
        return mySchemeManager;
    }

    public List<TemplateGroup> getTemplateGroups() {
        return mySchemeManager.getAllSchemes();
    }

    public List<TemplateImpl> collectMatchingCandidates(String key, @Nullable Character shortcutChar, boolean hasArgument) {
        final Collection<TemplateImpl> templates = getTemplates(key);
        List<TemplateImpl> candidates = new ArrayList<>();
        for (TemplateImpl template : templates) {
            if (template.isDeactivated()) {
                continue;
            }
            if (shortcutChar != null && getShortcutChar(template) != shortcutChar) {
                continue;
            }
            if (hasArgument && !template.hasArgument()) {
                continue;
            }
            candidates.add(template);
        }
        return candidates;
    }

    @Override
    public char getShortcutChar(Template template) {
        char c = template.getShortcutChar();
        return c == DEFAULT_CHAR ? getDefaultShortcutChar() : c;
    }

    public List<TemplateKey> getDeletedTemplates() {
        return myState.deletedKeys;
    }

    public void reset(Application application) {
        myState.deletedKeys.clear();
        loadDefaultLiveTemplates(application);
    }
}
