// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.impl.internal;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.platform.PlatformTarget;
import consulo.application.platform.PlatformTargetListener;
import consulo.application.platform.PlatformTargetManager;
import consulo.codeEditor.EditorFactory;
import consulo.component.ComponentManager;
import consulo.component.messagebus.MessageBus;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.FileDocumentSynchronizationVetoer;
import consulo.document.event.DocumentEvent;
import consulo.document.event.FileDocumentManagerListener;
import consulo.document.impl.FrozenDocument;
import consulo.document.internal.RecomputeFileTypeMarker;
import consulo.document.internal.DocumentEx;
import consulo.document.internal.FileDocumentManagerEx;
import consulo.document.internal.PrioritizedDocumentListener;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.internal.RealTextEditor;
import consulo.fileEditor.localize.FileEditorLocalize;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.editor.internal.EditorFactoryImpl;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.internal.FileViewProviderInternal;
import consulo.language.internal.PomAspectGuard;
import consulo.language.internal.PsiFileInternal;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.internal.ExternalChangeAction;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.project.ProjectManager;
import consulo.project.internal.ProjectManagerEx;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.localize.UILocalize;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.virtualFileSystem.event.VFileContentChangeEvent;
import consulo.virtualFileSystem.event.VFileDeleteEvent;
import consulo.virtualFileSystem.event.VFilePropertyChangeEvent;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import consulo.virtualFileSystem.internal.SafeWriteRequestor;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@ServiceImpl
@Singleton
public class FileDocumentManagerImpl implements FileDocumentManagerEx, SafeWriteRequestor {
    private static final Logger LOG = Logger.getInstance(FileDocumentManagerImpl.class);

    private static final Key<String> LINE_SEPARATOR_KEY = Key.create("LINE_SEPARATOR_KEY");
    private static final Key<VirtualFile> FILE_KEY = Key.create("FILE_KEY");
    private static final Key<Boolean> BIG_FILE_PREVIEW = Key.create("BIG_FILE_PREVIEW");

    private final Set<Document> myUnsavedDocuments = ContainerUtil.newConcurrentSet();
    // the unsaved documents whose files are on a machine which is not connected now - saved when it is connected again
    private final Set<Document> myOfflineDocuments = ContainerUtil.newConcurrentSet();
    // the notifications about the unsaved files of a machine which is not connected, by the id of its target
    private final Map<String, Notification> myOfflineNotifications = new ConcurrentHashMap<>();
    // the ids of the targets which are not connected now
    private final Set<String> myDisconnectedTargets = ConcurrentHashMap.newKeySet();
    // the local copies of the offline documents - kept while typing, written a moment after the last change
    private final OfflineDocumentBackups myBackups = new OfflineDocumentBackups();
    private final Set<Document> myBackupQueue = ContainerUtil.newConcurrentSet();
    private volatile boolean myBackupScheduled;

    private final MessageBus myBus;

    private static final Object lock = new Object();
    private final FileDocumentManagerListener myMultiCaster;
    private final TrailingSpacesStripper myTrailingSpacesStripper = new TrailingSpacesStripper();

    private boolean myOnClose;

    protected volatile MemoryDiskConflictResolver myConflictResolver = new MemoryDiskConflictResolver();

    private final PrioritizedDocumentListener myPhysicalDocumentChangeTracker = new PrioritizedDocumentListener() {
        @Override
        public int getPriority() {
            return Integer.MIN_VALUE;
        }

        @Override
        public void documentChanged(DocumentEvent e) {
            Document document = e.getDocument();
            if (!Application.get().hasWriteAction(ExternalChangeAction.ExternalDocumentChange.class)) {
                myUnsavedDocuments.add(document);
                if (myOfflineDocuments.contains(document) || isOnDisconnectedTarget(document)) {
                    myOfflineDocuments.add(document);
                    scheduleBackup(document);
                }
            }
            Project project = CommandProcessor.getInstance().getCurrentCommandProject();
            if (project == null) {
                project = ProjectLocator.getInstance().guessProjectForFile(getFile(document));
            }
            String lineSeparator = CodeStyle.getProjectOrDefaultSettings(project).getLineSeparator();
            document.putUserData(LINE_SEPARATOR_KEY, lineSeparator);

            // avoid documents piling up during batch processing
            if (areTooManyDocumentsInTheQueue(myUnsavedDocuments)) {
                saveAllDocumentsLater();
            }
        }
    };

    @Inject
    public FileDocumentManagerImpl(Application application, ProjectManager projectManager) {
        ((ProjectManagerEx) projectManager).registerCloseProjectVeto(new MyProjectCloseHandler());

        myBus = application.getMessageBus();
        InvocationHandler handler = (proxy, method, args) -> {
            multiCast(method, args);
            return null;
        };

        ClassLoader loader = FileDocumentManagerListener.class.getClassLoader();
        myMultiCaster =
            (FileDocumentManagerListener) Proxy.newProxyInstance(loader, new Class[]{FileDocumentManagerListener.class}, handler);

        myBus.connect().subscribe(PlatformTargetListener.class, new PlatformTargetListener() {
            @Override
            public void targetConnected(PlatformTarget target) {
                myDisconnectedTargets.remove(target.getId());
                application.invokeLater(() -> saveOfflineDocuments(target));
            }

            @Override
            public void targetDisconnected(PlatformTarget target) {
                myDisconnectedTargets.add(target.getId());
                application.invokeLater(() -> {
                    List<Document> documents = new ArrayList<>();
                    for (Document document : myUnsavedDocuments) {
                        VirtualFile file = getFile(document);
                        if (file != null && target.getId().equals(targetIdOf(file))) {
                            documents.add(document);
                        }
                    }
                    if (!documents.isEmpty()) {
                        keepOffline(documents, true);
                    }
                });
            }
        });
    }

    static final class MyProjectCloseHandler implements Predicate<Project> {
        @Override
        @RequiredUIAccess
        public boolean test(Project project) {
            FileDocumentManagerImpl manager = (FileDocumentManagerImpl) FileDocumentManager.getInstance();
            if (!manager.myUnsavedDocuments.isEmpty()) {
                manager.saveOnClose();
            }
            List<Document> offline = manager.offlineDocumentsOf(project);
            if (!offline.isEmpty() && !manager.closeWithOfflineDocuments(project, offline)) {
                return false;
            }
            // the offline documents of the other projects wait for the close of those
            return manager.myOfflineDocuments.containsAll(manager.myUnsavedDocuments);
        }
    }

    private static void unwrapAndRethrow(Exception e) {
        Throwable unwrapped = e;
        if (e instanceof InvocationTargetException) {
            unwrapped = e.getCause() == null ? e : e.getCause();
        }
        ExceptionUtil.rethrowUnchecked(unwrapped);
        LOG.error(unwrapped);
    }

    @SuppressWarnings("OverlyBroadCatchBlock")
    private void multiCast(Method method, Object[] args) {
        try {
            method.invoke(myBus.syncPublisher(FileDocumentManagerListener.class), args);
        }
        catch (ClassCastException e) {
            LOG.error("Arguments: " + Arrays.toString(args), e);
        }
        catch (Exception e) {
            unwrapAndRethrow(e);
        }

        // stripping trailing spaces
        try {
            method.invoke(myTrailingSpacesStripper, args);
        }
        catch (Exception e) {
            unwrapAndRethrow(e);
        }
    }

    @Override
    @RequiredUIAccess
    public @Nullable Document getDocument(VirtualFile file) {
        Application.get().assertReadAccessAllowed();
        DocumentEx document = (DocumentEx) getCachedDocument(file);
        if (document == null) {
            if (!file.isValid() || file.isDirectory() || isBinaryWithoutDecompiler(file)) {
                return null;
            }

            boolean tooLarge = RawFileLoader.getInstance().isLargeForContentLoading(file.getLength());
            if (file.getFileType().isBinary() && tooLarge) {
                return null;
            }

            CharSequence text = tooLarge ? LoadTextUtil.loadText(file, getPreviewCharCount(file)) : LoadTextUtil.loadText(file);
            synchronized (lock) {
                document = (DocumentEx) getCachedDocument(file);
                if (document != null) {
                    return document; // Double checking
                }

                document = (DocumentEx) createDocument(text, file);
                document.setModificationStamp(file.getModificationStamp());
                document.putUserData(BIG_FILE_PREVIEW, tooLarge ? Boolean.TRUE : null);
                FileType fileType = file.getFileType();
                document.setReadOnly(tooLarge || !file.isWritable() || fileType.isBinary());

                if (!(file instanceof LightVirtualFile || file.getFileSystem() instanceof NonPhysicalFileSystem)) {
                    document.addDocumentListener(myPhysicalDocumentChangeTracker);
                }

                if (file instanceof LightVirtualFile) {
                    registerDocumentImpl(document, file);
                }
                else {
                    document.putUserData(FILE_KEY, file);
                    cacheDocument(file, document);
                }
            }

            myMultiCaster.fileContentLoaded(file, document);
        }

        return document;
    }

    public static boolean areTooManyDocumentsInTheQueue(Collection<? extends Document> documents) {
        if (documents.size() > 100) {
            return true;
        }
        int totalSize = 0;
        for (Document document : documents) {
            totalSize += document.getTextLength();
            if (RawFileLoader.getInstance().isLargeForContentLoading(totalSize)) {
                return true;
            }
        }
        return false;
    }

    private static Document createDocument(CharSequence text, VirtualFile file) {
        boolean acceptSlashR = file instanceof LightVirtualFile && StringUtil.indexOf(text, '\r') >= 0;
        boolean freeThreaded = Boolean.TRUE.equals(file.getUserData(FileViewProviderInternal.FREE_THREADED));
        DocumentEx document = (DocumentEx) ((EditorFactoryImpl) EditorFactory.getInstance()).createDocument(text, acceptSlashR, freeThreaded);
        Project project = ProjectLocator.getInstance().guessProjectForFile(file);
        int tabSize;
        if (project == null) {
            tabSize = CodeStyle.getDefaultSettings().getTabSize(file.getFileType());
        }
        else {
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
            if (psiFile != null) {
                tabSize = CodeStyle.getSettings(psiFile).getTabSize(file.getFileType());
            } else {
                tabSize = CodeStyle.getSettings(project, document).getTabSize(file.getFileType());
            }
        }
        // calculate and pass tab size here since it's the ony place we have access to CodeStyle.
        // tabSize might be needed by PersistentRangeMarkers to be able to restore from (line;col) info to offset
        document.documentCreatedFrom(file, tabSize);
        return document;
    }

    @Override
    public @Nullable Document getCachedDocument(VirtualFile file) {
        Document hard = file.getUserData(HARD_REF_TO_DOCUMENT_KEY);
        return hard != null ? hard : getDocumentFromCache(file);
    }

    @Override
    public void registerDocument(Document document, VirtualFile virtualFile) {
        registerDocumentImpl(document, virtualFile);
    }

    public static void registerDocumentImpl(Document document, VirtualFile virtualFile) {
        synchronized (lock) {
            document.putUserData(FILE_KEY, virtualFile);
            virtualFile.putUserData(HARD_REF_TO_DOCUMENT_KEY, document);
        }
    }

    @Override
    public @Nullable VirtualFile getFile(Document document) {
        return document instanceof FrozenDocument ? null : document.getUserData(FILE_KEY);
    }

    @TestOnly
    @RequiredWriteAction
    public void dropAllUnsavedDocuments() {
        if (!Application.get().isUnitTestMode()) {
            throw new RuntimeException("This method is only for test mode!");
        }
        Application.get().assertWriteAccessAllowed();
        if (!myUnsavedDocuments.isEmpty()) {
            myUnsavedDocuments.clear();
            fireUnsavedDocumentsDropped();
        }
    }

    private void saveAllDocumentsLater() {
        // later because some document might have been blocked by PSI right now
        Application.get().invokeLater(() -> {
            Document[] unsavedDocuments = getUnsavedDocuments();
            for (Document document : unsavedDocuments) {
                VirtualFile file = getFile(document);
                if (file == null) {
                    continue;
                }
                Project project = ProjectLocator.getInstance().guessProjectForFile(file);
                if (project != null && !PsiDocumentManager.getInstance(project).isDocumentBlockedByPsi(document)) {
                    saveDocument(document);
                }
            }
        });
    }

    /**
     * @param isExplicit caused by user directly (Save action) or indirectly (e.g. Compile)
     */
    @Override
    @RequiredUIAccess
    public void saveAllDocuments(boolean isExplicit) {
        UIAccess.assertIsUIThread();

        myMultiCaster.beforeAllDocumentsSaving();
        if (myUnsavedDocuments.isEmpty()) {
            return;
        }

        Map<Document, IOException> failedToSave = saveUnsavedDocuments(isExplicit);

        if (!failedToSave.isEmpty()) {
            handleErrorsOnSave(failedToSave);
        }
    }

    @Override
    public void saveAllDocuments(UIAccess uiAccess) {
        Application.get().assertWriteAccessAllowed();

        myMultiCaster.beforeAllDocumentsSaving();
        if (myUnsavedDocuments.isEmpty()) {
            return;
        }

        Map<Document, IOException> failedToSave = saveUnsavedDocuments(true);

        if (!failedToSave.isEmpty()) {
            uiAccess.give(() -> handleErrorsOnSave(failedToSave));
        }
    }

    private Map<Document, IOException> saveUnsavedDocuments(boolean isExplicit) {
        Map<Document, IOException> failedToSave = new HashMap<>();
        Set<Document> vetoed = new HashSet<>();
        List<Document> offline = new ArrayList<>();
        while (true) {
            int count = 0;

            for (Document document : myUnsavedDocuments) {
                if (failedToSave.containsKey(document) || vetoed.contains(document)) {
                    continue;
                }
                try {
                    doSaveDocument(document, isExplicit);
                }
                catch (FileSystemUnavailableException e) {
                    // the machine of the file is not connected - the changes wait for it
                    vetoed.add(document);
                    offline.add(document);
                }
                catch (IOException e) {
                    failedToSave.put(document, e);
                }
                catch (SaveVetoException e) {
                    vetoed.add(document);
                }
                count++;
            }

            if (count == 0) {
                break;
            }
        }

        if (!offline.isEmpty()) {
            keepOffline(offline, !myOnClose);
        }
        return failedToSave;
    }

    @Override
    @RequiredUIAccess
    public void saveDocument(Document document) {
        saveDocument(document, true);
    }

    @RequiredUIAccess
    public void saveDocument(Document document, boolean explicit) {
        UIAccess.assertIsUIThread();

        if (!myUnsavedDocuments.contains(document)) {
            return;
        }

        try {
            doSaveDocument(document, explicit);
        }
        catch (FileSystemUnavailableException e) {
            keepOffline(List.of(document), !myOnClose);
        }
        catch (IOException e) {
            handleErrorsOnSave(Collections.singletonMap(document, e));
        }
        catch (SaveVetoException ignored) {
        }
    }

    @Override
    @RequiredUIAccess
    public void saveDocumentAsIs(Document document) {
        VirtualFile file = getFile(document);
        boolean spaceStrippingEnabled = true;
        if (file != null) {
            spaceStrippingEnabled = TrailingSpacesStripper.isEnabled(file);
            TrailingSpacesStripper.setEnabled(file, false);
        }
        try {
            saveDocument(document);
        }
        finally {
            if (file != null) {
                TrailingSpacesStripper.setEnabled(file, spaceStrippingEnabled);
            }
        }
    }

    private static class SaveVetoException extends Exception {
    }

    /**
     * The documents stay unsaved until the machine of their files is connected again, with local copies of their text -
     * the user is told once per machine.
     */
    private void keepOffline(Collection<Document> documents, boolean notify) {
        myOfflineDocuments.addAll(documents);

        for (Document document : documents) {
            VirtualFile file = getFile(document);
            if (file == null) {
                continue;
            }
            myBackups.save(file, document.getImmutableCharSequence());

            PlatformTarget target = notify ? findTarget(file) : null;
            if (target == null || myOfflineNotifications.containsKey(target.getId())) {
                continue;
            }

            Notification notification = NotificationService.getInstance()
                .newWarn(UnsavedFilesNotificationGroupContributor.TARGET_DISCONNECTED)
                .title(FileEditorLocalize.unsavedFilesTargetDisconnectedTitle(target.getDisplayName()))
                .content(FileEditorLocalize.unsavedFilesTargetDisconnectedContent(target.getDisplayName()))
                .addClosingAction(FileEditorLocalize.unsavedFilesTargetDisconnectedReconnect(), () -> target.connect())
                .whenExpired(() -> myOfflineNotifications.remove(target.getId()))
                .create();
            myOfflineNotifications.put(target.getId(), notification);
            notification.notify(ProjectLocator.getInstance().guessProjectForFile(file));
        }
    }

    /**
     * The machine is connected again - the documents kept for it are saved now. A file changed there meanwhile is
     * refreshed first, and its conflict with the document is resolved by the user, as for any file changed on disk.
     */
    @RequiredUIAccess
    private void saveOfflineDocuments(PlatformTarget target) {
        Notification notification = myOfflineNotifications.remove(target.getId());
        if (notification != null) {
            notification.expire();
        }

        for (Document document : new ArrayList<>(myOfflineDocuments)) {
            VirtualFile file = getFile(document);
            PlatformTarget documentTarget = file == null ? null : findTarget(file);
            if (documentTarget != null && !documentTarget.getId().equals(target.getId())) {
                continue;
            }
            myOfflineDocuments.remove(document);
            saveDocument(document);
        }
    }

    /**
     * The target of the machine the file is on.
     */
    private static @Nullable PlatformTarget findTarget(VirtualFile file) {
        String targetId = targetIdOf(file);
        return targetId == null ? null : Application.get().getInstance(PlatformTargetManager.class).findTarget(targetId);
    }

    /**
     * The id of the target of the machine the file is on, if it is another machine - the root of a file of another
     * machine is named by the id of its target.
     */
    private static @Nullable String targetIdOf(VirtualFile file) {
        if (!(file.getFileSystem() instanceof NewVirtualFileSystem fileSystem) || fileSystem instanceof LocalFileSystem) {
            return null;
        }
        return fileSystem.extractRootPath(file.getPath());
    }

    private boolean isOnDisconnectedTarget(Document document) {
        if (myDisconnectedTargets.isEmpty()) {
            return false;
        }
        VirtualFile file = getFile(document);
        String targetId = file == null ? null : targetIdOf(file);
        return targetId != null && myDisconnectedTargets.contains(targetId);
    }

    private void scheduleBackup(Document document) {
        myBackupQueue.add(document);
        if (!myBackupScheduled) {
            myBackupScheduled = true;
            AppExecutorUtil.getAppScheduledExecutorService().schedule(this::flushBackups, 2, TimeUnit.SECONDS);
        }
    }

    private void flushBackups() {
        myBackupScheduled = false;
        for (Document document : new ArrayList<>(myBackupQueue)) {
            myBackupQueue.remove(document);
            VirtualFile file = getFile(document);
            if (file != null && myOfflineDocuments.contains(document)) {
                myBackups.save(file, document.getImmutableCharSequence());
            }
        }
    }

    OfflineDocumentBackups getOfflineBackups() {
        return myBackups;
    }

    @RequiredUIAccess
    private void saveOnClose() {
        myOnClose = true;
        try {
            saveAllDocuments();
        }
        finally {
            myOnClose = false;
        }
    }

    /**
     * The offline documents of the files of the project - or of no project at all.
     */
    private List<Document> offlineDocumentsOf(Project project) {
        List<Document> documents = new ArrayList<>();
        for (Document document : myOfflineDocuments) {
            VirtualFile file = getFile(document);
            if (file == null || !myUnsavedDocuments.contains(document)) {
                continue;
            }
            Project fileProject = ProjectLocator.getInstance().guessProjectForFile(file);
            if (fileProject == null || fileProject == project) {
                documents.add(document);
            }
        }
        return documents;
    }

    /**
     * The project closes with documents which cannot be saved now - the user connects their machines again, keeps local
     * copies of them (restored the next time the project opens) or drops them.
     *
     * @return false if the project stays open
     */
    @RequiredUIAccess
    private boolean closeWithOfflineDocuments(Project project, List<Document> documents) {
        List<Document> offline = documents;
        while (!offline.isEmpty()) {
            Map<String, PlatformTarget> targets = new LinkedHashMap<>();
            for (Document document : offline) {
                VirtualFile file = getFile(document);
                PlatformTarget target = file == null ? null : findTarget(file);
                if (target != null) {
                    targets.putIfAbsent(target.getId(), target);
                }
            }
            String names = StringUtil.join(targets.values(), target -> target.getDisplayName().get(), ", ");

            int choice = Messages.showDialog(
                project,
                FileEditorLocalize.unsavedFilesCloseMessage(offline.size(), names).get(),
                FileEditorLocalize.unsavedFilesCloseTitle().get(),
                new String[]{
                    FileEditorLocalize.unsavedFilesCloseReconnect().get(),
                    FileEditorLocalize.unsavedFilesCloseKeepCopies().get(),
                    FileEditorLocalize.unsavedFilesCloseDiscard().get(),
                    CommonLocalize.buttonCancel().get()
                },
                0,
                Messages.getWarningIcon()
            );
            switch (choice) {
                case 0 -> {
                    ProgressManager.getInstance().runProcessWithProgressSynchronously(
                        () -> {
                            for (PlatformTarget target : targets.values()) {
                                try {
                                    target.connect().get(30, TimeUnit.SECONDS);
                                }
                                catch (Exception e) {
                                    LOG.info("Cannot connect " + target.getId() + " to save its files", e);
                                }
                            }
                        },
                        FileEditorLocalize.unsavedFilesCloseConnecting(names),
                        true,
                        project
                    );
                    saveOnClose();
                    offline = offlineDocumentsOf(project);
                }
                case 1 -> {
                    dropOfflineDocuments(offline, false);
                    return true;
                }
                case 2 -> {
                    dropOfflineDocuments(offline, true);
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * The documents leave the IDE unsaved - with their local copies kept up to date, or deleted.
     */
    private void dropOfflineDocuments(List<Document> documents, boolean deleteCopies) {
        for (Document document : documents) {
            myUnsavedDocuments.remove(document);
            myOfflineDocuments.remove(document);
            VirtualFile file = getFile(document);
            if (file == null) {
                continue;
            }
            if (deleteCopies) {
                myBackups.remove(file);
            }
            else {
                myBackups.save(file, document.getImmutableCharSequence());
            }
            // the text of the document is not the one of the file - it must not be found for the file again
            unbindFileFromDocument(file, document);
        }
        fireUnsavedDocumentsDropped();
    }

    private void doSaveDocument(Document document, boolean isExplicit) throws IOException, SaveVetoException {
        VirtualFile file = getFile(document);
        if (LOG.isTraceEnabled()) {
            LOG.trace("saving: " + file);
        }

        if (file == null || file instanceof LightVirtualFile || file.isValid() && !isFileModified(file)) {
            removeFromUnsaved(document);
            return;
        }

        if (file.isValid() && needsRefresh(file)) {
            LOG.trace("  refreshing...");
            file.refresh(false, false);
            if (!myUnsavedDocuments.contains(document)) {
                return;
            }
        }

        if (!maySaveDocument(file, document, isExplicit)) {
            throw new SaveVetoException();
        }

        LOG.trace("  writing...");
        WriteAction.run(() -> doSaveDocumentInWriteAction(document, file));
        LOG.trace("  done");
    }

    private boolean maySaveDocument(VirtualFile file, Document document, boolean isExplicit) {
        return !myConflictResolver.hasConflict(file)
            && Application.get().getExtensionPoint(FileDocumentSynchronizationVetoer.class)
            .allMatchSafe(vetoer -> vetoer.maySaveDocument(document, isExplicit));
    }

    @RequiredWriteAction
    private void doSaveDocumentInWriteAction(Document document, VirtualFile file) throws IOException {
        if (!file.isValid()) {
            removeFromUnsaved(document);
            return;
        }

        if (!file.equals(getFile(document))) {
            registerDocumentImpl(document, file);
        }

        boolean saveNeeded = false;
        IOException ioException = null;
        try {
            saveNeeded = isSaveNeeded(document, file);
        }
        catch (FileSystemUnavailableException e) {
            // the content on the machine of the file is unknown now - the document stays unsaved
            throw e;
        }
        catch (IOException e) {
            // in case of corrupted VFS try to stay consistent
            ioException = e;
        }
        if (!saveNeeded) {
            if (document instanceof DocumentEx documentEx) {
                documentEx.setModificationStamp(file.getModificationStamp());
            }
            removeFromUnsaved(document);
            updateModifiedProperty(file);
            if (ioException != null) {
                throw ioException;
            }
            return;
        }

        PomAspectGuard.guardPsiModificationsIn(Application.get(), () -> {
            myMultiCaster.beforeDocumentSaving(document);
            LOG.assertTrue(file.isValid());

            String text = document.getText();
            String lineSeparator = getLineSeparator(document, file);
            if (!lineSeparator.equals("\n")) {
                text = StringUtil.convertLineSeparators(text, lineSeparator);
            }

            Project project = ProjectLocator.getInstance().guessProjectForFile(file);
            LoadTextUtil.write(project, file, this, text, document.getModificationStamp());

            myUnsavedDocuments.remove(document);
            myOfflineDocuments.remove(document);
            myBackups.remove(file);
            LOG.assertTrue(!myUnsavedDocuments.contains(document));
            myTrailingSpacesStripper.clearLineModificationFlags(document);
        });
    }

    private static void updateModifiedProperty(VirtualFile file) {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            for (FileEditor editor : fileEditorManager.getAllEditors(file)) {
                if (editor instanceof RealTextEditor textEditor) {
                    textEditor.updateModifiedProperty();
                }
            }
        }
    }

    private void removeFromUnsaved(Document document) {
        myUnsavedDocuments.remove(document);
        myOfflineDocuments.remove(document);
        VirtualFile file = getFile(document);
        if (file != null) {
            myBackups.remove(file);
        }
        fireUnsavedDocumentsDropped();
        LOG.assertTrue(!myUnsavedDocuments.contains(document));
    }

    private static boolean isSaveNeeded(Document document, VirtualFile file) throws IOException {
        if (file.getFileType().isBinary() || document.getTextLength() > 1000 * 1000) {    // don't compare if the file is too big
            return true;
        }

        byte[] bytes = file.contentsToByteArray();
        CharSequence loaded = LoadTextUtil.getTextByBinaryPresentation(bytes, file, false, false);

        return !Comparing.equal(document.getCharsSequence(), loaded);
    }

    private static boolean needsRefresh(VirtualFile file) {
        VirtualFileSystem fs = file.getFileSystem();
        return fs instanceof NewVirtualFileSystem vfs && file.getTimeStamp() != vfs.getTimeStamp(file);
    }

    public static String getLineSeparator(Document document, VirtualFile file) {
        String lineSeparator = LoadTextUtil.getDetectedLineSeparator(file);
        if (lineSeparator == null) {
            lineSeparator = document.getUserData(LINE_SEPARATOR_KEY);
            assert lineSeparator != null : document;
        }
        return lineSeparator;
    }

    @Override
    public String getLineSeparator(@Nullable VirtualFile file, @Nullable ComponentManager project) {
        String lineSeparator = file == null ? null : LoadTextUtil.getDetectedLineSeparator(file);
        if (lineSeparator == null) {
            lineSeparator = CodeStyle.getProjectOrDefaultSettings((Project) project).getLineSeparator();
        }
        return lineSeparator;
    }

    @Override
    public boolean requestWriting(Document document, ComponentManager project) {
        return requestWritingStatus(document, project).hasWriteAccess();
    }

    @Override
    public WriteAccessStatus requestWritingStatus(Document document, @Nullable ComponentManager project) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (project != null && file != null && file.isValid()) {
            if (file.getFileType().isBinary()) {
                return WriteAccessStatus.NON_WRITABLE;
            }
            ReadonlyStatusHandler.OperationStatus writableStatus =
                ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(Collections.singletonList(file));
            if (writableStatus.hasReadonlyFiles()) {
                return new WriteAccessStatus(writableStatus.getReadonlyFilesMessage());
            }
            assert file.isWritable();
        }
        if (document.isWritable()) {
            return WriteAccessStatus.WRITABLE;
        }
        document.fireReadOnlyModificationAttempt();
        return WriteAccessStatus.NON_WRITABLE;
    }

    @Override
    @RequiredUIAccess
    public void reloadFiles(VirtualFile... files) {
        for (VirtualFile file : files) {
            if (file.exists()) {
                Document doc = getCachedDocument(file);
                if (doc != null) {
                    reloadFromDisk(doc);
                }
            }
        }
    }

    @Override
    public Document[] getUnsavedDocuments() {
        if (myUnsavedDocuments.isEmpty()) {
            return Document.EMPTY_ARRAY;
        }

        List<Document> list = new ArrayList<>(myUnsavedDocuments);
        return list.toArray(Document.EMPTY_ARRAY);
    }

    @Override
    public boolean isDocumentUnsaved(Document document) {
        return myUnsavedDocuments.contains(document);
    }

    @Override
    public boolean isFileModified(VirtualFile file) {
        Document doc = getCachedDocument(file);
        return doc != null && isDocumentUnsaved(doc) && doc.getModificationStamp() != file.getModificationStamp();
    }

    @Override
    public boolean isPartialPreviewOfALargeFile(Document document) {
        return Objects.equals(document.getUserData(BIG_FILE_PREVIEW), Boolean.TRUE);
    }

    @RequiredUIAccess
    protected void propertyChanged(VFilePropertyChangeEvent event) {
        VirtualFile file = event.getFile();
        if (VirtualFile.PROP_WRITABLE.equals(event.getPropertyName())) {
            Document document = getCachedDocument(file);
            if (document != null) {
                Application.get().runWriteAction(() -> document.setReadOnly(!file.isWritable()));
            }
        }
        else if (VirtualFile.PROP_NAME.equals(event.getPropertyName())) {
            Document document = getCachedDocument(file);
            if (document != null) {
                // a file is linked to a document - chances are it is an "unknown text file" now
                if (isBinaryWithoutDecompiler(file)) {
                    unbindFileFromDocument(file, document);
                }
            }
        }
    }

    private void unbindFileFromDocument(VirtualFile file, Document document) {
        removeDocumentFromCache(file);
        file.putUserData(HARD_REF_TO_DOCUMENT_KEY, null);
        document.putUserData(FILE_KEY, null);
    }

    private static boolean isBinaryWithDecompiler(VirtualFile file) {
        FileType ft = file.getFileType();
        return ft.isBinary() && BinaryFileDecompiler.forFileType(ft) != null;
    }

    private static boolean isBinaryWithoutDecompiler(VirtualFile file) {
        FileType fileType = file.getFileType();
        return fileType.isBinary() && BinaryFileDecompiler.forFileType(fileType) == null;
    }

    @RequiredUIAccess
    public void contentsChanged(VFileContentChangeEvent event) {
        VirtualFile virtualFile = event.getFile();
        Document document = getCachedDocument(virtualFile);

        if (event.isFromSave()) {
            return;
        }

        if (document == null || isBinaryWithDecompiler(virtualFile)) {
            myMultiCaster.fileWithNoDocumentChanged(virtualFile); // This will generate PSI event at FileManagerImpl
        }

        if (document != null && (document.getModificationStamp() == event.getOldModificationStamp() || !isDocumentUnsaved(document))) {
            reloadFromDisk(document);
        }
    }

    @Override
    @RequiredUIAccess
    public void reloadFromDisk(Document document) {
        UIAccess.assertIsUIThread();

        VirtualFile file = getFile(document);
        assert file != null;

        if (!fireBeforeFileContentReload(file, document)) {
            return;
        }

        Project project = ProjectLocator.getInstance().guessProjectForFile(file);
        SimpleReference<Boolean> isReloadable = SimpleReference.create(isReloadable(file, document, project));
        if (isReloadable.get()) {
            CommandProcessor.getInstance().newCommand()
                .project(project)
                .name(UILocalize.fileCacheConflictAction())
                .undoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION)
                .inWriteAction()
                .run(new ExternalChangeAction.ExternalDocumentChange(document, project) {
                    @Override
                    public void run() {
                        if (!isBinaryWithoutDecompiler(file)) {
                            LoadTextUtil.clearCharsetAutoDetectionReason(file);
                            file.setBOM(null); // reset BOM in case we had one and the external change stripped it away
                            file.setCharset(null, null, false);
                            boolean wasWritable = document.isWritable();
                            document.setReadOnly(false);
                            boolean tooLarge = RawFileLoader.getInstance().isTooLarge(file.getLength());
                            isReloadable.set(isReloadable(file, document, project));
                            if (isReloadable.get()) {
                                CharSequence reloaded = tooLarge
                                    ? LoadTextUtil.loadText(file, getPreviewCharCount(file))
                                    : LoadTextUtil.loadText(file);
                                ((DocumentEx) document).replaceText(reloaded, file.getModificationStamp());
                                document.putUserData(BIG_FILE_PREVIEW, tooLarge ? Boolean.TRUE : null);
                            }
                            document.setReadOnly(!wasWritable);
                        }
                    }
                });
        }
        if (isReloadable.get()) {
            myMultiCaster.fileContentReloaded(file, document);
        }
        else {
            unbindFileFromDocument(file, document);
            myMultiCaster.fileWithNoDocumentChanged(file);
        }
        myUnsavedDocuments.remove(document);
        myOfflineDocuments.remove(document);
        myBackups.remove(file);
    }

    private static boolean isReloadable(VirtualFile file, Document document, @Nullable Project project) {
        PsiFile cachedPsiFile = project == null ? null : PsiDocumentManager.getInstance(project).getCachedPsiFile(document);
        return !(RawFileLoader.getInstance().isTooLarge(file.getLength()) && file.getFileType().isBinary())
            && (cachedPsiFile == null || cachedPsiFile instanceof PsiFileInternal || isBinaryWithDecompiler(file))
            && document.getUserData(NOT_RELOADABLE_DOCUMENT_KEY) == null;
    }

    @TestOnly
    void setAskReloadFromDisk(Disposable disposable, MemoryDiskConflictResolver newProcessor) {
        MemoryDiskConflictResolver old = myConflictResolver;
        myConflictResolver = newProcessor;
        Disposer.register(disposable, () -> myConflictResolver = old);
    }

    protected void fileDeleted(VFileDeleteEvent event) {
        Document doc = getCachedDocument(event.getFile());
        if (doc != null) {
            myTrailingSpacesStripper.documentDeleted(doc);
        }
    }

    public static boolean recomputeFileTypeIfNecessary(VirtualFile virtualFile) {
        return RecomputeFileTypeMarker.recomputeFileTypeIfNecessary(virtualFile);
    }

    private void fireUnsavedDocumentsDropped() {
        myMultiCaster.unsavedDocumentsDropped();
    }

    private boolean fireBeforeFileContentReload(VirtualFile file, Document document) {
        boolean mayReload = Application.get().getExtensionPoint(FileDocumentSynchronizationVetoer.class)
            .allMatchSafe(vetoer -> vetoer.mayReloadFileContent(file, document));

        if (mayReload) {
            myMultiCaster.beforeFileContentReload(file, document);
        }
        return mayReload;
    }

    private static int getPreviewCharCount(VirtualFile file) {
        Charset charset = EncodingManager.getInstance().getEncoding(file, false);
        float bytesPerChar = charset == null ? 2 : charset.newEncoder().averageBytesPerChar();
        return (int) (RawFileLoader.getInstance().getLargeFilePreviewSize() / bytesPerChar);
    }

    @RequiredUIAccess
    private void handleErrorsOnSave(Map<Document, IOException> failures) {
        if (Application.get().isUnitTestMode()) {
            IOException ioException = ContainerUtil.getFirstItem(failures.values());
            if (ioException != null) {
                throw new RuntimeException(ioException);
            }
            return;
        }
        for (IOException exception : failures.values()) {
            LOG.warn(exception);
        }

        String text = StringUtil.join(failures.values(), Throwable::getMessage, "\n");

        DialogWrapper dialog = new DialogWrapper(null) {
            {
                init();
                setTitle(UILocalize.cannotSaveFilesDialogTitle());
            }

            @Override
            protected void createDefaultActions() {
                super.createDefaultActions();
                myOKAction.setText(
                    myOnClose
                        ? UILocalize.cannotSaveFilesDialogIgnoreChanges()
                        : UILocalize.cannotSaveFilesDialogRevertChanges()
                );
                myOKAction.putValue(DEFAULT_ACTION, null);

                if (!myOnClose) {
                    myCancelAction.putValue(Action.NAME, CommonLocalize.buttonClose().get());
                }
            }

            @Override
            protected JComponent createCenterPanel() {
                JPanel panel = new JPanel(new BorderLayout(0, 5));

                panel.add(new JLabel(UILocalize.cannotSaveFilesDialogMessage().get()), BorderLayout.NORTH);

                JTextPane area = new JTextPane();
                area.setText(text);
                area.setEditable(false);
                area.setMinimumSize(new Dimension(area.getMinimumSize().width, 50));
                panel.add(
                    new JBScrollPane(
                        area,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
                    ),
                    BorderLayout.CENTER
                );

                return panel;
            }
        };

        if (dialog.showAndGet()) {
            for (Document document : failures.keySet()) {
                reloadFromDisk(document);
            }
        }
    }

    private final Map<VirtualFile, Document> myDocumentCache = ContainerUtil.createConcurrentWeakValueMap();

    //temp setter for Rider 2017.1
    public static boolean ourConflictsSolverEnabled = true;

    protected void cacheDocument(VirtualFile file, Document document) {
        myDocumentCache.put(file, document);
    }

    protected void removeDocumentFromCache(VirtualFile file) {
        myDocumentCache.remove(file);
    }

    protected Document getDocumentFromCache(VirtualFile file) {
        return myDocumentCache.get(file);
    }
}
