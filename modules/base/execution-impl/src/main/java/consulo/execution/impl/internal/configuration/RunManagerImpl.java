/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.annotation.component.ServiceImpl;
import consulo.application.dumb.IndexNotReadyException;
import consulo.component.ProcessCanceledException;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.execution.*;
import consulo.execution.configuration.*;
import consulo.execution.event.RunManagerListener;
import consulo.execution.event.RunManagerListenerEvent;
import consulo.execution.executor.Executor;
import consulo.execution.impl.internal.RunConfigurationBeforeRunProvider;
import consulo.execution.impl.internal.UnknownBeforeRunTaskProvider;
import consulo.execution.internal.RunManagerConfig;
import consulo.execution.internal.RunManagerEx;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.logging.Logger;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.project.internal.UnknownFeaturesCollector;
import consulo.ui.ex.IconDeferrer;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.collection.SmartList;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizableStringList;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

@Singleton
@ServiceImpl
@State(name = "RunManager", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
public class RunManagerImpl extends RunManagerEx implements PersistentStateComponent<Element>, Disposable {
    private static final Logger LOG = Logger.getInstance(RunManagerImpl.class);

    protected static final String CONFIGURATION = "configuration";
    protected static final String RECENT = "recent_temporary";
    protected static final String NAME_ATTR = "name";
    protected static final String SELECTED_ATTR = "selected";
    private static final String METHOD = "method";
    private static final String OPTION = "option";

    private final Project myProject;

    private final Map<String, RunnerAndConfigurationSettings> myTemplateConfigurationsMap = new ConcurrentSkipListMap<>();
    private final Map<String, RunnerAndConfigurationSettings> myConfigurations = new LinkedHashMap<>();
    // template configurations are not included here
    private final Map<String, Boolean> mySharedConfigurations = new HashMap<>();
    private final Map<RunConfiguration, List<BeforeRunTask>> myConfigurationToBeforeTasksMap = ContainerUtil.createWeakMap();

    // When readExternal not all configuration may be loaded, so we need to remember the selected configuration
    // so that when it is eventually loaded, we can mark is as a selected.
    @Nullable
    private String myLoadedSelectedConfigurationUniqueName = null;
    @Nullable
    private String mySelectedConfigurationId = null;

    private final Map<String, Image> myIdToIcon = new HashMap<>();
    private final Map<String, Long> myIconCheckTimes = new HashMap<>();
    private final Map<String, Long> myIconCalcTime = Collections.synchronizedMap(new HashMap<String, Long>());

    private final RunManagerConfig myConfig;

    private List<Element> myUnknownElements = null;
    private final JDOMExternalizableStringList myOrder = new JDOMExternalizableStringList();
    private final ArrayList<RunConfiguration> myRecentlyUsedTemporaries = new ArrayList<>();
    private boolean myOrdered = true;

    @Inject
    public RunManagerImpl(@Nonnull Project project, @Nonnull ProjectPropertiesComponent propertiesComponent) {
        myConfig = new RunManagerConfig(propertiesComponent);
        myProject = project;

        myProject.getMessageBus().connect(myProject).subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                RunnerAndConfigurationSettings configuration = getSelectedConfiguration();
                if (configuration != null) {
                    myIconCheckTimes.remove(configuration.getUniqueID());//cache will be expired
                }
            }
        });
    }

    @Nonnull
    private ConfigurationTypeCache typeCache() {
        return myProject.getApplication().getExtensionPoint(ConfigurationType.class).getOrBuildCache(ConfigurationTypeCache.CACHE_KEY);
    }

    @Override
    @Nonnull
    public RunnerAndConfigurationSettings createConfiguration(@Nonnull String name, @Nonnull ConfigurationFactory factory) {
        return createConfiguration(doCreateConfiguration(name, factory, true), factory);
    }

    @Override
    public RunnerAndConfigurationSettings createConfiguration(@Nonnull RunConfiguration configuration, boolean isTemplate) {
        return new RunnerAndConfigurationSettingsImpl(this, configuration, isTemplate);
    }

    protected RunConfiguration doCreateConfiguration(
        @Nonnull String name,
        @Nonnull ConfigurationFactory factory,
        boolean fromTemplate
    ) {
        if (fromTemplate) {
            return factory.createConfiguration(name, getConfigurationTemplate(factory).getConfiguration());
        }
        else {
            RunConfiguration configuration = factory.createTemplateConfiguration(myProject, this);
            configuration.setName(name);
            return configuration;
        }
    }

    @Override
    @Nonnull
    public RunnerAndConfigurationSettings createConfiguration(
        @Nonnull RunConfiguration runConfiguration,
        @Nonnull ConfigurationFactory factory
    ) {
        RunnerAndConfigurationSettings template = getConfigurationTemplate(factory);
        RunnerAndConfigurationSettingsImpl settings = new RunnerAndConfigurationSettingsImpl(this, runConfiguration, false);
        settings.importRunnerAndConfigurationSettings((RunnerAndConfigurationSettingsImpl)template);
        if (!mySharedConfigurations.containsKey(settings.getUniqueID())) {
            shareConfiguration(settings, isConfigurationShared(template));
        }
        return settings;
    }

    @Override
    public void dispose() {
        myTemplateConfigurationsMap.clear();
    }

    @Override
    public RunManagerConfig getConfig() {
        return myConfig;
    }

    @Nonnull
    @Override
    public List<ConfigurationType> getConfigurationFactories(boolean includeUnknown) {
        if (includeUnknown) {
            return typeCache().getTypes();
        }

        List<ConfigurationType> types = new ArrayList<>();
        for (ConfigurationType configurationType : typeCache().getTypes()) {
            if (!(configurationType instanceof UnknownConfigurationType)) {
                types.add(configurationType);
            }
        }

        return types;
    }

    /**
     * Template configuration is not included
     */
    @Override
    @Nonnull
    public List<RunConfiguration> getConfigurationsList(@Nonnull ConfigurationType type) {
        List<RunConfiguration> result = null;
        for (RunnerAndConfigurationSettings settings : getSortedConfigurations()) {
            RunConfiguration configuration = settings.getConfiguration();
            if (type.getId().equals(configuration.getType().getId())) {
                if (result == null) {
                    result = new SmartList<>();
                }
                result.add(configuration);
            }
        }
        return Lists.notNullize(result);
    }

    @Override
    @Nonnull
    public List<RunConfiguration> getAllConfigurationsList() {
        Collection<RunnerAndConfigurationSettings> sortedConfigurations = getSortedConfigurations();
        if (sortedConfigurations.isEmpty()) {
            return Collections.emptyList();
        }

        List<RunConfiguration> result = new ArrayList<>(sortedConfigurations.size());
        for (RunnerAndConfigurationSettings settings : sortedConfigurations) {
            result.add(settings.getConfiguration());
        }
        return result;
    }

    @Nonnull
    @Override
    public RunConfiguration[] getAllConfigurations() {
        List<RunConfiguration> list = getAllConfigurationsList();
        return list.toArray(new RunConfiguration[list.size()]);
    }

    @Nonnull
    @Override
    public List<RunnerAndConfigurationSettings> getAllSettings() {
        return new ArrayList<>(getSortedConfigurations());
    }

    @Nullable
    public RunnerAndConfigurationSettings getSettings(@Nullable RunConfiguration configuration) {
        if (configuration == null) {
            return null;
        }

        for (RunnerAndConfigurationSettings settings : getSortedConfigurations()) {
            if (settings.getConfiguration() == configuration) {
                return settings;
            }
        }
        return null;
    }

    /**
     * Template configuration is not included
     */
    @Override
    @Nonnull
    public List<RunnerAndConfigurationSettings> getConfigurationSettingsList(@Nonnull ConfigurationType type) {
        List<RunnerAndConfigurationSettings> result = new SmartList<>();
        for (RunnerAndConfigurationSettings configuration : getSortedConfigurations()) {
            ConfigurationType configurationType = configuration.getType();
            if (configurationType != null && type.getId().equals(configurationType.getId())) {
                result.add(configuration);
            }
        }
        return result;
    }

    @Nonnull
    @Override
    public RunnerAndConfigurationSettings[] getConfigurationSettings(@Nonnull ConfigurationType type) {
        List<RunnerAndConfigurationSettings> list = getConfigurationSettingsList(type);
        return list.toArray(new RunnerAndConfigurationSettings[list.size()]);
    }

    @Nonnull
    @Override
    public RunConfiguration[] getConfigurations(@Nonnull ConfigurationType type) {
        RunnerAndConfigurationSettings[] settings = getConfigurationSettings(type);
        RunConfiguration[] result = new RunConfiguration[settings.length];
        for (int i = 0; i < settings.length; i++) {
            result[i] = settings[i].getConfiguration();
        }
        return result;
    }

    @Nonnull
    @Override
    public Map<String, List<RunnerAndConfigurationSettings>> getStructure(@Nonnull ConfigurationType type) {
        LinkedHashMap<String, List<RunnerAndConfigurationSettings>> map = new LinkedHashMap<>();
        List<RunnerAndConfigurationSettings> typeList = new ArrayList<>();
        List<RunnerAndConfigurationSettings> settings = getConfigurationSettingsList(type);
        for (RunnerAndConfigurationSettings setting : settings) {
            String folderName = setting.getFolderName();
            if (folderName == null) {
                typeList.add(setting);
            }
            else {
                List<RunnerAndConfigurationSettings> list = map.get(folderName);
                if (list == null) {
                    map.put(folderName, list = new ArrayList<>());
                }
                list.add(setting);
            }
        }
        LinkedHashMap<String, List<RunnerAndConfigurationSettings>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<RunnerAndConfigurationSettings>> entry : map.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        result.put(null, Collections.unmodifiableList(typeList));
        return Collections.unmodifiableMap(result);
    }

    @Override
    @Nonnull
    public RunnerAndConfigurationSettings getConfigurationTemplate(@Nonnull ConfigurationFactory factory) {
        RunnerAndConfigurationSettings template = myTemplateConfigurationsMap.get(factory.getType().getId() + "." + factory.getId());
        if (template == null) {
            template = new RunnerAndConfigurationSettingsImpl(this, factory.createTemplateConfiguration(myProject, this), true);
            template.setSingleton(factory.isConfigurationSingletonByDefault());
            if (template.getConfiguration() instanceof UnknownRunConfiguration unknownRunConfiguration) {
                unknownRunConfiguration.setDoNotStore(true);
            }
            myTemplateConfigurationsMap.put(factory.getType().getId() + "." + factory.getId(), template);
        }
        return template;
    }

    @Override
    public void addConfiguration(
        RunnerAndConfigurationSettings settings,
        boolean shared,
        List<BeforeRunTask> tasks,
        boolean addEnabledTemplateTasksIfAbsent
    ) {
        String existingId = findExistingConfigurationId(settings);
        String newId = settings.getUniqueID();
        RunnerAndConfigurationSettings existingSettings = null;

        if (existingId != null) {
            existingSettings = myConfigurations.remove(existingId);
            mySharedConfigurations.remove(existingId);
        }

        if (mySelectedConfigurationId != null && mySelectedConfigurationId.equals(existingId)) {
            setSelectedConfigurationId(newId);
        }
        myConfigurations.put(newId, settings);

        RunConfiguration configuration = settings.getConfiguration();
        if (existingId == null) {
            refreshUsagesList(configuration);
        }
        checkRecentsLimit();

        mySharedConfigurations.put(newId, shared);
        setBeforeRunTasks(configuration, tasks, addEnabledTemplateTasksIfAbsent);

        RunManagerListenerEvent event = new RunManagerListenerEvent(this, settings, existingId);
        if (existingSettings == settings) {
            getEventPublisher().runConfigurationChanged(event);
        }
        else {
            getEventPublisher().runConfigurationAdded(event);
        }
    }

    @Override
    public void refreshUsagesList(RunProfile profile) {
        if (!(profile instanceof RunConfiguration runConfiguration)) {
            return;
        }
        RunnerAndConfigurationSettings settings = getSettings(runConfiguration);
        if (settings != null && settings.isTemporary()) {
            myRecentlyUsedTemporaries.remove(runConfiguration);
            myRecentlyUsedTemporaries.add(0, runConfiguration);
            trimUsagesListToLimit();
        }
    }

    @Override
    public boolean hasSettings(RunnerAndConfigurationSettings settings) {
        return myConfigurations.get(settings.getUniqueID()) != null;
    }

    private void trimUsagesListToLimit() {
        while (myRecentlyUsedTemporaries.size() > getConfig().getRecentsLimit()) {
            myRecentlyUsedTemporaries.remove(myRecentlyUsedTemporaries.size() - 1);
        }
    }

    public void checkRecentsLimit() {
        trimUsagesListToLimit();
        List<RunnerAndConfigurationSettings> removed = new SmartList<>();
        while (getTempConfigurationsList().size() > getConfig().getRecentsLimit()) {
            for (Iterator<RunnerAndConfigurationSettings> it = myConfigurations.values().iterator(); it.hasNext(); ) {
                RunnerAndConfigurationSettings configuration = it.next();
                if (configuration.isTemporary() && !myRecentlyUsedTemporaries.contains(configuration.getConfiguration())) {
                    removed.add(configuration);
                    it.remove();
                    break;
                }
            }
        }
        fireRunConfigurationsRemoved(removed);
    }

    public void setOrdered(boolean ordered) {
        myOrdered = ordered;
    }

    public void saveOrder() {
        setOrder(null);
    }

    private void doSaveOrder(@Nullable Comparator<RunnerAndConfigurationSettings> comparator) {
        List<RunnerAndConfigurationSettings> sorted =
            new ArrayList<>(ContainerUtil.filter(myConfigurations.values(), o -> !(o.getType() instanceof UnknownConfigurationType)));
        if (comparator != null) {
            sorted.sort(comparator);
        }

        myOrder.clear();
        for (RunnerAndConfigurationSettings each : sorted) {
            myOrder.add(each.getUniqueID());
        }
    }

    public void setOrder(@Nullable Comparator<RunnerAndConfigurationSettings> comparator) {
        doSaveOrder(comparator);
        setOrdered(false);// force recache of configurations list
    }

    @Override
    public void removeConfiguration(@Nullable RunnerAndConfigurationSettings settings) {
        if (settings == null) {
            return;
        }

        for (Iterator<RunnerAndConfigurationSettings> it = getSortedConfigurations().iterator(); it.hasNext(); ) {
            RunnerAndConfigurationSettings configuration = it.next();
            if (configuration.equals(settings)) {
                if (mySelectedConfigurationId != null && mySelectedConfigurationId.equals(settings.getUniqueID())) {
                    setSelectedConfiguration(null);
                }

                it.remove();
                mySharedConfigurations.remove(settings.getUniqueID());
                myConfigurationToBeforeTasksMap.remove(settings.getConfiguration());
                myRecentlyUsedTemporaries.remove(settings.getConfiguration());
                getEventPublisher().runConfigurationRemoved(new RunManagerListenerEvent(this, configuration, null));
                break;
            }
        }
        for (Map.Entry<RunConfiguration, List<BeforeRunTask>> entry : myConfigurationToBeforeTasksMap.entrySet()) {
            for (Iterator<BeforeRunTask> iterator = entry.getValue().iterator(); iterator.hasNext(); ) {
                BeforeRunTask task = iterator.next();
                if (task instanceof RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask runConfigurableBeforeRunTask
                    && settings.equals(runConfigurableBeforeRunTask.getSettings())) {
                    iterator.remove();
                    RunnerAndConfigurationSettings changedSettings = getSettings(entry.getKey());
                    if (changedSettings != null) {
                        getEventPublisher().runConfigurationChanged(new RunManagerListenerEvent(this, changedSettings, null));
                    }
                }
            }
        }
    }

    @Override
    @Nullable
    public RunnerAndConfigurationSettings getSelectedConfiguration() {
        if (mySelectedConfigurationId == null && myLoadedSelectedConfigurationUniqueName != null) {
            setSelectedConfigurationId(myLoadedSelectedConfigurationUniqueName);
        }
        return mySelectedConfigurationId == null ? null : myConfigurations.get(mySelectedConfigurationId);
    }

    @Override
    public void setSelectedConfiguration(@Nullable RunnerAndConfigurationSettings settings) {
        setSelectedConfigurationId(settings == null ? null : settings.getUniqueID());
        fireRunConfigurationSelected();
    }

    private void setSelectedConfigurationId(@Nullable String id) {
        mySelectedConfigurationId = id;
        if (mySelectedConfigurationId != null) {
            myLoadedSelectedConfigurationUniqueName = null;
        }
    }

    @Nullable
    @Override
    public RunnerAndConfigurationSettings findSettings(RunConfiguration configuration) {
        for (RunnerAndConfigurationSettings settings : getSortedConfigurations()) {
            if (Objects.equals(settings.getConfiguration(), configuration)) {
                return settings;
            }
        }
        return null;
    }

    @Override
    @Nonnull
    public Collection<RunnerAndConfigurationSettings> getSortedConfigurations() {
        if (myOrdered) {
            return myConfigurations.values();
        }

        List<Pair<String, RunnerAndConfigurationSettings>> order = new ArrayList<>(myConfigurations.size());
        List<String> folderNames = new SmartList<>();
        for (RunnerAndConfigurationSettings each : myConfigurations.values()) {
            order.add(Pair.create(each.getUniqueID(), each));
            String folderName = each.getFolderName();
            if (folderName != null && !folderNames.contains(folderName)) {
                folderNames.add(folderName);
            }
        }
        folderNames.add(null);
        myConfigurations.clear();

        if (myOrder.isEmpty()) {
            // IDEA-63663 Sort run configurations alphabetically if clean checkout
            Collections.sort(order, (o1, o2) -> {
                boolean temporary1 = o1.getSecond().isTemporary();
                boolean temporary2 = o2.getSecond().isTemporary();
                if (temporary1 == temporary2) {
                    return o1.first.compareTo(o2.first);
                }
                else {
                    return temporary1 ? 1 : -1;
                }
            });
        }
        else {
            Collections.sort(order, (o1, o2) -> {
                int i1 = folderNames.indexOf(o1.getSecond().getFolderName());
                int i2 = folderNames.indexOf(o2.getSecond().getFolderName());
                if (i1 != i2) {
                    return i1 - i2;
                }
                boolean temporary1 = o1.getSecond().isTemporary();
                boolean temporary2 = o2.getSecond().isTemporary();
                if (temporary1 == temporary2) {
                    int index1 = myOrder.indexOf(o1.first);
                    int index2 = myOrder.indexOf(o2.first);
                    if (index1 == -1 && index2 == -1) {
                        return o1.second.getName().compareTo(o2.second.getName());
                    }
                    return index1 - index2;
                }
                else {
                    return temporary1 ? 1 : -1;
                }
            });
        }

        for (Pair<String, RunnerAndConfigurationSettings> each : order) {
            RunnerAndConfigurationSettings setting = each.second;
            myConfigurations.put(setting.getUniqueID(), setting);
        }

        myOrdered = true;
        return myConfigurations.values();
    }

    public boolean canRunConfiguration(@Nonnull ExecutionEnvironment environment) {
        RunnerAndConfigurationSettings runnerAndConfigurationSettings = environment.getRunnerAndConfigurationSettings();
        return runnerAndConfigurationSettings != null && canRunConfiguration(runnerAndConfigurationSettings, environment.getExecutor());
    }

    public static boolean canRunConfiguration(@Nonnull RunnerAndConfigurationSettings configuration, @Nonnull Executor executor) {
        try {
            configuration.checkSettings(executor);
        }
        catch (IndexNotReadyException | RuntimeConfigurationException ignored) {
            return false;
        }
        return true;
    }

    @Nullable
    @Override
    public Element getState() {
        Element parentNode = new Element("state");
        // writes temporary configurations here
        writeContext(parentNode);

        for (RunnerAndConfigurationSettings configuration : myTemplateConfigurationsMap.values()) {
            if (configuration.getConfiguration() instanceof UnknownRunConfiguration unknownRunConfiguration
                && unknownRunConfiguration.isDoNotStore()) {
                continue;
            }

            addConfigurationElement(parentNode, configuration);
        }

        for (RunnerAndConfigurationSettings configuration : getStableConfigurations(false)) {
            addConfigurationElement(parentNode, configuration);
        }

        JDOMExternalizableStringList order = null;
        for (RunnerAndConfigurationSettings each : myConfigurations.values()) {
            if (each.getType() instanceof UnknownConfigurationType) {
                continue;
            }

            if (order == null) {
                order = new JDOMExternalizableStringList();
            }
            order.add(each.getUniqueID());
        }
        if (order != null) {
            order.writeExternal(parentNode);
        }

        JDOMExternalizableStringList recentList = new JDOMExternalizableStringList();
        for (RunConfiguration each : myRecentlyUsedTemporaries) {
            if (each.getType() instanceof UnknownConfigurationType) {
                continue;
            }
            RunnerAndConfigurationSettings settings = getSettings(each);
            if (settings == null) {
                continue;
            }
            recentList.add(settings.getUniqueID());
        }
        if (!recentList.isEmpty()) {
            Element recent = new Element(RECENT);
            parentNode.addContent(recent);
            recentList.writeExternal(recent);
        }

        if (myUnknownElements != null) {
            for (Element unloadedElement : myUnknownElements) {
                parentNode.addContent(unloadedElement.clone());
            }
        }
        return parentNode;
    }

    public void writeContext(@Nonnull Element parentNode) {
        Collection<RunnerAndConfigurationSettings> values = new ArrayList<>(myConfigurations.values());
        for (RunnerAndConfigurationSettings configurationSettings : values) {
            if (configurationSettings.isTemporary()) {
                addConfigurationElement(parentNode, configurationSettings, CONFIGURATION);
            }
        }

        RunnerAndConfigurationSettings selected = getSelectedConfiguration();
        if (selected != null) {
            parentNode.setAttribute(SELECTED_ATTR, selected.getUniqueID());
        }
    }

    void addConfigurationElement(@Nonnull Element parentNode, RunnerAndConfigurationSettings template) {
        addConfigurationElement(parentNode, template, CONFIGURATION);
    }

    private void addConfigurationElement(@Nonnull Element parentNode, RunnerAndConfigurationSettings settings, String elementType) {
        Element configurationElement = new Element(elementType);
        parentNode.addContent(configurationElement);
        try {
            ((RunnerAndConfigurationSettingsImpl)settings).writeExternal(configurationElement);
        }
        catch (WriteExternalException e) {
            throw new RuntimeException(e);
        }

        if (settings.getConfiguration() instanceof UnknownRunConfiguration) {
            return;
        }

        List<BeforeRunTask> tasks = new ArrayList<>(getBeforeRunTasks(settings.getConfiguration()));
        Map<Key<BeforeRunTask>, BeforeRunTask> templateTasks = new HashMap<>();
        List<BeforeRunTask> beforeRunTasks = settings.isTemplate()
            ? getHardcodedBeforeRunTasks(settings.getConfiguration())
            : getBeforeRunTasks(getConfigurationTemplate(settings.getFactory()).getConfiguration());
        for (BeforeRunTask templateTask : beforeRunTasks) {
            templateTasks.put(templateTask.getProviderId(), templateTask);
            if (templateTask.isEnabled()) {
                boolean found = false;
                for (BeforeRunTask realTask : tasks) {
                    if (realTask.getProviderId() == templateTask.getProviderId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    BeforeRunTask clone = templateTask.clone();
                    clone.setEnabled(false);
                    tasks.add(0, clone);
                }
            }
        }

        Element methodsElement = new Element(METHOD);
        for (int i = 0, size = tasks.size(); i < size; i++) {
            BeforeRunTask task = tasks.get(i);
            int j = 0;
            BeforeRunTask templateTask = null;
            for (Map.Entry<Key<BeforeRunTask>, BeforeRunTask> entry : templateTasks.entrySet()) {
                if (entry.getKey() == task.getProviderId()) {
                    templateTask = entry.getValue();
                    break;
                }
                j++;
            }
            if (task.equals(templateTask) && i == j) {
                // not necessary saving if the task is the same as template and on the same place
                continue;
            }
            Element child = new Element(OPTION);
            child.setAttribute(NAME_ATTR, task.getProviderId().toString());
            task.writeExternal(child);
            methodsElement.addContent(child);
        }
        configurationElement.addContent(methodsElement);
    }

    @Override
    public void loadState(Element parentNode) {
        try {
            ourLocalRunManager.set(this);

            clear(false);

            List<Element> children = parentNode.getChildren(CONFIGURATION);
            Element[] sortedElements = children.toArray(new Element[children.size()]);
            // ensure templates are loaded first
            Arrays.sort(sortedElements, (a, b) -> {
                boolean aDefault = Boolean.valueOf(a.getAttributeValue("default", "false"));
                boolean bDefault = Boolean.valueOf(b.getAttributeValue("default", "false"));
                return aDefault == bDefault ? 0 : aDefault ? -1 : 1;
            });

            // element could be detached, so, we must not use for each
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, length = sortedElements.length; i < length; i++) {
                Element element = sortedElements[i];
                RunnerAndConfigurationSettings configurationSettings;
                try {
                    configurationSettings = loadConfiguration(element, false);
                }
                catch (ProcessCanceledException e) {
                    configurationSettings = null;
                }
                catch (Throwable e) {
                    LOG.error(e);
                    continue;
                }
                if (configurationSettings == null) {
                    if (myUnknownElements == null) {
                        myUnknownElements = new SmartList<>();
                    }
                    myUnknownElements.add(element.detach());
                }
            }

            myOrder.readExternal(parentNode);

            // migration (old ids to UUIDs)
            readList(myOrder);

            myRecentlyUsedTemporaries.clear();
            Element recentNode = parentNode.getChild(RECENT);
            if (recentNode != null) {
                JDOMExternalizableStringList list = new JDOMExternalizableStringList();
                list.readExternal(recentNode);
                readList(list);
                for (String name : list) {
                    RunnerAndConfigurationSettings settings = myConfigurations.get(name);
                    if (settings != null) {
                        myRecentlyUsedTemporaries.add(settings.getConfiguration());
                    }
                }
            }
            myOrdered = false;

            myLoadedSelectedConfigurationUniqueName = parentNode.getAttributeValue(SELECTED_ATTR);
            setSelectedConfigurationId(myLoadedSelectedConfigurationUniqueName);

            fireBeforeRunTasksUpdated();
            fireRunConfigurationSelected();
        }
        finally {
            ourLocalRunManager.remove();
        }
    }

    private void readList(@Nonnull JDOMExternalizableStringList list) {
        for (int i = 0; i < list.size(); i++) {
            for (RunnerAndConfigurationSettings settings : myConfigurations.values()) {
                RunConfiguration configuration = settings.getConfiguration();
                //noinspection deprecation
                if (configuration != null
                    && list.get(i).equals(
                    configuration.getType().getDisplayName() + "." + configuration.getName() +
                        (configuration instanceof UnknownRunConfiguration ? configuration.getUniqueID() : "")
                )) {
                    list.set(i, settings.getUniqueID());
                    break;
                }
            }
        }
    }

    public void readContext(Element parentNode) throws InvalidDataException {
        myLoadedSelectedConfigurationUniqueName = parentNode.getAttributeValue(SELECTED_ATTR);

        for (Object aChildren : parentNode.getChildren()) {
            Element element = (Element)aChildren;
            RunnerAndConfigurationSettings config = loadConfiguration(element, false);
            if (myLoadedSelectedConfigurationUniqueName == null && config != null
                && Boolean.valueOf(element.getAttributeValue(SELECTED_ATTR))) {
                myLoadedSelectedConfigurationUniqueName = config.getUniqueID();
            }
        }

        setSelectedConfigurationId(myLoadedSelectedConfigurationUniqueName);

        fireRunConfigurationSelected();
    }

    @Nullable
    private String findExistingConfigurationId(@Nullable RunnerAndConfigurationSettings settings) {
        if (settings != null) {
            for (Map.Entry<String, RunnerAndConfigurationSettings> entry : myConfigurations.entrySet()) {
                if (entry.getValue() == settings) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private void clear(boolean allConfigurations) {
        List<RunnerAndConfigurationSettings> configurations;
        if (allConfigurations) {
            myConfigurations.clear();
            mySharedConfigurations.clear();
            myConfigurationToBeforeTasksMap.clear();
            mySelectedConfigurationId = null;
            configurations = new ArrayList<>(myConfigurations.values());
        }
        else {
            configurations = new SmartList<>();
            for (Iterator<RunnerAndConfigurationSettings> iterator = myConfigurations.values().iterator(); iterator.hasNext(); ) {
                RunnerAndConfigurationSettings configuration = iterator.next();
                if (configuration.isTemporary() || !isConfigurationShared(configuration)) {
                    iterator.remove();

                    mySharedConfigurations.remove(configuration.getUniqueID());
                    myConfigurationToBeforeTasksMap.remove(configuration.getConfiguration());

                    configurations.add(configuration);
                }
            }

            if (mySelectedConfigurationId != null && myConfigurations.containsKey(mySelectedConfigurationId)) {
                mySelectedConfigurationId = null;
            }
        }

        myUnknownElements = null;
        myTemplateConfigurationsMap.clear();
        myLoadedSelectedConfigurationUniqueName = null;
        myIdToIcon.clear();
        myIconCheckTimes.clear();
        myIconCalcTime.clear();
        myRecentlyUsedTemporaries.clear();
        fireRunConfigurationsRemoved(configurations);
    }

    @Nullable
    public RunnerAndConfigurationSettings loadConfiguration(@Nonnull Element element, boolean isShared) {
        RunnerAndConfigurationSettingsImpl settings = new RunnerAndConfigurationSettingsImpl(this);
        try {
            settings.readExternal(element);
        }
        catch (InvalidDataException e) {
            LOG.error(e);
        }

        ConfigurationFactory factory = settings.getFactory();
        if (factory == null) {
            return null;
        }

        List<BeforeRunTask> tasks = readStepsBeforeRun(element.getChild(METHOD), settings);
        if (settings.isTemplate()) {
            myTemplateConfigurationsMap.put(factory.getType().getId() + "." + factory.getId(), settings);
            setBeforeRunTasks(settings.getConfiguration(), tasks, true);
        }
        else {
            addConfiguration(settings, isShared, tasks, true);
            if (Boolean.valueOf(element.getAttributeValue(SELECTED_ATTR))) { //to support old style
                setSelectedConfiguration(settings);
            }
        }
        return settings;
    }

    @Nonnull
    private List<BeforeRunTask> readStepsBeforeRun(@Nullable Element child, @Nonnull RunnerAndConfigurationSettings settings) {
        List<BeforeRunTask> result = null;
        if (child != null) {
            for (Element methodElement : child.getChildren(OPTION)) {
                Key<? extends BeforeRunTask> id = getProviderKey(methodElement.getAttributeValue(NAME_ATTR));
                BeforeRunTask beforeRunTask = getProvider(id).createTask(settings.getConfiguration());
                if (beforeRunTask != null) {
                    beforeRunTask.readExternal(methodElement);
                    if (result == null) {
                        result = new SmartList<>();
                    }
                    result.add(beforeRunTask);
                }
            }
        }
        return Lists.notNullize(result);
    }

    @Nullable
    public ConfigurationFactory getFactory(String typeName, String factoryName) {
        return getFactory(typeName, factoryName, false);
    }

    @Nullable
    public ConfigurationFactory getFactory(String typeName, String factoryName, boolean checkUnknown) {
        ConfigurationType type = typeCache().getConfigurationType(typeName);
        if (type == null && checkUnknown && typeName != null) {
            UnknownFeaturesCollector.getInstance(myProject).registerUnknownFeature(ConfigurationType.class, typeName);
        }
        if (factoryName == null) {
            factoryName = type != null ? type.getConfigurationFactories()[0].getId() : null;
        }
        return typeCache().findFactoryOfTypeNameId(typeName, factoryName);
    }

    @Override
    public void setTemporaryConfiguration(@Nullable RunnerAndConfigurationSettings tempConfiguration) {
        if (tempConfiguration == null) {
            return;
        }

        tempConfiguration.setTemporary(true);

        addConfiguration(
            tempConfiguration,
            isConfigurationShared(tempConfiguration),
            getBeforeRunTasks(tempConfiguration.getConfiguration()),
            false
        );

        setSelectedConfiguration(tempConfiguration);
    }

    @Nonnull
    Collection<RunnerAndConfigurationSettings> getStableConfigurations(boolean shared) {
        List<RunnerAndConfigurationSettings> result = null;
        for (RunnerAndConfigurationSettings configuration : myConfigurations.values()) {
            if (!configuration.isTemporary() && isConfigurationShared(configuration) == shared) {
                if (result == null) {
                    result = new SmartList<>();
                }
                result.add(configuration);
            }
        }
        return Lists.notNullize(result);
    }

    @Nonnull
    Collection<? extends RunnerAndConfigurationSettings> getConfigurationSettings() {
        return myConfigurations.values();
    }

    @Override
    public boolean isTemporary(@Nonnull RunConfiguration configuration) {
        return Arrays.asList(getTempConfigurations()).contains(configuration);
    }

    @Override
    @Nonnull
    public List<RunnerAndConfigurationSettings> getTempConfigurationsList() {
        List<RunnerAndConfigurationSettings> configurations =
            ContainerUtil.filter(myConfigurations.values(), RunnerAndConfigurationSettings::isTemporary);
        return Collections.unmodifiableList(configurations);
    }

    @Nonnull
    @Override
    public RunConfiguration[] getTempConfigurations() {
        List<RunnerAndConfigurationSettings> list = getTempConfigurationsList();
        RunConfiguration[] result = new RunConfiguration[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i).getConfiguration();
        }
        return result;
    }

    @Override
    public void makeStable(@Nonnull RunnerAndConfigurationSettings settings) {
        settings.setTemporary(false);
        myRecentlyUsedTemporaries.remove(settings.getConfiguration());
        if (!myOrder.isEmpty()) {
            setOrdered(false);
        }
        fireRunConfigurationChanged(settings);
    }

    @Override
    public void makeStable(@Nonnull RunConfiguration configuration) {
        RunnerAndConfigurationSettings settings = getSettings(configuration);
        if (settings != null) {
            makeStable(settings);
        }
    }

    @Override
    @Nonnull
    public RunnerAndConfigurationSettings createRunConfiguration(@Nonnull String name, @Nonnull ConfigurationFactory type) {
        return createConfiguration(name, type);
    }

    @Override
    public boolean isConfigurationShared(RunnerAndConfigurationSettings settings) {
        Boolean shared = mySharedConfigurations.get(settings.getUniqueID());
        if (shared == null) {
            RunnerAndConfigurationSettings template = getConfigurationTemplate(settings.getFactory());
            shared = mySharedConfigurations.get(template.getUniqueID());
        }
        return shared != null && shared;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BeforeRunTask> List<T> getBeforeRunTasks(Key<T> taskProviderID) {
        List<T> tasks = new ArrayList<>();
        List<RunnerAndConfigurationSettings> checkedTemplates = new ArrayList<>();
        List<RunnerAndConfigurationSettings> settingsList = new ArrayList<>(myConfigurations.values());
        for (RunnerAndConfigurationSettings settings : settingsList) {
            List<BeforeRunTask> runTasks = getBeforeRunTasks(settings.getConfiguration());
            for (BeforeRunTask task : runTasks) {
                if (task != null && task.isEnabled() && task.getProviderId() == taskProviderID) {
                    tasks.add((T)task);
                }
                else {
                    RunnerAndConfigurationSettings template = getConfigurationTemplate(settings.getFactory());
                    if (!checkedTemplates.contains(template)) {
                        checkedTemplates.add(template);
                        List<BeforeRunTask> templateTasks = getBeforeRunTasks(template.getConfiguration());
                        for (BeforeRunTask templateTask : templateTasks) {
                            if (templateTask != null && templateTask.isEnabled() && templateTask.getProviderId() == taskProviderID) {
                                tasks.add((T)templateTask);
                            }
                        }
                    }
                }
            }
        }
        return tasks;
    }

    @Nonnull
    @Override
    public Image getConfigurationIcon(@Nonnull RunnerAndConfigurationSettings settings) {
        String uniqueID = settings.getUniqueID();
        RunnerAndConfigurationSettings selectedConfiguration = getSelectedConfiguration();
        String selectedId = selectedConfiguration != null ? selectedConfiguration.getUniqueID() : "";
        if (selectedId.equals(uniqueID)) {
            Long lastCheckTime = myIconCheckTimes.get(uniqueID);
            Long calcTime = myIconCalcTime.get(uniqueID);
            if (calcTime == null || calcTime < 150) {
                calcTime = 150L;
            }
            if (lastCheckTime == null || System.currentTimeMillis() - lastCheckTime > calcTime * 10) {
                myIdToIcon.remove(uniqueID);//cache has expired
            }
        }
        Image icon = myIdToIcon.get(uniqueID);
        if (icon == null) {
            icon = IconDeferrer.getInstance().deferAutoUpdatable(
                ProgramRunnerUtil.getPrimaryIcon(settings),
                myProject.hashCode() ^ settings.hashCode(),
                param -> {
                    if (myProject.isDisposed()) {
                        return null;
                    }

                    myIconCalcTime.remove(uniqueID);
                    long startTime = System.currentTimeMillis();

                    Image ico;
                    try {
                        DumbService.getInstance(myProject).setAlternativeResolveEnabled(true);
                        settings.checkSettings();
                        ico = ProgramRunnerUtil.getConfigurationIcon(settings, false);
                    }
                    catch (IndexNotReadyException e) {
                        ico = ProgramRunnerUtil.getConfigurationIcon(settings, false);
                    }
                    catch (RuntimeConfigurationException ignored) {
                        ico = ProgramRunnerUtil.getConfigurationIcon(settings, true);
                    }
                    finally {
                        DumbService.getInstance(myProject).setAlternativeResolveEnabled(false);
                    }
                    myIconCalcTime.put(uniqueID, System.currentTimeMillis() - startTime);
                    return ico;
                }
            );

            myIdToIcon.put(uniqueID, icon);
            myIconCheckTimes.put(uniqueID, System.currentTimeMillis());
        }

        return icon;
    }

    public RunnerAndConfigurationSettings getConfigurationById(@Nonnull String id) {
        return myConfigurations.get(id);
    }

    @Override
    @Nullable
    public RunnerAndConfigurationSettings findConfigurationByName(@Nullable String name) {
        if (name == null) {
            return null;
        }
        for (RunnerAndConfigurationSettings each : myConfigurations.values()) {
            if (name.equals(each.getName())) {
                return each;
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public <T extends BeforeRunTask> List<T> getBeforeRunTasks(RunConfiguration settings, Key<T> taskProviderID) {
        if (settings instanceof WrappingRunConfiguration wrappingRunConfiguration) {
            return getBeforeRunTasks(wrappingRunConfiguration.getPeer(), taskProviderID);
        }
        List<BeforeRunTask> tasks = myConfigurationToBeforeTasksMap.get(settings);
        if (tasks == null) {
            tasks = getBeforeRunTasks(settings);
            myConfigurationToBeforeTasksMap.put(settings, tasks);
        }
        List<T> result = new SmartList<>();
        for (BeforeRunTask task : tasks) {
            if (task.getProviderId() == taskProviderID) {
                //noinspection unchecked
                result.add((T)task);
            }
        }
        return result;
    }

    @Override
    @Nonnull
    public List<BeforeRunTask> getBeforeRunTasks(RunConfiguration settings) {
        if (settings instanceof WrappingRunConfiguration wrappingRunConfiguration) {
            return getBeforeRunTasks(wrappingRunConfiguration.getPeer());
        }

        List<BeforeRunTask> tasks = myConfigurationToBeforeTasksMap.get(settings);
        return tasks == null ? getTemplateBeforeRunTasks(settings) : getCopies(tasks);
    }

    private List<BeforeRunTask> getTemplateBeforeRunTasks(@Nonnull RunConfiguration settings) {
        RunnerAndConfigurationSettings template = getConfigurationTemplate(settings.getFactory());
        List<BeforeRunTask> templateTasks = myConfigurationToBeforeTasksMap.get(template.getConfiguration());
        return templateTasks == null ? getHardcodedBeforeRunTasks(settings) : getCopies(templateTasks);
    }

    @Nonnull
    private List<BeforeRunTask> getHardcodedBeforeRunTasks(@Nonnull RunConfiguration settings) {
        List<BeforeRunTask> _tasks = new SmartList<>();
        for (BeforeRunTaskProvider<? extends BeforeRunTask> provider : BeforeRunTaskProvider.EP_NAME.getExtensionList(myProject)) {
            BeforeRunTask task = provider.createTask(settings);
            if (task != null && task.isEnabled()) {
                Key<? extends BeforeRunTask> providerID = provider.getId();
                settings.getFactory().configureBeforeRunTaskDefaults(providerID, task);
                if (task.isEnabled()) {
                    _tasks.add(task);
                }
            }
        }
        return _tasks;
    }

    @Nonnull
    private static List<BeforeRunTask> getCopies(@Nonnull List<BeforeRunTask> original) {
        List<BeforeRunTask> result = new SmartList<>();
        for (BeforeRunTask task : original) {
            if (task.isEnabled()) {
                result.add(task.clone());
            }
        }
        return result;
    }

    public void shareConfiguration(RunnerAndConfigurationSettings settings, boolean shareConfiguration) {
        boolean shouldFire = settings != null && isConfigurationShared(settings) != shareConfiguration;
        if (shareConfiguration && settings.isTemporary()) {
            makeStable(settings);
        }
        mySharedConfigurations.put(settings.getUniqueID(), shareConfiguration);
        if (shouldFire) {
            fireRunConfigurationChanged(settings);
        }
    }

    @Override
    public final void setBeforeRunTasks(
        RunConfiguration runConfiguration,
        @Nonnull List<BeforeRunTask> tasks,
        boolean addEnabledTemplateTasksIfAbsent
    ) {
        List<BeforeRunTask> result = new SmartList<>(tasks);
        if (addEnabledTemplateTasksIfAbsent) {
            List<BeforeRunTask> templates = getTemplateBeforeRunTasks(runConfiguration);
            Set<Key<BeforeRunTask>> idsToSet = new HashSet<>();
            for (BeforeRunTask task : tasks) {
                idsToSet.add(task.getProviderId());
            }
            int i = 0;
            for (BeforeRunTask template : templates) {
                if (!idsToSet.contains(template.getProviderId())) {
                    result.add(i, template);
                    i++;
                }
            }
        }
        myConfigurationToBeforeTasksMap.put(runConfiguration, Lists.notNullize(result));
        fireBeforeRunTasksUpdated();
    }

    public final void resetBeforeRunTasks(RunConfiguration runConfiguration) {
        myConfigurationToBeforeTasksMap.remove(runConfiguration);
        fireBeforeRunTasksUpdated();
    }

    @Override
    public void addConfiguration(RunnerAndConfigurationSettings settings, boolean isShared) {
        addConfiguration(settings, isShared, getTemplateBeforeRunTasks(settings.getConfiguration()), false);
    }

    private static final ThreadLocal<RunManagerImpl> ourLocalRunManager = ThreadLocal.withInitial(() -> null);

    public static RunManagerImpl getInstanceImpl(Project project) {
        RunManagerImpl runManager = ourLocalRunManager.get();
        if (runManager != null) {
            return runManager;
        }
        return (RunManagerImpl)RunManager.getInstance(project);
    }

    void removeNotExistingSharedConfigurations(@Nonnull Set<String> existing) {
        List<RunnerAndConfigurationSettings> removed = null;
        for (Iterator<Map.Entry<String, RunnerAndConfigurationSettings>> it = myConfigurations.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, RunnerAndConfigurationSettings> entry = it.next();
            RunnerAndConfigurationSettings settings = entry.getValue();
            if (!settings.isTemplate() && isConfigurationShared(settings) && !existing.contains(settings.getUniqueID())) {
                if (removed == null) {
                    removed = new SmartList<>();
                }
                removed.add(settings);
                it.remove();
            }
        }
        fireRunConfigurationsRemoved(removed);
    }

    public void fireRunConfigurationChanged(@Nonnull RunnerAndConfigurationSettings settings) {
        getEventPublisher().runConfigurationChanged(new RunManagerListenerEvent(this, settings, null));
    }

    private void fireRunConfigurationsRemoved(@Nullable List<RunnerAndConfigurationSettings> removed) {
        if (!ContainerUtil.isEmpty(removed)) {
            myRecentlyUsedTemporaries.removeAll(removed);
            for (RunnerAndConfigurationSettings settings : removed) {
                getEventPublisher().runConfigurationRemoved(new RunManagerListenerEvent(this, settings, null));
            }
        }
    }

    private void fireRunConfigurationSelected() {
        getEventPublisher().runConfigurationSelected(new RunManagerListenerEvent(this, getSelectedConfiguration(), null));
    }

    public void fireBeforeRunTasksUpdated() {
        getEventPublisher().beforeRunTasksChanged(new RunManagerListenerEvent(this, getSelectedConfiguration(), null));
    }

    public void fireBeginUpdate() {
        getEventPublisher().beginUpdate();
    }

    public void fireEndUpdate() {
        getEventPublisher().endUpdate();
    }

    private Map<Key<? extends BeforeRunTask>, BeforeRunTaskProvider> myBeforeStepsMap;
    private Map<String, Key<? extends BeforeRunTask>> myProviderKeysMap;

    @Nonnull
    private synchronized BeforeRunTaskProvider getProvider(Key<? extends BeforeRunTask> providerId) {
        if (myBeforeStepsMap == null) {
            initProviderMaps();
        }
        return myBeforeStepsMap.get(providerId);
    }

    @Nonnull
    public Map<ConfigurationType, Map<String, List<RunnerAndConfigurationSettings>>> getConfigurationsGroupedByTypeAndFolder(boolean isIncludeUnknown) {
        Map<ConfigurationType, Map<String, List<RunnerAndConfigurationSettings>>> result = new LinkedHashMap<>();

        for (RunnerAndConfigurationSettings settings : getAllSettings()) {
            ConfigurationType type = settings.getType();

            if (!isIncludeUnknown && type == UnknownConfigurationType.INSTANCE) {
                continue;
            }

            Map<String, List<RunnerAndConfigurationSettings>> groupAndSettings =
                result.computeIfAbsent(type, configurationType -> new LinkedHashMap<>());

            groupAndSettings.computeIfAbsent(settings.getFolderName(), s -> new SmartList<>()).add(settings);
        }
        return result;
    }

    @Nonnull
    private synchronized Key<? extends BeforeRunTask> getProviderKey(String keyString) {
        if (myProviderKeysMap == null) {
            initProviderMaps();
        }
        Key<? extends BeforeRunTask> id = myProviderKeysMap.get(keyString);
        if (id == null) {
            UnknownBeforeRunTaskProvider provider = new UnknownBeforeRunTaskProvider(keyString);
            id = provider.getId();
            myProviderKeysMap.put(keyString, id);
            myBeforeStepsMap.put(id, provider);
        }
        return id;
    }

    private void initProviderMaps() {
        myBeforeStepsMap = new LinkedHashMap<>();
        myProviderKeysMap = new LinkedHashMap<>();
        for (BeforeRunTaskProvider<? extends BeforeRunTask> provider : BeforeRunTaskProvider.EP_NAME.getExtensionList(myProject)) {
            Key<? extends BeforeRunTask> id = provider.getId();
            myBeforeStepsMap.put(id, provider);
            myProviderKeysMap.put(id.toString(), id);
        }
    }

    @Nonnull
    private RunManagerListener getEventPublisher() {
        return myProject.getMessageBus().syncPublisher(RunManagerListener.class);
    }
}
