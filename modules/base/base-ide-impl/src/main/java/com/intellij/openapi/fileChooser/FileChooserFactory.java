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
package com.intellij.openapi.fileChooser;

import consulo.disposer.Disposable;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import consulo.annotation.DeprecationInfo;
import consulo.ui.TextBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public abstract class FileChooserFactory {
  public static FileChooserFactory getInstance() {
    return ServiceManager.getService(FileChooserFactory.class);
  }

  @Nonnull
  public abstract FileChooserDialog createFileChooser(@Nonnull FileChooserDescriptor descriptor, @Nullable Project project, @Nullable Component parent);

  @Nonnull
  public abstract PathChooserDialog createPathChooser(@Nonnull FileChooserDescriptor descriptor, @Nullable Project project, @Nullable Component parent);

  /**
   * Creates Save File dialog.
   *
   * @param descriptor dialog descriptor
   * @param project    chooser options
   * @return Save File dialog
   * @since 9.0
   */
  @Nonnull
  public abstract FileSaverDialog createSaveFileDialog(@Nonnull FileSaverDescriptor descriptor, @Nullable Project project);

  @Nonnull
  public abstract FileSaverDialog createSaveFileDialog(@Nonnull FileSaverDescriptor descriptor, @Nonnull Component parent);

  @Nonnull
  @Deprecated
  @DeprecationInfo("See FileChooserTextBoxBuilder")
  public FileTextField createFileTextField(@Nonnull FileChooserDescriptor descriptor, boolean showHidden, @Nullable Disposable parent) {
    throw new AbstractMethodError();
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("See FileChooserTextBoxBuilder")
  public FileTextField createFileTextField(@Nonnull FileChooserDescriptor descriptor, @Nullable Disposable parent) {
    return createFileTextField(descriptor, true, parent);
  }

  /**
   * Adds path completion listener to a given text field.
   *
   * @param field      input field to add completion to
   * @param descriptor chooser options
   * @param showHidden include hidden files into completion variants
   * @param parent     if null then will be registered with {@link PlatformDataKeys#UI_DISPOSABLE}
   */
  public void installFileCompletion(@Nonnull JTextField field, @Nonnull FileChooserDescriptor descriptor, boolean showHidden, @Nullable Disposable parent) {
    // nothing by default, check platform implementation
  }

  /**
   * Adds path completion listener to a given text field.
   *
   * @param textBox    input field to add completion to
   * @param descriptor chooser options
   * @param showHidden include hidden files into completion variants
   * @param parent     if null then will be registered with {@link PlatformDataKeys#UI_DISPOSABLE}
   */
  public abstract void installFileCompletion(@Nonnull TextBox textBox, @Nonnull FileChooserDescriptor descriptor, boolean showHidden, @Nullable consulo.disposer.Disposable parent);
}
