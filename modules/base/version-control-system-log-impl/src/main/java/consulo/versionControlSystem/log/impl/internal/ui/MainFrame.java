package consulo.versionControlSystem.log.impl.internal.ui;

import com.google.common.primitives.Ints;
import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.AWTHasSuffixComponent;
import consulo.ui.ex.awt.table.ComponentsListFocusTraversalPolicy;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesBrowserFactory;
import consulo.versionControlSystem.change.ChangesBrowserUtil;
import consulo.versionControlSystem.change.RepositoryChangesBrowser;
import consulo.versionControlSystem.history.TextRevisionNumber;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.impl.internal.data.MainVcsLogUiProperties;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDataImpl;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogProgress;
import consulo.versionControlSystem.log.impl.internal.data.VisiblePack;
import consulo.versionControlSystem.log.impl.internal.ui.action.IntelliSortChooserPopupAction;
import consulo.versionControlSystem.log.impl.internal.ui.filter.VcsLogClassicFilterUi;
import consulo.versionControlSystem.log.internal.VcsLogActionPlaces;
import consulo.versionControlSystem.log.util.BekUtil;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.versionControlSystem.log.util.VcsUserUtil;
import consulo.versionControlSystem.versionBrowser.CommittedChangeListForRevision;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class MainFrame extends JPanel implements DataProvider, Disposable {
    private static final String HELP_ID = "reference.changesToolWindow.log";

    @Nonnull
    private final VcsLogDataImpl myLogData;
    @Nonnull
    private final VcsLogUiImpl myUi;
    @Nonnull
    private final VcsLog myLog;
    @Nonnull
    private final VcsLogClassicFilterUi myFilterUi;

    @Nonnull
    private final JBLoadingPanel myChangesLoadingPane;
    @Nonnull
    private final VcsLogGraphTable myGraphTable;
    @Nonnull
    private final DetailsPanel myDetailsPanel;
    @Nonnull
    private final Splitter myDetailsSplitter;
    @Nonnull
    private final JComponent myToolbar;
    @Nonnull
    private final RepositoryChangesBrowser myChangesBrowser;
    @Nonnull
    private final Splitter myChangesBrowserSplitter;
    @Nonnull
    private final SearchTextField myTextFilter;
    @Nonnull
    private final MainVcsLogUiProperties myUiProperties;

    @Nonnull
    private Runnable myContainingBranchesListener;
    @Nonnull
    private Runnable myMiniDetailsLoadedListener;

    public MainFrame(@Nonnull VcsLogDataImpl logData,
                     @Nonnull VcsLogUiImpl ui,
                     @Nonnull Project project,
                     @Nonnull MainVcsLogUiProperties uiProperties,
                     @Nonnull VcsLog log,
                     @Nonnull VisiblePack initialDataPack) {
        // collect info
        myLogData = logData;
        myUi = ui;
        myLog = log;
        myUiProperties = uiProperties;

        myFilterUi = new VcsLogClassicFilterUi(myUi, logData, myUiProperties, initialDataPack);

        // initialize components
        myGraphTable = new VcsLogGraphTable(ui, logData, initialDataPack);
        myDetailsPanel = new DetailsPanel(logData, ui.getColorManager(), this);

        ChangesBrowserFactory factory = Application.get().getInstance(ChangesBrowserFactory.class);
        myChangesBrowser = factory.createRepositoryChangeBrowser(project, toolBarGroup -> {
            toolBarGroup.add(ActionManager.getInstance().getAction(VcsLogActionPlaces.VCS_LOG_SHOW_DETAILS_ACTION));
        }, null, List.of(), null);
        myChangesBrowser.getViewerScrollPane().setBorder(IdeBorderFactory.createBorder(SideBorder.TOP));
        myChangesBrowser.getDiffAction().registerCustomShortcutSet(myChangesBrowser.getDiffAction().getShortcutSet(), getGraphTable());
        myChangesBrowser.getEditSourceAction().registerCustomShortcutSet(CommonShortcuts.getEditSource(), getGraphTable());
        myChangesBrowser.getViewer().setEmptyText("");
        myChangesLoadingPane = new JBLoadingPanel(new BorderLayout(), this, 300);
        myChangesLoadingPane.add(myChangesBrowser.getComponent());

        myDetailsSplitter = new OnePixelSplitter(true, "vcs.log.details.splitter.proportion", 0.7f);
        myDetailsSplitter.setFirstComponent(myChangesLoadingPane);
        setupDetailsSplitter(myUiProperties.get(MainVcsLogUiProperties.SHOW_DETAILS));

        myGraphTable.getSelectionModel().addListSelectionListener(new CommitSelectionListenerForDiff());
        myDetailsPanel.installCommitSelectionListener(myGraphTable);
        updateWhenDetailsAreLoaded();

        myTextFilter = myFilterUi.createTextFilter();
        myToolbar = createActionsToolbar();

        ActionGroup textFieldSettingGroup = (ActionGroup) ActionManager.getInstance().getAction(VcsLogActionPlaces.TEXT_FILTER_SETTINGS_ACTION_GROUP);
        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(
            VcsLogActionPlaces.TEXT_FILTER_SETTINGS_ACTION_GROUP,
            textFieldSettingGroup,
            ActionToolbar.Style.INPLACE
        );
        toolbar.setTargetComponent(myTextFilter);
        toolbar.updateActionsAsync();
        AWTHasSuffixComponent.setSuffixComponent(myTextFilter.getTextEditor(), toolbar.getComponent());

        ProgressStripe progressStripe =
            new ProgressStripe(setupScrolledGraph(), this, 300) {
                @Override
                public void updateUI() {
                    super.updateUI();
                    if (myDecorator != null && myLogData.getProgress().isRunning()) {
                        startLoadingImmediately();
                    }
                }
            };
        myLogData.getProgress().addProgressIndicatorListener(new VcsLogProgress.ProgressListener() {
            @Override
            public void progressStarted() {
                progressStripe.startLoading();
            }

            @Override
            public void progressStopped() {
                progressStripe.stopLoading();
            }
        }, this);


        JComponent toolbars = new JPanel(new BorderLayout());
        toolbars.add(myToolbar, BorderLayout.NORTH);
        JComponent toolbarsAndTable = new JPanel(new BorderLayout());
        toolbarsAndTable.add(toolbars, BorderLayout.NORTH);
        toolbarsAndTable.add(progressStripe, BorderLayout.CENTER);

        myChangesBrowserSplitter = new OnePixelSplitter(false, "vcs.log.changes.splitter.proportion", 0.7f);
        myChangesBrowserSplitter.setFirstComponent(toolbarsAndTable);
        myChangesBrowserSplitter.setSecondComponent(myDetailsSplitter);

        setLayout(new BorderLayout());
        add(myChangesBrowserSplitter);

        Disposer.register(ui, this);
        myGraphTable.resetDefaultFocusTraversalKeys();
        setFocusCycleRoot(true);
        setFocusTraversalPolicy(new MyFocusPolicy());
    }

    /**
     * Informs components that the actual DataPack has been updated (e.g. due to a log refresh). <br/>
     * Components may want to update their fields and/or rebuild.
     *
     * @param dataPack         new data pack.
     * @param permGraphChanged true if permanent graph itself was changed.
     */
    public void updateDataPack(@Nonnull VisiblePack dataPack, boolean permGraphChanged) {
        myFilterUi.updateDataPack(dataPack);
        myGraphTable.updateDataPack(dataPack, permGraphChanged);
    }

    private void updateWhenDetailsAreLoaded() {
        myMiniDetailsLoadedListener = () -> {
            myGraphTable.initColumnSize();
            myGraphTable.repaint();
        };
        myContainingBranchesListener = () -> {
            myDetailsPanel.branchesChanged();
            myGraphTable.repaint(); // we may need to repaint highlighters
        };
        myLogData.getMiniDetailsGetter().addDetailsLoadedListener(myMiniDetailsLoadedListener);
        myLogData.getContainingBranchesGetter().addTaskCompletedListener(myContainingBranchesListener);
    }

    public void setupDetailsSplitter(boolean state) {
        myDetailsSplitter.setSecondComponent(state ? myDetailsPanel : null);
    }

    @Nonnull
    private JScrollPane setupScrolledGraph() {
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myGraphTable, SideBorder.TOP);
        myGraphTable.viewportSet(scrollPane.getViewport());
        return scrollPane;
    }

    @Nonnull
    public VcsLogGraphTable getGraphTable() {
        return myGraphTable;
    }

    @Nonnull
    public VcsLogFilterUi getFilterUi() {
        return myFilterUi;
    }

    private JComponent createActionsToolbar() {
        ActionGroup.Builder mainGroup = ActionGroup.newImmutableBuilder();
        mainGroup.add(myFilterUi.createActionGroup());
        mainGroup.addSeparator();
        if (BekUtil.isBekEnabled()) {
            if (BekUtil.isLinearBekEnabled()) {
                mainGroup.add(new IntelliSortChooserPopupAction());
                // can not register both of the actions in xml file, choosing to register an action for the "outer world"
                // I can of course if linear bek is enabled replace the action on start but why bother
            }
            else {
                mainGroup.add(ActionManager.getInstance().getAction(VcsLogActionPlaces.VCS_LOG_INTELLI_SORT_ACTION));
            }
        }
        mainGroup.add(ActionManager.getInstance().getAction(VcsLogActionPlaces.TOOLBAR_ACTION_GROUP));
        ActionToolbar toolbar = createActionsToolbar(mainGroup.build());

        Wrapper textFilter = new Wrapper(myTextFilter);
        textFilter.setVerticalSizeReferent(toolbar.getComponent());
        textFilter.setBorder(JBUI.Borders.emptyLeft(5));

        ActionToolbar settings =
            createActionsToolbar(new DefaultActionGroup(ActionManager.getInstance().getAction(VcsLogActionPlaces.VCS_LOG_QUICK_SETTINGS_ACTION)));
        settings.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);

        JPanel panel = new JPanel(new MigLayout("ins 0, fill", "[left]0[left, fill]push[right]", "center"));
        panel.add(textFilter);
        panel.add(toolbar.getComponent());
        panel.add(settings.getComponent());
        return panel;
    }

    @Nonnull
    private ActionToolbar createActionsToolbar(@Nonnull ActionGroup mainGroup) {
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.CHANGES_VIEW_TOOLBAR, mainGroup, true);
        toolbar.setTargetComponent(this);
        return toolbar;
    }

    @Nonnull
    public JComponent getMainComponent() {
        return this;
    }

    @Nullable
    @Override
    public Object getData(@Nonnull Key<?> dataId) {
        if (VcsLog.KEY == dataId) {
            return myLog;
        }
        else if (VcsLogUi.KEY == dataId) {
            return myUi;
        }
        else if (VcsLogDataProvider.KEY == dataId) {
            return myLogData;
        }
        else if (VcsDataKeys.CHANGES == dataId || VcsDataKeys.SELECTED_CHANGES == dataId) {
            return ArrayUtil.toObjectArray(myChangesBrowser.getCurrentDisplayedChanges(), Change.class);
        }
        else if (VcsDataKeys.CHANGE_LISTS == dataId) {
            List<VcsFullCommitDetails> details = myLog.getSelectedDetails();
            if (details.size() > VcsLogUtil.MAX_SELECTED_COMMITS) {
                return null;
            }
            return ContainerUtil.map2Array(
                details,
                CommittedChangeListForRevision.class,
                detail -> new CommittedChangeListForRevision(
                    detail.getSubject(),
                    detail.getFullMessage(),
                    VcsUserUtil.getShortPresentation(detail.getCommitter()),
                    new Date(detail.getCommitTime()),
                    detail.getChanges(),
                    convertToRevisionNumber(detail.getId())
                )
            );
        }
        else if (VcsDataKeys.VCS_REVISION_NUMBERS == dataId) {
            List<CommitId> hashes = myLog.getSelectedCommits();
            if (hashes.size() > VcsLogUtil.MAX_SELECTED_COMMITS) {
                return null;
            }
            return ArrayUtil
                .toObjectArray(ContainerUtil.map(hashes, commitId -> convertToRevisionNumber(commitId.getHash())), VcsRevisionNumber.class);
        }
        else if (VcsDataKeys.VCS == dataId) {
            int[] selectedRows = myGraphTable.getSelectedRows();
            if (selectedRows.length == 0 || selectedRows.length > VcsLogUtil.MAX_SELECTED_COMMITS) {
                return null;
            }
            Set<VirtualFile> roots = ContainerUtil.map2Set(Ints.asList(selectedRows), row -> myGraphTable.getModel().getRoot(row));
            if (roots.size() == 1) {
                return myLogData.getLogProvider(ObjectUtil.assertNotNull(ContainerUtil.getFirstItem(roots))).getSupportedVcs();
            }
        }
        else if (VcsLogDataKeys.VCS_LOG_BRANCHES == dataId) {
            int[] selectedRows = myGraphTable.getSelectedRows();
            if (selectedRows.length != 1) {
                return null;
            }
            return myGraphTable.getModel().getBranchesAtRow(selectedRows[0]);
        }
        else if (HelpManager.HELP_ID == dataId) {
            return HELP_ID;
        }
        else if (VcsLogInternalDataKeys.LOG_UI_PROPERTIES == dataId) {
            return myUiProperties;
        }
        return null;
    }

    @Nonnull
    public JComponent getToolbar() {
        return myToolbar;
    }

    @Nonnull
    public SearchTextField getTextFilter() {
        return myTextFilter;
    }

    public boolean areGraphActionsEnabled() {
        return myGraphTable.getRowCount() > 0;
    }

    @Nonnull
    private static TextRevisionNumber convertToRevisionNumber(@Nonnull Hash hash) {
        return new TextRevisionNumber(hash.asString(), hash.toShortString());
    }

    public void showDetails(boolean state) {
        myDetailsSplitter.setSecondComponent(state ? myDetailsPanel : null);
    }

    @Override
    public void dispose() {
        myLogData.getMiniDetailsGetter().removeDetailsLoadedListener(myMiniDetailsLoadedListener);
        myLogData.getContainingBranchesGetter().removeTaskCompletedListener(myContainingBranchesListener);

        myDetailsSplitter.dispose();
        myChangesBrowserSplitter.dispose();
    }

    private class CommitSelectionListenerForDiff extends CommitSelectionListener {
        protected CommitSelectionListenerForDiff() {
            super(myLogData, MainFrame.this.myGraphTable, myChangesLoadingPane);
        }

        @RequiredUIAccess
        @Override
        protected void onDetailsLoaded(@Nonnull List<VcsFullCommitDetails> detailsList) {
            List<Change> changes = new ArrayList<>();
            List<VcsFullCommitDetails> detailsListReversed = ContainerUtil.reverse(detailsList);
            for (VcsFullCommitDetails details : detailsListReversed) {
                changes.addAll(details.getChanges());
            }
            changes = ChangesBrowserUtil.zipChanges(changes);
            myChangesBrowser.setChangesToDisplay(changes);
        }

        @RequiredUIAccess
        @Override
        protected void onSelection(@Nonnull int[] selection) {
            // just reset and wait for details to be loaded
            myChangesBrowser.setChangesToDisplay(Collections.emptyList());
            myChangesBrowser.getViewer().setEmptyText("");
        }

        @RequiredUIAccess
        @Override
        protected void onEmptySelection() {
            myChangesBrowser.getViewer().setEmptyText("No commits selected");
            myChangesBrowser.setChangesToDisplay(Collections.emptyList());
        }
    }

    private class MyFocusPolicy extends ComponentsListFocusTraversalPolicy {
        @Nonnull
        @Override
        protected List<Component> getOrderedComponents() {
            return Arrays.asList(myGraphTable, myChangesBrowser.getPreferredFocusedComponent(), myTextFilter.getTextEditor());
        }
    }
}
