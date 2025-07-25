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
package consulo.ide.impl.idea.openapi.fileTypes.impl;

import consulo.language.Language;
import consulo.language.template.TemplateDataLanguageMappings;
import consulo.language.template.TemplateLanguageFileType;
import consulo.ui.ex.awt.ListCellRendererWrapper;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author peter
 */
public class FileTypePatternDialog {
  private JTextField myPatternField;
  private JComboBox myLanguageCombo;
  private JLabel myTemplateDataLanguageButton;
  private JPanel myMainPanel;

  public FileTypePatternDialog(@Nullable String initialPatterns, FileType fileType, Language templateDataLanguage) {
    myPatternField.setText(initialPatterns);

    if (fileType instanceof TemplateLanguageFileType) {
      final DefaultComboBoxModel model = (DefaultComboBoxModel) myLanguageCombo.getModel();
      model.addElement(null);
      final List<Language> languages = TemplateDataLanguageMappings.getTemplateableLanguages();
      Collections.sort(languages, new Comparator<Language>() {
        @Override
        public int compare(final Language o1, final Language o2) {
          return o1.getID().compareTo(o2.getID());
        }
      });
      for (Language language : languages) {
        model.addElement(language);
      }
      myLanguageCombo.setRenderer(new ListCellRendererWrapper() {
        @Override
        public void customize(JList list, Object value, int index, boolean selected, boolean hasFocus) {
          setText(value == null ? "" : ((Language) value).getDisplayName().get());
          if (value != null) {
            final FileType type = ((Language)value).getAssociatedFileType();
            if (type != null) {
              setIcon(TargetAWT.to(type.getIcon()));
            }
          }
        }
      });
      myLanguageCombo.setSelectedItem(templateDataLanguage);
    } else {
      myLanguageCombo.setVisible(false);
      myTemplateDataLanguageButton.setVisible(false);
    }
  }

  public JTextField getPatternField() {
    return myPatternField;
  }

  public JPanel getMainPanel() {
    return myMainPanel;
  }

  @Nullable
  public Language getTemplateDataLanguage() {
    return (Language)myLanguageCombo.getSelectedItem();
  }
}
