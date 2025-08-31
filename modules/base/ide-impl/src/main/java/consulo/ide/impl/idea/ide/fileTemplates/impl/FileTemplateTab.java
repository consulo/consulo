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
import consulo.fileTemplate.impl.internal.FileTemplateBase;

import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexey Kudravtsev
 */
abstract class FileTemplateTab {
  protected final List<FileTemplateBase> myTemplates = new ArrayList<FileTemplateBase>();
  private final String myTitle;
  protected static final Color MODIFIED_FOREGROUND = new Color(0, 0, 210);

  protected FileTemplateTab(String title) {
    myTitle = title;
  }

  public abstract JComponent getComponent();

  @Nullable
  public abstract FileTemplate getSelectedTemplate();

  public abstract void selectTemplate(FileTemplate template);

  public abstract void removeSelected();
  public abstract void onTemplateSelected();

  public void init(FileTemplate[] templates) {
    FileTemplate oldSelection = getSelectedTemplate();
    String oldSelectionName = oldSelection != null? ((FileTemplateBase)oldSelection).getQualifiedName() : null;

    myTemplates.clear();
    FileTemplate newSelection = null;
    for (FileTemplate original : templates) {
      FileTemplateBase copy = (FileTemplateBase)original.clone();
      if (oldSelectionName != null && oldSelectionName.equals(copy.getQualifiedName())) {
        newSelection = copy;
      }
      myTemplates.add(copy);
    }
    initSelection(newSelection);
  }

  protected abstract void initSelection(FileTemplate selection);

  public abstract void fireDataChanged();

  @Nonnull
  public FileTemplate[] getTemplates() {
    return myTemplates.toArray(new FileTemplate[myTemplates.size()]);
  }

  public abstract void addTemplate(FileTemplate newTemplate);

  public String getTitle() {
    return myTitle;
  }

}
