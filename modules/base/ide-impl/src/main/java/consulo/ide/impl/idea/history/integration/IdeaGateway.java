/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.history.integration;

import consulo.ide.impl.idea.history.core.LocalHistoryFacade;
import consulo.ide.impl.idea.history.core.Paths;
import consulo.ide.impl.idea.history.core.StoredContent;
import consulo.ide.impl.idea.history.core.tree.DirectoryEntry;
import consulo.ide.impl.idea.history.core.tree.Entry;
import consulo.ide.impl.idea.history.core.tree.FileEntry;
import consulo.ide.impl.idea.history.core.tree.RootEntry;
import consulo.application.ApplicationManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.util.io.CharsetToolkit;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.file.FileTypeManager;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.util.lang.Clock;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.*;
import consulo.virtualFileSystem.encoding.EncodingRegistry;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.ide.impl.idea.openapi.vfs.newvfs.impl.VirtualFileSystemEntry;
import consulo.ide.impl.idea.util.NullableFunction;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IdeaGateway {
  private static final Key<ContentAndTimestamps> SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY
          = Key.create("LocalHistory.SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY");

  public boolean isVersioned(@Nonnull VirtualFile f) {
    return isVersioned(f, false);
  }

  public boolean isVersioned(@Nonnull VirtualFile f, boolean shouldBeInContent) {
    if (!f.isInLocalFileSystem()) return false;

    if (!f.isDirectory() && StringUtil.endsWith(f.getNameSequence(), ".class")) return false;

    VersionedFilterData versionedFilterData = getVersionedFilterData();

    boolean isInContent = false;
    int numberOfOpenProjects = versionedFilterData.myOpenedProjects.size();
    for (int i = 0; i < numberOfOpenProjects; ++i) {
      if (f.equals(versionedFilterData.myWorkspaceFiles.get(i))) return false;
      ProjectFileIndex index = versionedFilterData.myProjectFileIndices.get(i);

      if (index.isExcluded(f)) return false;
      isInContent |= index.isInContent(f);
    }
    if (shouldBeInContent && !isInContent) return false;

    // optimisation: FileTypeManager.isFileIgnored(f) already checked inside ProjectFileIndex.isIgnored()
    return numberOfOpenProjects != 0 || !FileTypeManager.getInstance().isFileIgnored(f);
  }

  @Nonnull
  protected static VersionedFilterData getVersionedFilterData() {
    VersionedFilterData versionedFilterData;
    VfsEventDispatchContext vfsEventDispatchContext = ourCurrentEventDispatchContext.get();
    if (vfsEventDispatchContext != null) {
      versionedFilterData = vfsEventDispatchContext.myFilterData;
      if (versionedFilterData == null) versionedFilterData = vfsEventDispatchContext.myFilterData = new VersionedFilterData();
    } else {
      versionedFilterData = new VersionedFilterData();
    }
    return versionedFilterData;
  }

  private static final ThreadLocal<VfsEventDispatchContext> ourCurrentEventDispatchContext = new ThreadLocal<>();

  private static class VfsEventDispatchContext implements AutoCloseable {
    final List<? extends VFileEvent> myEvents;
    final boolean myBeforeEvents;
    final VfsEventDispatchContext myPreviousContext;

    VersionedFilterData myFilterData;

    VfsEventDispatchContext(List<? extends VFileEvent> events, boolean beforeEvents) {
      myEvents = events;
      myBeforeEvents = beforeEvents;
      myPreviousContext = ourCurrentEventDispatchContext.get();
      if (myPreviousContext != null) {
        myFilterData = myPreviousContext.myFilterData;
      }
      ourCurrentEventDispatchContext.set(this);
    }

    public void close() {
      ourCurrentEventDispatchContext.set(myPreviousContext);
      if (myPreviousContext != null && myPreviousContext.myFilterData == null && myFilterData != null) {
        myPreviousContext.myFilterData = myFilterData;
      }
    }
  }

  public void runWithVfsEventsDispatchContext(List<? extends VFileEvent> events, boolean beforeEvents, Runnable action) {
    try (VfsEventDispatchContext ignored = new VfsEventDispatchContext(events, beforeEvents)) {
      action.run();
    }
  }

  private static class VersionedFilterData {
    final List<Project> myOpenedProjects = new ArrayList<>();
    final List<ProjectFileIndex> myProjectFileIndices = new ArrayList<>();
    final List<VirtualFile> myWorkspaceFiles = new ArrayList<>();

    VersionedFilterData() {
      Project[] openProjects = ProjectManager.getInstance().getOpenProjects();

      for (Project each : openProjects) {
        if (each.isDefault()) continue;
        if (!each.isInitialized()) continue;

        myWorkspaceFiles.add(each.getWorkspaceFile());
        myOpenedProjects.add(each);
        myProjectFileIndices.add(ProjectRootManager.getInstance(each).getFileIndex());
      }
    }
  }

  public boolean areContentChangesVersioned(@Nonnull VirtualFile f) {
    return isVersioned(f) && !f.isDirectory() && !f.getFileType().isBinary();
  }

  public boolean areContentChangesVersioned(@Nonnull String fileName) {
    return !FileTypeManager.getInstance().getFileTypeByFileName(fileName).isBinary();
  }

  public boolean ensureFilesAreWritable(@Nonnull Project p, @Nonnull List<VirtualFile> ff) {
    ReadonlyStatusHandler h = ReadonlyStatusHandler.getInstance(p);
    return !h.ensureFilesWritable(VfsUtilCore.toVirtualFileArray(ff)).hasReadonlyFiles();
  }

  @jakarta.annotation.Nullable
  public VirtualFile findVirtualFile(@Nonnull String path) {
    return LocalFileSystem.getInstance().findFileByPath(path);
  }

  @Nonnull
  public VirtualFile findOrCreateFileSafely(@Nonnull VirtualFile parent, @Nonnull String name, boolean isDirectory) throws IOException {
    VirtualFile f = parent.findChild(name);
    if (f != null && f.isDirectory() != isDirectory) {
      f.delete(this);
      f = null;
    }
    if (f == null) {
      f = isDirectory
          ? parent.createChildDirectory(this, name)
          : parent.createChildData(this, name);
    }
    return f;
  }

  @Nonnull
  public VirtualFile findOrCreateFileSafely(@Nonnull String path, boolean isDirectory) throws IOException {
    VirtualFile f = findVirtualFile(path);
    if (f != null && f.isDirectory() != isDirectory) {
      f.delete(this);
      f = null;
    }
    if (f == null) {
      VirtualFile parent = findOrCreateFileSafely(Paths.getParentOf(path), true);
      String name = Paths.getNameOf(path);
      f = isDirectory
          ? parent.createChildDirectory(this, name)
          : parent.createChildData(this, name);
    }
    return f;
  }

  public List<VirtualFile> getAllFilesFrom(@Nonnull String path) {
    VirtualFile f = findVirtualFile(path);
    if (f == null) return Collections.emptyList();
    return collectFiles(f, new ArrayList<>());
  }

  @Nonnull
  private static List<VirtualFile> collectFiles(@Nonnull VirtualFile f, @Nonnull List<VirtualFile> result) {
    if (f.isDirectory()) {
      for (VirtualFile child : iterateDBChildren(f)) {
        collectFiles(child, result);
      }
    }
    else {
      result.add(f);
    }
    return result;
  }

  @Nonnull
  public static Iterable<VirtualFile> iterateDBChildren(VirtualFile f) {
    if (!(f instanceof NewVirtualFile)) return Collections.emptyList();
    NewVirtualFile nf = (NewVirtualFile)f;
    return nf.iterInDbChildrenWithoutLoadingVfsFromOtherProjects();
  }

  @Nonnull
  public static Iterable<VirtualFile> loadAndIterateChildren(VirtualFile f) {
    if (!(f instanceof NewVirtualFile)) return Collections.emptyList();
    NewVirtualFile nf = (NewVirtualFile)f;
    return Arrays.asList(nf.getChildren());
  }

  @Nonnull
  public RootEntry createTransientRootEntry() {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    RootEntry root = new RootEntry();
    doCreateChildren(root, getLocalRoots(), false);
    return root;
  }

  @Nonnull
  public RootEntry createTransientRootEntryForPathOnly(@Nonnull String path) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    RootEntry root = new RootEntry();
    doCreateChildrenForPathOnly(root, path, getLocalRoots());
    return root;
  }

  private static List<VirtualFile> getLocalRoots() {
    return Arrays.asList(ManagingFS.getInstance().getLocalRoots());
  }

  private void doCreateChildrenForPathOnly(@Nonnull DirectoryEntry parent,
                                           @Nonnull String path,
                                           @Nonnull Iterable<VirtualFile> children) {
    for (VirtualFile child : children) {
      String name = StringUtil.trimStart(child.getName(), "/"); // on Mac FS root name is "/"
      if (!path.startsWith(name)) continue;
      String rest = path.substring(name.length());
      if (!rest.isEmpty() && rest.charAt(0) != '/') continue;
      if (!rest.isEmpty() && rest.charAt(0) == '/') {
        rest = rest.substring(1);
      }
      Entry e = doCreateEntryForPathOnly(child, rest);
      if (e == null) continue;
      parent.addChild(e);
    }
  }

  @jakarta.annotation.Nullable
  private Entry doCreateEntryForPathOnly(@Nonnull VirtualFile file, @Nonnull String path) {
    if (!file.isDirectory()) {
      if (!isVersioned(file)) return null;

      return doCreateFileEntry(file, getActualContentNoAcquire(file));
    }
    DirectoryEntry newDir = new DirectoryEntry(file.getName());
    doCreateChildrenForPathOnly(newDir, path, iterateDBChildren(file));
    if (!isVersioned(file) && newDir.getChildren().isEmpty()) return null;
    return newDir;
  }

  @jakarta.annotation.Nullable
  public Entry createTransientEntry(@Nonnull VirtualFile file) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return doCreateEntry(file, false);
  }

  @Nullable
  public Entry createEntryForDeletion(@Nonnull VirtualFile file) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return doCreateEntry(file, true);
  }

  @Nullable
  private Entry doCreateEntry(@Nonnull VirtualFile file, boolean forDeletion) {
    if (!file.isDirectory()) {
      if (!isVersioned(file)) return null;

      Pair<StoredContent, Long> contentAndStamps;
      if (forDeletion) {
        FileDocumentManager m = FileDocumentManager.getInstance();
        Document d = m.isFileModified(file) ? m.getCachedDocument(file) : null; // should not try to load document
        contentAndStamps = acquireAndClearCurrentContent(file, d);
      }
      else {
        contentAndStamps = getActualContentNoAcquire(file);
      }

      return doCreateFileEntry(file, contentAndStamps);
    }

    DirectoryEntry newDir = null;
    if (file instanceof VirtualFileSystemEntry) {
      int nameId = ((VirtualFileSystemEntry)file).getNameId();
      if (nameId > 0) {
        newDir = new DirectoryEntry(nameId);
      }
    }

    if (newDir == null) {
      newDir = new DirectoryEntry(file.getName());
    }

    doCreateChildren(newDir, iterateDBChildren(file), forDeletion);
    if (!isVersioned(file) && newDir.getChildren().isEmpty()) return null;
    return newDir;
  }

  @Nonnull
  private Entry doCreateFileEntry(@Nonnull VirtualFile file, Pair<StoredContent, Long> contentAndStamps) {
    if (file instanceof VirtualFileSystemEntry) {
      return new FileEntry(((VirtualFileSystemEntry)file).getNameId(), contentAndStamps.first, contentAndStamps.second, !file.isWritable());
    }
    return new FileEntry(file.getName(), contentAndStamps.first, contentAndStamps.second, !file.isWritable());
  }

  private void doCreateChildren(@Nonnull DirectoryEntry parent, Iterable<VirtualFile> children, final boolean forDeletion) {
    List<Entry> entries = ContainerUtil.mapNotNull(children, (NullableFunction<VirtualFile, Entry>)each -> doCreateEntry(each, forDeletion));
    parent.addChildren(entries);
  }

  public void registerUnsavedDocuments(@Nonnull final LocalHistoryFacade vcs) {
    ApplicationManager.getApplication().runReadAction(() -> {
      vcs.beginChangeSet();
      for (Document d : FileDocumentManager.getInstance().getUnsavedDocuments()) {
        VirtualFile f = getFile(d);
        if (!shouldRegisterDocument(f)) continue;
        registerDocumentContents(vcs, f, d);
      }
      vcs.endChangeSet(null);
    });
  }

  private boolean shouldRegisterDocument(@Nullable VirtualFile f) {
    return f != null && f.isValid() && areContentChangesVersioned(f);
  }

  private void registerDocumentContents(@Nonnull LocalHistoryFacade vcs, @Nonnull VirtualFile f, Document d) {
    Pair<StoredContent, Long> contentAndStamp = acquireAndUpdateActualContent(f, d);
    if (contentAndStamp != null) {
      vcs.contentChanged(f.getPath(), contentAndStamp.first, contentAndStamp.second);
    }
  }

  // returns null is content has not been changes since last time
  @jakarta.annotation.Nullable
  public Pair<StoredContent, Long> acquireAndUpdateActualContent(@Nonnull VirtualFile f, @Nullable Document d) {
    ContentAndTimestamps contentAndStamp = f.getUserData(SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY);
    if (contentAndStamp == null) {
      if (d != null) saveDocumentContent(f, d);
      return Pair.create(StoredContent.acquireContent(f), f.getTimeStamp());
    }

    // if no need to save current document content when simply return and clear stored one
    if (d == null) {
      f.putUserData(SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY, null);
      return Pair.create(contentAndStamp.content, contentAndStamp.registeredTimestamp);
    }

    // if the stored content equals the current one, do not store it and return null
    if (d.getModificationStamp() == contentAndStamp.documentModificationStamp) return null;

    // is current content has been changed, store it and return the previous one
    saveDocumentContent(f, d);
    return Pair.create(contentAndStamp.content, contentAndStamp.registeredTimestamp);
  }

  private static void saveDocumentContent(@Nonnull VirtualFile f, @Nonnull Document d) {
    f.putUserData(SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY,
                  new ContentAndTimestamps(Clock.getTime(),
                                           StoredContent.acquireContent(bytesFromDocument(d)),
                                           d.getModificationStamp()));
  }

  @Nonnull
  public Pair<StoredContent, Long> acquireAndClearCurrentContent(@Nonnull VirtualFile f, @Nullable Document d) {
    ContentAndTimestamps contentAndStamp = f.getUserData(SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY);
    f.putUserData(SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY, null);

    if (d != null && contentAndStamp != null) {
      // if previously stored content was not changed, return it
      if (d.getModificationStamp() == contentAndStamp.documentModificationStamp) {
        return Pair.create(contentAndStamp.content, contentAndStamp.registeredTimestamp);
      }
    }

    // release previously stored
    if (contentAndStamp != null) {
      contentAndStamp.content.release();
    }

    // take document's content if any
    if (d != null) {
      return Pair.create(StoredContent.acquireContent(bytesFromDocument(d)), Clock.getTime());
    }

    return Pair.create(StoredContent.acquireContent(f), f.getTimeStamp());
  }

  @Nonnull
  private static Pair<StoredContent, Long> getActualContentNoAcquire(@Nonnull VirtualFile f) {
    ContentAndTimestamps result = f.getUserData(SAVED_DOCUMENT_CONTENT_AND_STAMP_KEY);
    if (result == null) {
      return Pair.create(StoredContent.transientContent(f), f.getTimeStamp());
    }
    return Pair.create(result.content, result.registeredTimestamp);
  }

  private static byte[] bytesFromDocument(@Nonnull Document d) {
    try {
      return d.getText().getBytes(getFile(d).getCharset().name());
    }
    catch (UnsupportedEncodingException e) {
      return d.getText().getBytes();
    }
  }

  public String stringFromBytes(@Nonnull byte[] bytes, @Nonnull String path) {
    try {
      VirtualFile file = findVirtualFile(path);
      if (file == null) {
        return CharsetToolkit.bytesToString(bytes, EncodingRegistry.getInstance().getDefaultCharset());
      }
      return new String(bytes, file.getCharset().name());
    }
    catch (UnsupportedEncodingException e1) {
      return new String(bytes);
    }
  }

  public void saveAllUnsavedDocuments() {
    FileDocumentManager.getInstance().saveAllDocuments();
  }

  @Nullable
  private static VirtualFile getFile(@Nonnull Document d) {
    return FileDocumentManager.getInstance().getFile(d);
  }

  @Nullable
  public Document getDocument(@Nonnull String path) {
    return FileDocumentManager.getInstance().getDocument(findVirtualFile(path));
  }

  @Nonnull
  public FileType getFileType(@Nonnull String fileName) {
    return FileTypeManager.getInstance().getFileTypeByFileName(fileName);
  }

  private static class ContentAndTimestamps {
    long registeredTimestamp;
    StoredContent content;
    long documentModificationStamp;

    private ContentAndTimestamps(long registeredTimestamp, StoredContent content, long documentModificationStamp) {
      this.registeredTimestamp = registeredTimestamp;
      this.content = content;
      this.documentModificationStamp = documentModificationStamp;
    }
  }
}
