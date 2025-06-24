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
package consulo.externalService.impl.internal.plugin.ui;

import consulo.application.Application;
import consulo.application.internal.ApplicationEx;
import consulo.configurable.SearchableOptionsRegistrar;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.externalService.impl.internal.PlatformOrPluginsNotificationGroupContributor;
import consulo.externalService.impl.internal.plugin.ui.action.PluginSortActionGroup;
import consulo.externalService.impl.internal.plugin.ui.action.PluginSortFilterGroup;
import consulo.externalService.impl.internal.plugin.ui.action.PluginSorterAction;
import consulo.externalService.impl.internal.plugin.ui.action.PluginTagFilterGroup;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.RepositoryTagLocalize;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author stathik
 * @since 2003-10-25
 */
public abstract class PluginTab implements Disposable {
    public static final Key<PluginTab> KEY = Key.of(PluginTab.class);

    public static Logger LOG = Logger.getInstance(PluginTab.class);

    protected final PluginsPanel myPluginsPanel;

    private boolean requireShutdown = false;

    private Wrapper myRoot;

    private PluginDescriptionPanel myDescriptionPanel;

    protected JPanel myLeftPanel;

    protected PluginsList myPluginList;

    protected final MyPluginsFilter myFilter = new MyPluginsFilter();
    private boolean myDisposed = false;

    private InstalledPluginsTab myInstalledTab;
    private RepositoryPluginsTab myAvailableTab;

    public PluginTab(PluginsPanel pluginsPanel) {
        myPluginsPanel = pluginsPanel;
    }

    @RequiredUIAccess
    protected void init() {
        myRoot = new Wrapper();

        DataManager.registerDataProvider(myRoot, dataId -> {
            if (dataId == KEY) {
                return this;
            }

            return null;
        });

        OnePixelSplitter splitter = new OnePixelSplitter(false, 0.5f);
        myRoot.setContent(splitter);

        myDescriptionPanel = new PluginDescriptionPanel(myPluginsPanel);

        splitter.setSecondComponent(myDescriptionPanel.getPanel());

        myLeftPanel = new JPanel(new BorderLayout());
        splitter.setFirstComponent(myLeftPanel);

        myPluginList = new PluginsList(myPluginsPanel);

        JBList<PluginDescriptor> rawList = myPluginList.getComponent();

        rawList.addListSelectionListener(e -> refresh());

        myLeftPanel.add(ScrollPaneFactory.createScrollPane(rawList, true), BorderLayout.CENTER);

        myFilter.setBorder(JBUI.Borders.customLine(0, 0, 1, 0));

        myLeftPanel.add(myFilter, BorderLayout.NORTH);

        PluginSortFilterGroup group = new PluginSortFilterGroup();
        List<PluginSorter> sorters = getSorters();

        PluginSortActionGroup sortBy = new PluginSortActionGroup();
        for (PluginSorter sorter : sorters) {
            sortBy.add(new PluginSorterAction(sorter));
        }

        group.add(sortBy);
        group.add(new PluginTagFilterGroup());

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(getClass().getSimpleName() + "Toolbar",
            ActionGroup.newImmutableBuilder().add(group).build(),
            true);
        toolbar.setTargetComponent(myRoot);
        toolbar.updateActionsAsync(UIAccess.current());

        JComponent component = toolbar.getComponent();

        myFilter.getTextEditor().putClientProperty("JTextField.trailingComponent", component);
    }

    public abstract List<PluginSorter> getSorters();

    @Nullable
    public PluginDescriptor getSelectedPlugin() {
        return myPluginList.getComponent().getSelectedValue();
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    public boolean isDisposed() {
        return myDisposed;
    }

    public void filter(String filter) {
        myFilter.setSelectedItem(filter);
    }

    public void reset() {
        UiNotifyConnector.doWhenFirstShown(myPluginList.getComponent(), this::onShow);
    }

    @RequiredUIAccess
    protected void onShow() {
        ScrollingUtil.ensureSelectionExists(myPluginList.getComponent());
    }

    @RequiredUIAccess
    public void refresh() {
        myDescriptionPanel.update(
            myPluginList.getComponent().getSelectedValue(),
            myInstalledTab,
            myPluginList.getAll(),
            myFilter.getFilter(),
            false
        );
    }

    public void setRequireShutdown(boolean val) {
        requireShutdown |= val;
    }

    public List<PluginDescriptor> getDependentList(PluginDescriptor pluginDescriptor) {
        return dependent(pluginDescriptor);
    }

    private List<PluginDescriptor> dependent(PluginDescriptor plugin) {
        List<PluginDescriptor> list = new ArrayList<>();
        for (PluginDescriptor any : myPluginList.getAll()) {
            if (any.isLoaded()) {
                PluginId[] dep = any.getDependentPluginIds();
                for (PluginId id : dep) {
                    if (id == plugin.getPluginId()) {
                        list.add(any);
                        break;
                    }
                }
            }
        }
        return list;
    }

    protected void modifyPluginsList(List<PluginDescriptor> list) {
        myPluginList.modifyPluginsList(list);
    }

    public PluginSorter getSorter() {
        return myPluginList.getSorter();
    }

    public void reSort(PluginSorter pluginSorter) {
        myPluginList.reSort(pluginSorter);
    }

    public PluginsList getPluginList() {
        return myPluginList;
    }

    @Nonnull
    public PluginTab getAvailable() {
        return Objects.requireNonNull(myAvailableTab);
    }

    @Nonnull
    public PluginTab getInstalled() {
        return Objects.requireNonNull(myInstalledTab);
    }

    public void setAvailableTab(RepositoryPluginsTab availableTab) {
        myAvailableTab = availableTab;
    }

    public void setInstalledTab(InstalledPluginsTab installedTab) {
        myInstalledTab = installedTab;
    }

    public JPanel getMainPanel() {
        return myRoot;
    }

    protected void setDownloadStatus(boolean status) {
        myPluginList.getComponent().setPaintBusy(status);
    }

    public boolean isRequireShutdown() {
        return requireShutdown;
    }

    public void ignoreChanges() {
        requireShutdown = false;
    }

    public boolean isModified() {
        return requireShutdown;
    }

    public String apply() {
        final String applyMessage = canApply();
        if (applyMessage != null) {
            return applyMessage;
        }
        setRequireShutdown(true);
        return null;
    }

    @Nullable
    protected String canApply() {
        return null;
    }

    public void select(PluginId pluginId) {
        myPluginList.select(pluginId);
    }

    public static boolean isAccepted(String filter, Set<String> search, PluginDescriptor descriptor) {
        if (StringUtil.isEmpty(filter)) {
            return true;
        }
        if (isAccepted(search, filter, descriptor.getName())) {
            return true;
        }
        else {
            final String description = descriptor.getDescription();
            if (description != null && isAccepted(search, filter, description)) {
                return true;
            }
            for (LocalizeValue tag : getLocalizedTags(descriptor)) {
                if (isAccepted(search, filter, tag.getValue())) {
                    return true;
                }
            }
            final String changeNotes = descriptor.getChangeNotes();
            if (changeNotes != null && isAccepted(search, filter, changeNotes)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public static Collection<LocalizeValue> getLocalizedTags(PluginDescriptor pluginDescriptor) {
        Set<String> tags = pluginDescriptor.getTags();
        if (!tags.isEmpty()) {
            return tags.stream().map(PluginTab::getTagLocalizeValue).collect(Collectors.toList());
        }

        return List.of(LocalizeValue.of(pluginDescriptor.getCategory()));
    }

    @Nonnull
    public static LocalizeValue getTagLocalizeValue(@Nonnull String tagId) {
        return LocalizeKey.of(RepositoryTagLocalize.ID, tagId).getValue();
    }

    private static boolean isAccepted(final Set<String> search, @Nonnull final String filter, @Nonnull final String description) {
        if (StringUtil.containsIgnoreCase(description, filter)) {
            return true;
        }
        final SearchableOptionsRegistrar optionsRegistrar = SearchableOptionsRegistrar.getInstance();
        final HashSet<String> descriptionSet = new HashSet<>(search);
        descriptionSet.removeAll(optionsRegistrar.getProcessedWords(description));
        return descriptionSet.isEmpty();
    }

    @NonNls
    public static void notifyPluginsWereInstalled(
        @Nonnull Collection<? extends PluginDescriptor> installed,
        Project project
    ) {
        String pluginName = installed.size() == 1 ? installed.iterator().next().getName() : null;
        notifyPluginsWereUpdated(
            pluginName != null ? "Plugin \'" + pluginName + "\' was successfully installed" : "Plugins were installed",
            project
        );
    }

    public static void notifyPluginsWereUpdated(String title, Project project) {
        ApplicationEx app = (ApplicationEx) Application.get();
        LocalizeValue appName = app.getName();
        boolean restartCapable = app.isRestartCapable();
        String message = restartCapable
            ? ExternalServiceLocalize.messageIdeaRestartRequired(appName).get()
            : ExternalServiceLocalize.messageIdeaShutdownRequired(appName).get();
        message += "<br><a href=" + (restartCapable ? "\"restart\">Restart now" : "\"shutdown\">Shutdown") + "</a>";
        PlatformOrPluginsNotificationGroupContributor.ourPluginsLifecycleGroup.newInfo()
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(XmlStringUtil.wrapInHtml(message)))
            .hyperlinkListener((notification, event) -> {
                notification.expire();
                if (restartCapable) {
                    app.restart(true);
                }
                else {
                    app.exit(true, true);
                }
            })
            .notify(project);
    }

    public class MyPluginsFilter extends FilterComponent {

        public MyPluginsFilter() {
            super("PLUGIN_FILTER", 5);
            getTextEditor().setBorder(JBUI.Borders.empty(6));
        }

        @Override
        public void filter() {
            myPluginList.setTextFilter(getFilter().toLowerCase());

            ScrollingUtil.ensureSelectionExists(myPluginList.getComponent());
        }
    }

    @RequiredUIAccess
    public void reload() {
    }
}