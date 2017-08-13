/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.openapi.roots.ui.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.ex.FileChooserKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.roots.ui.componentsList.layout.VerticalStackLayout;
import com.intellij.openapi.roots.ui.configuration.actions.IconWithTextAction;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.ex.VirtualFileManagerAdapter;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.roots.ToolbarPanel;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 *         Date: Oct 4, 2003
 *         Time: 6:54:57 PM
 */
public class ContentEntriesEditor extends ModuleElementsEditor {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.roots.ui.configuration.ContentEntriesEditor");
  public static final String NAME = ProjectBundle.message("module.paths.title");
  private static final Color BACKGROUND_COLOR = UIUtil.getListBackground();

  protected ContentEntryTreeEditor myRootTreeEditor;
  private MyContentEntryEditorListener myContentEntryEditorListener;
  protected JPanel myEditorsPanel;
  private final Map<ContentEntry, ContentEntryEditor> myEntryToEditorMap = new HashMap<ContentEntry, ContentEntryEditor>();
  private ContentEntry mySelectedEntry;

  private VirtualFile myLastSelectedDir = null;
  private final String myModuleName;
  private final ModulesProvider myModulesProvider;
  private final ModuleConfigurationState myState;

  public ContentEntriesEditor(String moduleName, final ModuleConfigurationState state) {
    super(state);
    myState = state;
    myModuleName = moduleName;
    myModulesProvider = state.getModulesProvider();
    final VirtualFileManagerAdapter fileManagerListener = new VirtualFileManagerAdapter() {
      @Override
      public void afterRefreshFinish(boolean asynchronous) {
        if (state.getProject().isDisposed()) {
          return;
        }
        final Module module = getModule();
        if (module == null || module.isDisposed()) return;
        for (final ContentEntryEditor editor : myEntryToEditorMap.values()) {
          editor.update();
        }
      }
    };
    final VirtualFileManager fileManager = VirtualFileManager.getInstance();
    fileManager.addVirtualFileManagerListener(fileManagerListener);
    registerDisposable(new Disposable() {
      @Override
      public void dispose() {
        fileManager.removeVirtualFileManagerListener(fileManagerListener);
      }
    });
  }

  @Override
  protected ModifiableRootModel getModel() {
    return myState.getRootModel();
  }

  @Override
  public String getHelpTopic() {
    return "projectStructure.modules.sources";
  }

  @Override
  public String getDisplayName() {
    return NAME;
  }

  @Override
  public void disposeUIResources() {
    if (myRootTreeEditor != null) {
      myRootTreeEditor.setContentEntryEditor(null);
    }

    myEntryToEditorMap.clear();
    super.disposeUIResources();
  }

  @NotNull
  @Override
  public JPanel createComponentImpl() {
    final Module module = getModule();
    final Project project = module.getProject();

    myContentEntryEditorListener = new MyContentEntryEditorListener();

    final JPanel mainPanel = new JPanel(new BorderLayout());

    final JPanel entriesPanel = new JPanel(new BorderLayout());

    final DefaultActionGroup group = new DefaultActionGroup();
    final AddContentEntryAction action = new AddContentEntryAction();
    action.registerCustomShortcutSet(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK, mainPanel);
    group.add(action);

    myEditorsPanel = new ScrollablePanel(new VerticalStackLayout());
    myEditorsPanel.setBackground(BACKGROUND_COLOR);
    JScrollPane myScrollPane = ScrollPaneFactory.createScrollPane(myEditorsPanel, true);
    entriesPanel.add(new ToolbarPanel(myScrollPane, group), BorderLayout.CENTER);

    final JBSplitter splitter = new OnePixelSplitter(false);
    splitter.setProportion(0.6f);
    splitter.setHonorComponentsMinimumSize(true);

    myRootTreeEditor = new ContentEntryTreeEditor(project, myState);
    JComponent component = myRootTreeEditor.createComponent();
    component.setBorder(new CustomLineBorder(JBUI.scale(1),0,0,0));

    splitter.setFirstComponent(component);
    splitter.setSecondComponent(entriesPanel);
    JPanel contentPanel = new JPanel(new GridBagLayout());

    final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, myRootTreeEditor.getEditingActionsGroup(), true);
    contentPanel.add(new JLabel("Mark as:"),
                     new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, 0, new JBInsets(0, 5, 0, 5), 0,
                                            0));
    contentPanel.add(actionToolbar.getComponent(),
                     new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                                            new JBInsets(0, 0, 0, 0), 0, 0));
    contentPanel.add(splitter,
                     new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                                            new JBInsets(0, 0, 0, 0), 0, 0));

    mainPanel.add(contentPanel, BorderLayout.CENTER);

    final ModifiableRootModel model = getModel();
    if (model != null) {
      final ContentEntry[] contentEntries = model.getContentEntries();
      if (contentEntries.length > 0) {
        for (final ContentEntry contentEntry : contentEntries) {
          addContentEntryPanel(contentEntry);
        }
        selectContentEntry(contentEntries[0]);
      }
    }

    return mainPanel;
  }

  protected Module getModule() {
    return myModulesProvider.getModule(myModuleName);
  }

  protected void addContentEntryPanel(final ContentEntry contentEntry) {
    final ContentEntryEditor contentEntryEditor = createContentEntryEditor(contentEntry);
    contentEntryEditor.initUI();
    contentEntryEditor.addContentEntryEditorListener(myContentEntryEditorListener);
    registerDisposable(new Disposable() {
      @Override
      public void dispose() {
        contentEntryEditor.removeContentEntryEditorListener(myContentEntryEditorListener);
      }
    });
    myEntryToEditorMap.put(contentEntry, contentEntryEditor);
    final JComponent component = contentEntryEditor.getComponent();

    component.setBorder(JBUI.Borders.empty());
    myEditorsPanel.add(component);
  }

  protected ContentEntryEditor createContentEntryEditor(ContentEntry contentEntry) {
    return new ContentEntryEditor(contentEntry) {
      @Override
      protected ModifiableRootModel getModel() {
        return ContentEntriesEditor.this.getModel();
      }
    };
  }

  void selectContentEntry(@Nullable final ContentEntry contentEntryUrl) {
    if (mySelectedEntry != null && mySelectedEntry.equals(contentEntryUrl)) {
      return;
    }
    try {
      if (mySelectedEntry != null) {
        ContentEntryEditor editor = myEntryToEditorMap.get(mySelectedEntry);
        if (editor != null) {
          editor.setSelected(false);
        }
      }

      if (contentEntryUrl != null) {
        ContentEntryEditor editor = myEntryToEditorMap.get(contentEntryUrl);
        if (editor != null) {
          editor.setSelected(true);
          final JComponent component = editor.getComponent();
          final JComponent scroller = (JComponent)component.getParent();
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              scroller.scrollRectToVisible(component.getBounds());
            }
          });
          myRootTreeEditor.setContentEntryEditor(editor);
          myRootTreeEditor.requestFocus();
        }
      }
    }
    finally {
      mySelectedEntry = contentEntryUrl;
    }
  }

  @Override
  public void moduleStateChanged() {
    myEntryToEditorMap.clear();
    myEditorsPanel.removeAll();

    final ModifiableRootModel model = getModel();
    if (model != null) {
      final ContentEntry[] contentEntries = model.getContentEntries();
      if (contentEntries.length > 0) {
        for (final ContentEntry contentEntry : contentEntries) {
          addContentEntryPanel(contentEntry);
        }
        selectContentEntry(contentEntries[0]);
      }
      else {
        selectContentEntry(null);
        myRootTreeEditor.setContentEntryEditor(null);
      }
    }

    if(myRootTreeEditor != null) {
      myRootTreeEditor.update();
    }
  }

  @Nullable
  private ContentEntry getNextContentEntry(final ContentEntry contentEntryUrl) {
    return getAdjacentContentEntry(contentEntryUrl, 1);
  }

  @Nullable
  private ContentEntry getAdjacentContentEntry(final ContentEntry contentEntryUrl, int delta) {
    final ContentEntry[] contentEntries = getModel().getContentEntries();
    for (int idx = 0; idx < contentEntries.length; idx++) {
      ContentEntry entry = contentEntries[idx];
      if (contentEntryUrl.equals(entry)) {
        int nextEntryIndex = (idx + delta) % contentEntries.length;
        if (nextEntryIndex < 0) {
          nextEntryIndex += contentEntries.length;
        }
        return nextEntryIndex == idx ? null : contentEntries[nextEntryIndex];
      }
    }
    return null;
  }

  protected List<ContentEntry> addContentEntries(final VirtualFile[] files) {
    List<ContentEntry> contentEntries = new ArrayList<ContentEntry>();
    for (final VirtualFile file : files) {
      if (isAlreadyAdded(file)) {
        continue;
      }
      final ContentEntry contentEntry = getModel().addContentEntry(file);
      contentEntries.add(contentEntry);
    }
    return contentEntries;
  }

  private boolean isAlreadyAdded(VirtualFile file) {
    final VirtualFile[] contentRoots = getModel().getContentRoots();
    for (VirtualFile contentRoot : contentRoots) {
      if (contentRoot.equals(file)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void saveData() {
  }

  protected void addContentEntryPanels(ContentEntry[] contentEntriesArray) {
    for (ContentEntry contentEntry : contentEntriesArray) {
      addContentEntryPanel(contentEntry);
    }
    myEditorsPanel.revalidate();
    myEditorsPanel.repaint();
    selectContentEntry(contentEntriesArray[contentEntriesArray.length - 1]);
  }

  private final class MyContentEntryEditorListener extends ContentEntryEditorListenerAdapter {
    @Override
    public void editingStarted(@NotNull ContentEntryEditor editor) {
      selectContentEntry(editor.getContentEntry());
    }

    @Override
    public void beforeEntryDeleted(@NotNull ContentEntryEditor editor) {
      final ContentEntry entryUrl = editor.getContentEntry();
      if (mySelectedEntry != null && mySelectedEntry.equals(entryUrl)) {
        myRootTreeEditor.setContentEntryEditor(null);
      }
      final ContentEntry nextContentEntryUrl = getNextContentEntry(entryUrl);
      removeContentEntryPanel(entryUrl);
      selectContentEntry(nextContentEntryUrl);
      editor.removeContentEntryEditorListener(this);
    }

    @Override
    public void navigationRequested(@NotNull ContentEntryEditor editor, VirtualFile file) {
      if (mySelectedEntry != null && mySelectedEntry.equals(editor.getContentEntry())) {
        myRootTreeEditor.requestFocus();
        myRootTreeEditor.select(file);
      }
      else {
        selectContentEntry(editor.getContentEntry());
        myRootTreeEditor.requestFocus();
        myRootTreeEditor.select(file);
      }
    }

    private void removeContentEntryPanel(final ContentEntry contentEntry) {
      ContentEntryEditor editor = myEntryToEditorMap.get(contentEntry);
      if (editor != null) {
        myEditorsPanel.remove(editor.getComponent());
        myEntryToEditorMap.remove(contentEntry);
        myEditorsPanel.revalidate();
        myEditorsPanel.repaint();
      }
    }
  }

  private class AddContentEntryAction extends IconWithTextAction implements DumbAware {
    private final FileChooserDescriptor myDescriptor;

    public AddContentEntryAction() {
      super(ProjectBundle.message("module.paths.add.content.action"),
            ProjectBundle.message("module.paths.add.content.action.description"), AllIcons.Modules.AddContentEntry);
      myDescriptor = new FileChooserDescriptor(false, true, true, false, true, true) {
        @Override
        public void validateSelectedFiles(VirtualFile[] files) throws Exception {
          validateContentEntriesCandidates(files);
        }
      };
      myDescriptor.putUserData(LangDataKeys.MODULE_CONTEXT, getModule());
      myDescriptor.setTitle(ProjectBundle.message("module.paths.add.content.title"));
      myDescriptor.setDescription(ProjectBundle.message("module.paths.add.content.prompt"));
      myDescriptor.putUserData(FileChooserKeys.DELETE_ACTION_AVAILABLE, false);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      FileChooser.chooseFiles(myDescriptor, myProject, myLastSelectedDir, new Consumer<List<VirtualFile>>() {
        @Override
        public void consume(List<VirtualFile> files) {
          myLastSelectedDir = files.get(0);
          addContentEntries(VfsUtilCore.toVirtualFileArray(files));
        }
      });
    }

    private void validateContentEntriesCandidates(VirtualFile[] files) throws Exception {
      for (final VirtualFile file : files) {
        // check for collisions with already existing entries
        for (final ContentEntry contentEntry : myEntryToEditorMap.keySet()) {
          final VirtualFile contentEntryFile = contentEntry.getFile();
          if (contentEntryFile == null) {
            continue;  // skip invalid entry
          }
          if (contentEntryFile.equals(file)) {
            throw new Exception(ProjectBundle.message("module.paths.add.content.already.exists.error", file.getPresentableUrl()));
          }
          if (VfsUtilCore.isAncestor(contentEntryFile, file, true)) {
            // intersection not allowed
            throw new Exception(
              ProjectBundle.message("module.paths.add.content.intersect.error", file.getPresentableUrl(),
                                    contentEntryFile.getPresentableUrl()));
          }
          if (VfsUtilCore.isAncestor(file, contentEntryFile, true)) {
            // intersection not allowed
            throw new Exception(
              ProjectBundle.message("module.paths.add.content.dominate.error", file.getPresentableUrl(),
                                    contentEntryFile.getPresentableUrl()));
          }
        }
        // check if the same root is configured for another module
        final Module[] modules = myModulesProvider.getModules();
        for (final Module module : modules) {
          if (myModuleName.equals(module.getName())) {
            continue;
          }
          ModuleRootModel rootModel = myModulesProvider.getRootModel(module);
          LOG.assertTrue(rootModel != null);
          final VirtualFile[] moduleContentRoots = rootModel.getContentRoots();
          for (VirtualFile moduleContentRoot : moduleContentRoots) {
            if (file.equals(moduleContentRoot)) {
              throw new Exception(
                ProjectBundle.message("module.paths.add.content.duplicate.error", file.getPresentableUrl(), module.getName()));
            }
          }
        }
      }
    }

  }

}
