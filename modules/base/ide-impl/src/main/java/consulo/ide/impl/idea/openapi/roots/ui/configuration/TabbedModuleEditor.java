package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.module.Module;
import consulo.ide.impl.idea.openapi.module.ModuleConfigurationEditor;
import consulo.project.Project;
import consulo.ui.ex.awt.TabbedPaneWrapper;
import consulo.disposer.Disposable;
import consulo.configurable.internal.ConfigurableUIMigrationUtil;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
      myTabbedPane.addTab(editor.getDisplayName().get(), ConfigurableUIMigrationUtil.createComponent(editor, parentUIDisposable));
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
      int editorTabIndex = getEditorTabIndex(name);
      if (editorTabIndex >= 0 && editorTabIndex < myTabbedPane.getTabCount()) {
        myTabbedPane.setSelectedIndex(editorTabIndex);
      }
    }
  }

  private int getEditorTabIndex(String editorName) {
    if (myTabbedPane != null && editorName != null) {
      int tabCount = myTabbedPane.getTabCount();
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
