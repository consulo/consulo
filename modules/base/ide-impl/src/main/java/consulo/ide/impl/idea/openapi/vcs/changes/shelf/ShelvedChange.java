/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
 * Date: 23.11.2006
 * Time: 19:06:26
 */
package consulo.ide.impl.idea.openapi.vcs.changes.shelf;

import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.openapi.diff.impl.patch.ApplyPatchException;
import consulo.ide.impl.idea.openapi.diff.impl.patch.PatchSyntaxException;
import consulo.ide.impl.idea.openapi.diff.impl.patch.TextFilePatch;
import consulo.ide.impl.idea.openapi.diff.impl.patch.apply.GenericPatchApplier;
import consulo.ide.impl.idea.openapi.vcs.changes.TextRevisionNumber;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.base.FilePathImpl;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.change.CurrentContentRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ShelvedChange {
  private static final Logger LOG = Logger.getInstance(ShelvedChange.class);

  private final String myPatchPath;
  private final String myBeforePath;
  private final String myAfterPath;
  private final FileStatus myFileStatus;
  private final AtomicReference<Boolean> myIsConflicting;
  private Change myChange;

  public ShelvedChange(final String patchPath, final String beforePath, final String afterPath, final FileStatus fileStatus) {
    myPatchPath = patchPath;
    myBeforePath = beforePath;
    // optimisation: memory
    myAfterPath = Comparing.equal(beforePath, afterPath) ? beforePath : afterPath;
    myFileStatus = fileStatus;
    myIsConflicting = new AtomicReference<Boolean>();
  }

  public boolean isConflictingChange(final Project project) {
    Boolean isConflicting = myIsConflicting.get();
    if (isConflicting != null) return isConflicting;

    ContentRevision afterRevision = getChange(project).getAfterRevision();
    if (afterRevision == null) return false;
    try {
      afterRevision.getContent();
    }
    catch(VcsException e) {
      if (e.getCause() instanceof ApplyPatchException) {
        myIsConflicting.set(true);
        return true;
      }
    }
    myIsConflicting.set(false);
    return false;
  }

  public String getBeforePath() {
    return myBeforePath;
  }

  @Nullable
  public VirtualFile getBeforeVFUnderProject(final Project project) {
    if (myBeforePath == null || project.getBaseDir() == null) return null;
    final File baseDir = new File(project.getBaseDir().getPath());
    final File file = new File(baseDir, myBeforePath);
    return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
  }

  public String getAfterPath() {
    return myAfterPath;
  }

  @jakarta.annotation.Nullable
  public String getAfterFileName() {
    if (myAfterPath == null) return null;
    int pos = myAfterPath.lastIndexOf('/');
    if (pos >= 0) return myAfterPath.substring(pos+1);
    return myAfterPath;
  }

  public String getBeforeFileName() {
    int pos = myBeforePath.lastIndexOf('/');
    if (pos >= 0) return myBeforePath.substring(pos+1);
    return myBeforePath;
  }

  public String getBeforeDirectory() {
    int pos = myBeforePath.lastIndexOf('/');
    if (pos >= 0) return myBeforePath.substring(0, pos).replace('/', File.separatorChar);
    return File.separator;
  }

  public FileStatus getFileStatus() {
    return myFileStatus;
  }

  public Change getChange(Project project) {
    if (myChange == null) {
      File baseDir = new File(project.getBaseDir().getPath());

      File file = getAbsolutePath(baseDir, myBeforePath);
      final FilePathImpl beforePath = new FilePathImpl(file, false);
      beforePath.refresh();
      ContentRevision beforeRevision = null;
      if (myFileStatus != FileStatus.ADDED) {
        beforeRevision = new CurrentContentRevision(beforePath) {
          @Override
          @Nonnull
          public VcsRevisionNumber getRevisionNumber() {
            return new TextRevisionNumber(VcsLocalize.localVersionTitle().get());
          }
        };
      }
      ContentRevision afterRevision = null;
      if (myFileStatus != FileStatus.DELETED) {
        final FilePathImpl afterPath = new FilePathImpl(getAbsolutePath(baseDir, myAfterPath), false);
        afterRevision = new PatchedContentRevision(project, beforePath, afterPath);
      }
      myChange = new Change(beforeRevision, afterRevision, myFileStatus);
    }
    return myChange;
  }

  private static File getAbsolutePath(final File baseDir, final String relativePath) {
    File file;
    try {
      file = new File(baseDir, relativePath).getCanonicalFile();
    }
    catch (IOException e) {
      LOG.info(e);
      file = new File(baseDir, relativePath);
    }
    return file;
  }

  @jakarta.annotation.Nullable
  public TextFilePatch loadFilePatch(final Project project, CommitContext commitContext) throws IOException, PatchSyntaxException {
    List<TextFilePatch> filePatches = ShelveChangesManager.loadPatches(project, myPatchPath, commitContext);
    for(TextFilePatch patch: filePatches) {
      if (myBeforePath.equals(patch.getBeforeName())) {
        return patch;
      }
    }
    return null;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof ShelvedChange)) return false;

    final ShelvedChange that = (ShelvedChange)o;

    if (myAfterPath != null ? !myAfterPath.equals(that.myAfterPath) : that.myAfterPath != null) return false;
    if (myBeforePath != null ? !myBeforePath.equals(that.myBeforePath) : that.myBeforePath != null) return false;
    if (myFileStatus != null ? !myFileStatus.equals(that.myFileStatus) : that.myFileStatus != null) return false;
    if (myPatchPath != null ? !myPatchPath.equals(that.myPatchPath) : that.myPatchPath != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myPatchPath != null ? myPatchPath.hashCode() : 0;
    result = 31 * result + (myBeforePath != null ? myBeforePath.hashCode() : 0);
    result = 31 * result + (myAfterPath != null ? myAfterPath.hashCode() : 0);
    result = 31 * result + (myFileStatus != null ? myFileStatus.hashCode() : 0);
    return result;
  }

  private class PatchedContentRevision implements ContentRevision {
    private final Project myProject;
    private final FilePath myBeforeFilePath;
    private final FilePath myAfterFilePath;
    private String myContent;

    public PatchedContentRevision(Project project, final FilePath beforeFilePath, final FilePath afterFilePath) {
      myProject = project;
      myBeforeFilePath = beforeFilePath;
      myAfterFilePath = afterFilePath;
    }

    @Override
    @jakarta.annotation.Nullable
    public String getContent() throws VcsException {
      if (myContent == null) {
        try {
          myContent = loadContent();
        }
        catch (Exception e) {
          throw new VcsException(e);
        }
      }

      return myContent;
    }

    @jakarta.annotation.Nullable
    private String loadContent() throws IOException, PatchSyntaxException, ApplyPatchException {
      TextFilePatch patch = loadFilePatch(myProject, null);
      if (patch != null) {
        return loadContent(patch);
      }
      return null;
    }

    private String loadContent(final TextFilePatch patch) throws ApplyPatchException {
      if (patch.isNewFile()) {
        return patch.getNewFileText();
      }
      if (patch.isDeletedFile()) {
        return null;
      }
      final GenericPatchApplier applier = new GenericPatchApplier(getBaseContent(), patch.getHunks());
      if (applier.execute()) {
        return applier.getAfter();
      }
      throw new ApplyPatchException("Apply patch conflict");
    }

    private String getBaseContent() {
      myBeforeFilePath.refresh();
      final Document doc = FileDocumentManager.getInstance().getDocument(myBeforeFilePath.getVirtualFile());
      return doc.getText();
    }

    @Override
    @Nonnull
    public FilePath getFile() {
      return myAfterFilePath;
    }

    @Override
    @Nonnull
    public VcsRevisionNumber getRevisionNumber() {
      return new TextRevisionNumber(VcsLocalize.shelvedVersionName().get());
    }
  }

  public String getPatchPath() {
    return myPatchPath;
  }
}
