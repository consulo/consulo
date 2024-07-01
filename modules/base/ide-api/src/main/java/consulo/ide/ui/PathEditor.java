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
package consulo.ide.ui;

import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.dataContext.DataManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDialog;
import consulo.fileChooser.IdeaFileChooser;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.ToolbarDecorator;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ListUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.http.HttpFileSystem;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

/**
 * @author MYakovlev
 */
public class PathEditor {
  private static final Logger LOG = Logger.getInstance(PathEditor.class);

  protected JComponent myComponent;
  private JBList myList;
  private final DefaultListModel myModel;
  private final Set<VirtualFile> myAllFiles = new HashSet<>();
  private boolean myModified = false;
  protected boolean myEnabled = false;
  private final FileChooserDescriptor myDescriptor;
  private VirtualFile myAddBaseDir;

  public PathEditor(final FileChooserDescriptor descriptor) {
    myDescriptor = descriptor;
    myDescriptor.putUserData(FileChooserDialog.PREFER_LAST_OVER_TO_SELECT, Boolean.TRUE);
    myModel = createListModel();
  }

  public void setAddBaseDir(@Nullable VirtualFile addBaseDir) {
    myAddBaseDir = addBaseDir;
  }

  protected void setEnabled(boolean enabled) {
    myEnabled = enabled;
  }

  protected void setModified(boolean modified) {
    myModified = modified;
  }

  public boolean isModified() {
    return myModified;
  }

  public VirtualFile[] getRoots() {
    final int count = getRowCount();
    if (count == 0) {
      return VirtualFile.EMPTY_ARRAY;
    }
    final VirtualFile[] roots = new VirtualFile[count];
    for (int i = 0; i < count; i++) {
      roots[i] = getValueAt(i);
    }
    return roots;
  }

  public void resetPath(@Nonnull List<VirtualFile> paths) {
    keepSelectionState();
    clearList();
    setEnabled(true);
    for (VirtualFile file : paths) {
      addElement(file);
    }
    setModified(false);
  }

  protected boolean isImmutable() {
    return false;
  }

  public JComponent createComponent() {
    myList = new JBList(getListModel());
    myList.setCellRenderer(createListCellRenderer(myList));

    if (isImmutable()) {
      myComponent = ScrollPaneFactory.createScrollPane(myList, true);
    }
    else {
      ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(myList)
        .disableUpDownActions()
        .setAddAction(button-> {
          final VirtualFile[] added = doAdd();
          if (added.length > 0) {
            setModified(true);
          }
          requestDefaultFocus();
          setSelectedRoots(added);
        })
        .setRemoveAction(button-> {
          int[] idxs = myList.getSelectedIndices();
          doRemoveItems(idxs, myList);
        })
        .setAddActionUpdater(e-> myEnabled)
        .setRemoveActionUpdater(e-> {
          Object[] values = getSelectedRoots();
          return values.length > 0 && myEnabled;
        });

      addToolbarButtons(toolbarDecorator);

      myComponent = toolbarDecorator.createPanel();
      myComponent.setBorder(null);
    }

    return myComponent;
  }

  protected void addToolbarButtons(ToolbarDecorator toolbarDecorator) {
  }

  protected void doRemoveItems(int[] idxs, JList list) {
    List removedItems = ListUtil.removeIndices(list, idxs);
    itemsRemoved(removedItems);
  }

  protected DefaultListModel createListModel() {
    return new DefaultListModel();
  }

  protected ListCellRenderer createListCellRenderer(JBList list) {
    return new MyCellRenderer();
  }

  protected void itemsRemoved(List removedItems) {
    myAllFiles.removeAll(removedItems);
    if (removedItems.size() > 0) {
      setModified(true);
    }
    requestDefaultFocus();
  }

  private VirtualFile[] doAdd() {
    VirtualFile baseDir = myAddBaseDir;
    Project project = DataManager.getInstance().getDataContext(myComponent).getData(Project.KEY);
    if (baseDir == null && project != null) {
      baseDir = project.getBaseDir();
    }
    VirtualFile[] files = IdeaFileChooser.chooseFiles(myDescriptor, myComponent, project, baseDir);
    files = adjustAddedFileSet(myComponent, files);
    List<VirtualFile> added = new ArrayList<>(files.length);
    for (VirtualFile vFile : files) {
      if (addElement(vFile)) {
        added.add(vFile);
      }
    }
    return VirtualFileUtil.toVirtualFileArray(added);
  }

  /**
   * Implement this method to adjust adding behavior, this method is called right after the files
   * or directories are selected for added. This method allows adding UI that modify file set.
   * <p/>
   * The default implementation returns a value passed the parameter files and does nothing.
   *
   * @param component a component that could be used as a parent.
   * @param files     a selected file set
   * @return adjusted file set
   */
  protected VirtualFile[] adjustAddedFileSet(final Component component, final VirtualFile[] files) {
    return files;
  }

  protected boolean isUrlInserted() {
    return getRowCount() > 0 && ((VirtualFile)getListModel().lastElement()).getFileSystem() instanceof HttpFileSystem;
  }

  protected void requestDefaultFocus() {
    if (myList != null) {
      IdeFocusManager.getGlobalInstance()
        .doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myList, true));
    }
  }

  public void addPaths(VirtualFile... paths) {
    boolean added = false;
    keepSelectionState();
    for (final VirtualFile path : paths) {
      if (addElement(path)) {
        added = true;
      }
    }
    if (added) {
      setModified(true);
    }
  }

  public void removePaths(VirtualFile... paths) {
    final Set<VirtualFile> pathsSet = new HashSet<>(Arrays.asList(paths));
    int size = getRowCount();
    final IntList indicesToRemove = IntLists.newArrayList(paths.length);
    for (int idx = 0; idx < size; idx++) {
      VirtualFile path = getValueAt(idx);
      if (pathsSet.contains(path)) {
        indicesToRemove.add(idx);
      }
    }
    final List list = ListUtil.removeIndices(myList, indicesToRemove.toArray());
    itemsRemoved(list);
  }

  /**
   * Method adds element only if it is not added yet.
   */
  protected boolean addElement(VirtualFile item) {
    if (item == null) {
      return false;
    }
    if (myAllFiles.contains(item)) {
      return false;
    }
    if (isUrlInserted()) {
      getListModel().insertElementAt(item, getRowCount() - 1);
    }
    else {
      getListModel().addElement(item);
    }
    myAllFiles.add(item);
    return true;
  }

  protected DefaultListModel getListModel() {
    return myModel;
  }

  protected void setSelectedRoots(Object[] roots) {
    ArrayList<Object> rootsList = new ArrayList<>(roots.length);
    for (Object root : roots) {
      if (root != null) {
        rootsList.add(root);
      }
    }
    myList.getSelectionModel().clearSelection();
    int rowCount = getRowCount();
    for (int i = 0; i < rowCount; i++) {
      Object currObject = getValueAt(i);
      LOG.assertTrue(currObject != null);
      if (rootsList.contains(currObject)) {
        myList.getSelectionModel().addSelectionInterval(i, i);
      }
    }
  }

  private void keepSelectionState() {
    final Object[] selectedItems = getSelectedRoots();

    SwingUtilities.invokeLater(() -> {
      if (selectedItems != null) {
        setSelectedRoots(selectedItems);
      }
    });
  }

  protected Object[] getSelectedRoots() {
    return myList.getSelectedValues();
  }

  protected int getRowCount() {
    return getListModel().getSize();
  }

  protected VirtualFile getValueAt(int row) {
    return (VirtualFile)getListModel().get(row);
  }

  public void clearList() {
    getListModel().clear();
    myAllFiles.clear();
    setModified(true);
  }

  @Nullable
  private static FileType findFileType(final VirtualFile file) {
    return Application.get().runReadAction((Supplier<FileType>)() -> {
      VirtualFile tempFile = file;
      if ((file.getFileSystem() instanceof ArchiveFileSystem) && file.getParent() == null) {
        //[myakovlev] It was bug - directories with *.jar extensions was saved as files of JarFileSystem.
        //    so we can not just return true, we should filter such directories.
        String path = file.getPath().substring(0, file.getPath().length() - ArchiveFileSystem.ARCHIVE_SEPARATOR.length());
        tempFile = LocalFileSystem.getInstance().findFileByPath(path);
      }
      if (tempFile != null && !tempFile.isDirectory()) {
        return tempFile.getFileType();
      }
      return null;
    });
  }

  /**
   * @return icon for displaying parameter (ProjectRoot or VirtualFile)
   */
  private static consulo.ui.image.Image getIconForRoot(Object projectRoot) {
    if (projectRoot instanceof VirtualFile) {
      final VirtualFile file = (VirtualFile)projectRoot;
      if (!file.isValid()) {
        return AllIcons.Nodes.PpInvalid;
      }
      else if (isHttpRoot(file)) {
        return AllIcons.Nodes.PpWeb;
      }
      else {
        FileType fileType = findFileType(file);
        if(fileType instanceof ArchiveFileType) {
          return fileType.getIcon();
        }
        else if(file.isDirectory()) {
          return AllIcons.Nodes.Folder;
        }
        return file.getFileType().getIcon();
      }
    }
    return Image.empty(Image.DEFAULT_ICON_SIZE);
  }

  private static boolean isHttpRoot(VirtualFile virtualFileOrProjectRoot) {
    return virtualFileOrProjectRoot != null && (virtualFileOrProjectRoot.getFileSystem() instanceof HttpFileSystem);
  }

  private final class MyCellRenderer extends DefaultListCellRenderer {
    private String getPresentableString(final Object value) {
      return Application.get().runReadAction((Supplier<String>)() -> {
        //noinspection HardCodedStringLiteral
        return (value instanceof VirtualFile virtualFile) ? virtualFile.getPresentableUrl() : "UNKNOWN OBJECT";
      });
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, getPresentableString(value), index, isSelected, cellHasFocus);
      if (isSelected) {
        setForeground(UIUtil.getListSelectionForeground());
      }
      else {
        if (value instanceof VirtualFile) {
          VirtualFile file = (VirtualFile)value;
          if (!file.isValid()) {
            setForeground(JBColor.RED);
          }
        }
      }
      setIcon(TargetAWT.to(getIconForRoot(value)));
      return this;
    }
  }
}
