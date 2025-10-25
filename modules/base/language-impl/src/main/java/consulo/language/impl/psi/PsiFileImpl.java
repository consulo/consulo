// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.impl.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.application.util.Queryable;
import consulo.content.scope.SearchScope;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.FileContentUtilCore;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.*;
import consulo.language.file.FileViewProvider;
import consulo.language.file.light.ReadOnlyLightVirtualFile;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.ast.*;
import consulo.language.impl.file.AbstractFileViewProvider;
import consulo.language.impl.file.FreeThreadedFileViewProvider;
import consulo.language.impl.internal.file.FileManagerImpl;
import consulo.language.impl.internal.psi.*;
import consulo.language.impl.internal.psi.diff.BlockSupportImpl;
import consulo.language.internal.LanguageModuleUtilInternal;
import consulo.language.internal.PsiFileInternal;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.*;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.resolve.PsiScopeProcessor;
import consulo.language.psi.resolve.ResolveState;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.language.version.LanguageVersion;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.navigation.ItemPresentation;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.lazy.ClearableLazyValue;
import consulo.util.lang.ref.PatchedWeakReference;
import consulo.util.lang.ref.SoftReference;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class PsiFileImpl extends UserDataHolderBase
    implements PsiFileInternal, PsiFileWithStubSupport, Queryable, PsiElementWithSubtreeChangeNotifier, Cloneable {
    private static final Logger LOG = Logger.getInstance(PsiFileImpl.class);
    public static final String STUB_PSI_MISMATCH = "stub-psi mismatch";
    private static VarHandle ourTreeUpdater;

    static {
        try {
            ourTreeUpdater = MethodHandles.lookup().findVarHandle(PsiFileImpl.class, "myTrees", FileTrees.class);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private final ClearableLazyValue<Module> myModuleRef;

    private IElementType myElementType;
    protected IElementType myContentElementType;
    private long myModificationStamp;

    protected PsiFile myOriginalFile;
    private final AbstractFileViewProvider myViewProvider;
    private volatile FileTrees myTrees = FileTrees.noStub(null, this);
    private volatile boolean myPossiblyInvalidated;
    protected final PsiManagerEx myManager;
    public static final Key<Boolean> BUILDING_STUB = new Key<>("Don't use stubs mark!");
    private final PsiLock myPsiLock;
    private volatile boolean myLoadingAst;

    protected PsiFileImpl(@Nonnull IElementType elementType, IElementType contentElementType, @Nonnull FileViewProvider provider) {
        this(provider);
        init(elementType, contentElementType);
    }

    protected PsiFileImpl(@Nonnull FileViewProvider provider) {
        myManager = (PsiManagerEx) provider.getManager();
        myViewProvider = (AbstractFileViewProvider) provider;
        myPsiLock = myViewProvider.getFilePsiLock();
        myModuleRef = ClearableLazyValue.nullable(() -> LanguageModuleUtilInternal.findModuleForPsiElement(this));
    }

    public void setContentElementType(IElementType contentElementType) {
        LOG.assertTrue(contentElementType instanceof ILazyParseableElementType, contentElementType);
        myContentElementType = contentElementType;
    }

    public IElementType getContentElementType() {
        return myContentElementType;
    }

    protected void init(@Nonnull IElementType elementType, IElementType contentElementType) {
        myElementType = elementType;
        setContentElementType(contentElementType);
    }

    public TreeElement createContentLeafElement(CharSequence leafText) {
        if (myContentElementType instanceof ILazyParseableElementType lazyParseableElemType) {
            return ASTFactory.lazy(lazyParseableElemType, leafText);
        }
        return ASTFactory.leaf(myContentElementType, leafText);
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Nullable
    @RequiredReadAction
    public FileElement getTreeElement() {
        FileElement node = derefTreeElement();
        if (node != null) {
            return node;
        }

        if (!getViewProvider().isPhysical()) {
            return loadTreeElement();
        }

        return null;
    }

    FileElement derefTreeElement() {
        return myTrees.derefTreeElement();
    }

    @Override
    public VirtualFile getVirtualFile() {
        return getViewProvider().isEventSystemEnabled() ? getViewProvider().getVirtualFile() : null;
    }

    @Override
    public boolean processChildren(@Nonnull PsiElementProcessor<PsiFileSystemItem> processor) {
        return true;
    }

    @Override
    @RequiredReadAction
    public boolean isValid() {
        if (myManager.getProject().isDisposed()) {
            // normally FileManager.dispose would call markInvalidated
            // but there's temporary disposed project in tests, which doesn't actually dispose its components :(
            return false;
        }
        if (!myViewProvider.getVirtualFile().isValid()) {
            // PSI listeners receive VFS deletion events and do markInvalidated
            // but some VFS listeners receive the same events before that and ask PsiFile.isValid
            return false;
        }

        if (!myPossiblyInvalidated) {
            return true;
        }

        /*
        Originally, all PSI was invalidated on root change, to avoid UI freeze (IDEA-172762),
        but that has led to too many PIEAEs (like IDEA-191185, IDEA-188292, IDEA-184186, EA-114990).

        Ideally those clients should all be converted to smart pointers, but that proved to be quite hard to do, especially without breaking API.
        And they mostly worked before those batch invalidations.

        So now we have a smarter way of dealing with this issue. On root change, we mark
        PSI as "potentially invalid", and then, when someone calls "isValid"
        (hopefully not for all cached PSI at once, and hopefully in a background thread),
        we check if the old PSI is equivalent to the one that would be re-created in its place.
        If yes, we return valid. If no, we invalidate the old PSI forever and return the new one.
        */

        // synchronized by read-write action
        if (((FileManagerImpl) myManager.getFileManager()).evaluateValidity(this)) {
            myPossiblyInvalidated = false;
            PsiInvalidElementAccessException.setInvalidationTrace(this, null);
            return true;
        }
        return false;
    }

    @Override
    public final void markInvalidated() {
        myPossiblyInvalidated = true;
        DebugUtil.onInvalidated(this);
    }

    @Override
    public boolean isContentsLoaded() {
        return derefTreeElement() != null;
    }

    @RequiredReadAction
    protected void assertReadAccessAllowed() {
        if (myViewProvider.getVirtualFile() instanceof ReadOnlyLightVirtualFile) {
            return;
        }
        getApplication().assertReadAccessAllowed();
    }

    @Nonnull
    @RequiredReadAction
    private FileElement loadTreeElement() {
        assertReadAccessAllowed();

        if (myPossiblyInvalidated) {
            PsiUtilCore.ensureValid(this); // for invalidation trace diagnostics
        }

        FileViewProvider viewProvider = getViewProvider();
        if (viewProvider.isPhysical()) {
            VirtualFile vFile = viewProvider.getVirtualFile();
            AstLoadingFilter.assertTreeLoadingAllowed(vFile);
            if (myManager.isAssertOnFileLoading(vFile)) {
                LOG.error("Access to tree elements not allowed. path='" + vFile.getPresentableUrl() + "'");
            }
        }

        synchronized (myPsiLock) {
            FileElement treeElement = derefTreeElement();
            if (treeElement != null) {
                return treeElement;
            }

            treeElement = createFileElement(viewProvider.getContents());
            treeElement.setPsi(this);

            myLoadingAst = true;
            try {
                updateTrees(myTrees.withAst(createTreeElementPointer(treeElement)));
            }
            finally {
                myLoadingAst = false;
            }

            if (LOG.isDebugEnabled() && viewProvider.isPhysical()) {
                LOG.debug("Loaded text for file " + viewProvider.getVirtualFile().getPresentableUrl());
            }

            return treeElement;
        }
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public StubbedSpine getStubbedSpine() {
        StubTree tree = getGreenStubTree();
        if (tree != null) {
            return tree.getSpine();
        }

        AstSpine astSpine = calcTreeElement().getStubbedSpine();
        if (!myTrees.useSpineRefs()) {
            synchronized (myPsiLock) {
                updateTrees(myTrees.switchToSpineRefs(FileTrees.getAllSpinePsi(astSpine)));
            }
        }
        return astSpine;
    }

    @Nullable
    @Override
    @RequiredReadAction
    public IStubFileElementType getElementTypeForStubBuilder() {
        ParserDefinition definition = ParserDefinition.forLanguage(getLanguage());
        IFileElementType type = definition == null ? null : definition.getFileNodeType();
        return type instanceof IStubFileElementType stubFileElementType ? stubFileElementType : null;
    }

    @Nonnull
    protected FileElement createFileElement(CharSequence docText) {
        FileElement treeElement;
        TreeElement contentLeaf = createContentLeafElement(docText);

        if (contentLeaf instanceof FileElement fileElement) {
            treeElement = fileElement;
        }
        else {
            CompositeElement xxx = ASTFactory.composite(myElementType);
            assert xxx instanceof FileElement : "BUMM";
            treeElement = (FileElement) xxx;
            treeElement.rawAddChildrenWithoutNotifications(contentLeaf);
        }

        return treeElement;
    }

    @Override
    public void clearCaches() {
        myModificationStamp++;
        myModuleRef.clear();
    }

    @RequiredReadAction
    @Override
    public String getText() {
        ASTNode tree = derefTreeElement();
        if (!isValid()) {
            ProgressManager.checkCanceled();

            // even invalid PSI can calculate its text by concatenating its children
            if (tree != null) {
                return tree.getText();
            }

            throw new PsiInvalidElementAccessException(this);
        }
        String string = getViewProvider().getContents().toString();
        if (tree != null && string.length() != tree.getTextLength()) {
            throw new AssertionError("File text mismatch: tree.length=" + tree.getTextLength() + "; psi.length=" + string.length() + "; this=" + this + "; vp=" + getViewProvider());
        }
        return string;
    }

    @RequiredReadAction
    @Override
    public int getTextLength() {
        ASTNode tree = derefTreeElement();
        if (tree != null) {
            return tree.getTextLength();
        }

        PsiUtilCore.ensureValid(this);
        return getViewProvider().getContents().length();
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public TextRange getTextRange() {
        return new TextRange(0, getTextLength());
    }

    @RequiredReadAction
    @Override
    public PsiElement getNextSibling() {
        return SharedPsiElementImplUtil.getNextSibling(this);
    }

    @RequiredReadAction
    @Override
    public PsiElement getPrevSibling() {
        return SharedPsiElementImplUtil.getPrevSibling(this);
    }

    @Override
    @RequiredReadAction
    public long getModificationStamp() {
        PsiElement context = getContext();
        PsiFile contextFile = context == null || !context.isValid() ? null : context.getContainingFile();
        long contextStamp = contextFile == null ? 0 : contextFile.getModificationStamp();
        return myModificationStamp + contextStamp;
    }

    @Override
    @RequiredReadAction
    public void subtreeChanged() {
        FileElement tree = getTreeElement();
        if (tree != null) {
            tree.clearCaches();
        }

        synchronized (myPsiLock) {
            if (myTrees.useSpineRefs()) {
                LOG.error(
                    "Somebody has requested stubbed spine during PSI operations; " +
                        "not only is this expensive, but will also cause stub PSI invalidation"
                );
            }
            updateTrees(myTrees.clearStub("subtreeChanged"));
        }
        clearCaches();
        getViewProvider().rootChanged(this);
    }

    @Override
    @RequiredReadAction
    @SuppressWarnings("CloneDoesntCallSuperClone")
    protected PsiFileImpl clone() {
        FileViewProvider viewProvider = getViewProvider();
        FileViewProvider providerCopy = viewProvider.clone();
        Language language = getLanguage();
        if (providerCopy == null) {
            throw new AssertionError("Unable to clone the view provider: " + viewProvider + "; " + language);
        }
        PsiFileImpl clone = BlockSupportImpl.getFileCopy(this, providerCopy);
        copyCopyableDataTo(clone);

        if (getTreeElement() != null) {
            // not set by provider in clone
            FileElement treeClone = (FileElement) calcTreeElement().clone();
            clone.setTreeElementPointer(treeClone); // should not use setTreeElement here because cloned file still have VirtualFile (SCR17963)
            treeClone.setPsi(clone);
        }
        else {
            clone.setTreeElementPointer(null);
        }

        if (viewProvider.isEventSystemEnabled()) {
            clone.myOriginalFile = this;
        }
        else if (myOriginalFile != null) {
            clone.myOriginalFile = myOriginalFile;
        }

        FileManagerImpl.clearPsiCaches(providerCopy);

        return clone;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public String getName() {
        return getViewProvider().getVirtualFile().getName();
    }

    @Override
    @RequiredWriteAction
    public PsiElement setName(@Nonnull String name) throws IncorrectOperationException {
        checkSetName(name);
        return PsiFileImplUtil.setName(this, name);
    }

    @Override
    @RequiredUIAccess
    public void checkSetName(String name) {
        if (!getViewProvider().isEventSystemEnabled()) {
            return;
        }
        PsiFileImplUtil.checkSetName(this, name);
    }

    @Override
    public boolean isWritable() {
        return getViewProvider().getVirtualFile().isWritable();
    }

    @Override
    @RequiredReadAction
    public PsiDirectory getParent() {
        return getContainingDirectory();
    }

    @Nullable
    @Override
    @RequiredReadAction
    public PsiDirectory getContainingDirectory() {
        VirtualFile file = getViewProvider().getVirtualFile();
        VirtualFile parentFile = file.getParent();
        if (parentFile == null) {
            return null;
        }
        if (!parentFile.isValid()) {
            LOG.error("Invalid parent: " + parentFile + " of file " + file + ", file.valid=" + file.isValid());
            return null;
        }
        return getManager().findDirectory(parentFile);
    }

    @Override
    @Nonnull
    public PsiFile getContainingFile() {
        return this;
    }

    @Override
    @RequiredWriteAction
    public void delete() throws IncorrectOperationException {
        checkDelete();
        PsiFileImplUtil.doDelete(this);
    }

    @Override
    public void checkDelete() throws IncorrectOperationException {
        if (!getViewProvider().isEventSystemEnabled()) {
            throw new IncorrectOperationException();
        }
        CheckUtil.checkWritable(this);
    }

    @Override
    @Nonnull
    public PsiFile getOriginalFile() {
        return myOriginalFile == null ? this : myOriginalFile;
    }

    @Override
    public void setOriginalFile(@Nonnull PsiFile originalFile) {
        myOriginalFile = originalFile.getOriginalFile();

        FileViewProvider original = myOriginalFile.getViewProvider();
        ((AbstractFileViewProvider) original).registerAsCopy(myViewProvider);
    }

    @Override
    @Nonnull
    public PsiFile[] getPsiRoots() {
        FileViewProvider viewProvider = getViewProvider();
        Set<Language> languages = viewProvider.getLanguages();

        PsiFile[] roots = new PsiFile[languages.size()];
        int i = 0;
        for (Language language : languages) {
            PsiFile psi = viewProvider.getPsi(language);
            if (psi == null) {
                LOG.error("PSI is null for " + language + "; in file: " + this);
            }
            roots[i++] = psi;
        }
        if (roots.length > 1) {
            Arrays.sort(roots, FILE_BY_LANGUAGE_ID);
        }
        return roots;
    }

    private static final Comparator<PsiFile> FILE_BY_LANGUAGE_ID = Comparator.comparing(o -> o.getLanguage().getID());

    @Override
    public boolean isPhysical() {
        // TODO[ik] remove this shit with dummy file system
        return getViewProvider().isEventSystemEnabled();
    }

    @RequiredReadAction
    @Nonnull
    @Override
    public LanguageVersion getLanguageVersion() {
        VirtualFile file = getVirtualFile();
        if (file != null) {
            LanguageVersion version = file.getUserData(LanguageVersion.KEY);
            if (version != null && version.getLanguage() == getLanguage()) {
                return version;
            }
        }
        return PsiTreeUtil.getLanguageVersion(this);
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public Language getLanguage() {
        return myElementType.getLanguage();
    }

    @Nullable
    @Override
    public IFileElementType getFileElementType() {
        return myElementType instanceof IFileElementType fileElementType
            ? fileElementType
            : ObjectUtil.tryCast(myContentElementType, IFileElementType.class);
    }

    @Override
    @Nonnull
    public FileViewProvider getViewProvider() {
        return myViewProvider;
    }

    public void setTreeElementPointer(@Nullable FileElement element) {
        updateTrees(FileTrees.noStub(element, this));
    }

    @RequiredReadAction
    @Override
    public PsiElement findElementAt(int offset) {
        return getViewProvider().findElementAt(offset);
    }

    @RequiredReadAction
    @Override
    public PsiReference findReferenceAt(int offset) {
        return getViewProvider().findReferenceAt(offset);
    }

    @RequiredReadAction
    @Override
    @Nonnull
    public char[] textToCharArray() {
        return CharArrayUtil.fromSequence(getViewProvider().getContents());
    }

    @Nonnull
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public <T> T[] findChildrenByClass(Class<T> aClass) {
        List<T> result = new ArrayList<>();
        for (PsiElement child : getChildren()) {
            if (aClass.isInstance(child)) {
                result.add((T) child);
            }
        }
        return result.toArray(ArrayUtil.newArray(aClass, result.size()));
    }

    @Nullable
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public <T> T findChildByClass(Class<T> aClass) {
        for (PsiElement child : getChildren()) {
            if (aClass.isInstance(child)) {
                return (T) child;
            }
        }
        return null;
    }

    public boolean isTemplateDataFile() {
        return false;
    }

    @Override
    public PsiElement getContext() {
        return FileContextUtil.getFileContext(this);
    }

    @Override
    @RequiredWriteAction
    public void onContentReload() {
        getApplication().assertWriteAccessAllowed();

        clearContent("onContentReload");
    }

    final void clearContent(String reason) {
        DebugUtil.performPsiModification(reason, () -> {
            synchronized (myPsiLock) {
                FileElement treeElement = derefTreeElement();
                if (treeElement != null) {
                    treeElement.detachFromFile();
                    DebugUtil.onInvalidated(treeElement);
                }
                updateTrees(myTrees.clearStub(reason));
                setTreeElementPointer(null);
            }
        });
        clearCaches();
    }

    /**
     * @return a root stub of {@link #getStubTree()}, or null if the file is not stub-based or AST has been loaded.
     */
    @Nullable
    @RequiredReadAction
    public StubElement getStub() {
        StubTree stubHolder = getStubTree();
        return stubHolder != null ? stubHolder.getRoot() : null;
    }

    /**
     * A green stub is a stub object that can co-exist with tree (AST). So, contrary to {@link #getStub()}, can be non-null
     * even if the AST has been loaded in this file. It can be used in cases when retrieving information from a stub is cheaper
     * than from AST.
     *
     * @return a stub object corresponding to the file's content, or null if it's not available (e.g. has been garbage-collected)
     * @see #getStub()
     * @see #getStubTree()
     */
    @Nullable
    @RequiredReadAction
    public final StubElement getGreenStub() {
        StubTree stubHolder = getGreenStubTree();
        return stubHolder != null ? stubHolder.getRoot() : null;
    }

    /**
     * @return a stub tree, if this file has it, and only if AST isn't loaded
     */
    @Nullable
    @Override
    @RequiredReadAction
    public StubTree getStubTree() {
        assertReadAccessAllowed();

        if (getTreeElement() != null) {
            return null;
        }

        StubTree derefed = derefStub();
        if (derefed != null) {
            return derefed;
        }

        if (Boolean.TRUE.equals(getUserData(BUILDING_STUB)) || myLoadingAst || getElementTypeForStubBuilder() == null) {
            return null;
        }

        VirtualFile vFile = getVirtualFile();
        if (!(vFile instanceof VirtualFileWithId && vFile.isValid())) {
            return null;
        }

        ObjectStubTree tree = StubTreeLoader.getInstance().readOrBuild(getProject(), vFile, this);
        if (!(tree instanceof StubTree stubTree)) {
            return null;
        }
        FileViewProvider viewProvider = getViewProvider();
        List<Pair<IStubFileElementType, PsiFile>> roots = StubTreeBuilder.getStubbedRoots(viewProvider);

        synchronized (myPsiLock) {
            if (getTreeElement() != null) {
                return null;
            }

            StubTree derefdOnLock = derefStub();
            if (derefdOnLock != null) {
                return derefdOnLock;
            }

            PsiFileStubImpl baseRoot = (PsiFileStubImpl) stubTree.getRoot();
            if (!baseRoot.rootsAreSet()) {
                LOG.error("Stub roots must be set when stub tree was read or built with StubTreeLoader");
                return null;
            }
            PsiFileStub[] stubRoots = baseRoot.getStubRoots();
            if (stubRoots.length != roots.size()) {
                Function<PsiFileStub, String> stubToString =
                    stub -> "{" + stub.getClass().getSimpleName() + " " + stub.getType().getLanguage() + "}";
                LOG.error(
                    "readOrBuilt roots = " + StringUtil.join(stubRoots, stubToString, ", ") +
                        "; " + StubTreeLoader.getFileViewProviderMismatchDiagnostics(viewProvider)
                );
                rebuildStub();
                return null;
            }

            StubTree result = null;
            for (int i = 0; i < roots.size(); i++) {
                PsiFileImpl eachPsiRoot = (PsiFileImpl) roots.get(i).second;
                if (eachPsiRoot.derefStub() == null) {
                    StubTree eachStubTree = eachPsiRoot.setStubTree(stubRoots[i]);
                    if (eachPsiRoot == this) {
                        result = eachStubTree;
                    }
                }
            }

            assert result != null : "Current file not in root list: " + roots + ", vp=" + viewProvider;
            return result;
        }
    }

    @Nonnull
    @RequiredReadAction
    private StubTree setStubTree(PsiFileStub root) {
        //noinspection unchecked
        ((StubBase) root).setPsi(this);
        StubTree stubTree = new StubTree(root);
        FileElement fileElement = getTreeElement();
        stubTree.setDebugInfo("created in getStubTree(), with AST = " + (fileElement != null));
        updateTrees(myTrees.withStub(stubTree, fileElement));
        return stubTree;
    }

    @Nullable
    public StubTree derefStub() {
        return myTrees.derefStub();
    }

    private void updateTrees(@Nonnull FileTrees trees) {
        if (!ourTreeUpdater.compareAndSet(this, myTrees, trees)) {
            LOG.error("Non-atomic trees update");
            myTrees = trees;
        }
    }

    protected PsiFileImpl cloneImpl(FileElement treeElementClone) {
        PsiFileImpl clone = (PsiFileImpl) super.clone();
        clone.setTreeElementPointer(treeElementClone); // should not use setTreeElement here because cloned file still have VirtualFile (SCR17963)
        treeElementClone.setPsi(clone);
        return clone;
    }

    private boolean isKeepTreeElementByHardReference() {
        return !getViewProvider().isEventSystemEnabled();
    }

    @Nonnull
    private Supplier<FileElement> createTreeElementPointer(@Nonnull FileElement treeElement) {
        if (isKeepTreeElementByHardReference()) {
            return treeElement;
        }
        return myManager.isBatchFilesProcessingMode() ? new PatchedWeakReference<>(treeElement) : new SoftReference<>(treeElement);
    }

    @Override
    public final PsiManager getManager() {
        return myManager;
    }

    @Override
    public PsiElement getNavigationElement() {
        return this;
    }

    @Override
    public PsiElement getOriginalElement() {
        return getOriginalFile();
    }

    @Nonnull
    @RequiredReadAction
    public final FileElement calcTreeElement() {
        FileElement treeElement = getTreeElement();
        return treeElement != null ? treeElement : loadTreeElement();
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public PsiElement[] getChildren() {
        return calcTreeElement().getChildrenAsPsiElements((TokenSet) null, PsiElement.ARRAY_FACTORY);
    }

    @RequiredReadAction
    @Override
    public PsiElement getFirstChild() {
        return SharedImplUtil.getFirstChild(getNode());
    }

    @RequiredReadAction
    @Override
    public PsiElement getLastChild() {
        return SharedImplUtil.getLastChild(getNode());
    }

    @Override
    @RequiredReadAction
    public void acceptChildren(@Nonnull PsiElementVisitor visitor) {
        SharedImplUtil.acceptChildren(visitor, getNode());
    }

    @RequiredReadAction
    @Override
    public int getStartOffsetInParent() {
        return calcTreeElement().getStartOffsetInParent();
    }

    @Override
    @RequiredReadAction
    public int getTextOffset() {
        return calcTreeElement().getTextOffset();
    }

    @Override
    @RequiredReadAction
    public boolean textMatches(@Nonnull CharSequence text) {
        return calcTreeElement().textMatches(text);
    }

    @Override
    @RequiredReadAction
    public boolean textMatches(@Nonnull PsiElement element) {
        return calcTreeElement().textMatches(element);
    }

    @Override
    @RequiredReadAction
    public boolean textContains(char c) {
        return calcTreeElement().textContains(c);
    }

    @Override
    @RequiredReadAction
    public final PsiElement copy() {
        return clone();
    }

    @Override
    @RequiredWriteAction
    public PsiElement add(@Nonnull PsiElement element) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        calcTreeElement().addInternal(elementCopy, elementCopy, null, null);
        elementCopy = ChangeUtil.decodeInformation(elementCopy);
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    @Override
    @RequiredWriteAction
    public PsiElement addBefore(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        calcTreeElement().addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
        elementCopy = ChangeUtil.decodeInformation(elementCopy);
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    @Override
    @RequiredWriteAction
    public PsiElement addAfter(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        calcTreeElement().addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
        elementCopy = ChangeUtil.decodeInformation(elementCopy);
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    @Override
    public final void checkAdd(@Nonnull PsiElement element) {
        CheckUtil.checkWritable(this);
    }

    @Override
    @RequiredWriteAction
    public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, null, null);
    }

    @Override
    @RequiredWriteAction
    public PsiElement addRangeBefore(
        @Nonnull PsiElement first,
        @Nonnull PsiElement last,
        PsiElement anchor
    ) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
    }

    @Override
    @RequiredWriteAction
    public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
    }

    @Override
    @RequiredWriteAction
    public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        if (first == null) {
            LOG.assertTrue(last == null);
            return;
        }
        ASTNode firstElement = SourceTreeToPsiMap.psiElementToTree(first);
        ASTNode lastElement = SourceTreeToPsiMap.psiElementToTree(last);
        CompositeElement treeElement = calcTreeElement();
        LOG.assertTrue(firstElement.getTreeParent() == treeElement);
        LOG.assertTrue(lastElement.getTreeParent() == treeElement);
        CodeEditUtil.removeChildren(treeElement, firstElement, lastElement);
    }

    @Override
    @RequiredWriteAction
    public PsiElement replace(@Nonnull PsiElement newElement) throws IncorrectOperationException {
        CompositeElement treeElement = calcTreeElement();
        return SharedImplUtil.doReplace(this, treeElement, newElement);
    }

    @Override
    public PsiReference getReference() {
        return null;
    }

    @Override
    @Nonnull
    public PsiReference[] getReferences() {
        return SharedPsiElementImplUtil.getReferences(this);
    }

    @Override
    public boolean processDeclarations(
        @Nonnull PsiScopeProcessor processor,
        @Nonnull ResolveState state,
        PsiElement lastParent,
        @Nonnull PsiElement place
    ) {
        return true;
    }

    @Override
    @Nonnull
    public GlobalSearchScope getResolveScope() {
        return ResolveScopeManager.getElementResolveScope(this);
    }

    @Override
    @Nonnull
    public SearchScope getUseScope() {
        return ResolveScopeManager.getElementUseScope(this);
    }

    @Override
    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Override
            @RequiredReadAction
            public String getPresentableText() {
                return getName();
            }

            @Override
            @RequiredReadAction
            public String getLocationString() {
                PsiDirectory psiDirectory = getParent();
                if (psiDirectory != null) {
                    return psiDirectory.getVirtualFile().getPresentableUrl();
                }
                return null;
            }

            @Override
            @RequiredReadAction
            public Image getIcon() {
                return IconDescriptorUpdaters.getIcon(PsiFileImpl.this, 0);
            }
        };
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
        assert canNavigate() : this;
        //noinspection ConstantConditions
        PsiNavigationSupport.getInstance().getDescriptor(this).navigate(requestFocus);
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        return PsiNavigationSupport.getInstance().canNavigate(this);
    }

    @Override
    @RequiredReadAction
    public boolean canNavigateToSource() {
        return canNavigate();
    }

    @Override
    @Nonnull
    public final Project getProject() {
        return getManager().getProject();
    }

    @Nullable
    @Override
    @RequiredReadAction
    public Module getModule() throws PsiInvalidElementAccessException {
        if (!isValid()) {
            throw new PsiInvalidElementAccessException(this);
        }
        return myModuleRef.get();
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public FileASTNode getNode() {
        return calcTreeElement();
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        return this == another;
    }

    /**
     * @return a stub tree object having {@link #getGreenStub()} as a root, or null if there's no green stub available
     */
    @Nullable
    @RequiredReadAction
    public final StubTree getGreenStubTree() {
        StubTree result = derefStub();
        return result != null ? result : getStubTree();
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public StubTree calcStubTree() {
        StubTree tree = derefStub();
        if (tree != null) {
            return tree;
        }
        FileElement fileElement = calcTreeElement();
        synchronized (myPsiLock) {
            tree = derefStub();

            if (tree == null) {
                assertReadAccessAllowed();
                IStubFileElementType contentElementType = getElementTypeForStubBuilder();
                if (contentElementType == null) {
                    VirtualFile vFile = getVirtualFile();
                    String message = "ContentElementType: " + getContentElementType() + "; file: " + this + "\n\t" +
                        "Boolean.TRUE.equals(getUserData(BUILDING_STUB)) = " + Boolean.TRUE.equals(getUserData(BUILDING_STUB)) + "\n\t" +
                        "getTreeElement() = " + getTreeElement() + "\n\t" +
                        "vFile instanceof VirtualFileWithId = " + (vFile instanceof VirtualFileWithId) + "\n\t" +
                        "StubUpdatingIndex.canHaveStub(vFile) = " + StubTreeLoader.getInstance().canHaveStub(vFile);
                    rebuildStub();
                    throw new AssertionError(message);
                }

                StubElement currentStubTree = contentElementType.getBuilder().buildStubTree(this);
                if (currentStubTree == null) {
                    throw new AssertionError("Stub tree wasn't built for " + contentElementType + "; file: " + this);
                }

                tree = new StubTree((PsiFileStub) currentStubTree);
                tree.setDebugInfo("created in calcStubTree");
                updateTrees(myTrees.withStub(tree, fileElement));
            }

            return tree;
        }
    }

    final void rebuildStub() {
        Application application = getApplication();
        application.invokeLater(
            () -> {
                if (!myManager.isDisposed()) {
                    myManager.dropPsiCaches();
                }

                VirtualFile vFile = getVirtualFile();
                if (vFile != null && vFile.isValid()) {
                    Document doc = FileDocumentManager.getInstance().getCachedDocument(vFile);
                    if (doc != null) {
                        FileDocumentManager.getInstance().saveDocument(doc);
                    }

                    FileContentUtilCore.reparseFiles(vFile);
                    StubTreeLoader.getInstance().rebuildStubTree(vFile);
                }
            },
            application.getNoneModalityState()
        );
    }

    @Override
    @RequiredReadAction
    public void putInfo(@Nonnull Map<String, String> info) {
        putInfo(this, info);
    }

    @RequiredReadAction
    public static void putInfo(@Nonnull PsiFile psiFile, @Nonnull Map<String, String> info) {
        info.put("fileName", psiFile.getName());
        info.put("fileType", psiFile.getFileType().toString());
    }

    @Override
    public String toString() {
        return myElementType.toString();
    }

    public final void beforeAstChange() {
        checkWritable();
        synchronized (myPsiLock) {
            FileTrees updated = myTrees.switchToStrongRefs();
            if (updated != myTrees) {
                updateTrees(updated);
            }
        }
    }

    private void checkWritable() {
        PsiDocumentManager docManager = PsiDocumentManager.getInstance(getProject());
        if (docManager instanceof PsiDocumentManagerBase docManagerBase
            && !docManagerBase.isCommitInProgress()
            && !(myViewProvider instanceof FreeThreadedFileViewProvider)) {
            CheckUtil.checkWritable(this);
        }
    }
}
