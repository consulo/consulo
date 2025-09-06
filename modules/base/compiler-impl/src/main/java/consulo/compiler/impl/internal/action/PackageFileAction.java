/**
 * @author cdr
 */
package consulo.compiler.impl.internal.action;

import consulo.compiler.CompilerManager;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.internal.ArtifactBySourceFileFinder;
import consulo.compiler.localize.CompilerLocalize;
import consulo.document.FileDocumentManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.EmptyRunnable;
import consulo.util.lang.StringUtil;
import consulo.util.lang.SyncDateFormat;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PackageFileAction extends AnAction {
  private static final SyncDateFormat TIME_FORMAT = new SyncDateFormat(new SimpleDateFormat("h:mm:ss a"));

  public PackageFileAction() {
    super(
      CompilerLocalize.actionNamePackageFile(),
      CompilerLocalize.actionDescriptionPackageFile(),
      null
    );
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    boolean visible = false;
    Project project = e.getData(Project.KEY);
    if (project != null) {
      List<VirtualFile> files = getFilesToPackage(e, project);
      if (!files.isEmpty()) {
        visible = true;
        e.getPresentation().setTextValue(
          files.size() == 1
            ? CompilerLocalize.actionNamePackageFile()
            : CompilerLocalize.actionNamePackageFiles()
        );
      }
    }

    e.getPresentation().setVisible(visible);
  }

  @Nonnull
  private static List<VirtualFile> getFilesToPackage(@Nonnull AnActionEvent e, @Nonnull Project project) {
    VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
    if (files == null) return Collections.emptyList();

    List<VirtualFile> result = new ArrayList<>();
    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    CompilerManager compilerManager = CompilerManager.getInstance(project);
    for (VirtualFile file : files) {
      if (file == null || file.isDirectory() ||
          fileIndex.isInSourceContent(file) && compilerManager.isCompilableFileType(file.getFileType())) {
        return Collections.emptyList();
      }
      Collection<? extends Artifact> artifacts = ArtifactBySourceFileFinder.getInstance(project).findArtifacts(file);
      for (Artifact artifact : artifacts) {
        if (!StringUtil.isEmpty(artifact.getOutputPath())) {
          result.add(file);
          break;
        }
      }
    }
    return result;
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent event) {
    Project project = event.getRequiredData(Project.KEY);
    FileDocumentManager.getInstance().saveAllDocuments();
    List<VirtualFile> files = getFilesToPackage(event, project);
    Artifact[] allArtifacts = ArtifactManager.getInstance(project).getArtifacts();
    PackageFileWorker.startPackagingFiles(project, files, allArtifacts, EmptyRunnable.getInstance());
  }
}
