package consulo.versionControlSystem.internal;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.*;

public abstract class FilesProcessorImpl implements FilesProcessor {
  private Project project;

  private final Set<VirtualFile> files = new HashSet<VirtualFile>();

  public FilesProcessorImpl(@Nonnull Project project, @Nonnull Disposable parentDisposable) {
    this.project = project;
    Disposer.register(parentDisposable, this);
  }

  public abstract void doActionOnChosenFiles(@Nonnull Collection<? extends VirtualFile> files);

  @Nonnull
  public abstract Collection<VirtualFile> doFilterFiles(
    @Nonnull Collection<? extends VirtualFile> files);

  public void processFiles(@Nonnull Collection<VirtualFile> files) {
    Collection<VirtualFile> filteredFiles = doFilterFiles(files);
    if (filteredFiles.isEmpty()) {
      return;
    }
    addNewFiles(filteredFiles);
    if (needDoForCurrentProject()) {
      doActionOnChosenFiles(acquireValidFiles());
    }
    else {
      handleProcessingForCurrentProject();
    }
  }

  protected void handleProcessingForCurrentProject() {
  }

  protected final boolean removeFiles(@Nonnull Collection<? extends VirtualFile> filesToRemove) {
    synchronized (files) {
      return VcsUtil.removeAllFromSet(files, filesToRemove);
    }
  }

  protected final boolean isFilesEmpty() {
    synchronized (files) {
      return files.isEmpty();
    }
  }

  private void addNewFiles(Collection<? extends VirtualFile> filesToAdd) {
    synchronized (files) {
      files.addAll(filesToAdd);
    }
  }

  @Nonnull
  protected final List<VirtualFile> selectValidFiles() {
    synchronized (files) {
      files.removeIf(it -> !it.isValid());
      return new ArrayList(files);
    }
  }

  @Nonnull
  protected final List<VirtualFile> acquireValidFiles() {
    synchronized (files) {
      List<VirtualFile> result = files.stream().filter(it -> it.isValid()).toList();
      files.clear();
      return result;
    }
  }

  protected final void clearFiles() {
    synchronized (files) {
      files.clear();
    }
  }

  public void dispose() {
    clearFiles();
  }

  protected abstract boolean needDoForCurrentProject();

  @Nonnull
  protected final Project getProject() {
    return project;
  }
}
