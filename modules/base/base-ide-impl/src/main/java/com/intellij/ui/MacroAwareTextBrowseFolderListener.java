package com.intellij.ui;

import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextBrowseFolderListener;
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
    Project project = getProject();
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