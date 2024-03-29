/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInspection.actions;

import consulo.ide.impl.idea.find.impl.FindDialog;
import consulo.ide.impl.idea.find.impl.FindInProjectUtil;
import consulo.module.Module;
import consulo.util.lang.function.Condition;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.content.scope.SearchScope;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 *         Date: 10/11/11
 */
class FileFilterPanel {
  private JCheckBox myUseFileMask;
  private JComboBox myFileMask;
  private JPanel myPanel;

  void init() {
    FindDialog.initFileFilter(myFileMask, myUseFileMask);
  }

  @Nullable
  SearchScope getSearchScope() {
    if (!myUseFileMask.isSelected()) return null;
    String text = (String)myFileMask.getSelectedItem();
    if (text == null) return null;

    final Condition<CharSequence> patternCondition = FindInProjectUtil.createFileMaskCondition(text);
    return new GlobalSearchScope() {
      @Override
      public boolean contains(@Nonnull VirtualFile file) {
        return patternCondition.value(file.getNameSequence());
      }

      @Override
      public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
        return 0;
      }

      @Override
      public boolean isSearchInModuleContent(@Nonnull Module aModule) {
        return true;
      }

      @Override
      public boolean isSearchInLibraries() {
        return true;
      }
    };
  }
  
  JPanel getPanel() {
    return myPanel;
  }
}
