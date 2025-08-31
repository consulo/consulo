/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileTextField;
import consulo.disposer.Disposable;
import consulo.ide.impl.fileChooser.FileChooserFactoryImpl;
import consulo.ui.TextBox;
import consulo.desktop.awt.ui.impl.textBox.TextBoxWithTextField;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

@Singleton
@ServiceImpl
public class DesktopFileChooserFactoryImpl extends FileChooserFactoryImpl {
  @Nonnull
  @Override
  public FileTextField createFileTextField(@Nonnull FileChooserDescriptor descriptor, boolean showHidden, @Nullable Disposable parent) {
    return new FileTextFieldImpl.Vfs(new JTextField(), getMacroMap(), parent, new LocalFsFinder.FileChooserFilter(descriptor, showHidden));
  }

  @Override
  public void installFileCompletion(@Nonnull JTextField field, @Nonnull FileChooserDescriptor descriptor, boolean showHidden, @Nullable Disposable parent) {
    if (!ApplicationManager.getApplication().isUnitTestMode() && !ApplicationManager.getApplication().isHeadlessEnvironment()) {
      new FileTextFieldImpl.Vfs(field, getMacroMap(), parent, new LocalFsFinder.FileChooserFilter(descriptor, showHidden));
    }
  }

  @Override
  public void installFileCompletion(@Nonnull TextBox textBox, @Nonnull FileChooserDescriptor descriptor, boolean showHidden, @Nullable consulo.disposer.Disposable parent) {
    if(!(textBox instanceof TextBoxWithTextField)) {
      throw new UnsupportedOperationException(textBox + " is not TextBoxWithTextField");
    }

    JTextField textField = ((TextBoxWithTextField)textBox).getTextField();

    new FileTextFieldImpl.Vfs(textField, getMacroMap(), parent, new LocalFsFinder.FileChooserFilter(descriptor, showHidden));
  }
}
