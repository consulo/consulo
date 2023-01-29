/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.fileChooser.impl;

import consulo.application.SaveAndSyncHandler;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.UIBundle;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.NodeRenderer;
import consulo.project.ui.wm.event.ApplicationActivationListener;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.ide.impl.idea.openapi.fileChooser.FileSystemTree;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileNodeDescriptor;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileSystemTreeImpl;
import consulo.ide.impl.idea.openapi.fileChooser.ex.PathField;
import consulo.ide.impl.idea.openapi.fileChooser.impl.FileChooserUtil;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.action.*;
import consulo.ui.ex.JBColor;
import consulo.component.util.Iconable;
import consulo.application.util.registry.Registry;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.dataContext.DataProvider;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awt.util.Update;
import consulo.application.ApplicationManager;
import consulo.component.ComponentManager;
import consulo.desktop.awt.application.DesktopSaveAndSyncHandlerImpl;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDialog;
import consulo.fileChooser.IdeaFileChooser;
import consulo.fileChooser.PathChooserDialog;
import consulo.ide.impl.fileChooser.FileChooserFactoryImpl;
import consulo.ide.impl.VfsIconUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public class FileChooserDialogImpl extends DialogWrapper implements FileChooserDialog, PathChooserDialog, FileLookup {
  public static final String FILE_CHOOSER_SHOW_PATH_PROPERTY = "FileChooser.ShowPath";
  public static final String RECENT_FILES_KEY = "file.chooser.recent.files";
  public static final String DRAG_N_DROP_HINT = "Drag and drop a file into the space above to quickly locate it in the tree";
  private final FileChooserDescriptor myChooserDescriptor;
  protected FileSystemTreeImpl myFileSystemTree;
  private Project myProject;
  private VirtualFile[] myChosenFiles = VirtualFile.EMPTY_ARRAY;

  private JPanel myNorthPanel;
  private TextFieldAction myTextFieldAction;
  protected FileTextFieldImpl myPathTextField;
  private ComboBox<String> myPath;

  private MergingUpdateQueue myUiUpdater;
  private boolean myTreeIsUpdating;

  public FileChooserDialogImpl(@Nonnull final FileChooserDescriptor descriptor, @Nullable Project project) {
    super(project, true);
    myChooserDescriptor = descriptor;
    myProject = project;
    setTitle(getChooserTitle(descriptor));
  }

  public FileChooserDialogImpl(@Nonnull final FileChooserDescriptor descriptor, @Nonnull Component parent) {
    this(descriptor, parent, null);
  }

  public FileChooserDialogImpl(@Nonnull final FileChooserDescriptor descriptor, @Nonnull Component parent, @Nullable Project project) {
    super(parent, true);
    myChooserDescriptor = descriptor;
    myProject = project;
    setTitle(getChooserTitle(descriptor));
  }

  private static String getChooserTitle(final FileChooserDescriptor descriptor) {
    final String title = descriptor.getTitle();
    return title != null ? title : UIBundle.message("file.chooser.default.title");
  }

  @Override
  @Nonnull
  public VirtualFile[] choose(@Nullable final ComponentManager project, @Nonnull final VirtualFile... toSelect) {
    init();
    if ((myProject == null) && (project != null)) {
      myProject = (Project)project;
    }
    if (toSelect.length == 1) {
      restoreSelection(toSelect[0]);
    }
    else if (toSelect.length == 0) {
      restoreSelection(null); // select last opened file
    }
    else {
      selectInTree(toSelect, true);
    }

    show();

    return myChosenFiles;
  }

  @Override
  public void choose(@Nullable VirtualFile toSelect, @Nonnull Consumer<List<VirtualFile>> callback) {
    init();
    restoreSelection(toSelect);
    show();
    if (myChosenFiles.length > 0) {
      callback.accept(Arrays.asList(myChosenFiles));
    }
    else if (callback instanceof IdeaFileChooser.FileChooserConsumer) {
      ((IdeaFileChooser.FileChooserConsumer)callback).cancelled();
    }
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public AsyncResult<VirtualFile[]> chooseAsync(@Nullable ComponentManager project, @Nonnull VirtualFile[] toSelect) {
    init();
    if ((myProject == null) && (project != null)) {
      myProject = (Project)project;
    }
    if (toSelect.length == 1) {
      restoreSelection(toSelect[0]);
    }
    else if (toSelect.length == 0) {
      restoreSelection(null); // select last opened file
    }
    else {
      selectInTree(toSelect, true);
    }

    AsyncResult<VirtualFile[]> result = AsyncResult.undefined();
    AsyncResult<Void> showAsync = showAsync();
    showAsync.doWhenDone(() -> {
      if (myChosenFiles.length > 0) {
        result.setDone(myChosenFiles);
      }
      else {
        result.setRejected();
      }
    });
    showAsync.doWhenRejected((Runnable)result::setRejected);
    return result;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<VirtualFile[]> chooseAsync(@Nullable VirtualFile toSelect) {
    init();
    restoreSelection(toSelect);

    AsyncResult<VirtualFile[]> result = AsyncResult.undefined();
    AsyncResult<Void> showAsync = showAsync();
    showAsync.doWhenDone(() -> {
      if (myChosenFiles.length > 0) {
        result.setDone(myChosenFiles);
      }
      else {
        result.setRejected();
      }
    });
    showAsync.doWhenRejected((Runnable)result::setRejected);
    return result;
  }

  protected void restoreSelection(@Nullable VirtualFile toSelect) {
    final VirtualFile lastOpenedFile = FileChooserUtil.getLastOpenedFile(myProject);
    final VirtualFile file = FileChooserUtil.getFileToSelect(myChooserDescriptor, myProject, toSelect, lastOpenedFile);

    if (file != null && file.isValid()) {
      myFileSystemTree.select(file, new Runnable() {
        public void run() {
          if (!file.equals(myFileSystemTree.getSelectedFile())) {
            VirtualFile parent = file.getParent();
            if (parent != null) {
              myFileSystemTree.select(parent, null);
            }
          }
          else if (file.isDirectory()) {
            myFileSystemTree.expand(file, null);
          }
        }
      });
    }
  }

  protected void storeSelection(@Nullable VirtualFile file) {
    FileChooserUtil.setLastOpenedFile(myProject, file);
    if (file != null && file.getFileSystem() instanceof LocalFileSystem) {
      saveRecent(file.getPath());
    }
  }

  protected void saveRecent(String path) {
    final List<String> files = new ArrayList<>(Arrays.asList(getRecentFiles()));
    files.remove(path);
    files.add(0, path);
    while (files.size() > 30) {
      files.remove(files.size() - 1);
    }
    PropertiesComponent.getInstance().setValues(RECENT_FILES_KEY, ArrayUtil.toStringArray(files));
  }

  @Nonnull
  private String[] getRecentFiles() {
    String[] array = PropertiesComponent.getInstance().getValues(RECENT_FILES_KEY);
    if (array == null) {
      return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    if (array.length > 0 && myPathTextField != null && myPathTextField.getField().getText().replace('\\', '/').equals(array[0])) {
      return Arrays.copyOfRange(array, 1, array.length);
    }
    return array;
  }

  protected DefaultActionGroup createActionGroup() {
    registerFileChooserShortcut(IdeActions.ACTION_DELETE, "FileChooser.Delete");
    registerFileChooserShortcut(IdeActions.ACTION_SYNCHRONIZE, "FileChooser.Refresh");

    return (DefaultActionGroup)ActionManager.getInstance().getAction("FileChooserToolbar");
  }

  private void registerFileChooserShortcut(final String baseActionId, final String fileChooserActionId) {
    final JTree tree = myFileSystemTree.getTree();
    final AnAction syncAction = ActionManager.getInstance().getAction(fileChooserActionId);

    AnAction original = ActionManager.getInstance().getAction(baseActionId);
    syncAction.registerCustomShortcutSet(original.getShortcutSet(), tree, myDisposable);
  }

  @Nullable
  protected final JComponent createTitlePane() {
    final String description = myChooserDescriptor.getDescription();
    if (StringUtil.isEmptyOrSpaces(description)) return null;

    final JLabel label = new JLabel(description);
    label.setBorder(BorderFactory.createCompoundBorder(new SideBorder(UIUtil.getPanelBackground().darker(), SideBorder.BOTTOM), JBUI.Borders.empty(0, 5, 10, 5)));
    return label;
  }

  protected JComponent createCenterPanel() {
    JPanel panel = new MyPanel();

    myUiUpdater = new MergingUpdateQueue("FileChooserUpdater", 200, false, panel);
    Disposer.register(myDisposable, myUiUpdater);
    new UiNotifyConnector(panel, myUiUpdater);

    panel.setBorder(JBUI.Borders.empty());

    createTree();

    final DefaultActionGroup group = createActionGroup();
    ActionToolbar toolBar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
    toolBar.setTargetComponent(panel);

    final JPanel toolbarPanel = new JPanel(new BorderLayout());
    toolbarPanel.add(toolBar.getComponent(), BorderLayout.CENTER);

    myTextFieldAction = new TextFieldAction() {
      public void linkSelected(final LinkLabel aSource, final Object aLinkData) {
        toggleShowTextField();
      }
    };
    toolbarPanel.add(myTextFieldAction, BorderLayout.EAST);
    JPanel extraToolbarPanel = createExtraToolbarPanel();
    if (extraToolbarPanel != null) {
      toolbarPanel.add(extraToolbarPanel, BorderLayout.SOUTH);
    }

    myPath = new ComboBox<>(getRecentFiles());
    myPath.setEditable(true);
    myPath.setRenderer(SimpleListCellRenderer.<String>create((label, value, index) -> {
      label.setText(value);
      VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(value));
      label.setIcon(file == null ? Image.empty(Image.DEFAULT_ICON_SIZE) : VfsIconUtil.getIcon(file, Iconable.ICON_FLAG_READ_STATUS, null));
    }));

    myPathTextField = new FileTextFieldImpl.Vfs((JTextField)myPath.getEditor().getEditorComponent(), FileChooserFactoryImpl.getMacroMap(), getDisposable(),
                                                new LocalFsFinder.FileChooserFilter(myChooserDescriptor, myFileSystemTree)) {
      @Override
      protected void onTextChanged(final String newValue) {
        myUiUpdater.cancelAllUpdates();
        updateTreeFromPath(newValue);
      }
    };
    Disposer.register(myDisposable, myPathTextField);

    myNorthPanel = new JPanel(new BorderLayout());
    myNorthPanel.add(toolbarPanel, BorderLayout.NORTH);

    updateTextFieldShowing();

    panel.add(myNorthPanel, BorderLayout.NORTH);

    registerMouseListener(group);

    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myFileSystemTree.getTree());
    //scrollPane.setBorder(BorderFactory.createLineBorder(new Color(148, 154, 156)));
    panel.add(scrollPane, BorderLayout.CENTER);
    panel.setPreferredSize(JBUI.size(400));


    JLabel hintLabel = new JLabel(DRAG_N_DROP_HINT, SwingConstants.CENTER);
    hintLabel.setForeground(JBColor.gray);
    hintLabel.setFont(JBUI.Fonts.smallFont());
    panel.add(hintLabel, BorderLayout.SOUTH);

    ApplicationManager.getApplication().getMessageBus().connect(getDisposable()).subscribe(ApplicationActivationListener.class, new ApplicationActivationListener() {
      @Override
      public void applicationActivated(IdeFrame ideFrame) {
        ((DesktopSaveAndSyncHandlerImpl)SaveAndSyncHandler.getInstance()).maybeRefresh(IdeaModalityState.current());
      }
    });

    return panel;
  }

  @Nullable
  protected JPanel createExtraToolbarPanel() {
    return null;
  }

  @RequiredUIAccess
  public JComponent getPreferredFocusedComponent() {
    if (isToShowTextField()) {
      return myPathTextField != null ? myPathTextField.getField() : null;
    }
    else {
      return myFileSystemTree != null ? myFileSystemTree.getTree() : null;
    }
  }

  public final void dispose() {
    LocalFileSystem.getInstance().removeWatchedRoots(myRequests.values());
    super.dispose();
  }

  private boolean isTextFieldActive() {
    return myPathTextField.getField().getRootPane() != null;
  }

  protected void doOKAction() {
    if (!isOKActionEnabled()) {
      return;
    }

    if (isTextFieldActive()) {
      final String text = myPathTextField.getTextFieldText();
      final LookupFile file = myPathTextField.getFile();
      if (text == null || file == null || !file.exists()) {
        setErrorText("Specified path cannot be found");
        return;
      }
    }

    final List<VirtualFile> selectedFiles = Arrays.asList(getSelectedFilesInt());
    final VirtualFile[] files = VfsUtilCore.toVirtualFileArray(FileChooserUtil.getChosenFiles(myChooserDescriptor, selectedFiles));
    if (files.length == 0) {
      myChosenFiles = VirtualFile.EMPTY_ARRAY;
      close(CANCEL_EXIT_CODE);
      return;
    }

    try {
      myChooserDescriptor.validateSelectedFiles(files);
    }
    catch (Exception e) {
      Messages.showErrorDialog(getContentPane(), e.getMessage(), getTitle());
      return;
    }

    myChosenFiles = files;
    storeSelection(files[files.length - 1]);

    super.doOKAction();
  }

  public final void doCancelAction() {
    myChosenFiles = VirtualFile.EMPTY_ARRAY;
    super.doCancelAction();
  }

  protected JTree createTree() {
    Tree internalTree = createInternalTree();
    myFileSystemTree = new FileSystemTreeImpl(myProject, myChooserDescriptor, internalTree, null, null, null);
    internalTree.setRootVisible(myChooserDescriptor.isTreeRootVisible());
    internalTree.setShowsRootHandles(true);
    Disposer.register(myDisposable, myFileSystemTree);

    myFileSystemTree.addOkAction(new Runnable() {
      public void run() {
        doOKAction();
      }
    });
    JTree tree = myFileSystemTree.getTree();
    if (!Registry.is("file.chooser.async.tree.model")) tree.setCellRenderer(new NodeRenderer());
    tree.getSelectionModel().addTreeSelectionListener(new FileTreeSelectionListener());
    tree.addTreeExpansionListener(new FileTreeExpansionListener());
    setOKActionEnabled(false);

    myFileSystemTree.addListener(new FileSystemTree.Listener() {
      public void selectionChanged(final List<? extends VirtualFile> selection) {
        updatePathFromTree(selection, false);
      }
    }, myDisposable);

    new FileDrop(tree, new FileDrop.Target() {
      public FileChooserDescriptor getDescriptor() {
        return myChooserDescriptor;
      }

      public boolean isHiddenShown() {
        return myFileSystemTree.areHiddensShown();
      }

      public void dropFiles(final List<VirtualFile> files) {
        if (!myChooserDescriptor.isChooseMultiple() && files.size() > 0) {
          selectInTree(new VirtualFile[]{files.get(0)}, true);
        }
        else {
          selectInTree(VfsUtilCore.toVirtualFileArray(files), true);
        }
      }
    });

    return tree;
  }

  @Nonnull
  protected Tree createInternalTree() {
    return new Tree();
  }

  protected final void registerMouseListener(final ActionGroup group) {
    myFileSystemTree.registerMouseListener(group);
  }

  private VirtualFile[] getSelectedFilesInt() {
    if (myTreeIsUpdating || !myUiUpdater.isEmpty()) {
      if (isTextFieldActive() && !StringUtil.isEmpty(myPathTextField.getTextFieldText())) {
        LookupFile toFind = myPathTextField.getFile();
        if (toFind instanceof LocalFsFinder.VfsFile && toFind.exists()) {
          VirtualFile file = ((LocalFsFinder.VfsFile)toFind).getFile();
          if (file != null) {
            return new VirtualFile[]{file};
          }
        }
      }
      return VirtualFile.EMPTY_ARRAY;
    }

    return myFileSystemTree.getSelectedFiles();
  }

  private final Map<String, LocalFileSystem.WatchRequest> myRequests = new HashMap<>();

  private static boolean isToShowTextField() {
    return PropertiesComponent.getInstance().getBoolean(FILE_CHOOSER_SHOW_PATH_PROPERTY, true);
  }

  private static void setToShowTextField(boolean toShowTextField) {
    PropertiesComponent.getInstance().setValue(FILE_CHOOSER_SHOW_PATH_PROPERTY, Boolean.toString(toShowTextField));
  }

  private final class FileTreeExpansionListener implements TreeExpansionListener {
    public void treeExpanded(TreeExpansionEvent event) {
      final Object[] path = event.getPath().getPath();
      if (path.length == 2 && path[1] instanceof DefaultMutableTreeNode) {
        // top node has been expanded => watch disk recursively
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)path[1];
        Object userObject = node.getUserObject();
        if (userObject instanceof FileNodeDescriptor) {
          final VirtualFile file = ((FileNodeDescriptor)userObject).getElement().getFile();
          if (file != null && file.isDirectory()) {
            final String rootPath = file.getPath();
            if (myRequests.get(rootPath) == null) {
              final LocalFileSystem.WatchRequest watchRequest = LocalFileSystem.getInstance().addRootToWatch(rootPath, true);
              myRequests.put(rootPath, watchRequest);
            }
          }
        }
      }
    }

    public void treeCollapsed(TreeExpansionEvent event) {
    }
  }

  private final class FileTreeSelectionListener implements TreeSelectionListener {
    public void valueChanged(TreeSelectionEvent e) {
      TreePath[] paths = e.getPaths();

      boolean enabled = true;
      for (TreePath treePath : paths) {
        if (e.isAddedPath(treePath)) {
          VirtualFile file = FileSystemTreeImpl.getVirtualFile(treePath);
          if (file == null || !myChooserDescriptor.isFileSelectable(file)) enabled = false;
        }
      }
      setOKActionEnabled(enabled);
    }
  }

  protected final class MyPanel extends JPanel implements DataProvider {
    public MyPanel() {
      super(new BorderLayout(0, 0));
    }

    public Object getData(@Nonnull Key<?> dataId) {
      if (CommonDataKeys.VIRTUAL_FILE_ARRAY == dataId) {
        return myFileSystemTree.getSelectedFiles();
      }
      else if (PathField.PATH_FIELD == dataId) {
        return (PathField)() -> toggleShowTextField();
      }
      else if (FileSystemTree.DATA_KEY == dataId) {
        return myFileSystemTree;
      }
      return myChooserDescriptor.getUserData(dataId);
    }
  }

  public void toggleShowTextField() {
    setToShowTextField(!isToShowTextField());
    updateTextFieldShowing();
  }

  private void updateTextFieldShowing() {
    myTextFieldAction.update();
    myNorthPanel.remove(myPath);
    if (isToShowTextField()) {
      final ArrayList<VirtualFile> selection = new ArrayList<>();
      if (myFileSystemTree.getSelectedFile() != null) {
        selection.add(myFileSystemTree.getSelectedFile());
      }
      updatePathFromTree(selection, true);
      myNorthPanel.add(myPath, BorderLayout.CENTER);
    }
    else {
      setErrorText(null);
    }
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
      IdeFocusManager.getGlobalInstance().requestFocus(myPathTextField.getField(), true);
    });

    myNorthPanel.revalidate();
    myNorthPanel.repaint();
  }


  private void updatePathFromTree(final List<? extends VirtualFile> selection, boolean now) {
    if (!isToShowTextField() || myTreeIsUpdating) return;

    String text = "";
    if (selection.size() > 0) {
      text = VfsUtil.getReadableUrl(selection.get(0));
    }
    else {
      final List<VirtualFile> roots = myChooserDescriptor.getRoots();
      if (!myFileSystemTree.getTree().isRootVisible() && roots.size() == 1) {
        text = VfsUtil.getReadableUrl(roots.get(0));
      }
    }

    myPathTextField.setText(text, now, new Runnable() {
      public void run() {
        myPathTextField.getField().selectAll();
        setErrorText(null);
      }
    });
  }

  private void updateTreeFromPath(final String text) {
    if (!isToShowTextField()) return;
    if (myPathTextField.isPathUpdating()) return;
    if (text == null) return;

    myUiUpdater.queue(new Update("treeFromPath.1") {
      public void run() {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
          public void run() {
            final LocalFsFinder.VfsFile toFind = (LocalFsFinder.VfsFile)myPathTextField.getFile();
            if (toFind == null || !toFind.exists()) return;

            myUiUpdater.queue(new Update("treeFromPath.2") {
              public void run() {
                selectInTree(toFind.getFile(), text);
              }
            });
          }
        });
      }
    });
  }

  private void selectInTree(final VirtualFile vFile, String fromText) {
    if (vFile != null && vFile.isValid()) {
      if (fromText == null || fromText.equalsIgnoreCase(myPathTextField.getTextFieldText())) {
        selectInTree(new VirtualFile[]{vFile}, false);
      }
    }
    else {
      reportFileNotFound();
    }
  }

  private void selectInTree(final VirtualFile[] array, final boolean requestFocus) {
    myTreeIsUpdating = true;
    final List<VirtualFile> fileList = Arrays.asList(array);
    if (!Arrays.asList(myFileSystemTree.getSelectedFiles()).containsAll(fileList)) {
      myFileSystemTree.select(array, new Runnable() {
        public void run() {
          if (!myFileSystemTree.areHiddensShown() && !Arrays.asList(myFileSystemTree.getSelectedFiles()).containsAll(fileList)) {
            myFileSystemTree.showHiddens(true);
            selectInTree(array, requestFocus);
            return;
          }

          myTreeIsUpdating = false;
          setErrorText(null);
          if (requestFocus) {
            IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myFileSystemTree.getTree());
          }
        }
      });
    }
    else {
      myTreeIsUpdating = false;
      setErrorText(null);
    }
  }

  private void reportFileNotFound() {
    myTreeIsUpdating = false;
    setErrorText(null);
  }

  @Override
  protected String getDimensionServiceKey() {
    return "FileChooserDialogImpl";
  }

  @Override
  protected String getHelpId() {
    return "platform/dialogs/open.dialog";
  }
}
