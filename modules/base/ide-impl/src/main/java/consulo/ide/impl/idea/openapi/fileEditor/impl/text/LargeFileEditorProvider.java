/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileEditor.impl.text;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.highlight.BackgroundEditorHighlighter;
import consulo.fileEditor.*;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.application.dumb.DumbAware;
import consulo.fileEditor.text.TextEditorState;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.impl.file.SingleRootFileViewProvider;
import consulo.fileEditor.text.TextEditorProvider;
import kava.beans.PropertyChangeListener;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

/**
 * @author peter
 */
@ExtensionImpl
public class LargeFileEditorProvider implements FileEditorProvider, DumbAware {

  @Override
  public boolean accept(@Nonnull Project project, @Nonnull VirtualFile file) {
    return TextEditorProvider.isTextFile(file) && SingleRootFileViewProvider.isTooLargeForContentLoading(file);
  }

  @RequiredUIAccess
  @Override
  @Nonnull
  public FileEditor createEditor(@Nonnull Project project, @Nonnull final VirtualFile file) {
    return new LargeFileEditor(file, this);
  }

  @Override
  @Nonnull
  public String getEditorTypeId() {
    return "LargeFileEditor";
  }

  @Override
  @Nonnull
  public FileEditorPolicy getPolicy() {
    return FileEditorPolicy.NONE;
  }

  private static class LargeFileEditor extends UserDataHolderBase implements FileEditor {
    private final VirtualFile myFile;
    private final LargeFileEditorProvider myFileEditorProvider;

    public LargeFileEditor(VirtualFile file, LargeFileEditorProvider fileEditorProvider) {
      myFile = file;
      myFileEditorProvider = fileEditorProvider;
    }

    @Nonnull
    @Override
    public FileEditorProvider getProvider() {
      return myFileEditorProvider;
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
      JLabel label = new JLabel(
              "File " + myFile.getPath() + " is too large (" + StringUtil.formatFileSize(myFile.getLength()) + ")");
      label.setHorizontalAlignment(SwingConstants.CENTER);
      return label;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
      return null;
    }

    @Nonnull
    @Override
    public String getName() {
      return "Large file editor";
    }

    @Nonnull
    @Override
    public FileEditorState getState(@Nonnull FileEditorStateLevel level) {
      return new TextEditorState();
    }

    @Override
    public void setState(@Nonnull FileEditorState state) {
    }

    @Override
    public boolean isModified() {
      return false;
    }

    @Override
    public boolean isValid() {
      return myFile.isValid();
    }

    @Override
    public void selectNotify() {
    }

    @Override
    public void deselectNotify() {
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    }

    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
      return null;
    }

    @Override
    public FileEditorLocation getCurrentLocation() {
      return null;
    }

    @Override
    public StructureViewBuilder getStructureViewBuilder() {
      return null;
    }

    @Nullable
    @Override
    public VirtualFile getFile() {
      return myFile;
    }

    @Override
    public void dispose() {
    }

  }
}
