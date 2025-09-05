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

package consulo.localHistory.impl.internal.ui.view;

import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.application.util.function.Computable;
import consulo.diff.content.DiffContent;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localHistory.LocalHistoryBundle;
import consulo.localHistory.impl.internal.*;
import consulo.localHistory.impl.internal.ui.model.FileDifferenceModel;
import consulo.localHistory.impl.internal.ui.model.HistoryDialogModel;
import consulo.localHistory.impl.internal.ui.model.RevisionProcessingProgress;
import consulo.localHistory.localize.LocalHistoryLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import consulo.util.io.FileUtil;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.impl.internal.ui.awt.CreatePatchConfigurationPanel;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Consumer;

public abstract class HistoryDialog<T extends HistoryDialogModel> extends FrameWrapper {
    private static final int UPDATE_DIFFS = 1;
    private static final int UPDATE_REVS = UPDATE_DIFFS + 1;

    protected final Project myProject;
    protected final IdeaGateway myGateway;
    protected final VirtualFile myFile;
    private Splitter mySplitter;
    private RevisionsList myRevisionsList;
    private MyDiffContainer myDiffView;
    private ActionToolbar myToolBar;

    private T myModel;

    private MergingUpdateQueue myUpdateQueue;
    private boolean isUpdating;

    protected HistoryDialog(@Nonnull Project project, IdeaGateway gw, VirtualFile f, boolean doInit) {
        super(project);
        myProject = project;
        myGateway = gw;
        myFile = f;

        setProject(project);
        setDimensionKey(getPropertiesKey());
        setImage(TargetAWT.toAWTImage(PlatformIconGroup.actionsDiff()));
        closeOnEsc();

        if (doInit) {
            init();
        }
    }

    protected void init() {
        LocalHistoryFacade facade = LocalHistoryImpl.getInstanceImpl().getFacade();

        myModel = createModel(facade);
        setTitle(myModel.getTitle());
        JComponent root = createComponent();
        setComponent(root);

        setPreferredFocusedComponent(showRevisionsList() ? myRevisionsList.getComponent() : myDiffView);

        myUpdateQueue = new MergingUpdateQueue(getClass() + ".revisionsUpdate", 500, true, root, this, null, false);
        myUpdateQueue.setRestartTimerOnAdd(true);

        facade.addListener(new LocalHistoryFacade.Listener() {
            @Override
            public void changeSetFinished() {
                scheduleRevisionsUpdate(null);
            }
        }, this);

        scheduleRevisionsUpdate(null);
    }

    protected void scheduleRevisionsUpdate(@Nullable Consumer<T> configRunnable) {
        doScheduleUpdate(UPDATE_REVS, () -> {
            synchronized (myModel) {
                if (configRunnable != null) {
                    configRunnable.accept(myModel);
                }
                myModel.clearRevisions();
                myModel.getRevisions();// force load
            }
            return () -> myRevisionsList.updateData(myModel);
        });
    }

    protected abstract T createModel(LocalHistoryFacade vcs);

    protected JComponent createComponent() {
        JPanel root = new JPanel(new BorderLayout());

        ExcludingTraversalPolicy traversalPolicy = new ExcludingTraversalPolicy();
        root.setFocusTraversalPolicy(traversalPolicy);
        root.setFocusTraversalPolicyProvider(true);

        Pair<JComponent, Dimension> diffAndToolbarSize = createDiffPanel(root, traversalPolicy);
        myDiffView = new MyDiffContainer(diffAndToolbarSize.first);
        Disposer.register(this, myDiffView);

        JComponent revisionsSide = createRevisionsSide(diffAndToolbarSize.second);

        if (showRevisionsList()) {
            mySplitter = new Splitter(false, 0.3f);

            mySplitter.setFirstComponent(revisionsSide);
            mySplitter.setSecondComponent(myDiffView);

            restoreSplitterProportion();

            root.add(mySplitter);
            setDiffBorder(IdeBorderFactory.createBorder(SideBorder.TOP | SideBorder.LEFT));
        }
        else {
            setDiffBorder(IdeBorderFactory.createBorder(SideBorder.TOP | SideBorder.BOTTOM));
            root.add(myDiffView);
        }

        return root;
    }

    protected boolean showRevisionsList() {
        return true;
    }

    protected abstract void setDiffBorder(Border border);

    @Override
    public void dispose() {
        saveSplitterProportion();
        super.dispose();
    }

    protected abstract Pair<JComponent, Dimension> createDiffPanel(JPanel root, ExcludingTraversalPolicy traversalPolicy);

    private JComponent createRevisionsSide(Dimension prefToolBarSize) {
        ActionGroup actions = createRevisionsActions();

        myToolBar = createRevisionsToolbar(actions);
        myRevisionsList = new RevisionsList((first, last) -> scheduleDiffUpdate(Couple.of(first, last)));
        addPopupMenuToComponent(myRevisionsList.getComponent(), actions);

        JPanel result = new JPanel(new BorderLayout());
        JPanel toolBarPanel = new JPanel(new BorderLayout());
        myToolBar.setTargetComponent(result);
        toolBarPanel.add(myToolBar.getComponent());
        if (prefToolBarSize != null) {
            toolBarPanel.setPreferredSize(new Dimension(1, prefToolBarSize.height));
        }
        result.add(toolBarPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myRevisionsList.getComponent());
        scrollPane.setBorder(IdeBorderFactory.createBorder(SideBorder.TOP | SideBorder.RIGHT));
        result.add(scrollPane, BorderLayout.CENTER);

        return result;
    }

    private ActionToolbar createRevisionsToolbar(ActionGroup actions) {
        ActionManager am = ActionManager.getInstance();
        return am.createActionToolbar(ActionPlaces.UNKNOWN, actions, true);
    }

    private ActionGroup createRevisionsActions() {
        DefaultActionGroup result = new DefaultActionGroup();
        result.add(new RevertAction());
        result.add(new CreatePatchAction());
        result.add(AnSeparator.getInstance());
        result.add(new ContextHelpAction(getHelpId()));
        return result;
    }

    private void addPopupMenuToComponent(JComponent comp, final ActionGroup ag) {
        comp.addMouseListener(new PopupHandler() {
            @Override
            public void invokePopup(Component c, int x, int y) {
                ActionPopupMenu m = createPopupMenu(ag);
                m.getComponent().show(c, x, y);
            }
        });
    }

    private ActionPopupMenu createPopupMenu(ActionGroup ag) {
        ActionManager m = ActionManager.getInstance();
        return m.createActionPopupMenu(ActionPlaces.UNKNOWN, ag);
    }

    private void scheduleDiffUpdate(@Nullable Couple<Integer> toSelect) {
        doScheduleUpdate(UPDATE_DIFFS, () -> {
            synchronized (myModel) {
                if (toSelect == null) {
                    myModel.resetSelection();
                }
                else {
                    myModel.selectRevisions(toSelect.first, toSelect.second);
                }
                return doUpdateDiffs(myModel);
            }
        });
    }

    private void doScheduleUpdate(int id, final Computable<Runnable> update) {
        myUpdateQueue.queue(new Update(HistoryDialog.this, id) {
            @Override
            public boolean canEat(Update update1) {
                return getPriority() >= update1.getPriority();
            }

            @Override
            public void run() {
                if (isDisposed() || myProject.isDisposed()) {
                    return;
                }

                invokeAndWait(() -> {
                    if (isDisposed() || myProject.isDisposed()) {
                        return;
                    }

                    isUpdating = true;
                    updateActions();
                    myDiffView.startUpdating();
                });

                Runnable apply = null;
                try {
                    apply = update.compute();
                }
                catch (Exception e) {
                    LocalHistoryLog.LOG.error(e);
                }

                Runnable finalApply = apply;
                invokeAndWait(() -> {
                    if (isDisposed() || myProject.isDisposed()) {
                        return;
                    }

                    isUpdating = false;
                    if (finalApply != null) {
                        try {
                            finalApply.run();
                        }
                        catch (Exception e) {
                            LocalHistoryLog.LOG.error(e);
                        }
                    }
                    updateActions();
                    myDiffView.finishUpdating();
                });
            }
        });
    }

    private void invokeAndWait(Runnable runnable) {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                runnable.run();
            }
            else {
                SwingUtilities.invokeAndWait(runnable);
            }
        }
        catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @RequiredUIAccess
    protected void updateActions() {
        if (showRevisionsList()) {
            myToolBar.updateActionsImmediately();
        }
    }

    protected abstract Runnable doUpdateDiffs(T model);

    protected ContentDiffRequest createDifference(final FileDifferenceModel m) {
        final Ref<ContentDiffRequest> requestRef = new Ref<>();

        new Task.Modal(myProject, LocalHistoryBundle.message("message.processing.revisions"), false) {
            @Override
            public void run(@Nonnull ProgressIndicator i) {
                Application.get().runReadAction(() -> {
                    RevisionProcessingProgressAdapter p = new RevisionProcessingProgressAdapter(i);
                    p.processingLeftRevision();
                    DiffContent left = m.getLeftDiffContent(p);

                    p.processingRightRevision();
                    DiffContent right = m.getRightDiffContent(p);

                    requestRef.set(new SimpleDiffRequest(m.getTitle(), left, right, m.getLeftTitle(p), m.getRightTitle(p)));
                });
            }
        }.queue();

        return requestRef.get();
    }

    private void saveSplitterProportion() {
        SplitterProportionsData d = createSplitterData();
        d.saveSplitterProportions(mySplitter);
        d.externalizeToDimensionService(getPropertiesKey());
    }

    private void restoreSplitterProportion() {
        SplitterProportionsData d = createSplitterData();
        d.externalizeFromDimensionService(getPropertiesKey());
        d.restoreSplitterProportions(mySplitter);
    }

    private SplitterProportionsData createSplitterData() {
        return new SplitterProportionsDataImpl();
    }

    protected String getPropertiesKey() {
        return getClass().getName();
    }

    //todo
    protected abstract String getHelpId();

    protected void revert() {
        revert(myModel.createReverter());
    }

    private boolean isRevertEnabled() {
        return myModel.isRevertEnabled();
    }

    @RequiredUIAccess
    protected void revert(Reverter r) {
        try {
            if (!askForProceeding(r)) {
                return;
            }

            List<String> errors = r.checkCanRevert();
            if (!errors.isEmpty()) {
                showError(LocalHistoryBundle.message("message.cannot.revert.because", formatErrors(errors)));
                return;
            }
            r.revert();

            showNotification(r.getCommandName());
        }
        catch (IOException e) {
            showError(LocalHistoryBundle.message("message.error.during.revert", e));
        }
    }

    @RequiredUIAccess
    private boolean askForProceeding(Reverter r) throws IOException {
        List<String> questions = r.askUserForProceeding();
        if (questions.isEmpty()) {
            return true;
        }

        return Messages.showYesNoDialog(
            myProject,
            LocalHistoryBundle.message("message.do.you.want.to.proceed", formatQuestions(questions)),
            CommonLocalize.titleWarning().get(),
            UIUtil.getWarningIcon()
        ) == Messages.YES;
    }

    private String formatQuestions(List<String> questions) {
        // format into something like this:
        // 1) message one
        // message one continued
        // 2) message two
        // message one continued
        // ...

        if (questions.size() == 1) {
            return questions.get(0);
        }

        String result = "";
        for (int i = 0; i < questions.size(); i++) {
            result += (i + 1) + ") " + questions.get(i) + "\n";
        }
        return result.substring(0, result.length() - 1);
    }

    private void showNotification(String title) {
        SwingUtilities.invokeLater(() -> {
            Balloon b =
                JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(title, null, null, null)
                    .setFadeoutTime(3000)
                    .setShowCallout(false)
                    .createBalloon();

            Dimension size = myDiffView.getSize();
            RelativePoint point = new RelativePoint(myDiffView, new Point(size.width / 2, size.height / 2));
            b.show(point, Balloon.Position.above);
        });
    }

    private String formatErrors(List<String> errors) {
        if (errors.size() == 1) {
            return errors.get(0);
        }

        String result = "";
        for (String e : errors) {
            result += "\n    -" + e;
        }
        return result;
    }

    private boolean isCreatePatchEnabled() {
        return myModel.isCreatePatchEnabled();
    }

    @RequiredUIAccess
    private void createPatch() {
        try {
            if (!myModel.canPerformCreatePatch()) {
                showError(LocalHistoryBundle.message("message.cannot.create.patch.because.of.unavailable.content"));
                return;
            }

            CreatePatchConfigurationPanel p = new CreatePatchConfigurationPanel(myProject);
            p.setFileName(getDefaultPatchFile());
            p.setCommonParentPath(ChangesUtil.findCommonAncestor(myModel.getChanges()));
            if (!showAsDialog(p)) {
                return;
            }
            myModel.createPatch(p.getFileName(), p.getBaseDirName(), p.isReversePatch(), p.getEncoding());

            showNotification(LocalHistoryBundle.message("message.patch.created"));

            Platform.current().openFileInFileManager(new File(p.getFileName()), UIAccess.current());
        }
        catch (VcsException | IOException e) {
            showError(LocalHistoryBundle.message("message.error.during.create.patch", e));
        }
    }

    private File getDefaultPatchFile() {
        return FileUtil.findSequentNonexistentFile(new File(myProject.getBasePath()), "local_history", "patch");
    }

    @RequiredUIAccess
    private boolean showAsDialog(CreatePatchConfigurationPanel p) {
        DialogWrapper dialogWrapper = new MyDialogWrapper(myProject, p);
        dialogWrapper.setTitle(LocalHistoryLocalize.createPatchDialogTitle());
        dialogWrapper.setModal(true);
        dialogWrapper.show();
        return dialogWrapper.getExitCode() == DialogWrapper.OK_EXIT_CODE;
    }


    @RequiredUIAccess
    public void showError(String s) {
        Messages.showErrorDialog(myProject, s, CommonLocalize.titleError().get());
    }

    protected void showHelp() {
        HelpManager.getInstance().invokeHelp(getHelpId());
    }

    protected abstract class MyAction extends AnAction {
        protected MyAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, Image icon) {
            super(text, description, icon);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            doPerform(myModel);
        }

        protected abstract void doPerform(T model);

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabled(isEnabled());
        }

        private boolean isEnabled() {
            return !isUpdating && isEnabled(myModel);
        }

        protected abstract boolean isEnabled(T model);

        public void performIfEnabled() {
            if (isEnabled()) {
                doPerform(myModel);
            }
        }
    }

    private class RevertAction extends MyAction {
        public RevertAction() {
            super(LocalHistoryLocalize.actionRevert(), LocalizeValue.of(), AllIcons.Actions.Rollback);
        }

        @Override
        protected void doPerform(T model) {
            revert();
        }

        @Override
        protected boolean isEnabled(T model) {
            return isRevertEnabled();
        }
    }

    private class CreatePatchAction extends MyAction {
        public CreatePatchAction() {
            super(LocalHistoryLocalize.actionCreatePatch(), LocalizeValue.of(), PlatformIconGroup.filetypesPatch());
        }

        @Override
        protected void doPerform(T model) {
            createPatch();
        }

        @Override
        protected boolean isEnabled(T model) {
            return isCreatePatchEnabled();
        }
    }

    private static class RevisionProcessingProgressAdapter implements RevisionProcessingProgress {
        private final ProgressIndicator myIndicator;

        public RevisionProcessingProgressAdapter(ProgressIndicator i) {
            myIndicator = i;
        }

        @Override
        public void processingLeftRevision() {
            myIndicator.setText(LocalHistoryBundle.message("message.processing.left.revision"));
        }

        @Override
        public void processingRightRevision() {
            myIndicator.setText(LocalHistoryBundle.message("message.processing.right.revision"));
        }

        @Override
        public void processed(int percentage) {
            myIndicator.setFraction(percentage / 100.0);
        }
    }

    private static class MyDiffContainer extends JBLayeredPane implements Disposable {
        private AnimatedIconComponent myIcon = new AsyncProcessIcon.Big(this.getClass().getName());

        private JComponent myContent;
        private JComponent myLoadingPanel;

        private MyDiffContainer(JComponent content) {
            setLayout(new MyOverlayLayout());
            myContent = content;
            myLoadingPanel = new JPanel(new MyPanelLayout());
            myLoadingPanel.setOpaque(false);
            myLoadingPanel.add(myIcon);

            add(myContent);
            add(myLoadingPanel, JLayeredPane.POPUP_LAYER);

            finishUpdating();
        }

        @Override
        public void dispose() {
            myIcon.dispose();
        }

        public void startUpdating() {
            myLoadingPanel.setVisible(true);
            myIcon.resume();
        }

        public void finishUpdating() {
            myIcon.suspend();
            myLoadingPanel.setVisible(false);
        }

        private class MyOverlayLayout extends AbstractLayoutManager {
            @Override
            public void layoutContainer(Container parent) {
                myContent.setBounds(0, 0, getWidth(), getHeight());
                myLoadingPanel.setBounds(0, 0, getWidth(), getHeight());
            }

            @Override
            public Dimension preferredLayoutSize(Container parent) {
                return myContent.getPreferredSize();
            }
        }

        private class MyPanelLayout extends AbstractLayoutManager {
            @Override
            public void layoutContainer(Container parent) {
                Dimension size = myIcon.getPreferredSize();
                myIcon.setBounds((getWidth() - size.width) / 2, (getHeight() - size.height) / 2, size.width, size.height);
            }

            @Override
            public Dimension preferredLayoutSize(Container parent) {
                return myContent.getPreferredSize();
            }
        }
    }

    private static class MyDialogWrapper extends DialogWrapper {
        @Nonnull
        private final CreatePatchConfigurationPanel myPanel;

        protected MyDialogWrapper(@Nullable Project project, @Nonnull CreatePatchConfigurationPanel centralPanel) {
            super(project, true);
            myPanel = centralPanel;
            init();
            initValidation();
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            return myPanel.getPanel();
        }

        @Nullable
        @Override
        @RequiredUIAccess
        public JComponent getPreferredFocusedComponent() {
            return IdeFocusTraversalPolicy.getPreferredFocusedComponent(myPanel.getPanel());
        }

        @Nullable
        @Override
        @RequiredUIAccess
        protected ValidationInfo doValidate() {
            return myPanel.validateFields();
        }
    }
}
