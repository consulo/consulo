package com.intellij.ui;

import com.intellij.openapi.actionSystem.LangDataKeys;
import consulo.component.macro.PathMacroManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.module.Module;
import consulo.project.Project;
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
      path = PathMacroManager.getInstance(project).expandPath(path);
    }

    Module module = myFileChooserDescriptor.getUserData(LangDataKeys.MODULE_CONTEXT);
    if (module == null) {
      module = myFileChooserDescriptor.getUserData(LangDataKeys.MODULE);
    }
    if (module != null) {
      path = PathMacroManager.getInstance(module).expandPath(path);
    }

    return super.expandPath(path);
  }
}