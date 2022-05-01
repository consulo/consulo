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
package consulo.ide.impl.idea.util.ui;

import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.application.util.SystemInfo;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.ide.impl.idea.util.Consumer;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.IdeaFileChooser;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LocalPathCellEditor extends AbstractTableCellEditor {
  private final String myTitle;
  private final Project myProject;

  private FileChooserDescriptor myFileChooserDescriptor;
  private boolean myNormalizePath;

  protected CellEditorComponentWithBrowseButton<JTextField> myComponent;

  public LocalPathCellEditor(@Nullable String title, @Nullable Project project) {
    myTitle = title;
    myProject = project;
  }

  public LocalPathCellEditor(@Nullable Project project) {
    this(null, project);
  }

  public LocalPathCellEditor() {
    this(null, null);
  }

  public LocalPathCellEditor fileChooserDescriptor(@Nonnull FileChooserDescriptor value) {
    myFileChooserDescriptor = value;
    return this;
  }

  /**
   * If true, path will be nullified and converted to system dependent
   */
  public LocalPathCellEditor normalizePath(boolean value) {
    myNormalizePath = value;
    return this;
  }

  @Override
  public Object getCellEditorValue() {
    String value = myComponent.getChildComponent().getText();
    return myNormalizePath ? PathUtil.toSystemDependentName(StringUtil.nullize(value)) : value;
  }

  @Override
  public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, final int row, int column) {
    myComponent = new CellEditorComponentWithBrowseButton<JTextField>(new TextFieldWithBrowseButton(createActionListener(table)), this);
    myComponent.getChildComponent().setText((String)value);
    return myComponent;
  }

  protected ActionListener createActionListener(final JTable table) {
    return new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String initial = (String)getCellEditorValue();
        VirtualFile initialFile = StringUtil.isNotEmpty(initial) ? LocalFileSystem.getInstance().findFileByPath(initial) : null;
        IdeaFileChooser.chooseFile(getFileChooserDescriptor(), myProject, table, initialFile, new Consumer<VirtualFile>() {
          @Override
          public void consume(VirtualFile file) {
            String path = file.getPresentableUrl();
            if (SystemInfo.isWindows && path.length() == 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':') {
              path += "\\"; // make path absolute
            }
            myComponent.getChildComponent().setText(path);
          }
        });
      }
    };
  }

  public FileChooserDescriptor getFileChooserDescriptor() {
    if (myFileChooserDescriptor == null) {
      myFileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
      if (myTitle != null) {
        myFileChooserDescriptor.setTitle(myTitle);
      }
      myFileChooserDescriptor.setShowFileSystemRoots(true);
    }
    return myFileChooserDescriptor;
  }
}
