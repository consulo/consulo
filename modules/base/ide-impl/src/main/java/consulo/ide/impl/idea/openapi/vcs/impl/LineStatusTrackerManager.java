/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

/*
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 31.07.2006
 * Time: 13:24:17
 */
package consulo.ide.impl.idea.openapi.vcs.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.EditorFactoryAdapter;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentBulkUpdateListener;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.vcs.VcsApplicationSettings;
import consulo.ide.impl.idea.openapi.vcs.ex.LineStatusTracker;
import consulo.vcs.history.VcsRevisionNumber;
import consulo.ide.impl.idea.util.Consumer;
import consulo.ide.impl.idea.util.concurrency.QueueProcessorRemovePartner;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.file.light.LightVirtualFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.VirtualFileAdapter;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.event.VirtualFilePropertyEvent;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusListener;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@ServiceImpl
public class LineStatusTrackerManager implements LineStatusTrackerManagerI {
  private static final Logger LOG = Logger.getInstance(LineStatusTrackerManager.class);

  @NonNls protected static final String IGNORE_CHANGEMARKERS_KEY = "idea.ignore.changemarkers";

  @Nonnull
  public final Object myLock = new Object();

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final VcsBaseContentProvider myStatusProvider;
  @Nonnull
  private final Application myApplication;
  @Nonnull
  private final Provider<FileEditorManager> myFileEditorManager;
  @Nonnull
  private final Disposable myDisposable;

  @Nonnull
  private final Map<Document, TrackerData> myLineStatusTrackers;

  @Nonnull
  private final QueueProcessorRemovePartner<Document, BaseRevisionLoader> myPartner;
  private long myLoadCounter;

  public static LineStatusTrackerManagerI getInstance(final Project project) {
    return project.getComponent(LineStatusTrackerManagerI.class);
  }

  @Inject
  public LineStatusTrackerManager(@Nonnull final Project project,
                                  @Nonnull final Application application,
                                  @Nonnull final Provider<FileEditorManager> fileEditorManager) {
    myLoadCounter = 0;
    myProject = project;
    myStatusProvider = project.getInstance(VcsFileStatusProvider.class);

    myApplication = application;
    myFileEditorManager = fileEditorManager;

    myLineStatusTrackers = new HashMap<>();
    myPartner = new QueueProcessorRemovePartner<>(myProject, (Consumer<BaseRevisionLoader>)baseRevisionLoader -> baseRevisionLoader.run());

    myDisposable = new Disposable() {
      @Override
      public void dispose() {
        synchronized (myLock) {
          for (final TrackerData data : myLineStatusTrackers.values()) {
            data.tracker.release();
          }

          myLineStatusTrackers.clear();
          myPartner.clear();
        }
      }
    };
    Disposer.register(myProject, myDisposable);

    if (myProject.isDefault()) {
      return;
    }

    MessageBusConnection busConnection = project.getMessageBus().connect();
    busConnection.subscribe(DocumentBulkUpdateListener.class, new DocumentBulkUpdateListener() {
      @Override
      public void updateStarted(@Nonnull final Document doc) {
        final LineStatusTracker tracker = getLineStatusTracker(doc);
        if (tracker != null) tracker.startBulkUpdate();
      }

      @Override
      public void updateFinished(@Nonnull final Document doc) {
        final LineStatusTracker tracker = getLineStatusTracker(doc);
        if (tracker != null) tracker.finishBulkUpdate();
      }
    });
    busConnection.subscribe(LineStatusTrackerSettingListener.class, new LineStatusTrackerSettingListener() {
      @Override
      public void settingsUpdated() {
        synchronized (myLock) {
          LineStatusTracker.Mode mode = getMode();
          for (TrackerData data : myLineStatusTrackers.values()) {
            data.tracker.setMode(mode);
          }
        }
      }
    });

    final MyFileStatusListener fileStatusListener = new MyFileStatusListener();
    final EditorFactoryListener editorFactoryListener = new MyEditorFactoryListener();
    final MyVirtualFileListener virtualFileListener = new MyVirtualFileListener();

    final FileStatusManager fsManager = FileStatusManager.getInstance(myProject);
    fsManager.addFileStatusListener(fileStatusListener, myDisposable);

    final EditorFactory editorFactory = EditorFactory.getInstance();
    editorFactory.addEditorFactoryListener(editorFactoryListener, myDisposable);

    final VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
    virtualFileManager.addVirtualFileListener(virtualFileListener, myDisposable);
  }

  public boolean isDisabled() {
    return !myProject.isOpen() || myProject.isDisposed();
  }

  @Override
  @Nullable
  public LineStatusTracker getLineStatusTracker(final Document document) {
    synchronized (myLock) {
      if (isDisabled()) return null;
      TrackerData data = myLineStatusTrackers.get(document);
      return data != null ? data.tracker : null;
    }
  }

  private void resetTrackers() {
    synchronized (myLock) {
      if (isDisabled()) return;
      log("resetTrackers", null);

      List<LineStatusTracker> trackers = ContainerUtil.map(myLineStatusTrackers.values(), (data) -> data.tracker);
      for (LineStatusTracker tracker : trackers) {
        resetTracker(tracker.getDocument(), tracker.getVirtualFile(), tracker);
      }

      final VirtualFile[] openFiles = myFileEditorManager.get().getOpenFiles();
      for (final VirtualFile openFile : openFiles) {
        resetTracker(openFile, true);
      }
    }
  }

  private void resetTracker(@Nonnull final VirtualFile virtualFile) {
    resetTracker(virtualFile, false);
  }

  private void resetTracker(@Nonnull final VirtualFile virtualFile, boolean insertOnly) {
    final Document document = FileDocumentManager.getInstance().getCachedDocument(virtualFile);
    if (document == null) {
      log("resetTracker: no cached document", virtualFile);
      return;
    }

    synchronized (myLock) {
      if (isDisabled()) return;

      final LineStatusTracker tracker = getLineStatusTracker(document);
      if (insertOnly && tracker != null) return;
      resetTracker(document, virtualFile, tracker);
    }
  }

  private void resetTracker(@Nonnull Document document, @Nonnull VirtualFile virtualFile, @Nullable LineStatusTracker tracker) {
    final boolean editorOpened = myFileEditorManager.get().isFileOpen(virtualFile);
    final boolean shouldBeInstalled = editorOpened && shouldBeInstalled(virtualFile);

    log("resetTracker: shouldBeInstalled - " + shouldBeInstalled + ", tracker - " + (tracker == null ? "null" : "found"), virtualFile);

    if (tracker != null && shouldBeInstalled) {
      refreshTracker(tracker);
    }
    else if (tracker != null) {
      releaseTracker(document);
    }
    else if (shouldBeInstalled) {
      installTracker(virtualFile, document);
    }
  }

  private boolean shouldBeInstalled(@Nullable final VirtualFile virtualFile) {
    if (isDisabled()) return false;

    if (virtualFile == null || virtualFile instanceof LightVirtualFile) return false;
    final FileStatusManager statusManager = FileStatusManager.getInstance(myProject);
    if (statusManager == null) return false;
    if (!myStatusProvider.isSupported(virtualFile)) {
      log("shouldBeInstalled failed: no support found", virtualFile);
      return false;
    }
    final FileStatus status = statusManager.getStatus(virtualFile);
    if (status == FileStatus.NOT_CHANGED || status == FileStatus.ADDED || status == FileStatus.UNKNOWN || status == FileStatus.IGNORED) {
      log("shouldBeInstalled skipped: status=" + status, virtualFile);
      return false;
    }
    return true;
  }

  private void refreshTracker(@Nonnull LineStatusTracker tracker) {
    synchronized (myLock) {
      if (isDisabled()) return;

      startAlarm(tracker.getDocument(), tracker.getVirtualFile());
    }
  }

  private void releaseTracker(@Nonnull final Document document) {
    synchronized (myLock) {
      if (isDisabled()) return;

      myPartner.remove(document);
      final TrackerData data = myLineStatusTrackers.remove(document);
      if (data != null) {
        data.tracker.release();
      }
    }
  }

  private void installTracker(@Nonnull final VirtualFile virtualFile, @Nonnull final Document document) {
    synchronized (myLock) {
      if (isDisabled()) return;

      if (myLineStatusTrackers.containsKey(document)) return;
      assert !myPartner.containsKey(document);

      final LineStatusTracker tracker = LineStatusTracker.createOn(virtualFile, document, myProject, getMode());
      myLineStatusTrackers.put(document, new TrackerData(tracker));

      startAlarm(document, virtualFile);
    }
  }

  @Nonnull
  private static LineStatusTracker.Mode getMode() {
    VcsApplicationSettings vcsApplicationSettings = VcsApplicationSettings.getInstance();
    if (!vcsApplicationSettings.SHOW_LST_GUTTER_MARKERS) return LineStatusTracker.Mode.SILENT;
    return vcsApplicationSettings.SHOW_WHITESPACES_IN_LST ? LineStatusTracker.Mode.SMART : LineStatusTracker.Mode.DEFAULT;
  }

  private void startAlarm(@Nonnull final Document document, @Nonnull final VirtualFile virtualFile) {
    synchronized (myLock) {
      myPartner.add(document, new BaseRevisionLoader(document, virtualFile));
    }
  }

  private class BaseRevisionLoader implements Runnable {
    @Nonnull
    private final VirtualFile myVirtualFile;
    @Nonnull
    private final Document myDocument;

    private BaseRevisionLoader(@Nonnull final Document document, @Nonnull final VirtualFile virtualFile) {
      myDocument = document;
      myVirtualFile = virtualFile;
    }

    @Override
    public void run() {
      if (isDisabled()) return;

      if (!myVirtualFile.isValid()) {
        log("BaseRevisionLoader failed: virtual file not valid", myVirtualFile);
        reportTrackerBaseLoadFailed();
        return;
      }

      VcsBaseContentProvider.BaseContent baseContent = myStatusProvider.getBaseRevision(myVirtualFile);
      if (baseContent == null) {
        log("BaseRevisionLoader failed: null returned for base revision", myVirtualFile);
        reportTrackerBaseLoadFailed();
        return;
      }

      // loads are sequential (in single threaded QueueProcessor);
      // so myLoadCounter can't take less value for greater base revision -> the only thing we want from it
      final VcsRevisionNumber revisionNumber = baseContent.getRevisionNumber();
      final Charset charset = myVirtualFile.getCharset();
      final long loadCounter = myLoadCounter;
      myLoadCounter++;

      synchronized (myLock) {
        final TrackerData data = myLineStatusTrackers.get(myDocument);
        if (data == null) {
          log("BaseRevisionLoader canceled: tracker already released", myVirtualFile);
          return;
        }
        if (!data.shouldBeUpdated(revisionNumber, charset, loadCounter)) {
          log("BaseRevisionLoader canceled: no need to update", myVirtualFile);
          return;
        }
      }

      String lastUpToDateContent = baseContent.loadContent();
      if (lastUpToDateContent == null) {
        log("BaseRevisionLoader failed: can't load up-to-date content", myVirtualFile);
        reportTrackerBaseLoadFailed();
        return;
      }

      final String converted = StringUtil.convertLineSeparators(lastUpToDateContent);
      final Runnable runnable = new Runnable() {
        @Override
        public void run() {
          synchronized (myLock) {
            final TrackerData data = myLineStatusTrackers.get(myDocument);
            if (data == null) {
              log("BaseRevisionLoader initializing: tracker already released", myVirtualFile);
              return;
            }
            if (!data.shouldBeUpdated(revisionNumber, charset, loadCounter)) {
              log("BaseRevisionLoader initializing: canceled", myVirtualFile);
              return;
            }

            log("BaseRevisionLoader initializing: success", myVirtualFile);
            myLineStatusTrackers.put(myDocument, new TrackerData(data.tracker, revisionNumber, charset, loadCounter));
            data.tracker.setBaseRevision(converted);
          }
        }
      };
      nonModalAliveInvokeLater(runnable);
    }

    private void nonModalAliveInvokeLater(@Nonnull Runnable runnable) {
      myApplication.invokeLater(runnable, IdeaModalityState.NON_MODAL, () -> isDisabled());
    }

    private void reportTrackerBaseLoadFailed() {
      synchronized (myLock) {
        releaseTracker(myDocument);
      }
    }
  }

  private class MyFileStatusListener implements FileStatusListener {
    @Override
    public void fileStatusesChanged() {
      resetTrackers();
    }

    @Override
    public void fileStatusChanged(@Nonnull VirtualFile virtualFile) {
      resetTracker(virtualFile);
    }
  }

  private class MyEditorFactoryListener extends EditorFactoryAdapter {
    @Override
    public void editorCreated(@Nonnull EditorFactoryEvent event) {
      // note that in case of lazy loading of configurables, this event can happen
      // outside of EDT, so the EDT check mustn't be done here
      Editor editor = event.getEditor();
      if (editor.getProject() != null && editor.getProject() != myProject) return;
      final Document document = editor.getDocument();
      final VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
      if (virtualFile == null) return;
      if (shouldBeInstalled(virtualFile)) {
        installTracker(virtualFile, document);
      }
    }

    @Override
    public void editorReleased(@Nonnull EditorFactoryEvent event) {
      final Editor editor = event.getEditor();
      if (editor.getProject() != null && editor.getProject() != myProject) return;
      final Document doc = editor.getDocument();
      final Editor[] editors = event.getFactory().getEditors(doc, myProject);
      if (editors.length == 0 || (editors.length == 1 && editor == editors[0])) {
        releaseTracker(doc);
      }
    }
  }

  private class MyVirtualFileListener extends VirtualFileAdapter {
    @Override
    public void beforeContentsChange(@Nonnull VirtualFileEvent event) {
      if (event.isFromRefresh()) {
        resetTracker(event.getFile());
      }
    }

    @Override
    public void propertyChanged(@Nonnull VirtualFilePropertyEvent event) {
      if (VirtualFile.PROP_ENCODING.equals(event.getPropertyName())) {
        resetTracker(event.getFile());
      }
    }
  }

  private static class TrackerData {
    @Nonnull
    public final LineStatusTracker tracker;
    @Nullable
    private final ContentInfo currentContent;

    public TrackerData(@Nonnull LineStatusTracker tracker) {
      this.tracker = tracker;
      this.currentContent = null;
    }

    public TrackerData(@Nonnull LineStatusTracker tracker,
                       @Nonnull VcsRevisionNumber revision,
                       @Nonnull Charset charset,
                       long loadCounter) {
      this.tracker = tracker;
      this.currentContent = new ContentInfo(revision, charset, loadCounter);
    }

    public boolean shouldBeUpdated(@Nonnull VcsRevisionNumber revision, @Nonnull Charset charset, long loadCounter) {
      if (currentContent == null) return true;
      if (currentContent.revision.equals(revision) && !currentContent.revision.equals(VcsRevisionNumber.NULL)) {
        return !currentContent.charset.equals(charset);
      }
      return currentContent.loadCounter < loadCounter;
    }
  }

  private static class ContentInfo {
    @Nonnull
    public final VcsRevisionNumber revision;
    @Nonnull
    public final Charset charset;
    public final long loadCounter;

    public ContentInfo(@Nonnull VcsRevisionNumber revision, @Nonnull Charset charset, long loadCounter) {
      this.revision = revision;
      this.charset = charset;
      this.loadCounter = loadCounter;
    }
  }

  private static void log(@Nonnull String message, @Nullable VirtualFile file) {
    if (LOG.isDebugEnabled()) {
      if (file != null) message += "; file: " + file.getPath();
      LOG.debug(message);
    }
  }
}
