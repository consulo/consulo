/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.component.ComponentManager;
import consulo.fileChooser.FileChooserDescriptor;

import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

public class TextBrowseFolderListener extends ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> {
  public TextBrowseFolderListener(@Nonnull FileChooserDescriptor fileChooserDescriptor) {
    this(fileChooserDescriptor, null);
  }

  public TextBrowseFolderListener(@Nonnull FileChooserDescriptor fileChooserDescriptor, @Nullable ComponentManager project) {
    super(
      LocalizeValue.empty(),
      LocalizeValue.empty(), 
      null,
      project,
      fileChooserDescriptor,
      TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
    );
  }

  void setOwnerComponent(@Nonnull TextFieldWithBrowseButton component) {
    myTextComponent = component;
  }

  FileChooserDescriptor getFileChooserDescriptor() {
    return myFileChooserDescriptor;
  }
}