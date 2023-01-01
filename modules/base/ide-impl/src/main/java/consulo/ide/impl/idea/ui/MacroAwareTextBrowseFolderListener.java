package consulo.ide.impl.idea.ui;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.language.editor.LangDataKeys;
import consulo.module.Module;
import consulo.module.macro.ModulePathMacroManager;
import consulo.project.Project;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.ui.ex.awt.TextBrowseFolderListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MacroAwareTextBrowseFolderListener extends TextBrowseFolderListener {
  public MacroAwareTextBrowseFolderListener(@Nonnull FileChooserDescriptor fileChooserDescriptor,
                                            @Nullable Project project) {
    super(fileChooserDescriptor, project);
  }

  @Nonnull
  @Override
  protected String expandPath(@Nonnull String path) {
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