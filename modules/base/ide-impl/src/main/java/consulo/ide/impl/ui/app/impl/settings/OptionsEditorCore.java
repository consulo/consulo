/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.ui.app.impl.settings;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.internal.ConfigurableWrapper;
import consulo.configurable.localize.ConfigurableLocalize;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.ide.impl.base.BaseShowSettingsUtil;
import consulo.ide.impl.configurable.ConfigurablePreselectStrategy;
import consulo.ide.impl.roots.ui.configuration.session.internal.ConfigurableSessionImpl;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class OptionsEditorCore {
    private static final Logger LOG = Logger.getInstance(OptionsEditorCore.class);

    public static final String MAIN_SPLITTER_PROPORTION = "options.splitter.main.proportions";

    private final Project myProject;
    private final Function<Project, Configurable[]> myConfigurablesBuilder;
    private final OptionsEditorHost myHost;
    private final OptionsEditorContext myContext = new OptionsEditorContext();
    private final OptionsEditorFilter myFilter;
    private final OptionsEditorSettings mySettings;

    private @Nullable ConfigurableSessionImpl myConfigurableSession;

    private volatile Configurable[] myConfigurables = Configurable.EMPTY_ARRAY;
    private volatile boolean myConfigurablesLoaded;
    private final Object myConfigurablesLock = new Object();

    @RequiredUIAccess
    public OptionsEditorCore(Project project, Function<Project, Configurable[]> configurablesBuilder, OptionsEditorHost host) {
        myProject = project;
        myConfigurablesBuilder = configurablesBuilder;
        myHost = host;
        myConfigurableSession = new ConfigurableSessionImpl(project, this::getConfigurables);
        myFilter = new OptionsEditorFilter(myContext, project, this::getConfigurables);
        mySettings = new OptionsEditorSettings(host);
    }

    public Project getProject() {
        return myProject;
    }

    public OptionsEditorContext getContext() {
        return myContext;
    }

    public OptionsEditorFilter getFilter() {
        return myFilter;
    }

    public OptionsEditorSettings getSettings() {
        return mySettings;
    }

    public Configurable[] getConfigurables() {
        return myConfigurables;
    }

    public boolean isConfigurablesLoaded() {
        return myConfigurablesLoaded;
    }

    public Configurable[] getOrBuildConfigurables() {
        if (myConfigurablesLoaded) {
            return myConfigurables;
        }

        synchronized (myConfigurablesLock) {
            if (!myConfigurablesLoaded) {
                try {
                    myConfigurables = myConfigurablesBuilder.apply(myProject);
                }
                catch (Exception e) {
                    if (e instanceof ControlFlowException) {
                        throw ControlFlowException.rethrow(e);
                    }

                    LOG.error(e);
                }
                myConfigurablesLoaded = true;
            }
            return myConfigurables;
        }
    }

    @RequiredUIAccess
    public void doSelect(ConfigurablePreselectStrategy preselectStrategy, String filter, Runnable afterTreeLoad) {
        myHost.rebuildTree(() -> {
            Configurable preselectedConfigurable = preselectStrategy.get(myConfigurables);

            if (!filter.isEmpty()) {
                if (preselectedConfigurable != null && preselectedConfigurable != BaseShowSettingsUtil.SKIP_SELECTION_CONFIGURATION) {
                    mySettings.select(preselectedConfigurable, filter);
                }
                else {
                    myHost.refilter(filter, true, true);
                }
            }
            else if (preselectedConfigurable != BaseShowSettingsUtil.SKIP_SELECTION_CONFIGURATION) {
                if (preselectedConfigurable != null) {
                    myHost.selectInTree(preselectedConfigurable);
                }
                else {
                    selectFirst();
                }
            }

            afterTreeLoad.run();
        });
    }

    private void selectFirst() {
        Configurable[] configurables = myConfigurables;
        if (configurables.length > 0) {
            myHost.selectInTree(configurables[0]);
        }
    }

    @RequiredUIAccess
    public void checkModified(@Nullable Configurable configurable) {
        for (Configurable each : collectAllParentsAndSiblings(configurable)) {
            checkModifiedItem(each);
        }
    }

    @RequiredUIAccess
    public void flushModifications() {
        checkModified(myContext.getCurrentConfigurable());
    }

    private List<@Nullable Configurable> collectAllParentsAndSiblings(@Nullable Configurable actual) {
        List<@Nullable Configurable> result = new ArrayList<>();
        Configurable nearestParent = myContext.getParentConfigurable(actual);

        if (nearestParent != null) {
            Configurable parent = nearestParent;
            while (parent != null) {
                result.add(parent);
                parent = myContext.getParentConfigurable(parent);
            }

            result.addAll(myContext.getChildren(nearestParent));
        }
        else {
            result.add(actual);
        }

        return result;
    }

    @RequiredUIAccess
    private void checkModifiedItem(@Nullable Configurable configurable) {
        if (configurable == null) {
            return;
        }

        if (!myHost.isPageCreated(configurable) && ConfigurableWrapper.hasOwnContent(configurable)) {
            UIAccess.current().give(() -> {
                if (myHost.isDisposed()) {
                    return;
                }

                myHost.createPage(configurable).whenComplete((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (myHost.isDisposed()) {
                        return;
                    }

                    fireModification(configurable);
                });
            });
        }
        else if (myHost.isPageCreated(configurable)) {
            fireModification(configurable);
        }
    }

    @RequiredUIAccess
    private void fireModification(Configurable configurable) {
        if (!myHost.isPageEditable(configurable)) {
            return;
        }

        if (configurable.isModified()) {
            myContext.fireModifiedAdded(configurable, null);
        }
        else if (!configurable.isModified() && !myContext.getErrors().containsKey(configurable)) {
            myContext.fireModifiedRemoved(configurable, null);
        }
    }

    public boolean canApply() {
        return !myContext.getModified().isEmpty();
    }

    @RequiredUIAccess
    public void apply() {
        Map<Configurable, ConfigurationException> errors = new LinkedHashMap<>();
        Set<Configurable> modified = myContext.getModified();
        for (Configurable each : modified) {
            try {
                each.apply();
                if (!each.isModified()) {
                    myContext.fireModifiedRemoved(each, null);
                }
            }
            catch (ConfigurationException e) {
                errors.put(each, e);
                LOG.debug(e);
            }
        }

        myContext.fireErrorsChanged(errors, null);

        if (!errors.isEmpty()) {
            myHost.selectInTree(errors.keySet().iterator().next());
        }

        ConfigurableSessionImpl session = myConfigurableSession;
        if (session != null) {
            session.commit();
        }
    }

    public @Nullable String getErrorMessage() {
        Iterator<ConfigurationException> iterator = myContext.getErrors().values().iterator();
        return iterator.hasNext() ? iterator.next().getMessage() : null;
    }

    @RequiredUIAccess
    public void reset(Configurable configurable, boolean notify) {
        configurable.reset();
        if (notify) {
            myContext.fireReset(configurable);
        }
    }

    public List<LocalizeValue> getBannerText(Configurable configurable) {
        List<LocalizeValue> result = new ArrayList<>();
        Configurable each = configurable;
        while (each != null) {
            result.add(each.getDisplayName().map(text -> text.replace('\n', ' ')));
            each = myContext.getParentConfigurable(each);
        }
        Collections.reverse(result);
        return result;
    }

    public @Nullable LocalizeValue getProjectIconTooltip(Configurable configurable) {
        if (!isProjectConfigurable(configurable)) {
            return null;
        }

        return myProject.isDefault()
            ? ConfigurableLocalize.configurableDefaultProjectTooltip()
            : ConfigurableLocalize.configurableCurrentProjectTooltip();
    }

    private static boolean isProjectConfigurable(Configurable configurable) {
        return ConfigurableWrapper.cast(configurable, ProjectConfigurable.class) != null;
    }

    public @Nullable String getHelpTopic() {
        Configurable current = myContext.getCurrentConfigurable();
        if (current == null || Configurable.DISABLED_HELP_ID.equals(current.getHelpTopic())) {
            return null;
        }

        Configurable target = current;
        List<String> ids = new ArrayList<>();
        while (target != null) {
            String helpTopic = target.getHelpTopic();
            if (Configurable.DISABLED_HELP_ID.equals(helpTopic)) {
                return null;
            }

            String id = null;
            if (target instanceof SearchableConfigurable) {
                id = target.getId();
            }

            if (target == current && helpTopic != null) {
                id = helpTopic;
            }

            id = formatHelpId(id);

            if (id == null) {
                return null;
            }

            ids.add(id);

            target = myContext.getParentConfigurable(target);
        }

        Collections.reverse(ids);

        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);

            for (int j = i + 1; j < ids.size(); j++) {
                String childId = ids.get(j);

                String idWithDot = id + ".";
                if (childId.startsWith(idWithDot)) {
                    ids.set(j, childId.substring(idWithDot.length()));
                }
            }
        }

        StringBuilder builder = new StringBuilder();

        PluginId pluginId = getPluginId(current);
        if (PluginIds.isPlatformPlugin(pluginId)) {
            builder.append("platform/settings/");
        }
        else {
            builder.append("plugins/").append(pluginId).append("/settings/");
        }
        builder.append(String.join("/", ids));

        return builder.toString();
    }

    private static PluginId getPluginId(Configurable configurable) {
        return configurable instanceof ConfigurableWrapper configurableWrapper
            ? PluginManager.getPluginId(configurableWrapper.getConfigurable().getClass())
            : PluginManager.getPluginId(configurable.getClass());
    }

    private static @Nullable String formatHelpId(@Nullable String id) {
        return id == null || StringUtil.isEmptyOrSpaces(id) || id.contains(" ") ? null : id;
    }

    @RequiredUIAccess
    public void dispose(Consumer<Configurable> configurableDisposer) {
        visitRecursive(myConfigurables, configurableDisposer);

        myConfigurables = Configurable.EMPTY_ARRAY;

        ConfigurableSessionImpl session = myConfigurableSession;
        if (session != null) {
            session.drop();
        }
        myConfigurableSession = null;
    }

    private static void visitRecursive(Configurable[] configurables, Consumer<Configurable> consumer) {
        for (Configurable configurable : configurables) {
            try {
                consumer.accept(configurable);
            }
            catch (Exception e) {
                LOG.error(e);
            }

            if (configurable instanceof Configurable.Composite composite) {
                visitRecursive(composite.getConfigurables(), consumer);
            }
        }
    }
}
