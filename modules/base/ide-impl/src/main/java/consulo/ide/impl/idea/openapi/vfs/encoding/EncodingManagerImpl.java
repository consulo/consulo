// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.encoding;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.impl.internal.concurent.BoundedTaskExecutor;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.component.messagebus.MessageBus;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.concurrency.JobSchedulerImpl;
import consulo.ide.impl.idea.ide.AppLifecycleListener;
import consulo.logging.Logger;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.language.impl.internal.psi.LoadTextUtil;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.project.ProjectManager;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderEx;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.ApplicationEncodingManager;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.virtualFileSystem.encoding.EncodingManagerListener;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NonNls;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@ServiceImpl
@State(name = "Encoding", storages = @Storage("encoding.xml"))
public class EncodingManagerImpl implements PersistentStateComponent<EncodingManagerImpl.State>, ApplicationEncodingManager, Disposable {
  private static final Logger LOG = Logger.getInstance(EncodingManagerImpl.class);

  static final class State {
    @Nonnull
    private EncodingReference myDefaultEncoding = new EncodingReference(StandardCharsets.UTF_8);
    @Nonnull
    private EncodingReference myDefaultConsoleEncoding = EncodingReference.DEFAULT;

    @Attribute("default_encoding")
    @Nonnull
    public String getDefaultCharsetName() {
      return myDefaultEncoding.getCharset() == null ? "" : myDefaultEncoding.getCharset().name();
    }

    public void setDefaultCharsetName(@Nonnull String name) {
      myDefaultEncoding = new EncodingReference(StringUtil.nullize(name));
    }

    @Attribute("default_console_encoding")
    @Nonnull
    public String getDefaultConsoleEncodingName() {
      return myDefaultConsoleEncoding.getCharset() == null ? "" : myDefaultConsoleEncoding.getCharset().name();
    }

    public void setDefaultConsoleEncodingName(@Nonnull String name) {
      myDefaultConsoleEncoding = new EncodingReference(StringUtil.nullize(name));
    }
  }

  private State myState = new State();

  private static final Key<Charset> CACHED_CHARSET_FROM_CONTENT = Key.create("CACHED_CHARSET_FROM_CONTENT");

  private final ExecutorService changedDocumentExecutor =
    AppExecutorUtil.createBoundedApplicationPoolExecutor("EncodingManagerImpl Document Pool",
                                                         AppExecutorUtil.getAppExecutorService(),
                                                         JobSchedulerImpl.getJobPoolParallelism(),
                                                         this);

  private final AtomicBoolean myDisposed = new AtomicBoolean();

  public EncodingManagerImpl() {
    ApplicationManager.getApplication().getMessageBus().connect().subscribe(AppLifecycleListener.class, new AppLifecycleListener() {
      @Override
      public void appClosing() {
        // should call before dispose in write action
        // prevent any further re-detection and wait for the queue to clear
        myDisposed.set(true);
        clearDocumentQueue();
      }
    });

    EditorFactory editorFactory = EditorFactory.getInstance();
    editorFactory.getEventMulticaster().addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@Nonnull DocumentEvent e) {
        Document document = e.getDocument();
        if (isEditorOpenedFor(document)) {
          queueUpdateEncodingFromContent(document);
        }
      }
    }, this);
    editorFactory.addEditorFactoryListener(new EditorFactoryListener() {
      @Override
      public void editorCreated(@Nonnull EditorFactoryEvent event) {
        queueUpdateEncodingFromContent(event.getEditor().getDocument());
      }
    }, this);
  }

  private static boolean isEditorOpenedFor(@Nonnull Document document) {
    VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
    if (virtualFile == null) return false;
    Project project = guessProject(virtualFile);
    return project != null && !project.isDisposed() && FileEditorManager.getInstance(project).isFileOpen(virtualFile);
  }

  @NonNls
  public static final String PROP_CACHED_ENCODING_CHANGED = "cachedEncoding";

  private void handleDocument(@Nonnull final Document document) {
    VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
    if (virtualFile == null) return;
    Project project = guessProject(virtualFile);
    while (true) {
      if (project != null && project.isDisposed()) break;
      int nRequests = addNumberOfRequestedRedetects(document, 0);
      Charset charset = LoadTextUtil.charsetFromContentOrNull(project, virtualFile, document.getImmutableCharSequence());
      Charset oldCached = getCachedCharsetFromContent(document);
      if (!Comparing.equal(charset, oldCached)) {
        setCachedCharsetFromContent(charset, oldCached, document);
      }
      if (addNumberOfRequestedRedetects(document, -nRequests) == 0) break;
    }
  }

  private static void setCachedCharsetFromContent(Charset charset, Charset oldCached, @Nonnull Document document) {
    document.putUserData(CACHED_CHARSET_FROM_CONTENT, charset);
    firePropertyChange(document, PROP_CACHED_ENCODING_CHANGED, oldCached, charset, null);
  }

  /**
   * @param virtualFile
   * @return returns null if charset set cannot be determined from content
   */
  @Nullable
  static Charset computeCharsetFromContent(@Nonnull final VirtualFile virtualFile) {
    final Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
    if (document == null) {
      return null;
    }
    Charset cached = ((EncodingManagerImpl)EncodingManager.getInstance()).getCachedCharsetFromContent(document);
    if (cached != null) {
      return cached;
    }

    final Project project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
    return ReadAction.compute(() -> {
      Charset charsetFromContent = LoadTextUtil.charsetFromContentOrNull(project, virtualFile, document.getImmutableCharSequence());
      if (charsetFromContent != null) {
        setCachedCharsetFromContent(charsetFromContent, null, document);
      }
      return charsetFromContent;
    });
  }

  @Override
  public void dispose() {
    myDisposed.set(true);
  }

  // stores number of re-detection requests for this document
  private static final Key<AtomicInteger> RUNNING_REDETECTS_KEY = Key.create("DETECTING_ENCODING_KEY");

  private static int addNumberOfRequestedRedetects(@Nonnull Document document, int delta) {
    AtomicInteger oldData = document.getUserData(RUNNING_REDETECTS_KEY);
    if (oldData == null) {
      oldData = ((UserDataHolderEx)document).putUserDataIfAbsent(RUNNING_REDETECTS_KEY, new AtomicInteger());
    }
    return oldData.addAndGet(delta);
  }

  void queueUpdateEncodingFromContent(@Nonnull Document document) {
    if (myDisposed.get()) return; // ignore re-detect requests on app close
    if (addNumberOfRequestedRedetects(document, 1) == 1) {
      changedDocumentExecutor.execute(new DocumentEncodingDetectRequest(document, myDisposed));
    }
  }

  private static final class DocumentEncodingDetectRequest implements Runnable {
    private final Reference<Document> ref;
    @Nonnull
    private final AtomicBoolean myDisposed;

    private DocumentEncodingDetectRequest(@Nonnull Document document, @Nonnull AtomicBoolean disposed) {
      ref = new WeakReference<>(document);
      myDisposed = disposed;
    }

    @Override
    public void run() {
      if (myDisposed.get()) return;
      Document document = ref.get();
      if (document == null) return; // document gced, don't bother
      ((EncodingManagerImpl)ApplicationEncodingManager.getInstance()).handleDocument(document);
    }
  }

  @Nullable
  public Charset getCachedCharsetFromContent(@Nonnull Document document) {
    return document.getUserData(CACHED_CHARSET_FROM_CONTENT);
  }

  @Override
  @Nonnull
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(@Nonnull State state) {
    myState = state;
  }

  @Override
  @Nonnull
  public Collection<Charset> getFavorites() {
    Collection<Charset> result = new HashSet<>();
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : projects) {
      result.addAll(EncodingProjectManager.getInstance(project).getFavorites());
    }
    result.addAll(EncodingProjectManagerImpl.widelyKnownCharsets());
    return result;
  }

  @Override
  @Nullable
  public Charset getEncoding(@Nullable VirtualFile virtualFile, boolean useParentDefaults) {
    Project project = guessProject(virtualFile);
    if (project == null) return null;
    EncodingProjectManager encodingManager = EncodingProjectManager.getInstance(project);
    if (encodingManager == null) return null; //tests
    return encodingManager.getEncoding(virtualFile, useParentDefaults);
  }

  public void clearDocumentQueue() {
    if (((BoundedTaskExecutor)changedDocumentExecutor).isEmpty()) return;
    if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
      throw new IllegalStateException(
        "Must not call clearDocumentQueue() from under write action because some queued detectors require read action");
    }
    ((BoundedTaskExecutor)changedDocumentExecutor).clearAndCancelAll();
    // after clear and canceling all queued tasks, make sure they all are finished
    waitAllTasksExecuted(1, TimeUnit.MINUTES);
  }

  void waitAllTasksExecuted(long timeout, @Nonnull TimeUnit unit) {
    try {
      ((BoundedTaskExecutor)changedDocumentExecutor).waitAllTasksExecuted(timeout, unit);
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }

  @Nullable
  private static Project guessProject(@Nullable VirtualFile virtualFile) {
    return ProjectLocator.getInstance().guessProjectForFile(virtualFile);
  }

  @Override
  public void setEncoding(@Nullable VirtualFile virtualFileOrDir, @Nullable Charset charset) {
    Project project = guessProject(virtualFileOrDir);
    if (project != null) {
      EncodingProjectManager.getInstance(project).setEncoding(virtualFileOrDir, charset);
    }
  }

  @Override
  public boolean isNative2Ascii(@Nonnull final VirtualFile virtualFile) {
    Project project = guessProject(virtualFile);
    return project != null && EncodingProjectManager.getInstance(project).isNative2Ascii(virtualFile);
  }

  @Override
  public boolean isNative2AsciiForPropertiesFiles() {
    Project project = guessProject(null);
    return project != null && EncodingProjectManager.getInstance(project).isNative2AsciiForPropertiesFiles();
  }

  @Override
  public void setNative2AsciiForPropertiesFiles(final VirtualFile virtualFile, final boolean native2Ascii) {
    Project project = guessProject(virtualFile);
    if (project == null) return;
    EncodingProjectManager.getInstance(project).setNative2AsciiForPropertiesFiles(virtualFile, native2Ascii);
  }

  @Override
  @Nonnull
  public Charset getDefaultCharset() {
    return myState.myDefaultEncoding.dereference();
  }

  @Override
  @Nonnull
  public String getDefaultCharsetName() {
    return myState.getDefaultCharsetName();
  }

  @Override
  public void setDefaultCharsetName(@Nonnull String name) {
    myState.setDefaultCharsetName(name);
  }

  @Override
  @Nullable
  public Charset getDefaultCharsetForPropertiesFiles(@Nullable final VirtualFile virtualFile) {
    Project project = guessProject(virtualFile);
    if (project == null) return null;
    return EncodingProjectManager.getInstance(project).getDefaultCharsetForPropertiesFiles(virtualFile);
  }

  @Override
  public void setDefaultCharsetForPropertiesFiles(@Nullable final VirtualFile virtualFile, final Charset charset) {
    Project project = guessProject(virtualFile);
    if (project == null) return;
    EncodingProjectManager.getInstance(project).setDefaultCharsetForPropertiesFiles(virtualFile, charset);
  }

  @Override
  public
  @Nonnull
  Charset getDefaultConsoleEncoding() {
    return myState.myDefaultConsoleEncoding.dereference();
  }

  /**
   * @return default console encoding reference
   */
  @Nonnull
  public EncodingReference getDefaultConsoleEncodingReference() {
    return myState.myDefaultConsoleEncoding;
  }

  /**
   * @param encodingReference default console encoding reference
   */
  public void setDefaultConsoleEncodingReference(@Nonnull EncodingReference encodingReference) {
    myState.myDefaultConsoleEncoding = encodingReference;
  }

  static void firePropertyChange(@Nullable Document document,
                                 @Nonnull String propertyName,
                                 final Object oldValue,
                                 final Object newValue,
                                 @Nullable Project project) {
    MessageBus messageBus = (project != null ? project : ApplicationManager.getApplication()).getMessageBus();
    EncodingManagerListener publisher = messageBus.syncPublisher(EncodingManagerListener.class);
    publisher.propertyChanged(document, propertyName, oldValue, newValue);
  }
}
