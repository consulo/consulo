/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.language.editor.HectorComponentPanel;
import consulo.configurable.ConfigurationException;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.language.psi.include.FileIncludeManager;
import consulo.ui.ex.awt.ComboboxWithBrowseButton;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author mike
 */
public class FileIncludeContextHectorPanel extends HectorComponentPanel {
  private ComboboxWithBrowseButton myContextFile;
  private JPanel myPanel;
  private final PsiFile myFile;
  private final FileIncludeManager myIncludeManager;

  public FileIncludeContextHectorPanel(PsiFile file, FileIncludeManager includeManager) {
    myFile = file;
    myIncludeManager = includeManager;

    reset();
  }

  @Override
  public JComponent createComponent() {
    return myPanel;
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
  }

  @Override
  public void reset() {
    JComboBox comboBox = myContextFile.getComboBox();

    comboBox.setRenderer(new MyListCellRenderer(comboBox));
    VirtualFile[] includingFiles = myIncludeManager.getIncludingFiles(myFile.getVirtualFile(), false);
    comboBox.setModel(new DefaultComboBoxModel(includingFiles));
    myContextFile.setTextFieldPreferredWidth(30);
  }

  @Override
  public void disposeUIResources() { }

  private class MyListCellRenderer extends DefaultListCellRenderer {
    private final JComboBox myComboBox;
    private int myMaxWidth;

    public MyListCellRenderer(JComboBox comboBox) {
      myComboBox = comboBox;
      myMaxWidth = comboBox.getPreferredSize().width;
    }

    @Override
    public Component getListCellRendererComponent(JList list,
                                                  Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {

      Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      String path = getPath(value);
      if (path != null) {
        int max = index == -1 ? myComboBox.getWidth() - myContextFile.getButton().getWidth() : myComboBox.getWidth() * 3;
        path = trimPath(path, myComboBox, "/", max);
        setText(path);
      }
      return rendererComponent;
    }

    @Nullable
    protected String getPath(Object value) {
      VirtualFile file = (VirtualFile)value;
      ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myFile.getProject()).getFileIndex();
      if (file != null) {
        VirtualFile root = fileIndex.getSourceRootForFile(file);
        if (root == null) {
          root = fileIndex.getContentRootForFile(file);
        }
        if (root != null) {
          return VfsUtilCore.getRelativePath(file, root, '/');
        }
      }
      return null;
    }

    private String trimPath(String path, Component component, String separator, int length) {

      FontMetrics fontMetrics = component.getFontMetrics(component.getFont());
      int maxWidth = fontMetrics.stringWidth(path);
      if (maxWidth <= length) {
        myMaxWidth = Math.max(maxWidth, myMaxWidth);
        return path;
      }
      StringBuilder result = new StringBuilder(path);
      if (path.startsWith(separator)) {
        result.delete(0, 1);
      }
      String[] strings = result.toString().split(separator);
      result.replace(0, strings[0].length(), "...");
      for (int i = 1; i < strings.length; i++) {
        String clipped = result.toString();
        int width = fontMetrics.stringWidth(clipped);
        if (width <= length) {
          myMaxWidth = Math.max(width, myMaxWidth);
          return clipped;
        }
        result.delete(4, 5 + strings[i].length());
      }
      return result.toString();
    }

  }

}
