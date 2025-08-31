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

package consulo.ide.impl.idea.ide.fileTemplates.impl;

import consulo.fileTemplate.FileTemplate;
import consulo.fileTemplate.FileTemplateUtil;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.speedSearch.ListSpeedSearch;
import consulo.ui.ex.awt.JBList;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexey Kudravtsev
 */
abstract class FileTemplateTabAsList extends FileTemplateTab {
  private final JList myList = new JBList();
  private MyListModel myModel;

  FileTemplateTabAsList(String title) {
    super(title);
    myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myList.setCellRenderer(new MyListCellRenderer());
    myList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        onTemplateSelected();
      }
    });
    new ListSpeedSearch(myList, o -> {
      if (o instanceof FileTemplate) {
        return ((FileTemplate)o).getName();
      }
      return null;
    });
  }

  private class MyListCellRenderer extends ColoredListCellRenderer<FileTemplate> {
    @Override
    protected void customizeCellRenderer(@Nonnull JList<? extends FileTemplate> list, FileTemplate value, int index, boolean selected, boolean hasFocus) {
      Image icon;
      icon = FileTemplateUtil.getIcon(value);
      boolean internalTemplate = AllFileTemplatesConfigurable.isInternalTemplate(value.getName(), getTitle());
      if (internalTemplate) {
        setFont(getFont().deriveFont(Font.BOLD));
        append(value.getName());
      }
      else {
        setFont(getFont().deriveFont(Font.PLAIN));
        append(value.getName());
      }

      if (!value.isDefault()) {
        if (!selected) {
          setForeground(MODIFIED_FOREGROUND);
        }
      }
      setIcon(icon);
    }
  }

  @Override
  public void removeSelected() {
    FileTemplate selectedTemplate = getSelectedTemplate();
    if (selectedTemplate == null) {
      return;
    }
    DefaultListModel model = (DefaultListModel) myList.getModel();
    int selectedIndex = myList.getSelectedIndex();
    model.remove(selectedIndex);
    if (!model.isEmpty()) {
      myList.setSelectedIndex(Math.min(selectedIndex, model.size() - 1));
    }
    onTemplateSelected();
  }

  private static class MyListModel extends DefaultListModel {
    public void fireListDataChanged() {
      int size = getSize();
      if (size > 0) {
        fireContentsChanged(this, 0, size - 1);
      }
    }
  }

  @Override
  protected void initSelection(FileTemplate selection) {
    myModel = new MyListModel();
    myList.setModel(myModel);
    for (FileTemplate template : myTemplates) {
      myModel.addElement(template);
    }
    if (selection != null) {
      selectTemplate(selection);
    }
    else if (myList.getModel().getSize() > 0) {
      myList.setSelectedIndex(0);
    }
  }

  @Override
  public void fireDataChanged() {
    myModel.fireListDataChanged();
  }

  @Override
  @Nonnull
  public FileTemplate[] getTemplates() {
    int size = myModel.getSize();
    List<FileTemplate> templates = new ArrayList<>(size);
    for (int i =0; i<size; i++) {
      templates.add((FileTemplate) myModel.getElementAt(i));
    }
    return templates.toArray(new FileTemplate[templates.size()]);
  }

  @Override
  public void addTemplate(FileTemplate newTemplate) {
    myModel.addElement(newTemplate);
  }

  @Override
  public void selectTemplate(FileTemplate template) {
    myList.setSelectedValue(template, true);
  }

  @Override
  public FileTemplate getSelectedTemplate() {
    Object value = myList.getSelectedValue();
    return value instanceof FileTemplate ? (FileTemplate) value : null;
  }

  @Override
  public JComponent getComponent() {
    return myList;
  }
}
