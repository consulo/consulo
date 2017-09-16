/*
 * Copyright 2013-2017 consulo.io
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
package consulo.fileChooser;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileChooser.FileTextField;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class WebFileChooserFactory extends FileChooserFactory {
  @NotNull
  @Override
  public FileChooserDialog createFileChooser(@NotNull FileChooserDescriptor descriptor, @Nullable Project project, @Nullable Component parent) {
    return null;
  }

  @NotNull
  @Override
  public PathChooserDialog createPathChooser(@NotNull FileChooserDescriptor descriptor, @Nullable Project project, @Nullable Component parent) {
    return new WebPathChooserDialog();
  }

  @NotNull
  @Override
  public FileSaverDialog createSaveFileDialog(@NotNull FileSaverDescriptor descriptor, @Nullable Project project) {
    return null;
  }

  @NotNull
  @Override
  public FileSaverDialog createSaveFileDialog(@NotNull FileSaverDescriptor descriptor, @NotNull Component parent) {
    return null;
  }

  @NotNull
  @Override
  public FileTextField createFileTextField(@NotNull FileChooserDescriptor descriptor, boolean showHidden, @Nullable Disposable parent) {
    return null;
  }

  @Override
  public void installFileCompletion(@NotNull JTextField field, @NotNull FileChooserDescriptor descriptor, boolean showHidden, @Nullable Disposable parent) {

  }
}
