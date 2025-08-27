// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.internal.file;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.util.LowMemoryWatcher;
import consulo.application.util.registry.Registry;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.Language;
import consulo.language.content.FileIndexFacade;
import consulo.language.file.FileViewProvider;
import consulo.language.file.FileViewProviderFactory;
import consulo.language.file.LanguageFileViewProviderFactory;
import consulo.language.file.VirtualFileViewProviderFactory;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.file.AbstractFileViewProvider;
import consulo.language.impl.file.FreeThreadedFileViewProvider;
import consulo.language.impl.file.SingleRootFileViewProvider;
import consulo.language.impl.internal.psi.PsiManagerImpl;
import consulo.language.impl.internal.psi.PsiTreeChangeEventImpl;
import consulo.language.impl.psi.PsiDirectoryImpl;
import consulo.language.psi.*;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.language.util.LanguageUtil;
import consulo.logging.Logger;
import consulo.project.event.DumbModeListener;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Maps;
import consulo.util.concurrent.ConcurrencyUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.StackOverflowPreventedException;
import consulo.virtualFileSystem.InvalidVirtualFileAccessException;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Provider;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public final class FileManagerImpl implements FileManager {
    private static final Key<Boolean> IN_COMA = Key.create("IN_COMA");
    private static final Logger LOG = Logger.getInstance(FileManagerImpl.class);
    private final Key<FileViewProvider> myPsiHardRefKey = Key.create("HARD_REFERENCE_TO_PSI"); //non-static!

    private final Application myApplication;
    private final PsiManagerImpl myManager;
    private final Provider<FileIndexFacade> myFileIndex;

    private final AtomicReference<ConcurrentMap<VirtualFile, PsiDirectory>> myVFileToPsiDirMap = new AtomicReference<>();
    private final AtomicReference<ConcurrentMap<VirtualFile, FileViewProvider>> myVFileToViewProviderMap = new AtomicReference<>();

    /**
     * Holds thread-local temporary providers that are sometimes needed while checking if a file is valid
     */
    private final ThreadLocal<Map<VirtualFile, FileViewProvider>> myTempProviders = ThreadLocal.withInitial(HashMap::new);

    private boolean myDisposed;

    private final MessageBusConnection myConnection;

    public FileManagerImpl(@Nonnull Application application,
                           @Nonnull PsiManagerImpl manager,
                           @Nonnull Provider<FileIndexFacade> fileIndex) {
        myApplication = application;
        myManager = manager;
        myFileIndex = fileIndex;
        myConnection = manager.getProject().getMessageBus().connect();

        Disposer.register(manager, this);
        LowMemoryWatcher.register(this::processQueue, this);

        myConnection.subscribe(DumbModeListener.class, new DumbModeListener() {
            @Override
            public void enteredDumbMode() {
                processFileTypesChanged(false);
            }

            @Override
            public void exitDumbMode() {
                processFileTypesChanged(false);
            }
        });
    }

    private static final VirtualFile NULL = new LightVirtualFile();

    public void processQueue() {
        // just to call processQueue()
        ConcurrentMap<VirtualFile, FileViewProvider> map = myVFileToViewProviderMap.get();
        if (map != null) {
            map.remove(NULL);
        }
    }

    @Nonnull
    public ConcurrentMap<VirtualFile, FileViewProvider> getVFileToViewProviderMap() {
        ConcurrentMap<VirtualFile, FileViewProvider> map = myVFileToViewProviderMap.get();
        if (map == null) {
            map = ConcurrencyUtil.cacheOrGet(myVFileToViewProviderMap, ContainerUtil.createConcurrentWeakValueMap());
        }
        return map;
    }

    @Nonnull
    private ConcurrentMap<VirtualFile, PsiDirectory> getVFileToPsiDirMap() {
        ConcurrentMap<VirtualFile, PsiDirectory> map = myVFileToPsiDirMap.get();
        if (map == null) {
            map = ConcurrencyUtil.cacheOrGet(myVFileToPsiDirMap, ContainerUtil.createConcurrentSoftValueMap());
        }
        return map;
    }

    public static void clearPsiCaches(@Nonnull FileViewProvider provider) {
        ((AbstractFileViewProvider) provider).getCachedPsiFiles().forEach(PsiFile::clearCaches);
    }

    @RequiredWriteAction
    @Override
    public void forceReload(@Nonnull VirtualFile vFile) {
        LanguageSubstitutors.cancelReparsing(vFile);
        FileViewProvider viewProvider = findCachedViewProvider(vFile);
        if (viewProvider == null) {
            return;
        }
        myApplication.assertWriteAccessAllowed();

        VirtualFile dir = vFile.getParent();
        PsiDirectory parentDir = dir == null ? null : getCachedDirectory(dir);
        PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(myManager);
        if (parentDir == null) {
            event.setPropertyName(PsiTreeChangeEvent.PROP_UNLOADED_PSI);

            myManager.beforePropertyChange(event);
            setViewProvider(vFile, null);
            myManager.propertyChanged(event);
        }
        else {
            event.setParent(parentDir);

            myManager.beforeChildrenChange(event);
            setViewProvider(vFile, null);
            myManager.childrenChanged(event);
        }
    }

    @RequiredWriteAction
    @Override
    public void firePropertyChangedForUnloadedPsi() {
        PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(myManager);
        event.setPropertyName(PsiTreeChangeEvent.PROP_UNLOADED_PSI);

        myManager.beforePropertyChange(event);
        myManager.propertyChanged(event);
    }

    @Override
    public void dispose() {
        myConnection.disconnect();
        clearViewProviders();

        myDisposed = true;
    }

    @RequiredWriteAction
    private void clearViewProviders() {
        myApplication.assertWriteAccessAllowed();
        DebugUtil.performPsiModification("clearViewProviders", () -> {
            ConcurrentMap<VirtualFile, FileViewProvider> map = myVFileToViewProviderMap.get();
            if (map != null) {
                for (FileViewProvider provider : map.values()) {
                    markInvalidated(provider);
                }
            }
            myVFileToViewProviderMap.set(null);
        });
    }

    @Override
    @TestOnly
    public void cleanupForNextTest() {
        myApplication.runWriteAction(this::clearViewProviders);

        myVFileToPsiDirMap.set(null);
        myManager.getModificationTracker().incCounter();
    }

    @RequiredReadAction
    @Override
    @Nonnull
    public FileViewProvider findViewProvider(@Nonnull VirtualFile file) {
        assert !file.isDirectory();
        FileViewProvider viewProvider = findCachedViewProvider(file);
        if (viewProvider != null) {
            return viewProvider;
        }
        if (file instanceof VirtualFileWindow) {
            throw new IllegalStateException("File " + file + " is invalid");
        }

        Map<VirtualFile, FileViewProvider> tempMap = myTempProviders.get();
        if (tempMap.containsKey(file)) {
            return Objects.requireNonNull(tempMap.get(file), "Recursive file view provider creation");
        }

        viewProvider = createFileViewProvider(file, true);
        if (file instanceof LightVirtualFile) {
            return file.putUserDataIfAbsent(myPsiHardRefKey, viewProvider);
        }
        return Maps.cacheOrGet(getVFileToViewProviderMap(), file, viewProvider);
    }

    @RequiredReadAction
    @Override
    public FileViewProvider findCachedViewProvider(@Nonnull VirtualFile file) {
        FileViewProvider viewProvider = getRawCachedViewProvider(file);

        if (viewProvider instanceof AbstractFileViewProvider && viewProvider.getUserData(IN_COMA) != null) {
            Map<VirtualFile, FileViewProvider> tempMap = myTempProviders.get();
            if (tempMap.containsKey(file)) {
                return tempMap.get(file);
            }

            if (!evaluateValidity((AbstractFileViewProvider) viewProvider)) {
                return null;
            }
        }
        return viewProvider;
    }

    @Nullable
    private FileViewProvider getRawCachedViewProvider(@Nonnull VirtualFile file) {
        ConcurrentMap<VirtualFile, FileViewProvider> map = myVFileToViewProviderMap.get();
        FileViewProvider viewProvider = map == null ? null : map.get(file);
        return viewProvider == null ? file.getUserData(myPsiHardRefKey) : viewProvider;
    }

    @RequiredReadAction
    @Override
    public void setViewProvider(@Nonnull VirtualFile virtualFile, @Nullable FileViewProvider fileViewProvider) {
        FileViewProvider prev = getRawCachedViewProvider(virtualFile);
        if (prev == fileViewProvider) {
            return;
        }
        if (prev != null) {
            DebugUtil.performPsiModification(null, () -> markInvalidated(prev));
        }

        if (fileViewProvider == null) {
            getVFileToViewProviderMap().remove(virtualFile);
        }
        else if (virtualFile instanceof LightVirtualFile) {
            virtualFile.putUserData(myPsiHardRefKey, fileViewProvider);
        }
        else {
            getVFileToViewProviderMap().put(virtualFile, fileViewProvider);
        }
    }

    @Override
    @Nonnull
    public FileViewProvider createFileViewProvider(@Nonnull VirtualFile file, boolean eventSystemEnabled) {
        FileType fileType = file.getFileType();
        Language language = LanguageUtil.getLanguageForPsi(myManager.getProject(), file);
        FileViewProviderFactory factory = language == null ? VirtualFileViewProviderFactory.forFileType(fileType) : LanguageFileViewProviderFactory.forLanguage(language);
        FileViewProvider viewProvider = factory == null ? null : factory.createFileViewProvider(file, language, myManager, eventSystemEnabled);

        return viewProvider == null ? new SingleRootFileViewProvider(myManager, file, eventSystemEnabled) : viewProvider;
    }

    /**
     * @deprecated Left for plugin compatibility
     */
    @Override
    @Deprecated
    public void markInitialized() {
    }

    /**
     * @deprecated Left for plugin compatibility
     */
    @Override
    @Deprecated
    public boolean isInitialized() {
        return true;
    }

    private boolean myProcessingFileTypesChange;

    public void processFileTypesChanged(boolean clearViewProviders) {
        if (myProcessingFileTypesChange) {
            return;
        }
        myProcessingFileTypesChange = true;
        DebugUtil.performPsiModification(null, () -> {
            try {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(myManager);
                    event.setPropertyName(PsiTreeChangeEvent.PROP_FILE_TYPES);
                    myManager.beforePropertyChange(event);

                    possiblyInvalidatePhysicalPsi();
                    if (clearViewProviders) {
                        clearViewProviders();
                    }

                    myManager.propertyChanged(event);
                });
            }
            finally {
                myProcessingFileTypesChange = false;
            }
        });
    }

    @RequiredWriteAction
    public void possiblyInvalidatePhysicalPsi() {
        myApplication.assertWriteAccessAllowed();
        removeInvalidDirs();
        for (FileViewProvider provider : getVFileToViewProviderMap().values()) {
            markPossiblyInvalidated(provider);
        }
    }

    @Override
    public void dispatchPendingEvents() {
        if (myDisposed) {
            LOG.error("Project is already disposed: " + myManager.getProject());
        }

        myConnection.deliverImmediately();
    }

    @TestOnly
    void checkConsistency() {
        for (VirtualFile file : new ArrayList<>(getVFileToViewProviderMap().keySet())) {
            findCachedViewProvider(file); // complete delayed validity checks
        }

        Map<VirtualFile, FileViewProvider> fileToViewProvider = new HashMap<>(getVFileToViewProviderMap());
        myVFileToViewProviderMap.set(null);
        for (Map.Entry<VirtualFile, FileViewProvider> entry : fileToViewProvider.entrySet()) {
            FileViewProvider fileViewProvider = entry.getValue();
            VirtualFile vFile = entry.getKey();
            LOG.assertTrue(vFile.isValid());
            PsiFile psiFile1 = findFile(vFile);
            if (psiFile1 != null && fileViewProvider != null && fileViewProvider.isPhysical()) { // might get collected
                PsiFile psi = fileViewProvider.getPsi(fileViewProvider.getBaseLanguage());
                assert psi != null : fileViewProvider + "; " + fileViewProvider.getBaseLanguage() + "; " + psiFile1;
                assert psiFile1.getClass().equals(psi.getClass()) : psiFile1 + "; " + psi + "; " + psiFile1.getClass() + "; " + psi.getClass();
            }
        }

        Map<VirtualFile, PsiDirectory> fileToPsiDirMap = new HashMap<>(getVFileToPsiDirMap());
        myVFileToPsiDirMap.set(null);

        for (VirtualFile vFile : fileToPsiDirMap.keySet()) {
            LOG.assertTrue(vFile.isValid());
            PsiDirectory psiDir1 = findDirectory(vFile);
            LOG.assertTrue(psiDir1 != null);

            VirtualFile parent = vFile.getParent();
            if (parent != null) {
                LOG.assertTrue(getVFileToPsiDirMap().get(parent) != null);
            }
        }
    }

    @RequiredReadAction
    @Override
    @Nullable
    public PsiFile findFile(@Nonnull VirtualFile vFile) {
        if (vFile.isDirectory()) {
            return null;
        }

        myApplication.assertReadAccessAllowed();
        if (!vFile.isValid()) {
            LOG.error("Invalid file: " + vFile);
            return null;
        }

        dispatchPendingEvents();
        FileViewProvider viewProvider = findViewProvider(vFile);
        return viewProvider.getPsi(viewProvider.getBaseLanguage());
    }

    @RequiredReadAction
    @Override
    @Nullable
    public PsiFile getCachedPsiFile(@Nonnull VirtualFile vFile) {
        myApplication.assertReadAccessAllowed();
        if (!vFile.isValid()) {
            throw new InvalidVirtualFileAccessException(vFile);
        }
        if (myDisposed) {
            LOG.error("Project is already disposed: " + myManager.getProject());
        }

        dispatchPendingEvents();

        return getCachedPsiFileInner(vFile);
    }

    @RequiredReadAction
    @Override
    @Nullable
    public PsiDirectory findDirectory(@Nonnull VirtualFile vFile) {
        if (myDisposed) {
            LOG.error("Access to psi files should not be performed after project disposal: " + myManager.getProject());
        }

        myApplication.assertReadAccessAllowed();
        if (!vFile.isValid()) {
            LOG.error("File is not valid:" + vFile);
            return null;
        }

        if (!vFile.isDirectory()) {
            return null;
        }
        dispatchPendingEvents();

        return findDirectoryImpl(vFile, getVFileToPsiDirMap());
    }

    @Nullable
    private PsiDirectory findDirectoryImpl(@Nonnull VirtualFile vFile, @Nonnull ConcurrentMap<VirtualFile, PsiDirectory> psiDirMap) {
        PsiDirectory psiDir = psiDirMap.get(vFile);
        if (psiDir != null) {
            return psiDir;
        }

        if (isExcludedOrIgnored(vFile)) {
            return null;
        }

        VirtualFile parent = vFile.getParent();
        if (parent != null) { //?
            findDirectoryImpl(parent, psiDirMap);// need to cache parent directory - used for firing events
        }

        psiDir = new PsiDirectoryImpl(myManager, vFile);
        return Maps.cacheOrGet(psiDirMap, vFile, psiDir);
    }

    private boolean isExcludedOrIgnored(@Nonnull VirtualFile vFile) {
        if (myManager.getProject().isDefault()) {
            return false;
        }
        FileIndexFacade fileIndexFacade = myFileIndex.get();
        return Registry.is("ide.hide.excluded.files") ? fileIndexFacade.isExcludedFile(vFile) : fileIndexFacade.isUnderIgnored(vFile);
    }

    @Override
    public PsiDirectory getCachedDirectory(@Nonnull VirtualFile vFile) {
        return getVFileToPsiDirMap().get(vFile);
    }

    @Override
    public void removeFilesAndDirsRecursively(@Nonnull VirtualFile vFile) {
        DebugUtil.performPsiModification("removeFilesAndDirsRecursively", () -> {
            VirtualFileUtil.visitChildrenRecursively(vFile, new VirtualFileVisitor<Void>() {
                @Override
                public boolean visitFile(@Nonnull VirtualFile file) {
                    if (file.isDirectory()) {
                        getVFileToPsiDirMap().remove(file);
                    }
                    else {
                        FileViewProvider viewProvider = getVFileToViewProviderMap().remove(file);
                        if (viewProvider != null) {
                            markInvalidated(viewProvider);
                        }
                    }
                    return true;
                }
            });
        });
    }

    private void markInvalidated(@Nonnull FileViewProvider viewProvider) {
        viewProvider.putUserData(IN_COMA, null);
        ((AbstractFileViewProvider) viewProvider).markInvalidated();
        viewProvider.getVirtualFile().putUserData(myPsiHardRefKey, null);
    }

    public static void markPossiblyInvalidated(@Nonnull FileViewProvider viewProvider) {
        LOG.assertTrue(!(viewProvider instanceof FreeThreadedFileViewProvider));
        viewProvider.putUserData(IN_COMA, true);
        ((AbstractFileViewProvider) viewProvider).markPossiblyInvalidated();
        clearPsiCaches(viewProvider);
    }

    @RequiredReadAction
    @Override
    @Nullable
    public PsiFile getCachedPsiFileInner(@Nonnull VirtualFile file) {
        FileViewProvider fileViewProvider = findCachedViewProvider(file);
        return fileViewProvider != null ? ((AbstractFileViewProvider) fileViewProvider).getCachedPsi(fileViewProvider.getBaseLanguage()) : null;
    }

    @Nonnull
    @Override
    public List<PsiFile> getAllCachedFiles() {
        List<PsiFile> files = new ArrayList<>();
        for (VirtualFile file : new ArrayList<>(getVFileToViewProviderMap().keySet())) {
            FileViewProvider provider = findCachedViewProvider(file);
            if (provider != null) {
                ContainerUtil.addAllNotNull(files, ((AbstractFileViewProvider) provider).getCachedPsiFiles());
            }
        }
        return files;
    }

    private void removeInvalidDirs() {
        myVFileToPsiDirMap.set(null);
    }

    @Override
    public void removeInvalidFilesAndDirs(boolean useFind) {
        removeInvalidDirs();

        // note: important to update directories map first - findFile uses findDirectory!
        Map<VirtualFile, FileViewProvider> fileToPsiFileMap = new HashMap<>(getVFileToViewProviderMap());
        Map<VirtualFile, FileViewProvider> originalFileToPsiFileMap = new HashMap<>(getVFileToViewProviderMap());
        if (useFind) {
            myVFileToViewProviderMap.set(null);
        }
        for (Iterator<VirtualFile> iterator = fileToPsiFileMap.keySet().iterator(); iterator.hasNext(); ) {
            VirtualFile vFile = iterator.next();

            if (!vFile.isValid()) {
                iterator.remove();
                continue;
            }

            FileViewProvider view = fileToPsiFileMap.get(vFile);
            if (useFind) {
                if (view == null) { // soft ref. collected
                    iterator.remove();
                    continue;
                }
                PsiFile psiFile1 = findFile(vFile);
                if (psiFile1 == null) {
                    iterator.remove();
                    continue;
                }

                if (!areViewProvidersEquivalent(view, psiFile1.getViewProvider())) {
                    iterator.remove();
                }
                else {
                    clearPsiCaches(view);
                }
            }
            else if (!evaluateValidity((AbstractFileViewProvider) view)) {
                iterator.remove();
            }
        }
        myVFileToViewProviderMap.set(null);
        getVFileToViewProviderMap().putAll(fileToPsiFileMap);

        markInvalidations(originalFileToPsiFileMap);
    }

    public static boolean areViewProvidersEquivalent(@Nonnull FileViewProvider view1, @Nonnull FileViewProvider view2) {
        if (view1.getClass() != view2.getClass() || view1.getFileType() != view2.getFileType()) {
            return false;
        }

        Language baseLanguage = view1.getBaseLanguage();
        if (baseLanguage != view2.getBaseLanguage()) {
            return false;
        }

        if (!view1.getLanguages().equals(view2.getLanguages())) {
            return false;
        }
        PsiFile psi1 = view1.getPsi(baseLanguage);
        PsiFile psi2 = view2.getPsi(baseLanguage);
        if (psi1 == null || psi2 == null) {
            return psi1 == psi2;
        }
        return psi1.getClass() == psi2.getClass();
    }

    private void markInvalidations(@Nonnull Map<VirtualFile, FileViewProvider> originalFileToPsiFileMap) {
        if (!originalFileToPsiFileMap.isEmpty()) {
            DebugUtil.performPsiModification(null, () -> {
                for (Map.Entry<VirtualFile, FileViewProvider> entry : originalFileToPsiFileMap.entrySet()) {
                    FileViewProvider viewProvider = entry.getValue();
                    if (getVFileToViewProviderMap().get(entry.getKey()) != viewProvider) {
                        markInvalidated(viewProvider);
                    }
                }
            });
        }
    }

    @RequiredWriteAction
    @Override
    public void reloadFromDisk(@Nonnull PsiFile file) {
        myApplication.assertWriteAccessAllowed();

        VirtualFile vFile = file.getVirtualFile();
        assert vFile != null;

        Document document = FileDocumentManager.getInstance().getCachedDocument(vFile);
        if (document != null) {
            FileDocumentManager.getInstance().reloadFromDisk(document);
        }
        else {
            reloadPsiAfterTextChange(file.getViewProvider(), vFile);
        }
    }

    public void reloadPsiAfterTextChange(@Nonnull FileViewProvider viewProvider, @Nonnull VirtualFile vFile) {
        if (!areViewProvidersEquivalent(viewProvider, createFileViewProvider(vFile, false))) {
            forceReload(vFile);
            return;
        }

        ((AbstractFileViewProvider) viewProvider).onContentReload();
    }

    /**
     * Should be called only from implementations of {@link PsiFile#isValid()}, only after they've been {@link PsiFileEx#markInvalidated()},
     * and only to check if they can be made valid again.
     * Synchronized by read-write action. Calls from several threads in read action for the same virtual file are allowed.
     *
     * @return if the file is still valid
     */
    @RequiredReadAction
    public boolean evaluateValidity(@Nonnull PsiFile file) {
        AbstractFileViewProvider vp = (AbstractFileViewProvider) file.getViewProvider();
        return evaluateValidity(vp) && vp.getCachedPsiFiles().contains(file);
    }

    @RequiredReadAction
    private boolean evaluateValidity(@Nonnull AbstractFileViewProvider viewProvider) {
        myApplication.assertReadAccessAllowed();

        VirtualFile file = viewProvider.getVirtualFile();
        if (getRawCachedViewProvider(file) != viewProvider) {
            return false;
        }

        if (viewProvider.getUserData(IN_COMA) == null) {
            return true;
        }

        if (shouldResurrect(viewProvider, file)) {
            viewProvider.putUserData(IN_COMA, null);
            LOG.assertTrue(getRawCachedViewProvider(file) == viewProvider);

            for (PsiFile psiFile : viewProvider.getCachedPsiFiles()) {
                // update "myPossiblyInvalidated" fields in files by calling "isValid"
                // that will call us recursively again, but since we're not IN_COMA now, we'll exit earlier and avoid SOE
                if (!psiFile.isValid()) {
                    LOG.error(new PsiInvalidElementAccessException(psiFile));
                }
            }
            return true;
        }

        getVFileToViewProviderMap().remove(file, viewProvider);
        file.replace(myPsiHardRefKey, viewProvider, null);
        viewProvider.putUserData(IN_COMA, null);

        return false;
    }

    private boolean shouldResurrect(@Nonnull FileViewProvider viewProvider, @Nonnull VirtualFile file) {
        if (!file.isValid()) {
            return false;
        }

        Map<VirtualFile, FileViewProvider> tempProviders = myTempProviders.get();
        if (tempProviders.containsKey(file)) {
            LOG.error(new StackOverflowPreventedException("isValid leads to endless recursion in " + viewProvider.getClass() + ": " + new ArrayList<>(viewProvider.getLanguages())));
        }
        tempProviders.put(file, null);
        try {
            FileViewProvider recreated = createFileViewProvider(file, true);
            tempProviders.put(file, recreated);
            return areViewProvidersEquivalent(viewProvider, recreated) && ((AbstractFileViewProvider) viewProvider).getCachedPsiFiles().stream().noneMatch(f -> hasInvalidOriginal(f));
        }
        finally {
            FileViewProvider temp = tempProviders.remove(file);
            if (temp != null) {
                DebugUtil.performPsiModification("invalidate temp view provider", ((AbstractFileViewProvider) temp)::markInvalidated);
            }
        }
    }

    private static boolean hasInvalidOriginal(@Nonnull PsiFile file) {
        PsiFile original = file.getOriginalFile();
        return original != file && !original.isValid();
    }
}
