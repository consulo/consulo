/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.vcs.changes.conflicts;

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.fileEditor.EditorNotifications;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.project.ui.wm.MergingQueue;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.*;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import org.jdom.Element;

import java.io.File;
import java.util.*;

/**
 * @author Dmitry Avdeev
 */
public class ChangelistConflictTracker {

  private final Map<String, Conflict> myConflicts = Collections.synchronizedMap(new LinkedHashMap<String, Conflict>());

  private final Options myOptions = new Options();
  private final Project myProject;

  private final ChangeListManager myChangeListManager;
  private final EditorNotifications myEditorNotifications;
  private final ChangeListListener myChangeListListener;

  private final FileDocumentManager myDocumentManager;
  private final DocumentAdapter myDocumentListener;

  private final FileStatusManager myFileStatusManager;
  private final Set<VirtualFile> myCheckSet;
  private final Object myCheckSetLock;

  public ChangelistConflictTracker(@Nonnull Project project,
                                   @Nonnull ChangeListManager changeListManager,
                                   @Nonnull FileStatusManager fileStatusManager,
                                   @Nonnull EditorNotifications editorNotifications,
                                   @Nonnull ApplicationConcurrency applicationConcurrency,
                                   @Nonnull FileDocumentManager fileDocumentManager) {
    myProject = project;

    myChangeListManager = changeListManager;
    myEditorNotifications = editorNotifications;
    myDocumentManager = fileDocumentManager;
    myFileStatusManager = fileStatusManager;
    myCheckSetLock = new Object();
    myCheckSet = new HashSet<>();

    final MergingQueue<Runnable> zipperUpdater = new MergingQueue<>(applicationConcurrency, project, 300, project, Runnable::run);
    final Runnable runnable = () -> {
      if (project.getApplication().isDisposed() || myProject.isDisposed() || !myProject.isOpen()) {
        return;
      }
      final Set<VirtualFile> localSet;
      synchronized (myCheckSetLock) {
        localSet = new HashSet<>();
        localSet.addAll(myCheckSet);
        myCheckSet.clear();
      }
      checkFiles(localSet);
    };
    myDocumentListener = new DocumentAdapter() {
      @Override
      public void documentChanged(DocumentEvent e) {
        if (!myOptions.TRACKING_ENABLED) {
          return;
        }
        Document document = e.getDocument();
        VirtualFile file = myDocumentManager.getFile(document);
        if (ProjectLocator.getInstance().guessProjectForFile(file) == myProject) {
          synchronized (myCheckSetLock) {
            myCheckSet.add(file);
          }
          zipperUpdater.queue(runnable);
        }
      }
    };

    myChangeListListener = new ChangeListListener() {
      @Override
      public void changeListChanged(ChangeList list) {
        if (myChangeListManager.isDefaultChangeList(list)) {
          clearChanges(list.getChanges());
        }
      }

      @Override
      public void changesMoved(Collection<Change> changes, ChangeList fromList, ChangeList toList) {
        if (myChangeListManager.isDefaultChangeList(toList)) {
          clearChanges(changes);
        }
      }

      @Override
      public void changesRemoved(Collection<Change> changes, ChangeList fromList) {
        clearChanges(changes);
      }

      @Override
      public void defaultListChanged(ChangeList oldDefaultList, ChangeList newDefaultList) {
        clearChanges(newDefaultList.getChanges());
      }
    };
  }

  private void checkFiles(final Collection<VirtualFile> files) {
    myChangeListManager.invokeAfterUpdate(() -> {
      final LocalChangeList list = myChangeListManager.getDefaultChangeList();
      for (VirtualFile file : files) {
        checkOneFile(file, list);
      }
    }, InvokeAfterUpdateMode.SILENT, null, null);
  }

  private void checkOneFile(VirtualFile file, LocalChangeList defaultList) {
    if (file == null) {
      return;
    }
    LocalChangeList changeList = myChangeListManager.getChangeList(file);
    if (changeList == null || Comparing.equal(changeList, defaultList) || ChangesUtil.isInternalOperation(file)) {
      return;
    }

    String path = file.getPath();
    boolean newConflict = false;
    synchronized (myConflicts) {
      Conflict conflict = myConflicts.get(path);
      if (conflict == null) {
        conflict = new Conflict();
        myConflicts.put(path, conflict);
        newConflict = true;
      }
    }

    if (newConflict && myOptions.HIGHLIGHT_CONFLICTS) {
      myFileStatusManager.fileStatusChanged(file);
      myEditorNotifications.updateNotifications(file);
    }
  }

  public boolean isWritingAllowed(@Nonnull VirtualFile file) {
    if (isFromActiveChangelist(file)) return true;
    Conflict conflict = myConflicts.get(file.getPath());
    return conflict != null && conflict.ignored;
  }

  public boolean isFromActiveChangelist(VirtualFile file) {
    LocalChangeList changeList = myChangeListManager.getChangeList(file);
    return changeList == null || myChangeListManager.isDefaultChangeList(changeList);
  }

  private void clearChanges(Collection<Change> changes) {
    for (Change change : changes) {
      ContentRevision revision = change.getAfterRevision();
      if (revision != null) {
        FilePath filePath = revision.getFile();
        String path = filePath.getPath();
        final Conflict wasRemoved = myConflicts.remove(path);
        final VirtualFile file = filePath.getVirtualFile();
        if (wasRemoved != null && file != null) {
          myEditorNotifications.updateNotifications(file);
          // we need to update status
          myFileStatusManager.fileStatusChanged(file);
        }
      }
    }
  }

  public void startTracking() {
    myChangeListManager.addChangeListListener(myChangeListListener);
    EditorFactory.getInstance().getEventMulticaster().addDocumentListener(myDocumentListener, myProject);
  }

  public void stopTracking() {
    myChangeListManager.removeChangeListListener(myChangeListListener);
  }

  public void saveState(Element to) {
    for (Map.Entry<String,Conflict> entry : myConflicts.entrySet()) {
      Element fileElement = new Element("file");
      fileElement.setAttribute("path", entry.getKey());
      fileElement.setAttribute("ignored", Boolean.toString(entry.getValue().ignored));
      to.addContent(fileElement);
    }
    XmlSerializer.serializeInto(myOptions, to);
  }

  public void loadState(Element from) {
    myConflicts.clear();
    List files = from.getChildren("file");
    for (Object file : files) {
      Element element = (Element)file;
      String path = element.getAttributeValue("path");
      if (path == null) {
        continue;
      }
      VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(new File(path));
      if (vf == null || myChangeListManager.getChangeList(vf) == null) {
        continue;
      }
      Conflict conflict = new Conflict();
      conflict.ignored = Boolean.parseBoolean(element.getAttributeValue("ignored"));
      myConflicts.put(path, conflict);
    }
    XmlSerializer.deserializeInto(myOptions, from);
  }

  public void optionsChanged() {
    for (Map.Entry<String, Conflict> entry : myConflicts.entrySet()) {
      VirtualFile file = LocalFileSystem.getInstance().findFileByPath(entry.getKey());
      if (file != null) {
        myFileStatusManager.fileStatusChanged(file);
        myEditorNotifications.updateNotifications(file);
      }
    }
  }

  public Map<String, Conflict> getConflicts() {
    return myConflicts;
  }

  public Collection<String> getIgnoredConflicts() {
    return ContainerUtil.mapNotNull(myConflicts.entrySet(), entry -> entry.getValue().ignored ? entry.getKey() : null);
  }

  public static class Conflict {
    boolean ignored;
  }

  public boolean hasConflict(@Nonnull VirtualFile file) {
    if (!myOptions.TRACKING_ENABLED) {
      return false;
    }
    String path = file.getPath();
    Conflict conflict = myConflicts.get(path);
    if (conflict != null && !conflict.ignored) {
      if (isFromActiveChangelist(file)) {
        myConflicts.remove(path);
        return false;
      }
      return true;
    }
    else {
      return false;
    }
  }

  public void ignoreConflict(@Nonnull VirtualFile file, boolean ignore) {
    String path = file.getPath();
    Conflict conflict = myConflicts.get(path);
    if (conflict == null) {
      conflict = new Conflict();
      myConflicts.put(path, conflict);
    }
    conflict.ignored = ignore;
    myEditorNotifications.updateNotifications(file);
    myFileStatusManager.fileStatusChanged(file);
  }

  public Project getProject() {
    return myProject;
  }

  public ChangeListManager getChangeListManager() {
    return myChangeListManager;
  }

  public Options getOptions() {
    return myOptions;
  }

  public static class Options {
    public boolean TRACKING_ENABLED = true;
    public boolean SHOW_DIALOG = false;
    public boolean HIGHLIGHT_CONFLICTS = true;
    public boolean HIGHLIGHT_NON_ACTIVE_CHANGELIST = false;
    public ChangelistConflictResolution LAST_RESOLUTION = ChangelistConflictResolution.IGNORE;
  }

}
