// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.impl.internal.psi.diff;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.*;
import consulo.language.file.FileViewProvider;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.impl.ast.*;
import consulo.language.impl.file.SingleRootFileViewProvider;
import consulo.language.impl.internal.ast.ASTStructure;
import consulo.language.impl.internal.psi.ASTShallowComparator;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.impl.internal.psi.PsiManagerImpl;
import consulo.language.impl.internal.psi.PsiTreeChangeEventImpl;
import consulo.language.impl.psi.DummyHolder;
import consulo.language.impl.psi.DummyHolderFactory;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.impl.psi.template.OuterLanguageElementType;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.psi.*;
import consulo.language.template.ITemplateDataElementType;
import consulo.language.util.CharTable;
import consulo.language.util.FlyweightCapableTreeStructure;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.logging.attachment.AttachmentFactory;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.function.Function;

@Singleton
@ServiceImpl
public class BlockSupportImpl extends BlockSupport {
    private static final Logger LOG = Logger.getInstance(BlockSupportImpl.class);

    @Override
    @Nonnull
    public DiffLog reparseRange(@Nonnull final PsiFile file,
                                @Nonnull FileASTNode oldFileNode,
                                @Nonnull TextRange changedPsiRange,
                                @Nonnull final CharSequence newFileText,
                                @Nonnull final ProgressIndicator indicator,
                                @Nonnull CharSequence lastCommittedText) {
        try (ReparseResult result = reparse(file, oldFileNode, changedPsiRange, newFileText, indicator, lastCommittedText)) {
            return result.log;
        }
    }

    public static class ReparseResult implements AutoCloseable {
        public final DiffLog log;
        public final ASTNode oldRoot;
        public final ASTNode newRoot;

        ReparseResult(DiffLog log, ASTNode oldRoot, ASTNode newRoot) {
            this.log = log;
            this.oldRoot = oldRoot;
            this.newRoot = newRoot;
        }

        @Override
        public void close() {
        }
    }

    // return diff log, old node to replace, new node (in dummy file)
    // MUST call .close() on the returned result
    @Nonnull
    public static ReparseResult reparse(@Nonnull final PsiFile file,
                                        @Nonnull FileASTNode oldFileNode,
                                        @Nonnull TextRange changedPsiRange,
                                        @Nonnull final CharSequence newFileText,
                                        @Nonnull final ProgressIndicator indicator,
                                        @Nonnull CharSequence lastCommittedText) {
        PsiFileImpl fileImpl = (PsiFileImpl) file;

        Couple<ASTNode> reparseableRoots = findReparseableRoots(fileImpl, oldFileNode, changedPsiRange, newFileText);
        if (reparseableRoots == null) {
            return makeFullParse(fileImpl, oldFileNode, newFileText, indicator, lastCommittedText);
        }
        ASTNode oldRoot = reparseableRoots.first;
        ASTNode newRoot = reparseableRoots.second;
        DiffLog diffLog = mergeTrees(fileImpl, oldRoot, newRoot, indicator, lastCommittedText);
        return new ReparseResult(diffLog, oldRoot, newRoot);
    }


    /**
     * Find ast node that could be reparsed incrementally
     *
     * @return Pair (target reparseable node, new replacement node)
     * or {@code null} if can't parse incrementally.
     */
    public static @Nullable Couple<ASTNode> findReparseableRoots(@Nonnull PsiFileImpl file,
                                                                 @Nonnull FileASTNode oldFileNode,
                                                                 @Nonnull TextRange changedPsiRange,
                                                                 @Nonnull CharSequence newFileText) {
        CharTable charTable = oldFileNode.getCharTable();
        int lengthShift = newFileText.length() - oldFileNode.getTextLength();

        if (isTooDeep(file)) {
            return null;
        }

        boolean isTemplateFile = oldFileNode.getElementType() instanceof ITemplateDataElementType;

        ASTNode leafAtStart = oldFileNode.findLeafElementAt(Math.max(0, changedPsiRange.getStartOffset() - 1));
        ASTNode leafAtEnd = oldFileNode.findLeafElementAt(Math.min(changedPsiRange.getEndOffset(), oldFileNode.getTextLength() - 1));
        ASTNode node = leafAtStart != null && leafAtEnd != null ? TreeUtil.findCommonParent(leafAtStart, leafAtEnd) : oldFileNode;
        Language baseLanguage = file.getViewProvider().getBaseLanguage();

        Function<ASTNode, Couple<ASTNode>> reparseNodeFunction = astNode -> {
            IElementType elementType = astNode.getElementType();
            if (elementType instanceof IReparseableElementTypeBase || elementType instanceof IReparseableLeafElementType) {
                TextRange textRange = astNode.getTextRange();

                if (textRange.getLength() + lengthShift > 0 &&
                    (baseLanguage.isKindOf(elementType.getLanguage()) || elementType instanceof IReparseableLeafElementType ||
                        !TreeUtil.containsOuterLanguageElements(astNode))) {
                    int start = textRange.getStartOffset();
                    int end = start + textRange.getLength() + lengthShift;
                    if (end > newFileText.length()) {
                        reportInconsistentLength(file, newFileText, astNode, start, end);
                        return Couple.of(null, null);
                    }

                    CharSequence newTextStr = newFileText.subSequence(start, end);

                    ASTNode newNode;
                    if (elementType instanceof IReparseableElementTypeBase) {
                        newNode =
                            tryReparseNode((IReparseableElementTypeBase) elementType, astNode, newTextStr, file.getManager(), baseLanguage, charTable);
                    }
                    else {
                        newNode = tryReparseLeaf((IReparseableLeafElementType) elementType, astNode, newTextStr);
                    }

                    if (newNode != null) {
                        if (newNode.getTextLength() != newTextStr.length()) {
                            String details = ApplicationManager.getApplication().isInternal()
                                ? "text=" + newTextStr + "; treeText=" + newNode.getText() + ";"
                                : "";
                            LOG.error("Inconsistent reparse: " + details + " type=" + elementType);
                        }

                        return Couple.of(astNode, newNode);
                    }
                }
            }
            return null;
        };

        TextRange startLeafRange = leafAtStart == null ? null : leafAtStart.getTextRange();
        TextRange endLeafRange = leafAtEnd == null ? null : leafAtEnd.getTextRange();

        IElementType startLeafType = PsiUtilCore.getElementType(leafAtStart);
        if (startLeafType instanceof IReparseableLeafElementType &&
            startLeafRange.getEndOffset() == changedPsiRange.getEndOffset() &&
            (!isTemplateFile || startLeafType instanceof OuterLanguageElementType)) {
            Couple<ASTNode> reparseResult = reparseNodeFunction.apply(leafAtStart);
            if (reparseResult != null && reparseResult.first != null) {
                return reparseResult;
            }
        }
        IElementType endLeafType = PsiUtilCore.getElementType(leafAtEnd);
        if (endLeafType instanceof IReparseableLeafElementType &&
            endLeafRange.getStartOffset() == changedPsiRange.getStartOffset() &&
            (!isTemplateFile || endLeafType instanceof OuterLanguageElementType)) {
            Couple<ASTNode> reparseResult = reparseNodeFunction.apply(leafAtEnd);
            if (reparseResult != null && reparseResult.first != null) {
                return reparseResult;
            }
        }

        while (node != null && !(node instanceof FileElement)) {
            if (isTemplateFile && !(PsiUtilCore.getElementType(node) instanceof OuterLanguageElementType)) {
                break;
            }
            Couple<ASTNode> couple = reparseNodeFunction.apply(node);
            if (couple != null) {
                if (couple.first == null) {
                    break;
                }
                return couple;
            }
            node = node.getTreeParent();
        }
        return null;
    }

    @Nullable
    protected static ASTNode tryReparseNode(@Nonnull IReparseableElementTypeBase reparseable,
                                            @Nonnull ASTNode node,
                                            @Nonnull CharSequence newTextStr,
                                            @Nonnull PsiManager manager,
                                            @Nonnull Language baseLanguage,
                                            @Nonnull CharTable charTable) {
        if (!reparseable.isParsable(node.getTreeParent(), newTextStr, baseLanguage, manager.getProject())) {
            return null;
        }
        ASTNode chameleon;
        if (reparseable instanceof ICustomParsingType) {
            chameleon = ((ICustomParsingType) reparseable).parse(newTextStr, SharedImplUtil.findCharTableByTree(node));
        }
        else if (reparseable instanceof ILazyParseableElementType) {
            chameleon = ((ILazyParseableElementType) reparseable).createNode(newTextStr);
        }
        else {
            throw new AssertionError(reparseable.getClass() + " must either implement ICustomParsingType or extend ILazyParseableElementType");
        }
        if (chameleon == null) {
            return null;
        }
        DummyHolder holder = DummyHolderFactory.createHolder(manager, null, node.getPsi(), charTable);
        holder.getTreeElement().rawAddChildren((TreeElement) chameleon);
        if (!reparseable.isValidReparse(node, chameleon)) {
            return null;
        }
        return chameleon;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected static ASTNode tryReparseLeaf(@Nonnull IReparseableLeafElementType reparseable, @Nonnull ASTNode node, @Nonnull CharSequence newTextStr) {
        return reparseable.reparseLeaf(node, newTextStr);
    }

    private static void reportInconsistentLength(PsiFile file, CharSequence newFileText, ASTNode node, int start, int end) {
        String message =
            "Index out of bounds: type=" + node.getElementType() + "; file=" + file + "; file.class=" + file.getClass() + "; start=" + start + "; end=" + end + "; length=" + node.getTextLength();
        String newTextBefore = newFileText.subSequence(0, start).toString();
        String oldTextBefore = file.getText().subSequence(0, start).toString();
        if (oldTextBefore.equals(newTextBefore)) {
            message += "; oldTextBefore==newTextBefore";
        }
        LOG.error(message, AttachmentFactory.get().create(file.getName() + "_oldNodeText.txt", node.getText()), AttachmentFactory.get().create(file.getName() + "_oldFileText.txt", file.getText()),
            AttachmentFactory.get().create(file.getName() + "_newFileText.txt", newFileText.toString()));
    }

    // returns diff log, new file element
    @Nonnull
    public static ReparseResult makeFullParse(@Nonnull PsiFileImpl fileImpl,
                                              @Nonnull FileASTNode oldFileNode,
                                              @Nonnull CharSequence newFileText,
                                              @Nonnull ProgressIndicator indicator,
                                              @Nonnull CharSequence lastCommittedText) {
        if (fileImpl instanceof PsiCodeFragment) {
            FileElement parent = fileImpl.getTreeElement();
            PsiElement context = fileImpl.getContext();
            DummyHolder dummyHolder = new DummyHolder(fileImpl.getManager(), context != null && context.isValid() ? context : null);
            FileElement holderElement = dummyHolder.getTreeElement();
            holderElement.rawAddChildren(fileImpl.createContentLeafElement(holderElement.getCharTable().intern(newFileText, 0, newFileText.length())));
            DiffLog diffLog = new DiffLog();
            diffLog.appendReplaceFileElement(parent, (FileElement) holderElement.getFirstChildNode());

            return new ReparseResult(diffLog, oldFileNode, holderElement) {
                @Override
                public void close() {
                    VirtualFile lightFile = dummyHolder.getViewProvider().getVirtualFile();
                    ((PsiManagerEx) fileImpl.getManager()).getFileManager().setViewProvider(lightFile, null);
                }
            };
        }
        FileViewProvider viewProvider = fileImpl.getViewProvider();
        viewProvider.getLanguages();
        VirtualFile virtualFile = viewProvider.getVirtualFile();
        FileType fileType = virtualFile.getFileType();
        String fileName = fileImpl.getName();
        LightVirtualFile lightFile = new LightVirtualFile(fileName, fileType, newFileText, virtualFile.getCharset(), viewProvider.getModificationStamp());
        lightFile.setOriginalFile(virtualFile);

        FileViewProvider providerCopy = viewProvider.createCopy(lightFile);
        if (providerCopy.isEventSystemEnabled()) {
            throw new AssertionError("Copied view provider must be non-physical for reparse to deliver correct events: " + viewProvider);
        }
        providerCopy.getLanguages();
        SingleRootFileViewProvider.doNotCheckFileSizeLimit(lightFile); // optimization: do not convert file contents to bytes to determine if we should codeinsight it
        PsiFileImpl newFile = getFileCopy(fileImpl, providerCopy);

        newFile.setOriginalFile(fileImpl);

        final FileElement newFileElement = (FileElement) newFile.getNode();
        final FileElement oldFileElement = (FileElement) oldFileNode;
        if (lastCommittedText.length() != oldFileElement.getTextLength()) {
            throw new IncorrectOperationException(viewProvider.toString());
        }
        DiffLog diffLog = mergeTrees(fileImpl, oldFileElement, newFileElement, indicator, lastCommittedText);

        return new ReparseResult(diffLog, oldFileElement, newFileElement) {
            @Override
            public void close() {
                ((PsiManagerEx) fileImpl.getManager()).getFileManager().setViewProvider(lightFile, null);
            }
        };
    }

    @Nonnull
    public static PsiFileImpl getFileCopy(@Nonnull PsiFileImpl originalFile, @Nonnull FileViewProvider providerCopy) {
        FileViewProvider viewProvider = originalFile.getViewProvider();
        Language language = originalFile.getLanguage();

        PsiFile file = providerCopy.getPsi(language);
        if (file != null && !(file instanceof PsiFileImpl)) {
            throw new RuntimeException("View provider " +
                viewProvider +
                " refused to provide PsiFileImpl for " +
                language +
                details(providerCopy, viewProvider) +
                " and returned this strange thing instead of PsiFileImpl: " +
                file +
                " (" +
                file.getClass() +
                ")");
        }

        PsiFileImpl newFile = (PsiFileImpl) file;

        if (newFile == null && language == PlainTextLanguage.INSTANCE && originalFile == viewProvider.getPsi(viewProvider.getBaseLanguage())) {
            newFile = (PsiFileImpl) providerCopy.getPsi(providerCopy.getBaseLanguage());
        }

        if (newFile == null) {
            throw new RuntimeException("View provider " + viewProvider + " refused to parse text with " + language + details(providerCopy, viewProvider));
        }

        return newFile;
    }

    private static String details(FileViewProvider providerCopy, FileViewProvider viewProvider) {
        return "; languages: " +
            viewProvider.getLanguages() +
            "; base: " +
            viewProvider.getBaseLanguage() +
            "; copy: " +
            providerCopy +
            "; copy.base: " +
            providerCopy.getBaseLanguage() +
            "; vFile: " +
            viewProvider.getVirtualFile() +
            "; copy.vFile: " +
            providerCopy.getVirtualFile() +
            "; fileType: " +
            viewProvider.getVirtualFile().getFileType() +
            "; copy.original(): " +
            (providerCopy.getVirtualFile() instanceof LightVirtualFile ? ((LightVirtualFile) providerCopy.getVirtualFile()).getOriginalFile() : null);
    }

    @Nonnull
    private static DiffLog replaceElementWithEvents(@Nonnull ASTNode oldRoot, @Nonnull ASTNode newRoot) {
        DiffLog diffLog = new DiffLog();
        if (oldRoot instanceof CompositeElement) {
            diffLog.appendReplaceElementWithEvents((CompositeElement) oldRoot, (CompositeElement) newRoot);
        }
        else {
            diffLog.nodeReplaced(oldRoot, newRoot);
        }
        return diffLog;
    }

    @Nonnull
    public static DiffLog mergeTrees(@Nonnull final PsiFileImpl fileImpl,
                                     @Nonnull final ASTNode oldRoot,
                                     @Nonnull final ASTNode newRoot,
                                     @Nonnull ProgressIndicator indicator,
                                     @Nonnull CharSequence lastCommittedText) {
        PsiUtilCore.ensureValid(fileImpl);
        if (newRoot instanceof FileElement) {
            FileElement fileImplElement = fileImpl.getTreeElement();
            if (fileImplElement != null) {
                ((FileElement) newRoot).setCharTable(fileImplElement.getCharTable());
            }
        }

        try {
            newRoot.putUserData(TREE_TO_BE_REPARSED, Pair.create(oldRoot, lastCommittedText));
            if (isReplaceWholeNode(fileImpl, newRoot)) {
                DiffLog treeChangeEvent = replaceElementWithEvents(oldRoot, newRoot);
                fileImpl.putUserData(TREE_DEPTH_LIMIT_EXCEEDED, Boolean.TRUE);

                return treeChangeEvent;
            }
            newRoot.getFirstChildNode();  // maybe reparsed in PsiBuilderImpl and have thrown exception here
        }
        catch (ReparsedSuccessfullyException e) {
            // reparsed in PsiBuilderImpl
            return e.getDiffLog();
        }
        finally {
            newRoot.putUserData(TREE_TO_BE_REPARSED, null);
        }

        final ASTShallowComparator comparator = new ASTShallowComparator(indicator);
        final ASTStructure treeStructure = createInterruptibleASTStructure(newRoot, indicator);

        DiffLog diffLog = new DiffLog();
        diffTrees(oldRoot, diffLog, comparator, treeStructure, indicator, lastCommittedText);
        return diffLog;
    }

    public static <T> void diffTrees(@Nonnull final ASTNode oldRoot,
                                     @Nonnull final DiffTreeChangeBuilder<ASTNode, T> builder,
                                     @Nonnull final ShallowNodeComparator<ASTNode, T> comparator,
                                     @Nonnull final FlyweightCapableTreeStructure<T> newTreeStructure,
                                     @Nonnull ProgressIndicator indicator,
                                     @Nonnull CharSequence lastCommittedText) {
        DiffTree.diff(createInterruptibleASTStructure(oldRoot, indicator), newTreeStructure, comparator, builder, lastCommittedText);
    }

    private static ASTStructure createInterruptibleASTStructure(@Nonnull final ASTNode oldRoot, @Nonnull final ProgressIndicator indicator) {
        return new ASTStructure(oldRoot) {
            @Override
            public int getChildren(@Nonnull ASTNode astNode, @Nonnull SimpleReference<ASTNode[]> into) {
                indicator.checkCanceled();
                return super.getChildren(astNode, into);
            }
        };
    }

    private static boolean isReplaceWholeNode(@Nonnull PsiFileImpl fileImpl, @Nonnull ASTNode newRoot) throws ReparsedSuccessfullyException {
        final Boolean data = fileImpl.getUserData(DO_NOT_REPARSE_INCREMENTALLY);
        if (data != null) {
            fileImpl.putUserData(DO_NOT_REPARSE_INCREMENTALLY, null);
        }

        boolean explicitlyMarkedDeep = Boolean.TRUE.equals(data);

        if (explicitlyMarkedDeep || isTooDeep(fileImpl)) {
            return true;
        }

        final ASTNode childNode = newRoot.getFirstChildNode();  // maybe reparsed in PsiBuilderImpl and have thrown exception here
        boolean childTooDeep = isTooDeep(childNode);
        if (childTooDeep) {
            childNode.putUserData(TREE_DEPTH_LIMIT_EXCEEDED, null);
            fileImpl.putUserData(TREE_DEPTH_LIMIT_EXCEEDED, Boolean.TRUE);
        }
        return childTooDeep;
    }

    public static void sendBeforeChildrenChangeEvent(@Nonnull PsiManagerImpl manager, @Nonnull PsiElement scope, boolean isGenericChange) {
        if (!scope.isPhysical()) {
            manager.beforeChange(false);
            return;
        }
        PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(manager);
        event.setParent(scope);
        event.setFile(scope.getContainingFile());
        TextRange range = scope.getTextRange();
        event.setOffset(range == null ? 0 : range.getStartOffset());
        event.setOldLength(scope.getTextLength());
        // the "generic" event is being sent on every PSI change. It does not carry any specific info except the fact that "something has changed"
        event.setGenericChange(isGenericChange);
        manager.beforeChildrenChange(event);
    }

    public static void sendAfterChildrenChangedEvent(@Nonnull PsiManagerImpl manager, @Nonnull PsiFile scope, int oldLength, boolean isGenericChange) {
        if (!scope.isPhysical()) {
            manager.afterChange(false);
            return;
        }
        PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(manager);
        event.setParent(scope);
        event.setFile(scope);
        event.setOffset(0);
        event.setOldLength(oldLength);
        event.setGenericChange(isGenericChange);
        manager.childrenChanged(event);
    }
}
