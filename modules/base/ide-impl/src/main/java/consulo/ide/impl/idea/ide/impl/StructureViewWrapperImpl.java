/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.impl;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.util.registry.Registry;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.*;
import consulo.fileEditor.impl.internal.FileEditorManagerImpl;
import consulo.fileEditor.structureView.*;
import consulo.ide.impl.idea.ide.structureView.newStructureView.StructureViewComponent;
import consulo.ide.localize.IdeLocalize;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.editor.structureView.StructureViewComposite;
import consulo.language.psi.PsiElement;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.CommonActionsManager;
import consulo.ui.ex.action.TimerListener;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerAdapter;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.internal.ToolWindowEx;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.dataholder.Key;
import consulo.util.lang.BitUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.RawFileLoader;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.List;

/**
 * @author Eugene Belyaev
 */
public class StructureViewWrapperImpl implements StructureViewWrapper, Disposable {
  private final Project myProject;
  private final ToolWindowEx myToolWindow;

  private VirtualFile myFile;

  private StructureView myStructureView;
  private FileEditor myFileEditor;
  private ModuleStructureComponent myModuleStructureComponent;

  private JPanel[] myPanels = new JPanel[0];
  private final MergingUpdateQueue myUpdateQueue;
  private static final Key<Object> ourDataSelectorKey = Key.create("DATA_SELECTOR");

  // -------------------------------------------------------------------------
  // Constructor
  // -------------------------------------------------------------------------

  private Runnable myPendingSelection;
  private boolean myFirstRun = true;

  public StructureViewWrapperImpl(Project project, ToolWindowEx toolWindow) {
    myProject = project;
    myToolWindow = toolWindow;

    myUpdateQueue = new MergingUpdateQueue("StructureView", Registry.intValue("structureView.coalesceTime"), false, myToolWindow.getComponent(), this, myToolWindow.getComponent(), true);
    myUpdateQueue.setRestartTimerOnAdd(true);

    TimerListener timerListener = new TimerListener() {
      @Override
      public IdeaModalityState getModalityState() {
        return IdeaModalityState.stateForComponent(myToolWindow.getComponent());
      }

      @Override
      public void run() {
        checkUpdate();
      }
    };
    ActionManager.getInstance().addTimerListener(500, timerListener);
    Disposer.register(this, () -> ActionManager.getInstance().removeTimerListener(timerListener));

    myToolWindow.getComponent().addHierarchyListener(e -> {
      if (BitUtil.isSet(e.getChangeFlags(), HierarchyEvent.DISPLAYABILITY_CHANGED)) {
        scheduleRebuild();
      }
    });
    myToolWindow.getContentManager().addContentManagerListener(new ContentManagerAdapter() {
      @Override
      public void selectionChanged(ContentManagerEvent event) {
        if (myStructureView instanceof StructureViewComposite) {
          StructureViewComposite.StructureViewDescriptor[] views = ((StructureViewComposite)myStructureView).getStructureViews();
          for (StructureViewComposite.StructureViewDescriptor view : views) {
            if (view.title.equals(event.getContent().getTabName())) {
              updateHeaderActions(view.structureView);
              break;
            }
          }
        }
      }
    });
    Disposer.register(myToolWindow.getContentManager(), this);
  }

  private void checkUpdate() {
    if (myProject.isDisposed()) return;

    Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    boolean insideToolwindow = SwingUtilities.isDescendingFrom(myToolWindow.getComponent(), owner);
    if (!myFirstRun && (insideToolwindow || JBPopupFactory.getInstance().isPopupActive())) {
      return;
    }

    DataContext dataContext = DataManager.getInstance().getDataContext(owner);
    if (dataContext.getData(ourDataSelectorKey) == this) return;
    if (dataContext.getData(Project.KEY) != myProject) return;

    VirtualFile[] files = hasFocus() ? null : dataContext.getData(VirtualFile.KEY_OF_ARRAY);
    if (!myToolWindow.isVisible()) {
      if (files != null && files.length > 0) {
        myFile = files[0];
      }
      return;
    }

    if (files != null && files.length == 1) {
      setFile(files[0]);
    }
    else if (files != null && files.length > 1) {
      setFile(null);
    } else if (myFirstRun) {
      FileEditorManagerImpl editorManager = (FileEditorManagerImpl)FileEditorManager.getInstance(myProject);
      List<Pair<VirtualFile, FileEditorWindow>> history = editorManager.getSelectionHistory();
      if (! history.isEmpty()) {
        setFile(history.get(0).getFirst());
      }
    }

    myFirstRun = false;
  }

  private boolean hasFocus() {
    JComponent tw = myToolWindow.getComponent();
    Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    while (owner != null) {
      if (owner == tw) return true;
      owner = owner.getParent();
    }
    return false;
  }

  private void setFile(VirtualFile file) {
    boolean forceRebuild = !Comparing.equal(file, myFile);
    if (!forceRebuild && myStructureView != null) {
      StructureViewModel model = myStructureView.getTreeModel();
      StructureViewTreeElement treeElement = model.getRoot();
      Object value = treeElement.getValue();
      if (value == null || value instanceof PsiElement && !((PsiElement)value).isValid()) {
        forceRebuild = true;
      }
    }
    if (forceRebuild) {
      myFile = file;
      scheduleRebuild();
    }
  }


  // -------------------------------------------------------------------------
  // StructureView interface implementation
  // -------------------------------------------------------------------------

  @Override
  public void dispose() {
    //we don't really need it
    //rebuild();
  }

  @Override
  public boolean selectCurrentElement(FileEditor fileEditor, VirtualFile file, boolean requestFocus) {
    //todo [kirillk]
    // this is dirty hack since some bright minds decided to used different TreeUi every time, so selection may be followed
    // by rebuild on completely different instance of TreeUi

    Runnable runnable = () -> {
      if (!Comparing.equal(myFileEditor, fileEditor)) {
        myFile = file;
        rebuild();
      }
      if (myStructureView != null) {
        myStructureView.navigateToSelectedElement(requestFocus);
      }
    };

    if (isStructureViewShowing()) {
      if (myUpdateQueue.isEmpty()) {
        runnable.run();
      } else {
        myPendingSelection = runnable;
      }
    } else {
      myPendingSelection = runnable;
    }

    return true;
  }

  private void scheduleRebuild() {
    myUpdateQueue.queue(new Update("rebuild") {
      @Override
      public void run() {
        if (myProject.isDisposed()) return;
        rebuild();
      }
    });
  }

  public void rebuild() {
    if (myProject.isDisposed()) return;

    Dimension referenceSize = null;

    if (myStructureView != null) {
      if (myStructureView instanceof StructureView.Scrollable) {
        referenceSize = ((StructureView.Scrollable)myStructureView).getCurrentSize();
      }

      myStructureView.storeState();
      Disposer.dispose(myStructureView);
      myStructureView = null;
      myFileEditor = null;
    }

    if (myModuleStructureComponent != null) {
      Disposer.dispose(myModuleStructureComponent);
      myModuleStructureComponent = null;
    }

    ContentManager contentManager = myToolWindow.getContentManager();
    contentManager.removeAllContents(true);
    if (!isStructureViewShowing()) {
      return;
    }

    VirtualFile file = myFile;
    if (file == null) {
      VirtualFile[] selectedFiles = FileEditorManager.getInstance(myProject).getSelectedFiles();
      if (selectedFiles.length > 0) {
        file = selectedFiles[0];
      }
    }

    String[] names = {""};
    if (file != null && file.isValid()) {
      if (file.isDirectory()) {
        if (ProjectRootsUtil.isModuleContentRoot(file, myProject)) {
          Module module = ModuleUtilCore.findModuleForFile(file, myProject);
          if (module != null) {
            myModuleStructureComponent = new ModuleStructureComponent(module);
            createSinglePanel(myModuleStructureComponent.getComponent());
            Disposer.register(this, myModuleStructureComponent);
          }
        }
      }
      else {
        FileEditor editor = FileEditorManager.getInstance(myProject).getSelectedEditor(file);
        boolean needDisposeEditor = false;
        if (editor == null) {
          editor = createTempFileEditor(file);
          needDisposeEditor = true;
        }
        if (editor != null && editor.isValid()) {
          StructureViewBuilder structureViewBuilder = editor.getStructureViewBuilder();
          if (structureViewBuilder != null) {
            myStructureView = structureViewBuilder.createStructureView(editor, myProject);
            myFileEditor = editor;
            Disposer.register(this, myStructureView);
            updateHeaderActions(myStructureView);

            if (myStructureView instanceof StructureView.Scrollable) {
              ((StructureView.Scrollable)myStructureView).setReferenceSizeWhileInitializing(referenceSize);
            }

            if (myStructureView instanceof StructureViewComposite) {
              StructureViewComposite composite = (StructureViewComposite)myStructureView;
              StructureViewComposite.StructureViewDescriptor[] views = composite.getStructureViews();
              myPanels = new JPanel[views.length];
              names = new String[views.length];
              for (int i = 0; i < myPanels.length; i++) {
                myPanels[i] = createContentPanel(views[i].structureView.getComponent());
                names[i] = views[i].title;
              }
            }
            else {
              createSinglePanel(myStructureView.getComponent());
            }

            myStructureView.restoreState();
            myStructureView.centerSelectedRow();
          }
        }
        if (needDisposeEditor && editor != null) {
          Disposer.dispose(editor);
        }
      }
    }

    if (myModuleStructureComponent == null && myStructureView == null) {
      createSinglePanel(new JLabel(IdeLocalize.messageNothingToShowInStructureView().get(), SwingConstants.CENTER));
    }

    for (int i = 0; i < myPanels.length; i++) {
      Content content = ContentFactory.getInstance().createContent(myPanels[i], names[i], false);
      contentManager.addContent(content);
      if (i == 0 && myStructureView != null) {
        Disposer.register(content, myStructureView);
      }
    }

    if (myPendingSelection != null) {
      Runnable selection = myPendingSelection;
      myPendingSelection = null;
      selection.run();
    }

  }

  private void updateHeaderActions(StructureView structureView) {
    AnAction[] titleActions = AnAction.EMPTY_ARRAY;
    if (structureView instanceof StructureViewComponent) {
      JTree tree = ((StructureViewComponent)structureView).getTree();
      titleActions = new AnAction[]{
              CommonActionsManager.getInstance().createExpandAllHeaderAction(tree),
              CommonActionsManager.getInstance().createCollapseAllHeaderAction(tree)};
    }
    myToolWindow.setTitleActions(titleActions);
  }

  private void createSinglePanel(JComponent component) {
    myPanels = new JPanel[1];
    myPanels[0] = createContentPanel(component);
  }

  private ContentPanel createContentPanel(JComponent component) {
    ContentPanel panel = new ContentPanel();
    panel.setBackground(UIUtil.getTreeTextBackground());
    panel.add(component, BorderLayout.CENTER);
    return panel;
  }

  @Nullable
  private FileEditor createTempFileEditor(@Nonnull VirtualFile file) {
    if (file.getLength() > RawFileLoader.getInstance().getMaxIntellisenseFileSize()) return null;

    FileEditorProviderManager editorProviderManager = FileEditorProviderManager.getInstance();
    FileEditorProvider[] providers = editorProviderManager.getProviders(myProject, file);
    return providers.length == 0 ? null : providers[0].createEditor(myProject, file);
  }


  protected boolean isStructureViewShowing() {
    ToolWindowManager windowManager = ToolWindowManager.getInstance(myProject);
    ToolWindow toolWindow = windowManager.getToolWindow(ToolWindowId.STRUCTURE_VIEW);
    // it means that window is registered
    return toolWindow != null && toolWindow.isVisible();
  }

  private class ContentPanel extends JPanel implements DataProvider {
    public ContentPanel() {
      super(new BorderLayout());
    }

    @Override
    public Object getData(@Nonnull @NonNls Key dataId) {
      if (dataId == ourDataSelectorKey) return StructureViewWrapperImpl.this;
      return null;
    }
  }
}
