package consulo.ide.impl.idea.ui;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.language.editor.LangDataKeys;
import consulo.module.Module;
import consulo.module.macro.ModulePathMacroManager;
import consulo.project.Project;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.ui.ex.awt.TextBrowseFolderListener;

import org.jspecify.annotations.Nullable;

public class MacroAwareTextBrowseFolderListener extends TextBrowseFolderListener {
  public MacroAwareTextBrowseFolderListener(FileChooserDescriptor fileChooserDescriptor,
                                            @Nullable Project project) {
    super(fileChooserDescriptor, project);
  }

  
  @Override
  protected String expandPath(String path) {
    Project project = (Project)getProject();
    if (project != null) {
      path = ProjectPathMacroManager.getInstance(project).expandPath(path);
    }

    Module module = myFileChooserDescriptor.getUserData(LangDataKeys.MODULE_CONTEXT);
    if (module == null) {
      module = myFileChooserDescriptor.getUserData(LangDataKeys.MODULE);
    }
    if (module != null) {
      path = ModulePathMacroManager.getInstance(module).expandPath(path);
    }

    return super.expandPath(path);
  }
}