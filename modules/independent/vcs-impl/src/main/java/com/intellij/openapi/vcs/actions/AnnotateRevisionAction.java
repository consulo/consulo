package com.intellij.openapi.vcs.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.annotate.UpToDateLineNumberListener;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsFileRevisionEx;
import com.intellij.openapi.vcs.vfs.VcsFileSystem;
import com.intellij.openapi.vcs.vfs.VcsVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

abstract class AnnotateRevisionAction extends AnnotateRevisionActionBase implements DumbAware, UpToDateLineNumberListener {
  @Nonnull
  private final FileAnnotation myAnnotation;
  @Nonnull
  private final AbstractVcs myVcs;

  private int currentLine;

  public AnnotateRevisionAction(@Nullable String text, @Nullable String description, @Nullable Image icon, @Nonnull FileAnnotation annotation, @Nonnull AbstractVcs vcs) {
    super(text, description, icon);
    myAnnotation = annotation;
    myVcs = vcs;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    if (Boolean.TRUE.equals(e.getData(PlatformDataKeys.IS_MODAL_CONTEXT))) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    if (myAnnotation.getFile() == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    if (getRevisions() == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    e.getPresentation().setVisible(true);

    super.update(e);
  }

  @Nullable
  protected abstract List<VcsFileRevision> getRevisions();

  @Nullable
  protected AbstractVcs getVcs(@Nonnull AnActionEvent e) {
    return myVcs;
  }

  @Nullable
  @Override
  protected VirtualFile getFile(@Nonnull AnActionEvent e) {
    VcsFileRevision revision = getFileRevision(e);
    if (revision == null) return null;

    final FileType currentFileType = myAnnotation.getFile().getFileType();
    FilePath filePath = (revision instanceof VcsFileRevisionEx ? ((VcsFileRevisionEx)revision).getPath() : VcsUtil.getFilePath(myAnnotation.getFile()));
    return new VcsVirtualFile(filePath.getPath(), revision, VcsFileSystem.getInstance()) {
      @Nonnull
      @Override
      public FileType getFileType() {
        FileType type = super.getFileType();
        if (!type.isBinary()) return type;
        if (!currentFileType.isBinary()) return currentFileType;
        return PlainTextFileType.INSTANCE;
      }
    };
  }

  @Nullable
  @Override
  protected VcsFileRevision getFileRevision(@Nonnull AnActionEvent e) {
    List<VcsFileRevision> revisions = getRevisions();
    assert revisions != null;

    if (currentLine < 0 || currentLine >= revisions.size()) return null;
    return revisions.get(currentLine);
  }

  @Override
  public void consume(Integer integer) {
    currentLine = integer;
  }
}
