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
package consulo.ide.impl.idea.openapi.vcs.history.impl;

import consulo.application.HelpManager;
import consulo.application.internal.BackgroundTaskUtil;
import consulo.application.progress.ProgressManager;
import consulo.dataContext.DataProvider;
import consulo.diff.Block;
import consulo.diff.DiffContentFactory;
import consulo.diff.DiffManager;
import consulo.diff.DiffRequestPanel;
import consulo.diff.content.DiffContent;
import consulo.diff.request.LoadingDiffRequest;
import consulo.diff.request.MessageDiffRequest;
import consulo.diff.request.NoDiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.diff.util.IntPair;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.versionControlSystem.action.VcsActions;
import consulo.ide.impl.idea.openapi.vcs.annotate.ShowAllAffectedGenericAction;
import consulo.ide.impl.idea.openapi.vcs.changes.issueLinks.IssueLinkHtmlRenderer;
import consulo.ide.impl.idea.openapi.vcs.changes.issueLinks.TableLinkMouseListener;
import consulo.ide.impl.idea.openapi.vcs.history.CurrentRevision;
import consulo.ide.impl.idea.openapi.vcs.history.FileHistoryPanelImpl;
import consulo.ide.impl.idea.openapi.vcs.history.StandardDiffFromHistoryHandler;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.ex.awt.table.TableView;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.PopupUtil;
import consulo.ui.ex.awt.util.Update;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.function.Predicates;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.history.*;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static consulo.util.lang.ObjectUtil.notNull;

public class VcsSelectionHistoryDialog extends FrameWrapper implements DataProvider {
    private static final Logger LOG = Logger.getInstance(VcsSelectionHistoryDialog.class);

    private static final VcsRevisionNumber LOCAL_REVISION_NUMBER = new VcsRevisionNumber() {
        @Override
        public String asString() {
            return "Local Changes";
        }

        @Override
        public int compareTo(@Nonnull VcsRevisionNumber vcsRevisionNumber) {
            return 0;
        }

        @Override
        public String toString() {
            return asString();
        }
    };

    private static final float DIFF_SPLITTER_PROPORTION = 0.5f;
    private static final float COMMENTS_SPLITTER_PROPORTION = 0.8f;
    private static final String DIFF_SPLITTER_PROPORTION_KEY = "file.history.selection.diff.splitter.proportion";
    private static final String COMMENTS_SPLITTER_PROPORTION_KEY = "file.history.selection.comments.splitter.proportion";

    private static final Block EMPTY_BLOCK = new Block("", 0, 0);

    @Nonnull
    private final Project myProject;
    @Nonnull
    private final VirtualFile myFile;
    @Nonnull
    private final AbstractVcs myActiveVcs;
    private final String myHelpId;

    private final List<VcsFileRevision> myRevisions = new ArrayList<>();
    private final CurrentRevision myLocalRevision;

    private final ListTableModel<VcsFileRevision> myListModel;
    private final TableView<VcsFileRevision> myList;

    private final Splitter mySplitter;
    private final DiffRequestPanel myDiffPanel;
    private final JCheckBox myChangesOnlyCheckBox = new JCheckBox(VcsLocalize.checkboxShowChangedRevisionsOnly().get());
    private final JLabel myStatusLabel = new JBLabel();
    private final AnimatedIconComponent myStatusSpinner = new AsyncProcessIcon("VcsSelectionHistoryDialog");
    private final JEditorPane myComments;

    @Nonnull
    private final MergingUpdateQueue myUpdateQueue;
    @Nonnull
    private final BlockLoader myBlockLoader;

    private boolean myIsDuringUpdate = false;
    private boolean myIsDisposed = false;

    public VcsSelectionHistoryDialog(
        @Nonnull Project project,
        @Nonnull VirtualFile file,
        @Nonnull Document document,
        @Nonnull VcsHistoryProvider vcsHistoryProvider,
        @Nonnull VcsHistorySession session,
        @Nonnull AbstractVcs vcs,
        int selectionStart,
        int selectionEnd,
        @Nonnull String title
    ) {
        super(project);
        myProject = project;
        myFile = file;
        myActiveVcs = vcs;
        myHelpId = notNull(vcsHistoryProvider.getHelpId(), "reference.dialogs.vcs.selection.history");

        myComments = new JEditorPane(UIUtil.HTML_MIME, "");
        myComments.setPreferredSize(new Dimension(150, 100));
        myComments.setEditable(false);
        myComments.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);

        JRootPane rootPane = ((RootPaneContainer)getFrame()).getRootPane();
        VcsDependentHistoryComponents components = vcsHistoryProvider.getUICustomization(session, rootPane);

        ColumnInfo[] defaultColumns = new ColumnInfo[]{
            new FileHistoryPanelImpl.RevisionColumnInfo(null),
            new FileHistoryPanelImpl.DateColumnInfo(),
            new FileHistoryPanelImpl.AuthorColumnInfo(),
            new FileHistoryPanelImpl.MessageColumnInfo(project)};
        ColumnInfo[] additionalColumns = notNull(components.getColumns(), ColumnInfo.EMPTY_ARRAY);
        myListModel = new ListTableModel<>(ArrayUtil.mergeArrays(defaultColumns, additionalColumns));
        myListModel.setSortable(false);
        myList = new TableView<>(myListModel);
        new TableLinkMouseListener().installOn(myList);

        myList.getEmptyText().setText(VcsLocalize.historyEmpty());

        myDiffPanel = DiffManager.getInstance().createRequestPanel(myProject, this, getFrame());
        myUpdateQueue = new MergingUpdateQueue("VcsSelectionHistoryDialog", 300, true, myList, this);

        myLocalRevision = new CurrentRevision(file, LOCAL_REVISION_NUMBER);
        myRevisions.add(myLocalRevision);
        myRevisions.addAll(session.getRevisionList());

        mySplitter = new JBSplitter(true, DIFF_SPLITTER_PROPORTION_KEY, DIFF_SPLITTER_PROPORTION);

        mySplitter.setFirstComponent(myDiffPanel.getComponent());
        mySplitter.setSecondComponent(createBottomPanel(components.getDetailsComponent()));

        ListSelectionListener selectionListener = e -> {
            VcsFileRevision revision;
            if (myList.getSelectedRowCount() == 1 && !myList.isEmpty()) {
                revision = myList.getItems().get(myList.getSelectedRow());
                String message = IssueLinkHtmlRenderer.formatTextIntoHtml(myProject, revision.getCommitMessage());
                myComments.setText(message);
                myComments.setCaretPosition(0);
            }
            else {
                revision = null;
                myComments.setText("");
            }
            if (components.getRevisionListener() != null) {
                components.getRevisionListener().accept(revision);
            }
            updateDiff();
        };
        myList.getSelectionModel().addListSelectionListener(selectionListener);

        VcsConfiguration configuration = VcsConfiguration.getInstance(myProject);
        myChangesOnlyCheckBox.setSelected(configuration.SHOW_ONLY_CHANGED_IN_SELECTION_DIFF);
        myChangesOnlyCheckBox.addActionListener(e -> {
            configuration.SHOW_ONLY_CHANGED_IN_SELECTION_DIFF = myChangesOnlyCheckBox.isSelected();
            updateRevisionsList();
        });

        DefaultActionGroup popupActions = new DefaultActionGroup();
        popupActions.add(new MyDiffAction());
        popupActions.add(new MyDiffLocalAction());
        popupActions.add(ActionManager.getInstance().getAction(VcsActions.ACTION_SHOW_ALL_AFFECTED));
        popupActions.add(ActionManager.getInstance().getAction(VcsActions.ACTION_COPY_REVISION_NUMBER));
        PopupHandler.installPopupHandler(myList, popupActions, ActionPlaces.UPDATE_POPUP, ActionManager.getInstance());

        for (AnAction action : popupActions.getChildren(null)) {
            action.registerCustomShortcutSet(action.getShortcutSet(), mySplitter);
        }

        setTitle(title);
        setComponent(mySplitter);
        setPreferredFocusedComponent(myList);
        setDimensionKey("VCS.FileHistoryDialog");
        closeOnEsc();


        myBlockLoader = new BlockLoader(myRevisions, myFile, document, selectionStart, selectionEnd) {
            @Override
            protected void notifyError(@Nonnull VcsException e) {
                SwingUtilities.invokeLater(() -> {
                    VcsSelectionHistoryDialog dialog = VcsSelectionHistoryDialog.this;
                    if (dialog.isDisposed() || !dialog.getFrame().isShowing()) {
                        return;
                    }
                    PopupUtil.showBalloonForComponent(mySplitter, canNoLoadMessage(e), NotificationType.ERROR, true, myProject);
                });
            }

            @Override
            protected void notifyUpdate() {
                myUpdateQueue.queue(new Update(this) {
                    @Override
                    public void run() {
                        updateStatusPanel();
                        updateRevisionsList();
                    }
                });
            }
        };
        myBlockLoader.start(this);

        updateRevisionsList();
        myList.getSelectionModel().setSelectionInterval(0, 0);
    }

    @Nonnull
    private static String canNoLoadMessage(@Nullable VcsException e) {
        return "Can not load revision contents" + (e != null ? ": " + e.getMessage() : "");
    }

    private void updateRevisionsList() {
        if (myIsDuringUpdate) {
            return;
        }
        try {
            myIsDuringUpdate = true;

            List<VcsFileRevision> newItems;
            if (myChangesOnlyCheckBox.isSelected()) {
                newItems = filteredRevisions();
            }
            else {
                newItems = myRevisions;
            }

            IntPair range = getSelectedRevisionsRange();
            List<VcsFileRevision> oldSelection = myRevisions.subList(range.val1, range.val2);

            myListModel.setItems(newItems);

            myList.setSelection(oldSelection);
            if (myList.getSelectedRowCount() == 0) {
                int index = getNearestVisibleRevision(ContainerUtil.getFirstItem(oldSelection));
                myList.getSelectionModel().setSelectionInterval(index, index);
            }
        }
        finally {
            myIsDuringUpdate = false;
        }

        updateDiff();
    }

    private void updateStatusPanel() {
        BlockData data = myBlockLoader.getLoadedData();

        if (data.isLoading()) {
            VcsFileRevision revision = data.getCurrentLoadingRevision();
            if (revision != null) {
                myStatusLabel.setText("<html>Loading revision <tt>" + revision.getRevisionNumber() + "</tt></html>");
            }
            else {
                myStatusLabel.setText("Loading...");
            }

            myStatusSpinner.resume();
            myStatusSpinner.setVisible(true);
        }
        else {
            myStatusLabel.setText("");
            myStatusSpinner.suspend();
            myStatusSpinner.setVisible(false);
        }
    }

    @Nonnull
    private IntPair getSelectedRevisionsRange() {
        List<VcsFileRevision> selection = myList.getSelectedObjects();
        if (selection.isEmpty()) {
            return new IntPair(0, 0);
        }
        int startIndex = myRevisions.indexOf(ContainerUtil.getFirstItem(selection));
        int endIndex = myRevisions.indexOf(ContainerUtil.getLastItem(selection));
        return new IntPair(startIndex, endIndex + 1);
    }

    private int getNearestVisibleRevision(@Nullable VcsFileRevision anchor) {
        int anchorIndex = myRevisions.indexOf(anchor);
        if (anchorIndex == -1) {
            return 0;
        }

        for (int i = anchorIndex - 1; i > 0; i--) {
            int index = myListModel.indexOf(myRevisions.get(i));
            if (index != -1) {
                return index;
            }
        }
        return 0;
    }

    private List<VcsFileRevision> filteredRevisions() {
        ArrayList<VcsFileRevision> result = new ArrayList<>();
        BlockData data = myBlockLoader.getLoadedData();

        int firstRevision;
        boolean foundInitialRevision = false;
        for (firstRevision = myRevisions.size() - 1; firstRevision > 0; firstRevision--) {
            Block block = data.getBlock(firstRevision);
            if (block == EMPTY_BLOCK) {
                foundInitialRevision = true;
            }
            if (block != null && block != EMPTY_BLOCK) {
                break;
            }
        }
        if (!foundInitialRevision && data.isLoading()) {
            firstRevision = myRevisions.size() - 1;
        }

        result.add(myRevisions.get(firstRevision));

        for (int i = firstRevision - 1; i >= 0; i--) {
            Block block1 = data.getBlock(i + 1);
            Block block2 = data.getBlock(i);
            if (block1 == null || block2 == null) {
                continue;
            }
            if (block1.getLines().equals(block2.getLines())) {
                continue;
            }
            result.add(myRevisions.get(i));
        }

        Collections.reverse(result);
        return result;
    }

    private void updateDiff() {
        if (myIsDisposed || myIsDuringUpdate) {
            return;
        }

        if (myList.getSelectedRowCount() == 0) {
            myDiffPanel.setRequest(NoDiffRequest.INSTANCE);
            return;
        }

        int count = myRevisions.size();
        IntPair range = getSelectedRevisionsRange();
        int revIndex1 = range.val2;
        int revIndex2 = range.val1;

        if (revIndex1 == count && revIndex2 == count) {
            myDiffPanel.setRequest(NoDiffRequest.INSTANCE);
            return;
        }

        BlockData blockData = myBlockLoader.getLoadedData();
        DiffContent content1 = createDiffContent(revIndex1, blockData);
        DiffContent content2 = createDiffContent(revIndex2, blockData);
        String title1 = createDiffContentTitle(revIndex1);
        String title2 = createDiffContentTitle(revIndex2);
        if (content1 != null && content2 != null) {
            myDiffPanel.setRequest(new SimpleDiffRequest(null, content1, content2, title1, title2), new IntPair(revIndex1, revIndex2));
            return;
        }

        if (blockData.isLoading()) {
            myDiffPanel.setRequest(new LoadingDiffRequest());
        }
        else {
            myDiffPanel.setRequest(new MessageDiffRequest(canNoLoadMessage(blockData.getException())));
        }
    }

    @Nullable
    private String createDiffContentTitle(int index) {
        if (index >= myRevisions.size()) {
            return null;
        }
        return VcsLocalize.diffContentTitleRevisionNumber(myRevisions.get(index).getRevisionNumber()).get();
    }

    @Nullable
    private DiffContent createDiffContent(int index, @Nonnull BlockData data) {
        if (index >= myRevisions.size()) {
            return DiffContentFactory.getInstance().createEmpty();
        }
        Block block = data.getBlock(index);
        if (block == null) {
            return null;
        }
        if (block == EMPTY_BLOCK) {
            return DiffContentFactory.getInstance().createEmpty();
        }
        return DiffContentFactory.getInstance().create(block.getBlockContent(), myFile.getFileType());
    }

    @Override
    public void dispose() {
        myIsDisposed = true;
        super.dispose();
    }

    private JComponent createBottomPanel(JComponent addComp) {
        JBSplitter splitter = new JBSplitter(true, COMMENTS_SPLITTER_PROPORTION_KEY, COMMENTS_SPLITTER_PROPORTION);
        splitter.setDividerWidth(4);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(ScrollPaneFactory.createScrollPane(myList), BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout());
        statusPanel.add(myStatusSpinner);
        statusPanel.add(myStatusLabel);

        JPanel separatorPanel = new JPanel(new BorderLayout());
        separatorPanel.add(myChangesOnlyCheckBox, BorderLayout.WEST);
        separatorPanel.add(statusPanel, BorderLayout.EAST);

        tablePanel.add(separatorPanel, BorderLayout.NORTH);

        splitter.setFirstComponent(tablePanel);
        splitter.setSecondComponent(createComments(addComp));

        return splitter;
    }

    private JComponent createComments(JComponent addComp) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(new JLabel("Commit ReflectionMessage:"), BorderLayout.NORTH);
        panel.add(ScrollPaneFactory.createScrollPane(myComments), BorderLayout.CENTER);

        Splitter splitter = new Splitter(false);
        splitter.setFirstComponent(panel);
        splitter.setSecondComponent(addComp);
        return splitter;
    }

    @Override
    public Object getData(@Nonnull Key<?> dataId) {
        if (Project.KEY == dataId) {
            return myProject;
        }
        else if (VcsDataKeys.VCS_VIRTUAL_FILE == dataId) {
            return myFile;
        }
        else if (VcsDataKeys.VCS_FILE_REVISION == dataId) {
            VcsFileRevision selectedObject = myList.getSelectedObject();
            return selectedObject instanceof CurrentRevision ? null : selectedObject;
        }
        else if (VcsDataKeys.VCS_FILE_REVISIONS == dataId) {
            List<VcsFileRevision> revisions = ContainerUtil.filter(myList.getSelectedObjects(), Predicates.notEqualTo(myLocalRevision));
            return ArrayUtil.toObjectArray(revisions, VcsFileRevision.class);
        }
        else if (VcsDataKeys.VCS == dataId) {
            return myActiveVcs.getKeyInstanceMethod();
        }
        else if (HelpManager.HELP_ID == dataId) {
            return myHelpId;
        }
        return null;
    }

    private class MyDiffAction extends DumbAwareAction {
        public MyDiffAction() {
            super(VcsLocalize.actionNameCompare(), VcsLocalize.actionDescriptionCompare(), PlatformIconGroup.actionsDiff());
            setShortcutSet(CommonShortcuts.getDiff());
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabled(
                myList.getSelectedRowCount() > 1 || myList.getSelectedRowCount() == 1 && myList.getSelectedObject() != myLocalRevision
            );
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            IntPair range = getSelectedRevisionsRange();

            VcsFileRevision beforeRevision = range.val2 < myRevisions.size() ? myRevisions.get(range.val2) : VcsFileRevision.NULL;
            VcsFileRevision afterRevision = myRevisions.get(range.val1);

            FilePath filePath = VcsUtil.getFilePath(myFile);

            if (range.val2 - range.val1 > 1) {
                getDiffHandler().showDiffForTwo(myProject, filePath, beforeRevision, afterRevision);
            }
            else {
                getDiffHandler().showDiffForOne(e, myProject, filePath, beforeRevision, afterRevision);
            }
        }
    }

    private class MyDiffLocalAction extends DumbAwareAction {
        public MyDiffLocalAction() {
            super(
                VcsLocalize.showDiffWithLocalActionText(),
                VcsLocalize.showDiffWithLocalActionDescription(),
                PlatformIconGroup.actionsDiffwithcurrent()
            );
            setShortcutSet(ActionManager.getInstance().getAction("Vcs.ShowDiffWithLocal").getShortcutSet());
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabled(myList.getSelectedRowCount() == 1 && myList.getSelectedObject() != myLocalRevision);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            VcsFileRevision revision = myList.getSelectedObject();
            if (revision == null) {
                return;
            }

            FilePath filePath = VcsUtil.getFilePath(myFile);

            getDiffHandler().showDiffForTwo(myProject, filePath, revision, myLocalRevision);
        }
    }

    @Nonnull
    private DiffFromHistoryHandler getDiffHandler() {
        VcsHistoryProvider historyProvider = myActiveVcs.getVcsHistoryProvider();
        DiffFromHistoryHandler handler = historyProvider != null ? historyProvider.getHistoryDiffHandler() : null;
        return handler != null ? handler : new StandardDiffFromHistoryHandler();
    }

    private abstract static class BlockLoader {
        @Nonnull
        private final Object LOCK = new Object();

        @Nonnull
        private final List<VcsFileRevision> myRevisions;
        @Nonnull
        private final Charset myCharset;

        @Nonnull
        private final List<Block> myBlocks = new ArrayList<>();
        @Nullable
        private VcsException myException;
        private boolean myIsLoading = true;
        private VcsFileRevision myCurrentLoadingRevision;

        public BlockLoader(
            @Nonnull List<VcsFileRevision> revisions,
            @Nonnull VirtualFile file,
            @Nonnull Document document,
            int selectionStart,
            int selectionEnd
        ) {
            myRevisions = revisions;
            myCharset = file.getCharset();

            String[] lastContent = Block.tokenize(document.getText());
            myBlocks.add(new Block(lastContent, selectionStart, selectionEnd + 1));
        }

        @Nonnull
        public BlockData getLoadedData() {
            synchronized (LOCK) {
                return new BlockData(myIsLoading, new ArrayList<>(myBlocks), myException, myCurrentLoadingRevision);
            }
        }

        public void start(@Nonnull Disposable disposable) {
            BackgroundTaskUtil.executeOnPooledThread(disposable, () -> {
                try {
                    // first block is loaded in constructor
                    for (int index = 1; index < myRevisions.size(); index++) {
                        ProgressManager.checkCanceled();

                        Block block = myBlocks.get(index - 1);
                        VcsFileRevision revision = myRevisions.get(index);

                        synchronized (LOCK) {
                            myCurrentLoadingRevision = revision;
                        }
                        notifyUpdate();

                        Block previousBlock = createBlock(block, revision);

                        synchronized (LOCK) {
                            myBlocks.add(previousBlock);
                        }
                        notifyUpdate();
                    }
                }
                catch (VcsException e) {
                    synchronized (LOCK) {
                        myException = e;
                    }
                    notifyError(e);
                }
                finally {
                    synchronized (LOCK) {
                        myIsLoading = false;
                        myCurrentLoadingRevision = null;
                    }
                    notifyUpdate();
                }
            });
        }

        protected abstract void notifyError(@Nonnull VcsException e);

        protected abstract void notifyUpdate();

        @Nonnull
        private Block createBlock(@Nonnull Block block, @Nonnull VcsFileRevision revision) throws VcsException {
            if (block == EMPTY_BLOCK) {
                return EMPTY_BLOCK;
            }

            String revisionContent = loadContents(revision);

            Block newBlock = block.createPreviousBlock(revisionContent);
            return newBlock.getStart() != newBlock.getEnd() ? newBlock : EMPTY_BLOCK;
        }

        @Nonnull
        private String loadContents(@Nonnull VcsFileRevision revision) throws VcsException {
            try {
                byte[] bytes = revision.loadContent();
                return new String(bytes, myCharset);
            }
            catch (IOException e) {
                throw new VcsException(e);
            }
        }
    }

    private static class BlockData {
        private final boolean myIsLoading;
        @Nonnull
        private final List<Block> myBlocks;
        @Nullable
        private final VcsException myException;
        @Nullable
        private final VcsFileRevision myCurrentLoadingRevision;

        public BlockData(
            boolean isLoading,
            @Nonnull List<Block> blocks,
            @Nullable VcsException exception,
            @Nullable VcsFileRevision currentLoadingRevision
        ) {
            myIsLoading = isLoading;
            myBlocks = blocks;
            myException = exception;
            myCurrentLoadingRevision = currentLoadingRevision;
        }

        public boolean isLoading() {
            return myIsLoading;
        }

        @Nullable
        public VcsException getException() {
            return myException;
        }

        @Nullable
        public VcsFileRevision getCurrentLoadingRevision() {
            return myCurrentLoadingRevision;
        }

        @Nullable
        public Block getBlock(int index) {
            if (myBlocks.size() <= index) {
                return null;
            }
            return myBlocks.get(index);
        }
    }
}
