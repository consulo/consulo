// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.*;
import consulo.application.internal.TransactionGuardEx;
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
import consulo.document.impl.DocumentImpl;
import consulo.document.impl.FrozenDocument;
import consulo.document.internal.DocumentEx;
import consulo.document.internal.FileDocumentManagerEx;
import consulo.document.internal.PrioritizedDocumentListener;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.openapi.editor.impl.EditorFactoryImpl;
import consulo.ide.impl.idea.openapi.editor.impl.TrailingSpacesStripper;
import consulo.ide.impl.idea.openapi.fileEditor.impl.text.TextEditorImpl;
import consulo.ide.impl.idea.openapi.project.ProjectUtil;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.SafeWriteRequestor;
import consulo.ide.impl.idea.pom.core.impl.PomModelImpl;
import consulo.ide.impl.idea.util.ExceptionUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.impl.file.AbstractFileViewProvider;
import consulo.language.impl.internal.psi.LoadTextUtil;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.internal.ExternalChangeAction;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.project.ProjectManager;
import consulo.project.internal.ProjectManagerEx;
import consulo.ui.ex.UIBundle;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.virtualFileSystem.event.VFileContentChangeEvent;
import consulo.virtualFileSystem.event.VFileDeleteEvent;
import consulo.virtualFileSystem.event.VFilePropertyChangeEvent;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.impl.internal.RawFileLoaderImpl;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import java.util.function.Predicate;

@ServiceImpl
@Singleton
public class FileDocumentManagerImpl implements FileDocumentManagerEx, SafeWriteRequestor {
  private static final Logger LOG = Logger.getInstance(FileDocumentManagerImpl.class);

  public static final Key<Document> HARD_REF_TO_DOCUMENT_KEY = Key.create("HARD_REF_TO_DOCUMENT_KEY");

  private static final Key<String> LINE_SEPARATOR_KEY = Key.create("LINE_SEPARATOR_KEY");
  private static final Key<VirtualFile> FILE_KEY = Key.create("FILE_KEY");
  protected static final Key<Boolean> MUST_RECOMPUTE_FILE_TYPE = Key.create("Must recompute file type");
  private static final Key<Boolean> BIG_FILE_PREVIEW = Key.create("BIG_FILE_PREVIEW");

  private final Set<Document> myUnsavedDocuments = ContainerUtil.newConcurrentSet();

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
    public void documentChanged(@Nonnull DocumentEvent e) {
      final Document document = e.getDocument();
      if (!ApplicationManager.getApplication().hasWriteAction(ExternalChangeAction.ExternalDocumentChange.class)) {
        myUnsavedDocuments.add(document);
      }
      final Runnable currentCommand = CommandProcessor.getInstance().getCurrentCommand();
      Project project = currentCommand == null ? null : CommandProcessor.getInstance().getCurrentCommandProject();
      if (project == null) project = ProjectUtil.guessProjectForFile(getFile(document));
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
    ((ProjectManagerEx)projectManager).registerCloseProjectVeto(new MyProjectCloseHandler());

    myBus = application.getMessageBus();
    InvocationHandler handler = (proxy, method, args) -> {
      multiCast(method, args);
      return null;
    };

    ClassLoader loader = FileDocumentManagerListener.class.getClassLoader();
    myMultiCaster = (FileDocumentManagerListener)Proxy.newProxyInstance(loader, new Class[]{FileDocumentManagerListener.class}, handler);
  }

  static final class MyProjectCloseHandler implements Predicate<Project> {
    @Override
    public boolean test(@Nonnull Project project) {
      FileDocumentManagerImpl manager = (FileDocumentManagerImpl)FileDocumentManager.getInstance();
      if (!manager.myUnsavedDocuments.isEmpty()) {
        manager.myOnClose = true;
        try {
          manager.saveAllDocuments();
        }
        finally {
          manager.myOnClose = false;
        }
      }
      return manager.myUnsavedDocuments.isEmpty();
    }
  }

  private static void unwrapAndRethrow(@Nonnull Exception e) {
    Throwable unwrapped = e;
    if (e instanceof InvocationTargetException) {
      unwrapped = e.getCause() == null ? e : e.getCause();
    }
    ExceptionUtil.rethrowUnchecked(unwrapped);
    LOG.error(unwrapped);
  }

  @SuppressWarnings("OverlyBroadCatchBlock")
  private void multiCast(@Nonnull Method method, Object[] args) {
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
  @Nullable
  public Document getDocument(@Nonnull final VirtualFile file) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    DocumentEx document = (DocumentEx)getCachedDocument(file);
    if (document == null) {
      if (!file.isValid() || file.isDirectory() || isBinaryWithoutDecompiler(file)) return null;

      boolean tooLarge = RawFileLoader.getInstance().isLargeForContentLoading(file.getLength());
      if (file.getFileType().isBinary() && tooLarge) return null;

      final CharSequence text = tooLarge ? LoadTextUtil.loadText(file, getPreviewCharCount(file)) : LoadTextUtil.loadText(file);
      synchronized (lock) {
        document = (DocumentEx)getCachedDocument(file);
        if (document != null) return document; // Double checking

        document = (DocumentEx)createDocument(text, file);
        document.setModificationStamp(file.getModificationStamp());
        document.putUserData(BIG_FILE_PREVIEW, tooLarge ? Boolean.TRUE : null);
        final FileType fileType = file.getFileType();
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

  public static boolean areTooManyDocumentsInTheQueue(@Nonnull Collection<? extends Document> documents) {
    if (documents.size() > 100) return true;
    int totalSize = 0;
    for (Document document : documents) {
      totalSize += document.getTextLength();
      if (RawFileLoader.getInstance().isLargeForContentLoading(totalSize)) return true;
    }
    return false;
  }

  @Nonnull
  private static Document createDocument(@Nonnull CharSequence text, @Nonnull VirtualFile file) {
    boolean acceptSlashR = file instanceof LightVirtualFile && StringUtil.indexOf(text, '\r') >= 0;
    boolean freeThreaded = Boolean.TRUE.equals(file.getUserData(AbstractFileViewProvider.FREE_THREADED));
    DocumentImpl document = (DocumentImpl)((EditorFactoryImpl)EditorFactory.getInstance()).createDocument(text, acceptSlashR, freeThreaded);
    document.documentCreatedFrom(file);
    return document;
  }

  @Override
  @Nullable
  public Document getCachedDocument(@Nonnull VirtualFile file) {
    Document hard = file.getUserData(HARD_REF_TO_DOCUMENT_KEY);
    return hard != null ? hard : getDocumentFromCache(file);
  }

  @Override
  public void registerDocument(@Nonnull Document document, @Nonnull VirtualFile virtualFile) {
    registerDocumentImpl(document, virtualFile);
  }

  public static void registerDocumentImpl(@Nonnull final Document document, @Nonnull VirtualFile virtualFile) {
    synchronized (lock) {
      document.putUserData(FILE_KEY, virtualFile);
      virtualFile.putUserData(HARD_REF_TO_DOCUMENT_KEY, document);
    }
  }

  @Override
  @Nullable
  public VirtualFile getFile(@Nonnull Document document) {
    if (document instanceof FrozenDocument) return null;
    return document.getUserData(FILE_KEY);
  }

  @TestOnly
  public void dropAllUnsavedDocuments() {
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      throw new RuntimeException("This method is only for test mode!");
    }
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    if (!myUnsavedDocuments.isEmpty()) {
      myUnsavedDocuments.clear();
      fireUnsavedDocumentsDropped();
    }
  }

  private void saveAllDocumentsLater() {
    // later because some document might have been blocked by PSI right now
    TransactionGuard.getInstance().submitTransactionLater(ApplicationManager.getApplication(), () -> {
      final Document[] unsavedDocuments = getUnsavedDocuments();
      for (Document document : unsavedDocuments) {
        VirtualFile file = getFile(document);
        if (file == null) continue;
        Project project = ProjectUtil.guessProjectForFile(file);
        if (project == null) continue;
        if (PsiDocumentManager.getInstance(project).isDocumentBlockedByPsi(document)) continue;

        saveDocument(document);
      }
    });
  }

  @Override
  public void saveAllDocuments() {
    saveAllDocuments(true);
  }

  /**
   * @param isExplicit caused by user directly (Save action) or indirectly (e.g. Compile)
   */
  public void saveAllDocuments(boolean isExplicit) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    ((TransactionGuardEx)TransactionGuard.getInstance()).assertWriteActionAllowed();

    myMultiCaster.beforeAllDocumentsSaving();
    if (myUnsavedDocuments.isEmpty()) return;

    final Map<Document, IOException> failedToSave = new HashMap<>();
    final Set<Document> vetoed = new HashSet<>();
    while (true) {
      int count = 0;

      for (Document document : myUnsavedDocuments) {
        if (failedToSave.containsKey(document)) continue;
        if (vetoed.contains(document)) continue;
        try {
          doSaveDocument(document, isExplicit);
        }
        catch (IOException e) {
          failedToSave.put(document, e);
        }
        catch (SaveVetoException e) {
          vetoed.add(document);
        }
        count++;
      }

      if (count == 0) break;
    }

    if (!failedToSave.isEmpty()) {
      handleErrorsOnSave(failedToSave);
    }
  }

  @Override
  public void saveDocument(@Nonnull final Document document) {
    saveDocument(document, true);
  }

  public void saveDocument(@Nonnull final Document document, final boolean explicit) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    ((TransactionGuardEx)TransactionGuard.getInstance()).assertWriteActionAllowed();

    if (!myUnsavedDocuments.contains(document)) return;

    try {
      doSaveDocument(document, explicit);
    }
    catch (IOException e) {
      handleErrorsOnSave(Collections.singletonMap(document, e));
    }
    catch (SaveVetoException ignored) {
    }
  }

  @Override
  public void saveDocumentAsIs(@Nonnull Document document) {
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

  private void doSaveDocument(@Nonnull final Document document, boolean isExplicit) throws IOException, SaveVetoException {
    VirtualFile file = getFile(document);
    if (LOG.isTraceEnabled()) LOG.trace("saving: " + file);

    if (file == null || file instanceof LightVirtualFile || file.isValid() && !isFileModified(file)) {
      removeFromUnsaved(document);
      return;
    }

    if (file.isValid() && needsRefresh(file)) {
      LOG.trace("  refreshing...");
      file.refresh(false, false);
      if (!myUnsavedDocuments.contains(document)) return;
    }

    if (!maySaveDocument(file, document, isExplicit)) {
      throw new SaveVetoException();
    }

    LOG.trace("  writing...");
    WriteAction.run(() -> doSaveDocumentInWriteAction(document, file));
    LOG.trace("  done");
  }

  private boolean maySaveDocument(@Nonnull VirtualFile file, @Nonnull Document document, boolean isExplicit) {
    return !myConflictResolver.hasConflict(file) && FileDocumentSynchronizationVetoer.EP_NAME.getExtensionList().stream().allMatch(vetoer -> vetoer.maySaveDocument(document, isExplicit));
  }

  private void doSaveDocumentInWriteAction(@Nonnull final Document document, @Nonnull final VirtualFile file) throws IOException {
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
    catch (IOException e) {
      // in case of corrupted VFS try to stay consistent
      ioException = e;
    }
    if (!saveNeeded) {
      if (document instanceof DocumentEx) {
        ((DocumentEx)document).setModificationStamp(file.getModificationStamp());
      }
      removeFromUnsaved(document);
      updateModifiedProperty(file);
      if (ioException != null) throw ioException;
      return;
    }

    PomModelImpl.guardPsiModificationsIn(() -> {
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
      LOG.assertTrue(!myUnsavedDocuments.contains(document));
      myTrailingSpacesStripper.clearLineModificationFlags(document);
    });
  }

  private static void updateModifiedProperty(@Nonnull VirtualFile file) {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
      for (FileEditor editor : fileEditorManager.getAllEditors(file)) {
        if (editor instanceof TextEditorImpl) {
          ((TextEditorImpl)editor).updateModifiedProperty();
        }
      }
    }
  }

  private void removeFromUnsaved(@Nonnull Document document) {
    myUnsavedDocuments.remove(document);
    fireUnsavedDocumentsDropped();
    LOG.assertTrue(!myUnsavedDocuments.contains(document));
  }

  private static boolean isSaveNeeded(@Nonnull Document document, @Nonnull VirtualFile file) throws IOException {
    if (file.getFileType().isBinary() || document.getTextLength() > 1000 * 1000) {    // don't compare if the file is too big
      return true;
    }

    byte[] bytes = file.contentsToByteArray();
    CharSequence loaded = LoadTextUtil.getTextByBinaryPresentation(bytes, file, false, false);

    return !Comparing.equal(document.getCharsSequence(), loaded);
  }

  private static boolean needsRefresh(@Nonnull VirtualFile file) {
    final VirtualFileSystem fs = file.getFileSystem();
    return fs instanceof NewVirtualFileSystem && file.getTimeStamp() != ((NewVirtualFileSystem)fs).getTimeStamp(file);
  }

  @Nonnull
  public static String getLineSeparator(@Nonnull Document document, @Nonnull VirtualFile file) {
    String lineSeparator = LoadTextUtil.getDetectedLineSeparator(file);
    if (lineSeparator == null) {
      lineSeparator = document.getUserData(LINE_SEPARATOR_KEY);
      assert lineSeparator != null : document;
    }
    return lineSeparator;
  }

  @Override
  @Nonnull
  public String getLineSeparator(@Nullable VirtualFile file, @Nullable ComponentManager project) {
    String lineSeparator = file == null ? null : LoadTextUtil.getDetectedLineSeparator(file);
    if (lineSeparator == null) {
      lineSeparator = CodeStyle.getProjectOrDefaultSettings((Project)project).getLineSeparator();
    }
    return lineSeparator;
  }

  @Override
  public boolean requestWriting(@Nonnull Document document, ComponentManager project) {
    return requestWritingStatus(document, project).hasWriteAccess();
  }

  @Nonnull
  @Override
  public WriteAccessStatus requestWritingStatus(@Nonnull Document document, @Nullable ComponentManager project) {
    final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (project != null && file != null && file.isValid()) {
      if (file.getFileType().isBinary()) return WriteAccessStatus.NON_WRITABLE;
      ReadonlyStatusHandler.OperationStatus writableStatus = ReadonlyStatusHandler.getInstance((Project)project).ensureFilesWritable(Collections.singletonList(file));
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
  public void reloadFiles(@Nonnull final VirtualFile... files) {
    for (VirtualFile file : files) {
      if (file.exists()) {
        final Document doc = getCachedDocument(file);
        if (doc != null) {
          reloadFromDisk(doc);
        }
      }
    }
  }

  @Override
  @Nonnull
  public Document[] getUnsavedDocuments() {
    if (myUnsavedDocuments.isEmpty()) {
      return Document.EMPTY_ARRAY;
    }

    List<Document> list = new ArrayList<>(myUnsavedDocuments);
    return list.toArray(Document.EMPTY_ARRAY);
  }

  @Override
  public boolean isDocumentUnsaved(@Nonnull Document document) {
    return myUnsavedDocuments.contains(document);
  }

  @Override
  public boolean isFileModified(@Nonnull VirtualFile file) {
    final Document doc = getCachedDocument(file);
    return doc != null && isDocumentUnsaved(doc) && doc.getModificationStamp() != file.getModificationStamp();
  }

  @Override
  public boolean isPartialPreviewOfALargeFile(@Nonnull Document document) {
    return document.getUserData(BIG_FILE_PREVIEW) == Boolean.TRUE;
  }

  protected void propertyChanged(@Nonnull VFilePropertyChangeEvent event) {
    final VirtualFile file = event.getFile();
    if (VirtualFile.PROP_WRITABLE.equals(event.getPropertyName())) {
      final Document document = getCachedDocument(file);
      if (document != null) {
        ApplicationManager.getApplication().runWriteAction((ExternalChangeAction)() -> document.setReadOnly(!file.isWritable()));
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

  private void unbindFileFromDocument(@Nonnull VirtualFile file, @Nonnull Document document) {
    removeDocumentFromCache(file);
    file.putUserData(HARD_REF_TO_DOCUMENT_KEY, null);
    document.putUserData(FILE_KEY, null);
  }

  private static boolean isBinaryWithDecompiler(@Nonnull VirtualFile file) {
    final FileType ft = file.getFileType();
    return ft.isBinary() && BinaryFileDecompiler.forFileType(ft) != null;
  }

  private static boolean isBinaryWithoutDecompiler(@Nonnull VirtualFile file) {
    final FileType fileType = file.getFileType();
    return fileType.isBinary() && BinaryFileDecompiler.forFileType(fileType) == null;
  }

  public void contentsChanged(VFileContentChangeEvent event) {
    VirtualFile virtualFile = event.getFile();
    Document document = getCachedDocument(virtualFile);

    if (event.isFromSave()) return;

    if (document == null || isBinaryWithDecompiler(virtualFile)) {
      myMultiCaster.fileWithNoDocumentChanged(virtualFile); // This will generate PSI event at FileManagerImpl
    }

    if (document != null && (document.getModificationStamp() == event.getOldModificationStamp() || !isDocumentUnsaved(document))) {
      reloadFromDisk(document);
    }
  }

  @Override
  public void reloadFromDisk(@Nonnull final Document document) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    final VirtualFile file = getFile(document);
    assert file != null;

    if (!fireBeforeFileContentReload(file, document)) {
      return;
    }

    final Project project = ProjectLocator.getInstance().guessProjectForFile(file);
    boolean[] isReloadable = {isReloadable(file, document, project)};
    if (isReloadable[0]) {
      CommandProcessor.getInstance().executeCommand(project, () -> ApplicationManager.getApplication().runWriteAction(new ExternalChangeAction.ExternalDocumentChange(document, project) {
        @Override
        public void run() {
          if (!isBinaryWithoutDecompiler(file)) {
            LoadTextUtil.clearCharsetAutoDetectionReason(file);
            file.setBOM(null); // reset BOM in case we had one and the external change stripped it away
            file.setCharset(null, null, false);
            boolean wasWritable = document.isWritable();
            document.setReadOnly(false);
            boolean tooLarge = RawFileLoader.getInstance().isTooLarge(file.getLength());
            isReloadable[0] = isReloadable(file, document, project);
            if (isReloadable[0]) {
              CharSequence reloaded = tooLarge ? LoadTextUtil.loadText(file, getPreviewCharCount(file)) : LoadTextUtil.loadText(file);
              ((DocumentEx)document).replaceText(reloaded, file.getModificationStamp());
              document.putUserData(BIG_FILE_PREVIEW, tooLarge ? Boolean.TRUE : null);
            }
            document.setReadOnly(!wasWritable);
          }
        }
      }), UIBundle.message("file.cache.conflict.action"), null, UndoConfirmationPolicy.REQUEST_CONFIRMATION);
    }
    if (isReloadable[0]) {
      myMultiCaster.fileContentReloaded(file, document);
    }
    else {
      unbindFileFromDocument(file, document);
      myMultiCaster.fileWithNoDocumentChanged(file);
    }
    myUnsavedDocuments.remove(document);
  }

  private static boolean isReloadable(@Nonnull VirtualFile file, @Nonnull Document document, @Nullable Project project) {
    PsiFile cachedPsiFile = project == null ? null : PsiDocumentManager.getInstance(project).getCachedPsiFile(document);
    return !(RawFileLoader.getInstance().isTooLarge(file.getLength()) && file.getFileType().isBinary()) &&
           (cachedPsiFile == null || cachedPsiFile instanceof PsiFileImpl || isBinaryWithDecompiler(file));
  }

  @TestOnly
  void setAskReloadFromDisk(@Nonnull Disposable disposable, @Nonnull MemoryDiskConflictResolver newProcessor) {
    final MemoryDiskConflictResolver old = myConflictResolver;
    myConflictResolver = newProcessor;
    Disposer.register(disposable, () -> myConflictResolver = old);
  }

  protected void fileDeleted(@Nonnull VFileDeleteEvent event) {
    Document doc = getCachedDocument(event.getFile());
    if (doc != null) {
      myTrailingSpacesStripper.documentDeleted(doc);
    }
  }

  public static boolean recomputeFileTypeIfNecessary(@Nonnull VirtualFile virtualFile) {
    if (virtualFile.getUserData(MUST_RECOMPUTE_FILE_TYPE) != null) {
      virtualFile.getFileType();
      virtualFile.putUserData(MUST_RECOMPUTE_FILE_TYPE, null);
      return true;
    }
    return false;
  }

  private void fireUnsavedDocumentsDropped() {
    myMultiCaster.unsavedDocumentsDropped();
  }

  private boolean fireBeforeFileContentReload(@Nonnull VirtualFile file, @Nonnull Document document) {
    for (FileDocumentSynchronizationVetoer vetoer : FileDocumentSynchronizationVetoer.EP_NAME.getExtensionList()) {
      try {
        if (!vetoer.mayReloadFileContent(file, document)) {
          return false;
        }
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }

    myMultiCaster.beforeFileContentReload(file, document);
    return true;
  }

  private static int getPreviewCharCount(@Nonnull VirtualFile file) {
    Charset charset = EncodingManager.getInstance().getEncoding(file, false);
    float bytesPerChar = charset == null ? 2 : charset.newEncoder().averageBytesPerChar();
    return (int)(RawFileLoaderImpl.LARGE_FILE_PREVIEW_SIZE / bytesPerChar);
  }

  private void handleErrorsOnSave(@Nonnull Map<Document, IOException> failures) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      IOException ioException = ContainerUtil.getFirstItem(failures.values());
      if (ioException != null) {
        throw new RuntimeException(ioException);
      }
      return;
    }
    for (IOException exception : failures.values()) {
      LOG.warn(exception);
    }

    final String text = StringUtil.join(failures.values(), Throwable::getMessage, "\n");

    final DialogWrapper dialog = new DialogWrapper(null) {
      {
        init();
        setTitle(UIBundle.message("cannot.save.files.dialog.title"));
      }

      @Override
      protected void createDefaultActions() {
        super.createDefaultActions();
        myOKAction.putValue(Action.NAME, UIBundle.message(myOnClose ? "cannot.save.files.dialog.ignore.changes" : "cannot.save.files.dialog.revert.changes"));
        myOKAction.putValue(DEFAULT_ACTION, null);

        if (!myOnClose) {
          myCancelAction.putValue(Action.NAME, CommonBundle.getCloseButtonText());
        }
      }

      @Override
      protected JComponent createCenterPanel() {
        final JPanel panel = new JPanel(new BorderLayout(0, 5));

        panel.add(new JLabel(UIBundle.message("cannot.save.files.dialog.message")), BorderLayout.NORTH);

        final JTextPane area = new JTextPane();
        area.setText(text);
        area.setEditable(false);
        area.setMinimumSize(new Dimension(area.getMinimumSize().width, 50));
        panel.add(new JBScrollPane(area, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

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

  protected void cacheDocument(@Nonnull VirtualFile file, @Nonnull Document document) {
    myDocumentCache.put(file, document);
  }

  protected void removeDocumentFromCache(@Nonnull VirtualFile file) {
    myDocumentCache.remove(file);
  }

  protected Document getDocumentFromCache(@Nonnull VirtualFile file) {
    return myDocumentCache.get(file);
  }
}
