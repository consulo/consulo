package consulo.ide.impl.idea.diff.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;

// from kotlin
@Service(ComponentScope.PROJECT)
public interface DiffEditorTabFilesManager {

  @Nonnull
  public static DiffEditorTabFilesManager getInstance(@Nonnull Project project) {
    return project.getInstance(DiffEditorTabFilesManager.class);
  }

  FileEditor[] showDiffFile(VirtualFile diffFile, boolean focusEditor);
}
