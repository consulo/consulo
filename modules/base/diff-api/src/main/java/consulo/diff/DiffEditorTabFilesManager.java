package consulo.diff;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;


@ServiceAPI(ComponentScope.PROJECT)
public interface DiffEditorTabFilesManager {

  
  public static DiffEditorTabFilesManager getInstance(Project project) {
    return project.getInstance(DiffEditorTabFilesManager.class);
  }

  FileEditor[] showDiffFile(VirtualFile diffFile, boolean focusEditor);
}
