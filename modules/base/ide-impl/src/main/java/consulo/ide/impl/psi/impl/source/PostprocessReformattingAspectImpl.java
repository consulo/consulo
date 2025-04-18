// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.impl.source;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.event.ApplicationListener;
import consulo.application.progress.ProgressManager;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.document.util.TextRangeUtil;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.ide.impl.psi.impl.source.codeStyle.CodeFormatterFacade;
import consulo.ide.impl.psi.impl.source.codeStyle.CodeStyleManagerImpl;
import consulo.ide.impl.psi.impl.source.codeStyle.IndentHelperImpl;
import consulo.ide.impl.psi.impl.source.tree.RecursiveTreeElementVisitor;
import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenType;
import consulo.language.codeStyle.*;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.ast.*;
import consulo.language.impl.file.AbstractFileViewProvider;
import consulo.language.impl.internal.file.FileManager;
import consulo.language.impl.internal.pom.ChangeInfoImpl;
import consulo.language.impl.internal.pom.TreeChangeImpl;
import consulo.language.impl.internal.psi.PsiDocumentManagerBase;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.impl.psi.CodeEditUtil;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.pom.PomModelAspect;
import consulo.language.pom.PomModelAspectRegistrator;
import consulo.language.pom.TreeAspect;
import consulo.language.pom.event.ChangeInfo;
import consulo.language.pom.event.PomModelEvent;
import consulo.language.pom.event.TreeChange;
import consulo.language.pom.event.TreeChangeEvent;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.util.collection.MultiMap;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@ExtensionImpl(id = "postprocessReformattingAspect", order = "last")
public class PostprocessReformattingAspectImpl implements PostprocessReformattingAspect {
    private static final Logger LOG = Logger.getInstance(PostprocessReformattingAspectImpl.class);
    private final Project myProject;
    private final PsiManager myPsiManager;
    private static final Key<Throwable> REFORMAT_ORIGINATOR = Key.create("REFORMAT_ORIGINATOR");
    private static final Key<Boolean> REPARSE_PENDING = Key.create("REPARSE_PENDING");

    private static class Holder {
        private static final boolean STORE_REFORMAT_ORIGINATOR_STACKTRACE = Application.get().isInternal();
    }

    private final ThreadLocal<Context> myContext = ThreadLocal.withInitial(Context::new);

    @Inject
    public PostprocessReformattingAspectImpl(Project project, PsiManager psiManager, CommandProcessor processor) {
        myProject = project;
        myPsiManager = psiManager;

        project.getApplication().addApplicationListener(
            new ApplicationListener() {
                @Override
                public void writeActionStarted(@Nonnull Object action) {
                    if (processor != null) {
                        Project project1 = processor.getCurrentCommandProject();
                        if (project1 == myProject) {
                            incrementPostponedCounter();
                        }
                    }
                }

                @Override
                @RequiredUIAccess
                public void writeActionFinished(@Nonnull Object action) {
                    if (processor != null) {
                        Project project1 = processor.getCurrentCommandProject();
                        if (project1 == myProject) {
                            decrementPostponedCounter();
                        }
                    }
                }
            },
            project
        );
    }

    @Override
    public void register(@Nonnull PomModelAspectRegistrator registrator) {
        TreeAspect treeAspect = registrator.getModelAspect(TreeAspect.class);

        registrator.register(PostprocessReformattingAspect.class, this, Set.of(treeAspect));
    }

    @Override
    public void disablePostprocessFormattingInside(@Nonnull Runnable runnable) {
        disablePostprocessFormattingInside(() -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public <T> T disablePostprocessFormattingInside(@Nonnull Supplier<T> computable) {
        try {
            getContext().myDisabledCounter++;
            return computable.get();
        }
        finally {
            getContext().myDisabledCounter--;
            LOG.assertTrue(getContext().myDisabledCounter > 0 || !isDisabled());
        }
    }

    @Override
    @RequiredUIAccess
    public void postponeFormattingInside(@Nonnull Runnable runnable) {
        postponeFormattingInside(() -> {
            runnable.run();
            return null;
        });
    }

    @Override
    @RequiredUIAccess
    public <T> T postponeFormattingInside(@Nonnull Supplier<T> computable) {
        myProject.getApplication().assertIsDispatchThread();
        try {
            incrementPostponedCounter();
            return computable.get();
        }
        finally {
            decrementPostponedCounter();
        }
    }

    private void incrementPostponedCounter() {
        getContext().myPostponedCounter++;
    }

    @RequiredUIAccess
    private void decrementPostponedCounter() {
        Application application = myProject.getApplication();
        myProject.getApplication().assertIsWriteThread();
        if (--getContext().myPostponedCounter == 0) {
            if (application.isWriteAccessAllowed()) {
                doPostponedFormatting();
            }
            else {
                application.runWriteAction((Runnable)this::doPostponedFormatting);
            }
        }
    }

    private static void atomic(@Nonnull Runnable r) {
        ProgressManager.getInstance().executeNonCancelableSection(r);
    }

    @Override
    public void update(@Nonnull PomModelEvent event) {
        atomic(new Runnable() {
            @Override
            public void run() {
                if (isDisabled() || getContext().myPostponedCounter == 0 && !Application.get().isUnitTestMode()) {
                    return;
                }
                TreeAspect treeAspect = myProject.getExtensionPoint(PomModelAspect.class).findExtensionOrFail(TreeAspect.class);
                TreeChangeEvent changeSet = (TreeChangeEvent)event.getChangeSet(treeAspect);
                if (changeSet == null) {
                    return;
                }
                PsiElement psiElement = changeSet.getRootElement().getPsi();
                if (psiElement == null) {
                    return;
                }
                PsiFile containingFile = InjectedLanguageManager.getInstance(psiElement.getProject()).getTopLevelFile(psiElement);
                FileViewProvider viewProvider = containingFile.getViewProvider();

                if (!viewProvider.isEventSystemEnabled()) {
                    return;
                }
                getContext().myUpdatedProviders.putValue(viewProvider, (FileElement)containingFile.getNode());
                for (ASTNode node : changeSet.getChangedElements()) {
                    TreeChange treeChange = changeSet.getChangesByElement(node);
                    for (ASTNode affectedChild : treeChange.getAffectedChildren()) {
                        if (changeMightBreakPsiTextConsistency(affectedChild)) {
                            containingFile.putUserData(REPARSE_PENDING, true);
                        }
                        else if (leavesEmptyRangeAtEdge((TreeChangeImpl)treeChange, affectedChild) && hasRaiseableEdgeChild(node)) {
                            getContext().myRaisingCandidates.putValue(viewProvider, node);
                        }

                        ChangeInfo childChange = treeChange.getChangeByChild(affectedChild);
                        switch (childChange.getChangeType()) {
                            case ChangeInfo.ADD:
                            case ChangeInfo.REPLACE:
                                postponeFormatting(viewProvider, affectedChild);
                                break;
                            case ChangeInfo.CONTENTS_CHANGED:
                                if (!CodeEditUtil.isNodeGenerated(affectedChild)) {
                                    ((TreeElement)affectedChild).acceptTree(new RecursiveTreeElementWalkingVisitor() {
                                        @Override
                                        protected void visitNode(TreeElement element) {
                                            if (CodeEditUtil.isNodeGenerated(element)
                                                && CodeEditUtil.isSuspendedNodesReformattingAllowed()) {
                                                postponeFormatting(viewProvider, element);
                                                return;
                                            }
                                            super.visitNode(element);
                                        }
                                    });
                                }
                                break;
                        }
                    }
                }
            }

            private boolean changeMightBreakPsiTextConsistency(ASTNode child) {
                return TreeUtil.containsOuterLanguageElements(child) || isRightAfterErrorElement(child);
            }

            private boolean leavesEmptyRangeAtEdge(TreeChangeImpl treeChange, ASTNode child) {
                ChangeInfoImpl info = treeChange.getChangeByChild(child);
                TreeElement newChild = info.getNewChild();
                return (newChild == null || newChild.getTextLength() == 0) && wasEdgeChild(treeChange, info.getOldChild());
            }

            private boolean wasEdgeChild(TreeChangeImpl treeChange, TreeElement oldChild) {
                List<TreeElement> initial = treeChange.getInitialChildren();
                return initial.size() > 0 && (oldChild == initial.get(0) || oldChild == initial.get(initial.size() - 1));
            }

            private boolean isRightAfterErrorElement(ASTNode _node) {
                Function<ASTNode, ASTNode> prevNode = node -> {
                    ASTNode prev = node.getTreePrev();
                    return prev != null ? TreeUtil.getLastChild(prev) : node.getTreeParent();
                };
                return JBIterable.generate(_node, prevNode)
                    .skip(1)
                    .takeWhile(e -> e instanceof PsiWhiteSpace || e.getTextLength() == 0)
                    .filter(PsiErrorElement.class)
                    .isNotEmpty();
            }
        });
    }

    @Override
    public void doPostponedFormatting() {
        atomic(() -> {
            if (isDisabled()) {
                return;
            }
            try {
                FileViewProvider[] viewProviders = getContext().myUpdatedProviders.keySet().toArray(new FileViewProvider[0]);
                for (FileViewProvider viewProvider : viewProviders) {
                    doPostponedFormatting(viewProvider);
                }
            }
            catch (Exception e) {
                LOG.error(e);
            }
            finally {
                LOG.assertTrue(getContext().myReformatElements.isEmpty(), getContext().myReformatElements);
            }
        });
    }

    @Override
    public void doPostponedFormatting(@Nonnull FileViewProvider viewProvider) {
        postponedFormattingImpl(viewProvider);
    }

    private void postponedFormattingImpl(@Nonnull FileViewProvider viewProvider) {
        atomic(() -> {
            if (isDisabled()) {
                return;
            }

            try {
                disablePostprocessFormattingInside(() -> doPostponedFormattingInner(viewProvider));
            }
            finally {
                getContext().myUpdatedProviders.remove(viewProvider);
                getContext().myRaisingCandidates.remove(viewProvider);
                getContext().myReformatElements.remove(viewProvider);
                viewProvider.putUserData(REFORMAT_ORIGINATOR, null);
            }
        });
    }

    @Override
    public boolean isViewProviderLocked(@Nonnull FileViewProvider fileViewProvider) {
        return getContext().myReformatElements.containsKey(fileViewProvider);
    }

    public static void assertDocumentChangeIsAllowed(@Nonnull PsiFile file) {
        PostprocessReformattingAspectImpl reformattingAspect =
            (PostprocessReformattingAspectImpl)PostprocessReformattingAspect.getInstance(file.getProject());
        reformattingAspect.assertDocumentChangeIsAllowed(file.getViewProvider());
    }

    /**
     * Checks that view provider doesn't contain any PSI modifications which will be used in postponed formatting and may conflict with
     * changes made to the document.
     *
     * @param viewProvider The view provider to validate.
     * @throws RuntimeException If the assertion fails.
     */
    public void assertDocumentChangeIsAllowed(@Nonnull FileViewProvider viewProvider) {
        if (isViewProviderLocked(viewProvider)) {
            Throwable cause = viewProvider.getUserData(REFORMAT_ORIGINATOR);
            String message = "Document is locked by write PSI operations. " +
                "Use PsiDocumentManager.doPostponedOperationsAndUnblockDocument() to commit PSI changes to the document." +
                "\nUnprocessed elements: " +
                dumpUnprocessedElements(viewProvider) +
                (cause == null ? "" : " \nSee cause stacktrace for the reason to lock.");
            throw cause == null ? new RuntimeException(message) : new RuntimeException(message, cause);
        }
    }

    private String dumpUnprocessedElements(@Nonnull FileViewProvider provider) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        List<ASTNode> nodes = myContext.get().myReformatElements.get(provider);
        for (ASTNode node : nodes) {
            if (count >= 5) {
                sb.append(" and ").append(nodes.size() - count).append(" more.");
                break;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(node.getElementType()).append(node.getTextRange());
            count++;
        }
        return sb.toString();
    }

    private void postponeFormatting(@Nonnull FileViewProvider viewProvider, @Nonnull ASTNode child) {
        if (!CodeEditUtil.isNodeGenerated(child) && child.getElementType() != TokenType.WHITE_SPACE) {
            int oldIndent = CodeEditUtil.getOldIndentation(child);
            LOG.assertTrue(
                oldIndent >= 0,
                "for not generated items old indentation must be defined: element=" + child + ", text=" + child.getText()
            );
        }
        List<ASTNode> list = getContext().myReformatElements.get(viewProvider);
        if (list == null) {
            list = new ArrayList<>();
            getContext().myReformatElements.put(viewProvider, list);
            if (Holder.STORE_REFORMAT_ORIGINATOR_STACKTRACE) {
                viewProvider.putUserData(REFORMAT_ORIGINATOR, new Throwable());
            }
        }
        list.add(child);
    }

    @RequiredUIAccess
    private void doPostponedFormattingInner(@Nonnull FileViewProvider key) {
        List<ASTNode> astNodes = getContext().myReformatElements.remove(key);
        Document document = key.getDocument();
        // Sort ranges by end offsets so that we won't need any offset adjustment after reformat or reindent
        if (document == null) {
            return;
        }

        VirtualFile virtualFile = key.getVirtualFile();
        if (!virtualFile.isValid()) {
            return;
        }

        if (key.getManager() instanceof PsiManagerEx managerEx) {
            FileManager fileManager = managerEx.getFileManager();
            FileViewProvider viewProvider = fileManager.findCachedViewProvider(virtualFile);
            if (viewProvider != key) { // viewProvider was invalidated e.g. due to language level change
                if (viewProvider == null) {
                    viewProvider = fileManager.findViewProvider(virtualFile);
                }
                if (viewProvider != null) {
                    key = viewProvider;
                    astNodes = getContext().myReformatElements.remove(key);
                }
            }
        }

        Collection<Disposable> toDispose = Collections.emptyList();
        try {
            // process all roots in viewProvider to find marked for reformat before elements and create appropriate range markers
            Set<PostprocessFormattingTask> postProcessTasks = new TreeSet<>();
            handleReformatMarkers(key, postProcessTasks);
            toDispose = new ArrayList<>(postProcessTasks);

            // then we create ranges by changed nodes. One per node. There ranges can intersect. Ranges are sorted by end offset.
            if (astNodes != null) {
                createActionsMap(astNodes, key, postProcessTasks);
            }

            while (!postProcessTasks.isEmpty()) {
                // now we have to normalize actions so that they not intersect and ordered in most appropriate way
                // (free reformatting -> reindent -> formatting under reindent)
                List<PostponedAction> normalizedActions = normalizeAndReorderPostponedActions(postProcessTasks, document);
                toDispose.addAll(normalizedActions);

                // only in following loop real changes in document are made
                FileViewProvider viewProvider = key;
                for (PostponedAction normalizedAction : normalizedActions) {
                    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(myPsiManager.getProject());
                    codeStyleManager.runWithDocCommentFormattingDisabled(
                        viewProvider.getPsi(viewProvider.getBaseLanguage()),
                        () -> normalizedAction.execute(viewProvider)
                    );
                }
            }
            reparseByTextIfNeeded(key, document);
        }
        finally {
            for (Disposable disposable : toDispose) {
                //noinspection SSBasedInspection
                disposable.dispose();
            }
        }
    }

    @RequiredUIAccess
    private void reparseByTextIfNeeded(@Nonnull FileViewProvider viewProvider, @Nonnull Document document) {
        if (PsiDocumentManager.getInstance(myProject).isCommitted(document)) {
            Set<PsiFile> rootsToReparse = new HashSet<>();
            for (ASTNode node : myContext.get().myRaisingCandidates.get(viewProvider)) {
                if (hasRaiseableEdgeChild(node)) {
                    // check again because AST might be changed again and there's no need to reparse child now
                    ContainerUtil.addIfNotNull(rootsToReparse, SharedImplUtil.getContainingFile(node));
                }
            }

            for (PsiFile file : viewProvider.getAllFiles()) {
                if (file.getUserData(REPARSE_PENDING) != null || rootsToReparse.contains(file)) {
                    ((PsiDocumentManagerBase)PsiDocumentManager.getInstance(myProject)).reparseFileFromText((PsiFileImpl)file);
                    file.putUserData(REPARSE_PENDING, null);
                }
            }
        }
    }

    private static boolean hasRaiseableEdgeChild(ASTNode node) {
        ASTNode first = node.getFirstChildNode();
        while (first != null && first.getTextLength() == 0) first = first.getTreeNext();

        ASTNode last = node.getLastChildNode();
        while (last != null && last.getTextLength() == 0) last = last.getTreePrev();

        return first == null || last == null || isRaiseable(first) || isRaiseable(last);
    }

    /**
     * @return true if the parser usually avoids placing this kind of node as first/last child (i.e. a whitespace or comment)
     */
    private static boolean isRaiseable(@Nullable ASTNode node) {
        if (node == null) {
            return false;
        }
        PsiElement psi = node.getPsi();
        return psi instanceof PsiWhiteSpace || psi instanceof PsiComment;
    }

    @Nonnull
    private List<PostponedAction> normalizeAndReorderPostponedActions(
        @Nonnull Set<PostprocessFormattingTask> rangesToProcess,
        @Nonnull Document document
    ) {
        List<PostprocessFormattingTask> freeFormattingActions = new ArrayList<>();
        List<ReindentTask> indentActions = new ArrayList<>();

        PostprocessFormattingTask accumulatedTask = null;
        Iterator<PostprocessFormattingTask> iterator = rangesToProcess.iterator();
        while (iterator.hasNext()) {
            PostprocessFormattingTask currentTask = iterator.next();
            if (accumulatedTask == null) {
                accumulatedTask = currentTask;
                iterator.remove();
            }
            else if (accumulatedTask.getStartOffset() > currentTask.getEndOffset() ||
                accumulatedTask.getStartOffset() == currentTask.getEndOffset() && !canStickActionsTogether(accumulatedTask, currentTask)) {
                // action can be pushed
                if (accumulatedTask instanceof ReindentTask reindentTask) {
                    indentActions.add(reindentTask);
                }
                else {
                    freeFormattingActions.add(accumulatedTask);
                }

                accumulatedTask = currentTask;
                iterator.remove();
            }
            else if (accumulatedTask instanceof ReformatTask && currentTask instanceof ReindentTask) {
                // split accumulated reformat range into two
                if (accumulatedTask.getStartOffset() < currentTask.getStartOffset()) {
                    RangeMarker endOfRange =
                        document.createRangeMarker(accumulatedTask.getStartOffset(), currentTask.getStartOffset());
                    // add heading reformat part
                    rangesToProcess.add(new ReformatTask(endOfRange));
                    // and manage heading whitespace because formatter does not edit it in previous action
                    iterator = rangesToProcess.iterator();
                    //noinspection StatementWithEmptyBody
                    while (iterator.next().getRange() != currentTask.getRange()) ;
                }
                RangeMarker rangeToProcess = document.createRangeMarker(currentTask.getEndOffset(), accumulatedTask.getEndOffset());
                freeFormattingActions.add(new ReformatWithHeadingWhitespaceTask(rangeToProcess));
                accumulatedTask = currentTask;
                iterator.remove();
            }
            else {
                if (!(accumulatedTask instanceof ReindentTask)) {
                    iterator.remove();

                    boolean withLeadingWhitespace = accumulatedTask instanceof ReformatWithHeadingWhitespaceTask;
                    if (accumulatedTask instanceof ReformatTask
                        && currentTask instanceof ReformatWithHeadingWhitespaceTask
                        && accumulatedTask.getStartOffset() == currentTask.getStartOffset()) {
                        withLeadingWhitespace = true;
                    }
                    else if (accumulatedTask instanceof ReformatWithHeadingWhitespaceTask
                        && currentTask instanceof ReformatTask
                        && accumulatedTask.getStartOffset() < currentTask.getStartOffset()) {
                        withLeadingWhitespace = false;
                    }
                    int newStart = Math.min(accumulatedTask.getStartOffset(), currentTask.getStartOffset());
                    int newEnd = Math.max(accumulatedTask.getEndOffset(), currentTask.getEndOffset());
                    RangeMarker rangeMarker;

                    if (accumulatedTask.getStartOffset() == newStart && accumulatedTask.getEndOffset() == newEnd) {
                        rangeMarker = accumulatedTask.getRange();
                    }
                    else if (currentTask.getStartOffset() == newStart && currentTask.getEndOffset() == newEnd) {
                        rangeMarker = currentTask.getRange();
                    }
                    else {
                        rangeMarker = document.createRangeMarker(newStart, newEnd);
                    }

                    accumulatedTask =
                        withLeadingWhitespace ? new ReformatWithHeadingWhitespaceTask(rangeMarker) : new ReformatTask(rangeMarker);
                }
                else if (currentTask instanceof ReindentTask) {
                    iterator.remove();
                } // TODO[ik]: need to be fixed to correctly process indent inside indent
            }
        }
        if (accumulatedTask != null) {
            if (accumulatedTask instanceof ReindentTask reindentTask) {
                indentActions.add(reindentTask);
            }
            else {
                freeFormattingActions.add(accumulatedTask);
            }
        }

        Collections.reverse(freeFormattingActions);
        Collections.reverse(indentActions);

        List<PostponedAction> result = new ArrayList<>();
        if (!freeFormattingActions.isEmpty()) {
            FormatTextRanges ranges = new FormatTextRanges();
            ranges.setExtendToContext(true);
            for (PostprocessFormattingTask action : freeFormattingActions) {
                TextRange range = TextRange.create(action);
                ranges.add(range, action instanceof ReformatWithHeadingWhitespaceTask);
            }
            result.add(new ReformatRangesAction(ranges));
        }

        if (!indentActions.isEmpty()) {
            ReindentRangesAction reindentRangesAction = new ReindentRangesAction();
            for (ReindentTask action : indentActions) {
                reindentRangesAction.add(action.getRange(), action.getOldIndent());
            }
            result.add(reindentRangesAction);
        }

        return result;
    }

    private static boolean canStickActionsTogether(PostprocessFormattingTask currentTask, PostprocessFormattingTask nextTask) {
        // empty reformat markers can't be stuck together with any action
        if (nextTask instanceof ReformatWithHeadingWhitespaceTask && nextTask.getStartOffset() == nextTask.getEndOffset()) {
            return false;
        }
        if (currentTask instanceof ReformatWithHeadingWhitespaceTask && currentTask.getStartOffset() == currentTask.getEndOffset()) {
            return false;
        }
        // reindent actions can't be be stuck at all
        return !(currentTask instanceof ReindentTask);
    }

    private static void createActionsMap(
        @Nonnull List<? extends ASTNode> astNodes,
        @Nonnull FileViewProvider provider,
        @Nonnull Collection<? super PostprocessFormattingTask> rangesToProcess
    ) {
        Set<ASTNode> nodesToProcess = new HashSet<>(astNodes);
        Document document = provider.getDocument();
        if (document == null) {
            return;
        }
        for (ASTNode node : astNodes) {
            nodesToProcess.remove(node);
            FileElement fileElement = TreeUtil.getFileElement((TreeElement)node);
            if (fileElement == null || ((PsiFile)fileElement.getPsi()).getViewProvider() != provider) {
                continue;
            }
            boolean isGenerated = CodeEditUtil.isNodeGenerated(node);

            ((TreeElement)node).acceptTree(new RecursiveTreeElementVisitor() {
                private boolean inGeneratedContext = !isGenerated;

                @Override
                @RequiredReadAction
                protected boolean visitNode(TreeElement element) {
                    if (nodesToProcess.contains(element)) {
                        return false;
                    }

                    boolean currentNodeGenerated = CodeEditUtil.isNodeGenerated(element);
                    CodeEditUtil.setNodeGenerated(element, false);
                    if (currentNodeGenerated && !inGeneratedContext) {
                        rangesToProcess.add(new ReformatTask(document.createRangeMarker(element.getTextRange())));
                        inGeneratedContext = true;
                    }
                    if (!currentNodeGenerated && inGeneratedContext) {
                        if (element.getElementType() == TokenType.WHITE_SPACE) {
                            return false;
                        }
                        int oldIndent = CodeEditUtil.getOldIndentation(element);
                        if (oldIndent < 0) {
                            LOG.warn("For not generated items old indentation must be defined: element " + element);
                            oldIndent = 0;
                        }
                        CodeEditUtil.setOldIndentation(element, -1);
                        for (TextRange indentRange : getEnabledRanges(element.getPsi())) {
                            rangesToProcess.add(new ReindentTask(document.createRangeMarker(indentRange), oldIndent));
                        }
                        inGeneratedContext = false;
                    }
                    return true;
                }

                @RequiredReadAction
                private Iterable<TextRange> getEnabledRanges(@Nonnull PsiElement element) {
                    List<TextRange> disabledRanges = new ArrayList<>();
                    for (DisabledIndentRangesProvider rangesProvider : DisabledIndentRangesProvider.EP_NAME.getExtensions()) {
                        Collection<TextRange> providedDisabledRanges = rangesProvider.getDisabledIndentRanges(element);
                        if (providedDisabledRanges != null) {
                            disabledRanges.addAll(providedDisabledRanges);
                        }
                    }
                    return TextRangeUtil.excludeRanges(element.getTextRange(), disabledRanges);
                }

                @Override
                public void visitComposite(CompositeElement composite) {
                    boolean oldGeneratedContext = inGeneratedContext;
                    super.visitComposite(composite);
                    inGeneratedContext = oldGeneratedContext;
                }

                @Override
                public void visitLeaf(LeafElement leaf) {
                    boolean oldGeneratedContext = inGeneratedContext;
                    super.visitLeaf(leaf);
                    inGeneratedContext = oldGeneratedContext;
                }
            });
        }
    }

    private static void handleReformatMarkers(
        @Nonnull FileViewProvider key,
        @Nonnull Set<? super PostprocessFormattingTask> rangesToProcess
    ) {
        Document document = key.getDocument();
        if (document == null) {
            return;
        }
        for (FileElement fileElement : ((AbstractFileViewProvider)key).getKnownTreeRoots()) {
            fileElement.acceptTree(new RecursiveTreeElementWalkingVisitor() {
                @Override
                protected void visitNode(TreeElement element) {
                    if (CodeEditUtil.isMarkedToReformatBefore(element)) {
                        CodeEditUtil.markToReformatBefore(element, false);
                        rangesToProcess.add(new ReformatWithHeadingWhitespaceTask(document.createRangeMarker(
                            element.getStartOffset(),
                            element.getStartOffset()
                        )));
                    }
                    else if (CodeEditUtil.isMarkedToReformat(element)) {
                        CodeEditUtil.markToReformat(element, false);
                        rangesToProcess.add(new ReformatWithHeadingWhitespaceTask(document.createRangeMarker(
                            element.getStartOffset(),
                            element.getStartOffset() + element.getTextLength()
                        )));
                    }
                    super.visitNode(element);
                }
            });
        }
    }

    private static void adjustIndentationInRange(
        @Nonnull PsiFile file,
        @Nonnull Document document,
        @Nonnull TextRange[] indents,
        int indentAdjustment
    ) {
        CharSequence charsSequence = document.getCharsSequence();
        for (TextRange indent : indents) {
            String oldIndentStr = charsSequence.subSequence(indent.getStartOffset() + 1, indent.getEndOffset()).toString();
            int oldIndent = IndentHelperImpl.getIndent(file, oldIndentStr, true);
            String newIndentStr =
                IndentHelperImpl.fillIndent(CodeStyle.getIndentOptions(file), Math.max(oldIndent + indentAdjustment, 0));
            document.replaceString(indent.getStartOffset() + 1, indent.getEndOffset(), newIndentStr);
        }
    }

    private static int getNewIndent(@Nonnull PsiFile psiFile, int firstWhitespace) {
        Document document = psiFile.getViewProvider().getDocument();
        assert document != null;
        int startOffset = document.getLineStartOffset(document.getLineNumber(firstWhitespace));
        int endOffset = startOffset;
        CharSequence charsSequence = document.getCharsSequence();
        //noinspection StatementWithEmptyBody
        while (Character.isWhitespace(charsSequence.charAt(endOffset++))) ;
        String newIndentStr = charsSequence.subSequence(startOffset, endOffset - 1).toString();
        return IndentHelperImpl.getIndent(psiFile, newIndentStr, true);
    }

    public boolean isDisabled() {
        return getContext().myDisabledCounter > 0;
    }

    @Nonnull
    private CodeFormatterFacade getFormatterFacade(@Nonnull FileViewProvider viewProvider) {
        CodeStyleSettings styleSettings = CodeStyle.getSettings(viewProvider.getPsi(viewProvider.getBaseLanguage()));
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myPsiManager.getProject());
        Document document = viewProvider.getDocument();
        assert document != null;
        CodeFormatterFacade codeFormatter = new CodeFormatterFacade(styleSettings, viewProvider.getBaseLanguage());

        documentManager.commitDocument(document);
        return codeFormatter;
    }

    private abstract static class PostprocessFormattingTask implements Comparable<PostprocessFormattingTask>, Segment, Disposable {
        @Nonnull
        private final RangeMarker myRange;

        PostprocessFormattingTask(@Nonnull RangeMarker rangeMarker) {
            myRange = rangeMarker;
        }

        @Override
        public int compareTo(@Nonnull PostprocessFormattingTask o) {
            RangeMarker o1 = myRange;
            RangeMarker o2 = o.myRange;
            if (o1.equals(o2)) {
                return 0;
            }
            int diff = o2.getEndOffset() - o1.getEndOffset();
            if (diff == 0) {
                if (o1.getStartOffset() == o2.getStartOffset()) {
                    return 0;
                }
                if (o1.getStartOffset() == o1.getEndOffset()) {
                    return -1; // empty ranges first
                }
                if (o2.getStartOffset() == o2.getEndOffset()) {
                    return 1; // empty ranges first
                }
                return o1.getStartOffset() - o2.getStartOffset();
            }
            return diff;
        }

        @Nonnull
        public RangeMarker getRange() {
            return myRange;
        }

        @Override
        public int getStartOffset() {
            return myRange.getStartOffset();
        }

        @Override
        public int getEndOffset() {
            return myRange.getEndOffset();
        }

        @Override
        public void dispose() {
            if (myRange.isValid()) {
                myRange.dispose();
            }
        }
    }

    private static class ReformatTask extends PostprocessFormattingTask {
        ReformatTask(@Nonnull RangeMarker rangeMarker) {
            super(rangeMarker);
        }
    }

    private static class ReformatWithHeadingWhitespaceTask extends PostprocessFormattingTask {
        ReformatWithHeadingWhitespaceTask(@Nonnull RangeMarker rangeMarker) {
            super(rangeMarker);
        }
    }

    private static class ReindentTask extends PostprocessFormattingTask {
        private final int myOldIndent;

        ReindentTask(@Nonnull RangeMarker rangeMarker, int oldIndent) {
            super(rangeMarker);
            myOldIndent = oldIndent;
        }

        int getOldIndent() {
            return myOldIndent;
        }
    }

    private interface PostponedAction extends Disposable {
        void execute(@Nonnull FileViewProvider viewProvider);
    }

    private class ReformatRangesAction implements PostponedAction {
        private final FormatTextRanges myRanges;

        ReformatRangesAction(@Nonnull FormatTextRanges ranges) {
            myRanges = ranges;
        }

        @Override
        @RequiredUIAccess
        public void execute(@Nonnull FileViewProvider viewProvider) {
            PsiFile file = viewProvider.getPsi(viewProvider.getBaseLanguage());
            FormatTextRanges textRanges = myRanges.ensureNonEmpty();
            textRanges.setExtendToContext(true);
            if (ExternalFormatProcessor.useExternalFormatter(file)) {
                CodeStyleManagerImpl.formatRanges(file, myRanges, null);
            }
            else {
                CodeFormatterFacade codeFormatter = getFormatterFacade(viewProvider);
                codeFormatter.processText(file, textRanges, false);
            }
        }

        @Override
        public void dispose() {
        }
    }

    private static class ReindentRangesAction implements PostponedAction {
        private final List<Pair<Integer, RangeMarker>> myRangesToReindent = new ArrayList<>();

        public void add(@Nonnull RangeMarker rangeMarker, int oldIndent) {
            myRangesToReindent.add(new Pair<>(oldIndent, rangeMarker));
        }

        @Override
        public void execute(@Nonnull FileViewProvider viewProvider) {
            Document document = viewProvider.getDocument();
            assert document != null;
            PsiFile psiFile = viewProvider.getPsi(viewProvider.getBaseLanguage());
            for (Pair<Integer, RangeMarker> integerRangeMarkerPair : myRangesToReindent) {
                RangeMarker marker = integerRangeMarkerPair.second;
                CharSequence charsSequence = document.getCharsSequence().subSequence(marker.getStartOffset(), marker.getEndOffset());
                int oldIndent = integerRangeMarkerPair.first;
                TextRange[] whitespaces = CharArrayUtil.getIndents(charsSequence, marker.getStartOffset());
                int indentAdjustment = getNewIndent(psiFile, marker.getStartOffset()) - oldIndent;
                if (indentAdjustment != 0) {
                    adjustIndentationInRange(psiFile, document, whitespaces, indentAdjustment);
                }
            }
        }

        @Override
        public void dispose() {
            for (Pair<Integer, RangeMarker> pair : myRangesToReindent) {
                RangeMarker marker = pair.second;
                if (marker.isValid()) {
                    marker.dispose();
                }
            }
        }
    }

    @TestOnly
    public void clear() {
        getContext().myReformatElements.clear();
    }

    private Context getContext() {
        return myContext.get();
    }

    private static class Context {
        private int myPostponedCounter;
        private int myDisabledCounter;
        private final MultiMap<FileViewProvider, FileElement> myUpdatedProviders = MultiMap.create();
        private final MultiMap<FileViewProvider, ASTNode> myRaisingCandidates = MultiMap.create();
        private final Map<FileViewProvider, List<ASTNode>> myReformatElements = new HashMap<>();
    }
}
