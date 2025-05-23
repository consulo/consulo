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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.registry.Registry;
import consulo.component.util.localize.BundleBase;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.diff.DiffPlaces;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.openapi.ui.InputException;
import consulo.ide.impl.idea.openapi.vcs.CacheChangeProcessorBridge;
import consulo.ide.impl.idea.openapi.vcs.CacheChangeProcessorBridgeFactory;
import consulo.ide.impl.idea.openapi.vcs.CacheChangeProcessorImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.*;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.ScheduleForAdditionAction;
import consulo.ide.impl.idea.openapi.vcs.checkin.CheckinChangeListSpecificComponent;
import consulo.ide.impl.idea.openapi.vcs.checkin.CheckinMetaHandler;
import consulo.ide.impl.idea.openapi.vcs.impl.CheckinHandlersManager;
import consulo.ide.impl.idea.openapi.vcs.ui.CommitMessage;
import consulo.ide.impl.idea.ui.SplitterWithSecondHideable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.laf.MultiLineLabelUI;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.checkin.*;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.Refreshable;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

public class CommitChangeListDialog extends DialogWrapper implements CheckinProjectPanel, TypeSafeDataProvider {
    private final static String outCommitHelpId = "reference.dialogs.vcs.commit";
    private static final int LAYOUT_VERSION = 2;

    private static final String SPLITTER_PROPORTION_OPTION = "CommitChangeListDialog.SPLITTER_PROPORTION_" + LAYOUT_VERSION;
    private static final String DETAILS_SPLITTER_PROPORTION_OPTION = "CommitChangeListDialog.DETAILS_SPLITTER_PROPORTION_" + LAYOUT_VERSION;
    private static final String DETAILS_SHOW_OPTION = "CommitChangeListDialog.DETAILS_SHOW_OPTION_";

    private static final float SPLITTER_PROPORTION_OPTION_DEFAULT = 0.5f;
    private static final float DETAILS_SPLITTER_PROPORTION_OPTION_DEFAULT = 0.6f;
    private static final boolean DETAILS_SHOW_OPTION_DEFAULT = false;

    @Nonnull
    private final CommitContext myCommitContext;
    @Nonnull
    private final CommitMessage myCommitMessageArea;
    private Splitter mySplitter;
    @Nullable
    private final JPanel myAdditionalOptionsPanel;

    @Nonnull
    private final ChangesBrowserBase<?> myBrowser;

    private CommitLegendPanel myLegend;
    @Nonnull
    private final CacheChangeProcessorBridge myDiffDetails;

    @Nonnull
    private final List<RefreshableOnComponent> myAdditionalComponents = new ArrayList<>();
    @Nonnull
    private final List<CheckinHandler> myHandlers = new ArrayList<>();
    @Nonnull
    private final LocalizeValue myActionName;
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final VcsConfiguration myVcsConfiguration;
    private final List<CommitExecutor> myExecutors;
    @Nonnull
    private final Alarm myOKButtonUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
    private String myLastKnownComment = "";
    private final boolean myAllOfDefaultChangeListChangesIncluded;
    private final CommitExecutorAction[] myExecutorActions;
    private final boolean myShowVcsCommit;
    @Nonnull
    private final Map<AbstractVcs, JPanel> myPerVcsOptionsPanels = new HashMap<>();

    @Nullable
    private final AbstractVcs myVcs;
    private final boolean myIsAlien;
    private boolean myDisposed = false;
    private boolean myUpdateDisabled = false;
    @Nonnull
    private final JLabel myWarningLabel;

    @Nonnull
    private final Map<String, CheckinChangeListSpecificComponent> myCheckinChangeListSpecificComponents;

    @Nonnull
    private final Map<String, String> myListComments;
    private String myLastSelectedListName;
    private ChangeInfoCalculator myChangesInfoCalculator;

    @Nonnull
    private final PseudoMap<Object, Object> myAdditionalData;
    private String myHelpId;

    private SplitterWithSecondHideable myDetailsSplitter;

    private final String myOkActionText;
    private CommitAction myCommitAction;
    @Nullable
    private CommitResultHandler myResultHandler;

    private static class MyUpdateButtonsRunnable implements Runnable {
        private CommitChangeListDialog myDialog;

        private MyUpdateButtonsRunnable(CommitChangeListDialog dialog) {
            myDialog = dialog;
        }

        public void cancel() {
            myDialog = null;
        }

        @Override
        public void run() {
            if (myDialog != null) {
                myDialog.updateButtons();
                myDialog.updateLegend();
            }
        }

        public void restart(CommitChangeListDialog dialog) {
            myDialog = dialog;
            run();
        }
    }

    @Nonnull
    private final MyUpdateButtonsRunnable myUpdateButtonsRunnable = new MyUpdateButtonsRunnable(this);

    @RequiredUIAccess
    public static boolean commitChanges(
        Project project,
        List<Change> changes,
        LocalChangeList initialSelection,
        List<CommitExecutor> executors,
        boolean showVcsCommit,
        String comment,
        @Nullable CommitResultHandler customResultHandler,
        boolean cancelIfNoChanges
    ) {
        return commitChanges(
            project,
            changes,
            initialSelection,
            executors,
            showVcsCommit,
            null,
            comment,
            customResultHandler,
            cancelIfNoChanges
        );
    }

    @RequiredUIAccess
    public static boolean commitChanges(
        Project project,
        List<Change> changes,
        LocalChangeList initialSelection,
        List<CommitExecutor> executors,
        boolean showVcsCommit,
        @Nullable AbstractVcs singleVcs,
        String comment,
        @Nullable CommitResultHandler customResultHandler,
        boolean cancelIfNoChanges
    ) {
        if (cancelIfNoChanges && changes.isEmpty() && !project.getApplication().isUnitTestMode()) {
            Messages.showInfoMessage(
                project,
                VcsLocalize.commitDialogNoChangesDetectedText().get(),
                VcsLocalize.commitDialogNoChangesDetectedTitle().get()
            );
            return false;
        }

        for (BaseCheckinHandlerFactory factory : getCheckInFactories(project)) {
            BeforeCheckinDialogHandler handler = factory.createSystemReadyHandler(project);
            if (handler != null && !handler.beforeCommitDialogShown(project, changes, executors, showVcsCommit)) {
                return false;
            }
        }

        ChangeListManager manager = ChangeListManager.getInstance(project);
        CommitChangeListDialog dialog = new CommitChangeListDialog(
            project,
            changes,
            initialSelection,
            executors,
            showVcsCommit,
            manager.getDefaultChangeList(),
            manager.getChangeListsCopy(),
            singleVcs,
            false,
            comment,
            customResultHandler
        );
        if (!project.getApplication().isUnitTestMode()) {
            dialog.show();
        }
        else {
            dialog.doOKAction();
        }
        return dialog.isOK();
    }

    private static List<BaseCheckinHandlerFactory> getCheckInFactories(@Nonnull Project project) {
        return CheckinHandlersManager.getInstance()
            .getRegisteredCheckinHandlerFactories(ProjectLevelVcsManager.getInstance(project).getAllActiveVcss());
    }

    // Used in plugins
    @SuppressWarnings("unused")
    @Nonnull
    public List<RefreshableOnComponent> getAdditionalComponents() {
        return Collections.unmodifiableList(myAdditionalComponents);
    }

    @RequiredUIAccess
    public static void commitPaths(
        Project project,
        Collection<FilePath> paths,
        LocalChangeList initialSelection,
        @Nullable CommitExecutor executor,
        String comment
    ) {
        ChangeListManager manager = ChangeListManager.getInstance(project);
        Collection<Change> changes = new HashSet<>();
        for (FilePath path : paths) {
            changes.addAll(manager.getChangesIn(path));
        }

        commitChanges(project, changes, initialSelection, executor, comment);
    }

    @RequiredUIAccess
    public static boolean commitChanges(
        Project project,
        Collection<Change> changes,
        LocalChangeList initialSelection,
        @Nullable CommitExecutor executor,
        String comment
    ) {
        if (executor == null) {
            return commitChanges(project, changes, initialSelection, collectExecutors(project, changes), true, comment, null);
        }
        else {
            return commitChanges(project, changes, initialSelection, Collections.singletonList(executor), false, comment, null);
        }
    }

    public static List<CommitExecutor> collectExecutors(@Nonnull Project project, @Nonnull Collection<Change> changes) {
        List<CommitExecutor> result = new ArrayList<>();
        for (AbstractVcs<?> vcs : ChangesUtil.getAffectedVcses(changes, project)) {
            result.addAll(vcs.getCommitExecutors());
        }
        result.addAll(ChangeListManager.getInstance(project).getRegisteredExecutors());
        return result;
    }

    /**
     * Shows the commit dialog, and performs the selected action: commit, commit & push, create patch, etc.
     *
     * @param customResultHandler If this is not null, after commit is completed, custom result handler is called instead of
     *                            showing the default notification in case of commit or failure.
     * @return true if user agreed to commit, false if he pressed "Cancel".
     */
    @RequiredUIAccess
    public static boolean commitChanges(
        Project project,
        Collection<Change> changes,
        LocalChangeList initialSelection,
        List<CommitExecutor> executors,
        boolean showVcsCommit,
        String comment,
        @Nullable CommitResultHandler customResultHandler
    ) {
        return commitChanges(
            project,
            new ArrayList<>(changes),
            initialSelection,
            executors,
            showVcsCommit,
            comment,
            customResultHandler,
            true
        );
    }

    @RequiredUIAccess
    public static void commitAlienChanges(
        Project project,
        List<Change> changes,
        AbstractVcs vcs,
        String changelistName,
        String comment
    ) {
        LocalChangeList lcl = new AlienLocalChangeList(changes, changelistName);
        new CommitChangeListDialog(
            project,
            changes,
            null,
            null,
            true,
            AlienLocalChangeList.DEFAULT_ALIEN,
            Collections.singletonList(lcl),
            vcs,
            true,
            comment,
            null
        ).show();
    }

    private CommitChangeListDialog(
        @Nonnull Project project,
        @Nonnull List<Change> changes,
        LocalChangeList initialSelection,
        List<CommitExecutor> executors,
        boolean showVcsCommit,
        @Nonnull LocalChangeList defaultChangeList,
        List<LocalChangeList> changeLists,
        @Nullable AbstractVcs singleVcs,
        boolean isAlien,
        String comment,
        @Nullable CommitResultHandler customResultHandler
    ) {
        super(project, true);
        myCommitContext = new CommitContext();
        myProject = project;
        myVcsConfiguration = ObjectUtil.assertNotNull(VcsConfiguration.getInstance(myProject));
        myExecutors = executors;
        myShowVcsCommit = showVcsCommit;
        myVcs = singleVcs;
        myResultHandler = customResultHandler;
        myListComments = new HashMap<>();
        myAdditionalData = new PseudoMap<>();
        myDiffDetails = myProject.getInstance(CacheChangeProcessorBridgeFactory.class).create(new MyChangeProcessor());

        if (!myShowVcsCommit && ContainerUtil.isEmpty(myExecutors)) {
            throw new IllegalArgumentException("nothing found to execute commit with");
        }

        myAllOfDefaultChangeListChangesIncluded = new HashSet<>(changes).containsAll(new HashSet<>(defaultChangeList.getChanges()));

        myIsAlien = isAlien;
        if (isAlien) {
            myBrowser =
                new AlienChangeListBrowser(project, changeLists, changes, initialSelection, true, true, singleVcs);
        }
        else {
            //noinspection unchecked
            boolean unversionedFilesEnabled = myShowVcsCommit && Registry.is("vcs.unversioned.files.in.commit");
            @SuppressWarnings("unchecked")
            MultipleChangeListBrowser browser = new MultipleChangeListBrowser(
                project,
                changeLists,
                (List)changes,
                initialSelection,
                true,
                true,
                (Runnable)this::updateWarning,
                (Runnable)() -> {
                    for (CheckinHandler handler : myHandlers) {
                        handler.includedChangesChanged();
                    }
                },
                unversionedFilesEnabled
            ) {
                @Override
                protected void afterDiffRefresh() {
                    myBrowser.rebuildList();
                    myBrowser.setDataIsDirty(false);
                    project.getApplication().invokeLater(
                        () -> IdeFocusManager.findInstance().requestFocus(myBrowser.getViewer().getPreferredFocusedComponent(), true)
                    );
                }
            };
            browser.addSelectedListChangeListener(this::updateOnListSelection);
            myBrowser = browser;
            myBrowser.setAlwayExpandList(false);
        }
        myBrowser.getViewer().addSelectionListener(() -> SwingUtilities.invokeLater(this::changeDetails));

        myCommitMessageArea = new CommitMessage(project);

        if (!myVcsConfiguration.CLEAR_INITIAL_COMMIT_MESSAGE) {
            setComment(initialSelection, comment);
        }

        myBrowser.setDiffBottomComponent(new DiffCommitMessageEditor(this));

        myActionName = VcsLocalize.commitDialogTitle();

        Box optionsBox = Box.createVerticalBox();

        boolean hasVcsOptions = false;
        Box vcsCommitOptions = Box.createVerticalBox();
        List<AbstractVcs> vcses = ContainerUtil.sorted(
            getAffectedVcses(),
            (o1, o2) -> o1.getKeyInstanceMethod().getName().compareToIgnoreCase(o2.getKeyInstanceMethod().getName())
        );
        myCheckinChangeListSpecificComponents = new HashMap<>();
        for (AbstractVcs vcs : vcses) {
            CheckinEnvironment checkinEnvironment = vcs.getCheckinEnvironment();
            if (checkinEnvironment != null) {
                RefreshableOnComponent options = checkinEnvironment.createAdditionalOptionsPanel(this, myAdditionalData);
                if (options != null) {
                    JPanel vcsOptions = new JPanel(new BorderLayout());
                    vcsOptions.add(options.getComponent(), BorderLayout.CENTER);
                    vcsOptions.setBorder(IdeBorderFactory.createTitledBorder(vcs.getDisplayName(), true));
                    vcsCommitOptions.add(vcsOptions);
                    myPerVcsOptionsPanels.put(vcs, vcsOptions);
                    myAdditionalComponents.add(options);
                    if (options instanceof CheckinChangeListSpecificComponent checkinChangeListSpecificComponent) {
                        myCheckinChangeListSpecificComponents.put(vcs.getName(), checkinChangeListSpecificComponent);
                    }
                    hasVcsOptions = true;
                }
            }
        }

        if (hasVcsOptions) {
            vcsCommitOptions.add(Box.createVerticalGlue());
            optionsBox.add(vcsCommitOptions);
        }

        boolean beforeVisible = false;
        boolean afterVisible = false;
        JPanel beforeBox = new JPanel(new VerticalFlowLayout());
        JPanel afterBox = new JPanel(new VerticalFlowLayout());
        for (BaseCheckinHandlerFactory factory : getCheckInFactories(project)) {
            CheckinHandler handler = factory.createHandler(this, myCommitContext);
            if (handler == null || CheckinHandler.DUMMY.equals(handler)) {
                continue;
            }

            myHandlers.add(handler);
            RefreshableOnComponent beforePanel = handler.getBeforeCheckinConfigurationPanel();
            if (beforePanel != null) {
                beforeBox.add(beforePanel.getComponent());
                beforeVisible = true;
                myAdditionalComponents.add(beforePanel);
            }

            RefreshableOnComponent afterPanel = handler.getAfterCheckinConfigurationPanel(getDisposable());
            if (afterPanel != null) {
                afterBox.add(afterPanel.getComponent());
                afterVisible = true;
                myAdditionalComponents.add(afterPanel);
            }
        }

        String actionName = getCommitActionName();
        String borderTitleName = actionName.replace("_", "").replace("&", "");
        if (beforeVisible) {
            beforeBox.add(Box.createVerticalGlue());
            JPanel beforePanel = new JPanel(new BorderLayout());
            beforePanel.add(beforeBox);
            beforePanel.setBorder(
                IdeBorderFactory.createTitledBorder(VcsLocalize.borderStandardCheckinOptionsGroup(borderTitleName).get(), true)
            );
            optionsBox.add(beforePanel);
        }

        if (afterVisible) {
            afterBox.add(Box.createVerticalGlue());
            JPanel afterPanel = new JPanel(new BorderLayout());
            afterPanel.add(afterBox);
            afterPanel.setBorder(
                IdeBorderFactory.createTitledBorder(VcsLocalize.borderStandardAfterCheckinOptionsGroup(borderTitleName).get(), true)
            );
            optionsBox.add(afterPanel);
        }

        if (hasVcsOptions || beforeVisible || afterVisible) {
            optionsBox.add(Box.createVerticalGlue());
            myAdditionalOptionsPanel = new JPanel(new BorderLayout());
            myAdditionalOptionsPanel.add(optionsBox, BorderLayout.NORTH);
        }
        else {
            myAdditionalOptionsPanel = null;
        }

        myOkActionText = actionName.replace(BundleBase.MNEMONIC, '&');

        setTitle(myShowVcsCommit ? myActionName : LocalizeValue.localizeTODO(trimEllipsis(myExecutors.get(0).getActionText())));

        restoreState();

        if (myExecutors != null) {
            myExecutorActions = new CommitExecutorAction[myExecutors.size()];

            for (int i = 0; i < myExecutors.size(); i++) {
                CommitExecutor commitExecutor = myExecutors.get(i);
                myExecutorActions[i] = new CommitExecutorAction(commitExecutor, i == 0 && !myShowVcsCommit);
            }
        }
        else {
            myExecutorActions = null;
        }

        myWarningLabel = new JLabel();
        myWarningLabel.setUI(new MultiLineLabelUI());
        myWarningLabel.setForeground(JBColor.RED);

        updateWarning();

        init();
        updateButtons();
        updateVcsOptionsVisibility();

        updateOnListSelection();
        myCommitMessageArea.requestFocusInMessage();

        for (EditChangelistSupport support : EditChangelistSupport.EP_NAME.getExtensionList(project)) {
            support.installSearch(myCommitMessageArea.getEditorField(), myCommitMessageArea.getEditorField());
        }

        showDetailsIfSaved();
    }

    private void setComment(LocalChangeList initialSelection, String comment) {
        if (comment != null) {
            setCommitMessage(comment);
            myLastKnownComment = comment;
            myLastSelectedListName = initialSelection == null ? myBrowser.getSelectedChangeList().getName() : initialSelection.getName();
        }
        else {
            updateComment();

            if (StringUtil.isEmptyOrSpaces(myCommitMessageArea.getComment())) {
                setCommitMessage(myVcsConfiguration.LAST_COMMIT_MESSAGE);
                String messageFromVcs = getInitialMessageFromVcs();
                if (messageFromVcs != null) {
                    myCommitMessageArea.setText(messageFromVcs);
                }
            }
        }
    }

    private void showDetailsIfSaved() {
        boolean showDetails = PropertiesComponent.getInstance().getBoolean(DETAILS_SHOW_OPTION, DETAILS_SHOW_OPTION_DEFAULT);
        if (showDetails) {
            myDetailsSplitter.initOn();
        }
        SwingUtilities.invokeLater(this::changeDetails);
    }

    private void updateOnListSelection() {
        updateComment();
        updateVcsOptionsVisibility();
        for (CheckinChangeListSpecificComponent component : myCheckinChangeListSpecificComponents.values()) {
            component.onChangeListSelected((LocalChangeList)myBrowser.getSelectedChangeList());
        }
    }

    private void updateWarning() {
        // check for null since can be called from constructor before field initialization
        //noinspection ConstantConditions
        if (myWarningLabel != null) {
            myWarningLabel.setVisible(false);
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored") VcsException updateException =
                ((ChangeListManagerImpl)ChangeListManager.getInstance(myProject)).getUpdateException();
            if (updateException != null) {
                String[] messages = updateException.getMessages();
                if (messages != null && messages.length > 0) {
                    String message = messages[0];
                    myWarningLabel.setText("Warning: not all local changes may be shown due to an error: " + message);
                    myWarningLabel.setVisible(true);
                }
            }
        }
    }

    private void updateVcsOptionsVisibility() {
        Collection<AbstractVcs> affectedVcses = ChangesUtil.getAffectedVcses(myBrowser.getSelectedChangeList().getChanges(), myProject);
        for (Map.Entry<AbstractVcs, JPanel> entry : myPerVcsOptionsPanels.entrySet()) {
            entry.getValue().setVisible(affectedVcses.contains(entry.getKey()));
        }
    }

    @Override
    protected String getHelpId() {
        return myHelpId;
    }

    private class CommitAction extends LocalizeAction implements OptionAction {
        private Action[] myOptions = new Action[0];

        private CommitAction() {
            super(myOkActionText);
            putValue(DEFAULT_ACTION, Boolean.TRUE);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(ActionEvent e) {
            doOKAction();
        }

        @Nonnull
        @Override
        public Action[] getOptions() {
            return myOptions;
        }

        public void setOptions(Action[] actions) {
            myOptions = actions;
        }
    }

    @Override
    @RequiredUIAccess
    protected void doOKAction() {
        if (!myIsAlien && !addUnversionedFiles() || !saveDialogState()) {
            return;
        }
        saveComments(true);
        DefaultListCleaner defaultListCleaner = new DefaultListCleaner();

        Runnable callCommit = () -> {
            try {
                CheckinHandler.ReturnResult result = runBeforeCommitHandlers(() -> {
                    CommitChangeListDialog.super.doOKAction();
                    doCommit(myResultHandler);
                }, null);

                if (result == CheckinHandler.ReturnResult.COMMIT) {
                    defaultListCleaner.clean();
                }
            }
            catch (InputException ex) {
                ex.show();
            }
        };
        if (myBrowser.isDataIsDirty()) {
            ensureDataIsActual(callCommit);
        }
        else {
            callCommit.run();
        }
    }

    @RequiredUIAccess
    private boolean addUnversionedFiles() {
        return ScheduleForAdditionAction.addUnversioned(
            myProject,
            myBrowser.getIncludedUnversionedFiles(),
            ChangeListManagerImpl.getDefaultUnversionedFileCondition(),
            myBrowser
        );
    }

    @Nonnull
    @Override
    protected LocalizeAction getOKAction() {
        return new CommitAction();
    }

    @Override
    @Nonnull
    protected Action[] createActions() {
        List<Action> actions = new ArrayList<>();

        myCommitAction = null;
        if (myShowVcsCommit) {
            myCommitAction = new CommitAction();
            actions.add(myCommitAction);
            myHelpId = outCommitHelpId;
        }
        if (myExecutors != null) {
            if (myCommitAction != null) {
                myCommitAction.setOptions(myExecutorActions);
            }
            else {
                actions.addAll(Arrays.asList(myExecutorActions));
            }
            for (CommitExecutor executor : myExecutors) {
                if (myHelpId != null) {
                    break;
                }
                if (executor instanceof CommitExecutorWithHelp commitExecutorWithHelp) {
                    myHelpId = commitExecutorWithHelp.getHelpId();
                }
            }
        }
        actions.add(getCancelAction());
        if (myHelpId != null) {
            actions.add(getHelpAction());
        }

        return actions.toArray(new Action[actions.size()]);
    }

    @RequiredUIAccess
    private void execute(CommitExecutor commitExecutor) {
        if (!saveDialogState()) {
            return;
        }
        saveComments(true);
        CommitSession session = commitExecutor.createCommitSession();
        if (session instanceof CommitSessionContextAware commitSessionContextAware) {
            commitSessionContextAware.setContext(myCommitContext);
        }
        if (session == CommitSession.VCS_COMMIT) {
            doOKAction();
            return;
        }
        boolean isOK = true;
        JComponent configurationUI = SessionDialog.createConfigurationUI(session, getIncludedChanges(), getCommitMessage());
        if (configurationUI != null) {
            DialogWrapper sessionDialog =
                new SessionDialog(
                    commitExecutor.getActionText(),
                    getProject(),
                    session,
                    getIncludedChanges(),
                    getCommitMessage(),
                    configurationUI
                );
            isOK = sessionDialog.showAndGet();
        }
        if (isOK) {
            DefaultListCleaner defaultListCleaner = new DefaultListCleaner();
            runBeforeCommitHandlers(
                () -> {
                    boolean success = false;
                    try {
                        boolean completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(
                            () -> session.execute(getIncludedChanges(), getCommitMessage()),
                            commitExecutor.getActionText(),
                            true,
                            getProject()
                        );

                        if (completed) {
                            for (CheckinHandler handler : myHandlers) {
                                handler.checkinSuccessful();
                            }

                            success = true;
                            defaultListCleaner.clean();
                            close(OK_EXIT_CODE);
                        }
                        else {
                            session.executionCanceled();
                        }
                    }
                    catch (Throwable e) {
                        Messages.showErrorDialog(
                            VcsLocalize.errorExecutingCommit(commitExecutor.getActionText(), e.getLocalizedMessage()).get(),
                            commitExecutor.getActionText()
                        );

                        for (CheckinHandler handler : myHandlers) {
                            handler.checkinFailed(Collections.singletonList(new VcsException(e)));
                        }
                    }
                    finally {
                        if (myResultHandler != null) {
                            if (success) {
                                myResultHandler.onSuccess(getCommitMessage());
                            }
                            else {
                                myResultHandler.onFailure();
                            }
                        }
                    }
                },
                commitExecutor
            );
        }
        else {
            session.executionCanceled();
        }
    }

    @Nullable
    private String getInitialMessageFromVcs() {
        List<Change> list = getIncludedChanges();
        SimpleReference<String> result = new SimpleReference<>();
        ChangesUtil.processChangesByVcs(myProject, list, (vcs, items) -> {
            if (result.isNull()) {
                CheckinEnvironment checkinEnvironment = vcs.getCheckinEnvironment();
                if (checkinEnvironment != null) {
                    Collection<FilePath> paths = ChangesUtil.getPaths(items);
                    String defaultMessage = checkinEnvironment.getDefaultMessageFor(paths.toArray(new FilePath[paths.size()]));
                    if (defaultMessage != null) {
                        result.set(defaultMessage);
                    }
                }
            }
        });
        return result.get();
    }

    private void saveCommentIntoChangeList() {
        if (myLastSelectedListName != null) {
            String actualCommentText = myCommitMessageArea.getComment();
            String saved = myListComments.get(myLastSelectedListName);
            if (!Objects.equals(saved, actualCommentText)) {
                myListComments.put(myLastSelectedListName, actualCommentText);
            }
        }
    }

    private void updateComment() {
        if (myVcsConfiguration.CLEAR_INITIAL_COMMIT_MESSAGE) {
            return;
        }
        LocalChangeList list = (LocalChangeList)myBrowser.getSelectedChangeList();
        if (list == null || (list.getName().equals(myLastSelectedListName))) {
            return;
        }
        else if (myLastSelectedListName != null) {
            saveCommentIntoChangeList();
        }
        myLastSelectedListName = list.getName();

        String listComment = list.getComment();
        if (StringUtil.isEmptyOrSpaces(listComment)) {
            String listTitle = list.getName();
            if (!list.hasDefaultName()) {
                listComment = listTitle;
            }
            else {
                // use last know comment; it is already stored in list
                listComment = myLastKnownComment;
            }
        }

        myCommitMessageArea.setText(listComment);
    }


    @Override
    public void dispose() {
        myDisposed = true;
        Disposer.dispose(myBrowser);
        Disposer.dispose(myCommitMessageArea);
        Disposer.dispose(myOKButtonUpdateAlarm);
        myUpdateButtonsRunnable.cancel();
        super.dispose();
        Disposer.dispose(myDiffDetails);
        PropertiesComponent.getInstance()
            .setValue(SPLITTER_PROPORTION_OPTION, mySplitter.getProportion(), SPLITTER_PROPORTION_OPTION_DEFAULT);
        float usedProportion = myDetailsSplitter.getUsedProportion();
        if (usedProportion > 0) {
            PropertiesComponent.getInstance()
                .setValue(DETAILS_SPLITTER_PROPORTION_OPTION, usedProportion, DETAILS_SPLITTER_PROPORTION_OPTION_DEFAULT);
        }
        PropertiesComponent.getInstance().setValue(DETAILS_SHOW_OPTION, myDetailsSplitter.isOn(), DETAILS_SHOW_OPTION_DEFAULT);
    }

    @Override
    public String getCommitActionName() {
        String name = null;
        for (AbstractVcs vcs : getAffectedVcses()) {
            CheckinEnvironment checkinEnvironment = vcs.getCheckinEnvironment();
            if (name == null && checkinEnvironment != null) {
                name = checkinEnvironment.getCheckinOperationName();
            }
            else {
                name = VcsLocalize.commitDialogDefaultCommitOperationName().get();
            }
        }
        return name != null ? name : VcsLocalize.commitDialogDefaultCommitOperationName().get();
    }

    @Override
    public boolean isCheckSpelling() {
        return myVcsConfiguration.CHECK_COMMIT_MESSAGE_SPELLING;
    }

    @Override
    public void setCheckSpelling(boolean checkSpelling) {
        myVcsConfiguration.CHECK_COMMIT_MESSAGE_SPELLING = checkSpelling;
        myCommitMessageArea.setCheckSpelling(checkSpelling);
    }

    @RequiredUIAccess
    private boolean checkComment() {
        if (myVcsConfiguration.FORCE_NON_EMPTY_COMMENT && getCommitMessage().isEmpty()) {
            int requestForCheckin = Messages.showYesNoDialog(
                VcsLocalize.confirmationTextCheckInWithEmptyComment().get(),
                VcsLocalize.confirmationTitleCheckInWithEmptyComment().get(),
                UIUtil.getWarningIcon()
            );
            return requestForCheckin == Messages.YES;
        }
        else {
            return true;
        }
    }

    private void stopUpdate() {
        myUpdateDisabled = true;
        myUpdateButtonsRunnable.cancel();
    }

    private void restartUpdate() {
        myUpdateDisabled = false;
        myUpdateButtonsRunnable.restart(this);
    }

    private CheckinHandler.ReturnResult runBeforeCommitHandlers(Runnable okAction, CommitExecutor executor) {
        @RequiredUIAccess
        Supplier<CheckinHandler.ReturnResult> proceedRunnable = () -> {
            FileDocumentManager.getInstance().saveAllDocuments();

            for (CheckinHandler handler : myHandlers) {
                if (!(handler.acceptExecutor(executor))) {
                    continue;
                }
                CheckinHandler.ReturnResult result = handler.beforeCheckin(executor, myAdditionalData);
                if (result == CheckinHandler.ReturnResult.COMMIT) {
                    continue;
                }
                if (result == CheckinHandler.ReturnResult.CANCEL) {
                    restartUpdate();
                    return CheckinHandler.ReturnResult.CANCEL;
                }

                if (result == CheckinHandler.ReturnResult.CLOSE_WINDOW) {
                    ChangeList changeList = myBrowser.getSelectedChangeList();
                    CommitHelper.moveToFailedList(
                        changeList,
                        getCommitMessage(),
                        getIncludedChanges(),
                        VcsLocalize.commitDialogRejectedCommitTemplate(changeList.getName()).get(),
                        myProject
                    );
                    doCancelAction();
                    return CheckinHandler.ReturnResult.CLOSE_WINDOW;
                }
            }

            okAction.run();
            return CheckinHandler.ReturnResult.COMMIT;
        };

        stopUpdate();
        SimpleReference<CheckinHandler.ReturnResult> compoundResultRef = SimpleReference.create();
        Runnable runnable = () -> compoundResultRef.set(proceedRunnable.get());
        for (CheckinHandler handler : myHandlers) {
            if (handler instanceof CheckinMetaHandler checkinMetaHandler) {
                Runnable previousRunnable = runnable;
                runnable = () -> checkinMetaHandler.runCheckinHandlers(previousRunnable);
            }
        }
        runnable.run();
        return compoundResultRef.get();
    }

    @RequiredUIAccess
    private boolean saveDialogState() {
        if (!checkComment()) {
            return false;
        }

        saveCommentIntoChangeList();
        myVcsConfiguration.saveCommitMessage(getCommitMessage());
        try {
            saveState();
        }
        catch (InputException ex) {
            ex.show();
            return false;
        }
        return true;
    }

    private class DefaultListCleaner {
        private final boolean myToClean;

        private DefaultListCleaner() {
            int selectedSize = getIncludedChanges().size();
            ChangeList selectedList = myBrowser.getSelectedChangeList();
            int totalSize = selectedList.getChanges().size();
            myToClean = (totalSize == selectedSize) && (((LocalChangeList)selectedList).hasDefaultName());
        }

        void clean() {
            if (myToClean) {
                ChangeListManager clManager = ChangeListManager.getInstance(myProject);
                clManager.editComment(LocalChangeList.DEFAULT_NAME, "");
            }
        }
    }

    private void saveComments(boolean isOk) {
        ChangeListManager clManager = ChangeListManager.getInstance(myProject);
        if (isOk) {
            int selectedSize = getIncludedChanges().size();
            ChangeList selectedList = myBrowser.getSelectedChangeList();
            int totalSize = selectedList.getChanges().size();
            if (totalSize > selectedSize) {
                myListComments.remove(myLastSelectedListName);
            }
        }
        for (Map.Entry<String, String> entry : myListComments.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            clManager.editComment(name, value);
        }
    }

    @Override
    public void doCancelAction() {
        for (CheckinChangeListSpecificComponent component : myCheckinChangeListSpecificComponents.values()) {
            component.saveState();
        }
        saveCommentIntoChangeList();
        saveComments(false);
        //VcsConfiguration.getInstance(myProject).saveCommitMessage(getCommitMessage());
        super.doCancelAction();
    }

    private void doCommit(@Nullable CommitResultHandler customResultHandler) {
        CommitHelper helper = new CommitHelper(
            myProject,
            myBrowser.getSelectedChangeList(),
            getIncludedChanges(),
            myActionName,
            getCommitMessage(),
            myHandlers,
            myAllOfDefaultChangeListChangesIncluded,
            false,
            myAdditionalData,
            customResultHandler
        );

        if (myIsAlien) {
            helper.doAlienCommit(myVcs);
        }
        else {
            helper.doCommit(myVcs);
        }
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        mySplitter = new Splitter(true);
        mySplitter.setHonorComponentsMinimumSize(true);
        mySplitter.setFirstComponent(myBrowser);
        mySplitter.setSecondComponent(myCommitMessageArea);
        initMainSplitter();

        myChangesInfoCalculator = new ChangeInfoCalculator();
        myLegend = new CommitLegendPanel(myChangesInfoCalculator);

        myBrowser.getBottomPanel().add(JBUI.Panels.simplePanel().addToRight(myLegend.getComponent()), BorderLayout.SOUTH);

        JPanel mainPanel;
        if (myAdditionalOptionsPanel != null) {
            JScrollPane optionsPane = ScrollPaneFactory.createScrollPane(myAdditionalOptionsPanel, true);
            optionsPane.getVerticalScrollBar().setUnitIncrement(10);
            JPanel infoPanel = JBUI.Panels.simplePanel(optionsPane).withBorder(JBUI.Borders.emptyLeft(10));

            mainPanel = new JPanel(new MyOptionsLayout(mySplitter, infoPanel, JBUI.scale(250)));
            mainPanel.add(mySplitter);
            mainPanel.add(infoPanel);
        }
        else {
            mainPanel = mySplitter;
        }

        myWarningLabel.setBorder(JBUI.Borders.empty(5, 5, 0, 5));
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(myWarningLabel, new GridBag().anchor(GridBagConstraints.NORTHWEST).weightx(1));

        JPanel rootPane = JBUI.Panels.simplePanel(mainPanel).addToBottom(panel);

        // TODO: there are no reason to use such heavy interface for a simple task.
        myDetailsSplitter = new SplitterWithSecondHideable(true, "Diff", rootPane, new SplitterWithSecondHideable.OnOffListener<>() {
            @Override
            public void on(Integer integer) {
                if (integer == 0) {
                    return;
                }
                myDiffDetails.refresh();
                mySplitter.skipNextLayout();
                myDetailsSplitter.getComponent().skipNextLayout();
                Dimension dialogSize = getSize();
                setSize(dialogSize.width, dialogSize.height + integer);
                repaint();
            }

            @Override
            public void off(Integer integer) {
                if (integer == 0) {
                    return;
                }
                myDiffDetails.clear(); // TODO: we may want to keep it in memory
                mySplitter.skipNextLayout();
                myDetailsSplitter.getComponent().skipNextLayout();
                Dimension dialogSize = getSize();
                setSize(dialogSize.width, dialogSize.height - integer);
                repaint();
            }
        }) {
            @Override
            protected RefreshablePanel createDetails() {
                JPanel panel = JBUI.Panels.simplePanel(myDiffDetails.getComponent());
                return new RefreshablePanel() {
                    @Override
                    public boolean refreshDataSynch() {
                        return false;
                    }

                    @Override
                    public void dataChanged() {
                    }

                    @Override
                    public void refresh() {
                    }

                    @Override
                    public JPanel getPanel() {
                        return panel;
                    }

                    @Override
                    public void away() {
                    }

                    @Override
                    public boolean isStillValid(Object o) {
                        return false;
                    }

                    @Override
                    public void dispose() {
                    }
                };
            }

            @Override
            protected float getSplitterInitialProportion() {
                float value = PropertiesComponent.getInstance()
                    .getFloat(DETAILS_SPLITTER_PROPORTION_OPTION, DETAILS_SPLITTER_PROPORTION_OPTION_DEFAULT);
                return value <= 0.05 || value >= 0.95 ? DETAILS_SPLITTER_PROPORTION_OPTION_DEFAULT : value;
            }
        };

        return myDetailsSplitter.getComponent();
    }

    private void initMainSplitter() {
        mySplitter.setProportion(
            PropertiesComponent.getInstance().getFloat(SPLITTER_PROPORTION_OPTION, SPLITTER_PROPORTION_OPTION_DEFAULT)
        );
    }

    @Nonnull
    public Set<AbstractVcs> getAffectedVcses() {
        return myShowVcsCommit ? myBrowser.getAffectedVcses() : Collections.emptySet();
    }

    @Nonnull
    @Override
    public Collection<VirtualFile> getRoots() {
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);

        return ContainerUtil.map2SetNotNull(
            myBrowser.getCurrentDisplayedChanges(),
            (change) -> vcsManager.getVcsRootFor(ChangesUtil.getFilePath(change))
        );
    }

    @Override
    public JComponent getComponent() {
        return mySplitter;
    }

    @Override
    public boolean hasDiffs() {
        return !getIncludedChanges().isEmpty() || !myBrowser.getIncludedUnversionedFiles().isEmpty();
    }

    @Nonnull
    @Override
    public Collection<VirtualFile> getVirtualFiles() {
        return ContainerUtil.mapNotNull(getIncludedChanges(), (change) -> ChangesUtil.getFilePath(change).getVirtualFile());
    }

    @Nonnull
    @Override
    public Collection<Change> getSelectedChanges() {
        return new ArrayList<>(getIncludedChanges());
    }

    @Nonnull
    @Override
    public Collection<File> getFiles() {
        return ContainerUtil.map(getIncludedChanges(), (change) -> ChangesUtil.getFilePath(change).getIOFile());
    }

    @Nonnull
    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public boolean vcsIsAffected(String name) {
        // tod +- performance?
        return ProjectLevelVcsManager.getInstance(myProject).checkVcsIsActive(name)
            && ContainerUtil.exists(myBrowser.getAffectedVcses(), vcs -> Objects.equals(vcs.getName(), name));
    }

    @Override
    public void setCommitMessage(String currentDescription) {
        setCommitMessageText(currentDescription);
        myCommitMessageArea.requestFocusInMessage();
    }

    private void setCommitMessageText(String currentDescription) {
        myLastKnownComment = currentDescription;
        myCommitMessageArea.setText(currentDescription);
    }

    @Nonnull
    @Override
    public String getCommitMessage() {
        return myCommitMessageArea.getComment();
    }

    @Override
    public void refresh() {
        ChangeListManager.getInstance(myProject).invokeAfterUpdate(
            () -> {
                myBrowser.rebuildList();
                for (RefreshableOnComponent component : myAdditionalComponents) {
                    component.refresh();
                }
            },
            InvokeAfterUpdateMode.SILENT,
            "commit dialog",
            Application.get().getCurrentModalityState()
        );   // title not shown for silently
    }

    @Override
    public void saveState() {
        for (RefreshableOnComponent component : myAdditionalComponents) {
            component.saveState();
        }
    }

    @Override
    public void restoreState() {
        for (RefreshableOnComponent component : myAdditionalComponents) {
            component.restoreState();
        }
    }

    private void updateButtons() {
        if (myDisposed || myUpdateDisabled) {
            return;
        }
        boolean enabled = hasDiffs();
        setOKActionEnabled(enabled);
        if (myCommitAction != null) {
            myCommitAction.setEnabled(enabled);
        }
        if (myExecutorActions != null) {
            for (CommitExecutorAction executorAction : myExecutorActions) {
                executorAction.updateEnabled(enabled);
            }
        }
        myOKButtonUpdateAlarm.cancelAllRequests();
        myOKButtonUpdateAlarm.addRequest(myUpdateButtonsRunnable, 300, Application.get().getModalityStateForComponent(myBrowser));
    }

    private void updateLegend() {
        if (myDisposed || myUpdateDisabled) {
            return;
        }
        myChangesInfoCalculator.update(
            myBrowser.getCurrentDisplayedChanges(),
            getIncludedChanges(),
            myBrowser.getUnversionedFilesCount(),
            myBrowser.getIncludedUnversionedFiles().size()
        );
        myLegend.update();
    }

    @Nonnull
    private List<Change> getIncludedChanges() {
        return myBrowser.getCurrentIncludedChanges();
    }

    @Override
    protected String getDimensionServiceKey() {
        return "CommitChangelistDialog" + LAYOUT_VERSION;
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myCommitMessageArea.getEditorField();
    }

    @Override
    public void calcData(Key<?> key, DataSink sink) {
        if (key == Refreshable.PANEL_KEY) {
            sink.put(Refreshable.PANEL_KEY, this);
        }
        else {
            myBrowser.calcData(key, sink);
        }
    }

    static String trimEllipsis(String title) {
        if (title.endsWith("...")) {
            return title.substring(0, title.length() - 3);
        }
        else {
            return title;
        }
    }

    private void ensureDataIsActual(Runnable runnable) {
        ChangeListManager.getInstance(myProject).invokeAfterUpdate(
            runnable,
            InvokeAfterUpdateMode.SYNCHRONOUS_CANCELLABLE,
            "Refreshing changelists...",
            Application.get().getCurrentModalityState()
        );
    }

    private class CommitExecutorAction extends AbstractAction {
        @Nonnull
        private final CommitExecutor myCommitExecutor;

        public CommitExecutorAction(@Nonnull CommitExecutor commitExecutor, boolean isDefault) {
            super(commitExecutor.getActionText());
            myCommitExecutor = commitExecutor;
            if (isDefault) {
                putValue(DEFAULT_ACTION, Boolean.TRUE);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            @RequiredUIAccess
            Runnable callExecutor = () -> execute(myCommitExecutor);
            if (myBrowser.isDataIsDirty()) {
                ensureDataIsActual(callExecutor);
            }
            else {
                callExecutor.run();
            }
        }

        public void updateEnabled(boolean hasDiffs) {
            setEnabled(
                hasDiffs || myCommitExecutor instanceof CommitExecutorBase commitExecutorBase && !commitExecutorBase.areChangesRequired()
            );
        }
    }

    private static class DiffCommitMessageEditor extends CommitMessage implements Disposable {
        public DiffCommitMessageEditor(CommitChangeListDialog dialog) {
            super(dialog.getProject());
            getEditorField().setDocument(dialog.myCommitMessageArea.getEditorField().getDocument());
        }

        @Override
        public Dimension getPreferredSize() {
            // we don't want to be squeezed to one line
            return new Dimension(400, 120);
        }
    }

    private void changeDetails() {
        if (myDetailsSplitter.isOn()) {
            myDiffDetails.refresh();
        }
    }

    private class MyChangeProcessor implements CacheChangeProcessorImpl {
        @Override
        public String getPlace() {
            return DiffPlaces.COMMIT_DIALOG;
        }

        @Override
        public void init(Project project, CacheChangeProcessorBridge bridge) {
            bridge.putContextUserData(DiffUserDataKeysEx.SHOW_READ_ONLY_LOCK, true);
        }

        @Nonnull
        @Override
        public List<Change> getSelectedChanges() {
            return myBrowser.getSelectedChanges();
        }

        @Nonnull
        @Override
        public List<Change> getAllChanges() {
            return myBrowser.getAllChanges();
        }

        @Override
        public void selectChange(@Nonnull Change change) {
            //noinspection unchecked
            myBrowser.select((List)Collections.singletonList(change));
        }

        @Override
        public void onAfterNavigate() {
            doCancelAction();
        }
    }

    private static class MyOptionsLayout extends AbstractLayoutManager {
        private final JComponent myPanel;
        private final JComponent myOptions;
        private final int myMinOptionsWidth;

        public MyOptionsLayout(@Nonnull JComponent panel, @Nonnull JComponent options, int minOptionsWidth) {
            myPanel = panel;
            myOptions = options;
            myMinOptionsWidth = minOptionsWidth;
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Dimension size1 = myPanel.getPreferredSize();
            Dimension size2 = myOptions.getPreferredSize();
            return new Dimension(size1.width + size2.width, Math.max(size1.height, size2.height));
        }

        @Override
        public void layoutContainer(Container parent) {
            Rectangle bounds = parent.getBounds();
            int availableWidth = bounds.width - myPanel.getPreferredSize().width;
            int preferredWidth = myOptions.getPreferredSize().width;
            int optionsWidth = Math.max(Math.min(availableWidth, preferredWidth), myMinOptionsWidth);
            myPanel.setBounds(new Rectangle(0, 0, bounds.width - optionsWidth, bounds.height));
            myOptions.setBounds(new Rectangle(bounds.width - optionsWidth, 0, optionsWidth, bounds.height));
        }
    }
}
