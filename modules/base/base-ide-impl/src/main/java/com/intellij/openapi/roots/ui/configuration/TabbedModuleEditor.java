package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.TabbedPaneWrapper;
import consulo.disposer.Disposable;
import consulo.options.ConfigurableUIMigrationUtil;
import consulo.roots.ui.configuration.LibrariesConfigurator;
import consulo.roots.ui.configuration.ModulesConfigurator;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author ksafonov
 */
public class TabbedModuleEditor extends ModuleEditor {
  private TabbedPaneWrapper myTabbedPane;

  public TabbedModuleEditor(Project project, ModulesConfigurator modulesProvider, LibrariesConfigurator librariesConfigurator, @Nonnull Module module) {
    super(project, modulesProvider, librariesConfigurator, module);
  }

  @Override
  @RequiredUIAccess
  protected JComponent createCenterPanel(Disposable parentUIDisposable) {
    myTabbedPane = new TabbedPaneWrapper(this);

    for (ModuleConfigurationEditor editor : myEditors) {
      myTabbedPane.addTab(editor.getDisplayName(), ConfigurableUIMigrationUtil.createComponent(editor, parentUIDisposable));
      editor.initialize();
      editor.reset();
    }
    restoreSelectedEditor();

    return myTabbedPane.getComponent();
  }

  @Override
  protected void restoreSelectedEditor() {
    myTabbedPane.setSelectedIndex(0);
  }

  @Nullable
  private String getSelectedTabName() {
    return myTabbedPane == null || myTabbedPane.getSelectedIndex() == -1 ? null : myTabbedPane.getTitleAt(myTabbedPane.getSelectedIndex());
  }

  @Override
  public void selectEditor(@Nullable String name) {
    if (name != null) {
      //getPanel(parentUIDisposable);
      final int editorTabIndex = getEditorTabIndex(name);
      if (editorTabIndex >= 0 && editorTabIndex < myTabbedPane.getTabCount()) {
        myTabbedPane.setSelectedIndex(editorTabIndex);
      }
    }
  }

  private int getEditorTabIndex(final String editorName) {
    if (myTabbedPane != null && editorName != null) {
      final int tabCount = myTabbedPane.getTabCount();
      for (int idx = 0; idx < tabCount; idx++) {
        if (editorName.equals(myTabbedPane.getTitleAt(idx))) {
          return idx;
        }
      }
    }
    return -1;
  }

  @Override
  @Nullable
  public ModuleConfigurationEditor getEditor(@Nonnull String displayName) {
    int index = getEditorTabIndex(displayName);
    if (0 <= index && index < myEditors.size()) {
      return myEditors.get(index);
    }
    return null;
  }

  @Override
  public ModuleConfigurationEditor getSelectedEditor() {
    if (myTabbedPane == null) {
      return null;
    }

    String title = myTabbedPane.getSelectedTitle();
    if (title == null) {
      return null;
    }

    return getEditor(title);
  }

  @Override
  protected void disposeCenterPanel() {
    if (myTabbedPane != null) {
      myTabbedPane = null;
    }
  }
}
