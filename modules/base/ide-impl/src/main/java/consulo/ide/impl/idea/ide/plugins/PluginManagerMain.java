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
package consulo.ide.impl.idea.ide.plugins;

import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.application.internal.ApplicationEx;
import consulo.configurable.ConfigurableSession;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.disposer.Disposable;
import consulo.externalService.update.UpdateSettings;
import consulo.webBrowser.BrowserUtil;
import consulo.ide.impl.idea.ide.ui.search.SearchableOptionsRegistrar;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.ide.impl.localize.PluginLocalize;
import consulo.ide.impl.plugins.PluginDescriptionPanel;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.platform.base.localize.RepositoryTagLocalize;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchBase;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TableModelListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author stathik
 * @since 2003-12-25
 */
public abstract class PluginManagerMain implements Disposable {
    public static final NotificationGroup ourPluginsLifecycleGroup = new NotificationGroup(
        "pluginsLifecycleGroup",
        PluginLocalize.messagePluginsLifecycleGroup(),
        NotificationDisplayType.STICKY_BALLOON,
        true
    );

    public static Logger LOG = Logger.getInstance(PluginManagerMain.class);

    private boolean requireShutdown = false;

    private Wrapper myRoot;

    private PluginDescriptionPanel myDescriptionPanel;

    protected JPanel myTablePanel;
    protected PluginTableModel myPluginsModel;
    protected PluginTable myPluginTable;

    protected final MyPluginsFilter myFilter = new MyPluginsFilter();
    private boolean myDisposed = false;
    private boolean myBusy = false;

    private InstalledPluginsManagerMain myInstalledTab;
    private AvailablePluginsManagerMain myAvailableTab;

    public PluginManagerMain() {
    }

    protected void init() {
        myRoot = new Wrapper();

        OnePixelSplitter splitter = new OnePixelSplitter(false, 0.5f);
        myRoot.setContent(splitter);

        myDescriptionPanel = new PluginDescriptionPanel();

        splitter.setSecondComponent(myDescriptionPanel.getPanel());

        myTablePanel = new JPanel(new BorderLayout());
        splitter.setFirstComponent(myTablePanel);

        PluginTable table = createTable();
        myPluginTable = table;

        installTableActions();
        myTablePanel.add(ScrollPaneFactory.createScrollPane(table, true), BorderLayout.CENTER);

        final JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(UIUtil.getPanelBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setBorder(new CustomLineBorder(0, 0, JBUI.scale(1), 0));

        LabelPopup sortLabel = new LabelPopup(PluginLocalize.actionSortByLabel(), labelPopup -> createSortersGroup());

        header.add(myFilter, BorderLayout.CENTER);
        JPanel rightHelpPanel = new JPanel(new HorizontalLayout(JBUI.scale(5)));
        rightHelpPanel.add(sortLabel);
        addCustomFilters(rightHelpPanel::add);

        BorderLayoutPanel botton = new BorderLayoutPanel();
        botton.setBorder(new CustomLineBorder(JBUI.scale(1), 0, 0, 0));
        header.add(botton.addToRight(rightHelpPanel), BorderLayout.SOUTH);

        myTablePanel.add(header, BorderLayout.NORTH);

        final TableModelListener modelListener = e -> sortLabel.setPrefixedText(myPluginsModel.getSortBy().getTitle());
        myPluginTable.getModel().addTableModelListener(modelListener);
        modelListener.tableChanged(null);
    }

    protected void addCustomFilters(Consumer<JComponent> adder) {
    }

    @Nonnull
    protected abstract PluginTable createTable();

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
        UiNotifyConnector.doWhenFirstShown(
            getPluginTable(),
            () -> {
                requireShutdown = false;
                TableUtil.ensureSelectionExists(getPluginTable());
            }
        );
    }

    public PluginTable getPluginTable() {
        return myPluginTable;
    }

    public PluginTableModel getPluginsModel() {
        return myPluginsModel;
    }

    protected void installTableActions() {
        myPluginTable.getSelectionModel().addListSelectionListener(e -> refresh());

        //PopupHandler.installUnknownPopupHandler(pluginTable, getActionGroup(false), ActionManager.getInstance());

        new MySpeedSearchBar(myPluginTable);
    }

    @RequiredUIAccess
    public void refresh() {
        final PluginDescriptor[] descriptors = myPluginTable.getSelectedObjects();
        List<PluginDescriptor> allPlugins = myPluginsModel.getAllPlugins();
        myDescriptionPanel.update(
            descriptors != null && descriptors.length == 1 ? descriptors[0] : null,
            this,
            allPlugins,
            myFilter.getFilter()
        );
    }

    public void setRequireShutdown(boolean val) {
        requireShutdown |= val;
    }

    public List<PluginDescriptor> getDependentList(PluginDescriptor pluginDescriptor) {
        return myPluginsModel.dependent(pluginDescriptor);
    }

    protected void modifyPluginsList(List<PluginDescriptor> list) {
        PluginDescriptor selected = myPluginTable.getSelectedObject();
        myPluginsModel.updatePluginsList(list);
        myPluginsModel.filter(myFilter.getFilter().toLowerCase());
        if (selected != null) {
            select(selected.getPluginId());
        }
    }

    public abstract ActionGroup getActionGroup();

    @Nonnull
    protected PluginManagerMain getAvailable() {
        return Objects.requireNonNull(myAvailableTab);
    }

    @Nonnull
    protected PluginManagerMain getInstalled() {
        return Objects.requireNonNull(myInstalledTab);
    }

    public void setAvailableTab(AvailablePluginsManagerMain availableTab) {
        myAvailableTab = availableTab;
    }

    public void setInstalledTab(InstalledPluginsManagerMain installedTab) {
        myInstalledTab = installedTab;
    }

    public JPanel getMainPanel() {
        return myRoot;
    }

    /**
     * Start a new thread which downloads new list of plugins from the site in
     * the background and updates a list of plugins in the table.
     *
     * @param earlyAccessProgramManager
     */
    protected void loadPluginsFromHostInBackground(EarlyAccessProgramManager earlyAccessProgramManager) {
        setDownloadStatus(true);

        Application.get().executeOnPooledThread(() -> {
            SimpleReference<List<PluginDescriptor>> ref = SimpleReference.create();
            List<String> errorMessages = new ArrayList<>();

            try {
                ref.set(RepositoryHelper.loadOnlyPluginsFromRepository(
                    null,
                    UpdateSettings.getInstance().getChannel(),
                    earlyAccessProgramManager
                ));
            }
            catch (Throwable e) {
                LOG.info(e);
                errorMessages.add(e.getMessage());
            }

            UIUtil.invokeLaterIfNeeded(() -> {
                setDownloadStatus(false);
                List<PluginDescriptor> list = ref.get();

                if (list != null) {
                    modifyPluginsList(list);
                    propagateUpdates(list);
                }
                if (!errorMessages.isEmpty()) {
                    if (Messages.showOkCancelDialog(
                        IdeLocalize.errorListOfPluginsWasNotLoaded(StringUtil.join(errorMessages, ", ")).get(),
                        IdeLocalize.titlePlugins().get(),
                        CommonLocalize.buttonRetry().get(),
                        CommonLocalize.buttonCancel().get(),
                        UIUtil.getErrorIcon()
                    ) == Messages.OK) {
                        loadPluginsFromHostInBackground(earlyAccessProgramManager);
                    }
                }
            });
        });
    }

    protected abstract void propagateUpdates(List<PluginDescriptor> list);

    protected void setDownloadStatus(boolean status) {
        myPluginTable.setPaintBusy(status);
        myBusy = status;
    }

    @RequiredUIAccess
    protected void loadAvailablePlugins() {
        EarlyAccessProgramManager earlyAccessProgramManager =
            ConfigurableSession.get().getOrCopy(Application.get(), EarlyAccessProgramManager.class);

        loadPluginsFromHostInBackground(earlyAccessProgramManager);
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

    protected DefaultActionGroup createSortersGroup() {
        final DefaultActionGroup group = new DefaultActionGroup(PluginLocalize.actionSortByText(), true);
        group.addAction(new SortByAction(SortBy.STATUS, myPluginTable, myPluginsModel));
        return group;
    }

    public static class MyHyperlinkListener implements HyperlinkListener {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                JEditorPane pane = (JEditorPane)e.getSource();
                if (e instanceof HTMLFrameHyperlinkEvent evt) {
                    HTMLDocument doc = (HTMLDocument)pane.getDocument();
                    doc.processHTMLFrameHyperlinkEvent(evt);
                }
                else {
                    URL url = e.getURL();
                    if (url != null) {
                        BrowserUtil.browse(url);
                    }
                }
            }
        }
    }

    private static class MySpeedSearchBar extends SpeedSearchBase<PluginTable> {
        public MySpeedSearchBar(PluginTable cmp) {
            super(cmp);
        }

        @Override
        protected int convertIndexToModel(int viewIndex) {
            return getComponent().convertRowIndexToModel(viewIndex);
        }

        @Override
        public int getSelectedIndex() {
            return myComponent.getSelectedRow();
        }

        @Nonnull
        @Override
        public Object[] getAllElements() {
            return myComponent.getElements();
        }

        @Override
        public String getElementText(Object element) {
            return ((PluginDescriptor)element).getName();
        }

        @Override
        public void selectElement(Object element, String selectedText) {
            for (int i = 0; i < myComponent.getRowCount(); i++) {
                if (myComponent.getObjectAt(i).getName().equals(((PluginDescriptor)element).getName())) {
                    myComponent.setRowSelectionInterval(i, i);
                    TableUtil.scrollSelectionToVisible(myComponent);
                    break;
                }
            }
        }
    }

    public void select(PluginId pluginId) {
        myPluginTable.select(pluginId);
    }

    protected static boolean isAccepted(String filter, Set<String> search, PluginDescriptor descriptor) {
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
            return tags.stream().map(PluginManagerMain::getTagLocalizeValue).collect(Collectors.toList());
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

    public static void notifyPluginsWereInstalled(
        @Nonnull Collection<? extends PluginDescriptor> installed,
        Project project
    ) {
        notifyPluginsWereUpdated(
            PluginLocalize.messagePluginsWereInstalled(
                StringUtil.join(installed, PluginDescriptor::getName, ", "),
                installed.size()
            ).get(),
            project
        );
    }

    public static void notifyPluginsWereUpdated(final String title, final Project project) {
        final ApplicationEx app = (ApplicationEx)Application.get();
        final LocalizeValue appName = app.getName();
        final boolean restartCapable = app.isRestartCapable();
        String message = restartCapable
            ? IdeLocalize.messageIdeaRestartRequired(appName).get()
            : IdeLocalize.messageIdeaShutdownRequired(appName).get();
        message += "<br><a href=" + (
            restartCapable
                ? "\"restart\">" + IdeLocalize.ideRestartAction()
                : "\"shutdown\">" + IdeLocalize.ideShutdownAction()
        ) + "</a>";
        ourPluginsLifecycleGroup.createNotification(
            title,
            XmlStringUtil.wrapInHtml(message),
            NotificationType.INFORMATION,
            (notification, event) -> {
                notification.expire();
                if (restartCapable) {
                    app.restart(true);
                }
                else {
                    app.exit(true, true);
                }
            }
        ).notify(project);
    }

    public class MyPluginsFilter extends FilterComponent {
        public MyPluginsFilter() {
            super("PLUGIN_FILTER", 5);
            getTextEditor().setBorder(JBUI.Borders.empty(2));
        }

        @Override
        public void filter() {
            myPluginsModel.filter(getFilter().toLowerCase());
            TableUtil.ensureSelectionExists(getPluginTable());
        }
    }

    protected class RefreshAction extends DumbAwareAction {
        public RefreshAction() {
            super(
                PluginLocalize.actionReloadListOfPluginsText(),
                PluginLocalize.actionReloadListOfPluginsDescription(),
                AllIcons.Actions.Refresh
            );
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            loadAvailablePlugins();
            myFilter.setFilter("");
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(!myBusy);
        }
    }
}
