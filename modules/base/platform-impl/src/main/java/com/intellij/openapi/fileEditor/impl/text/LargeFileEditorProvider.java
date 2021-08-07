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
package com.intellij.openapi.fileEditor.impl.text;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.UserDataHolderBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.SingleRootFileViewProvider;
import consulo.fileEditor.impl.text.TextEditorProvider;
import kava.beans.PropertyChangeListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author peter
 */
public class LargeFileEditorProvider implements FileEditorProvider, DumbAware {

  @Override
  public boolean accept(@Nonnull Project project, @Nonnull VirtualFile file) {
    return TextEditorProvider.isTextFile(file) && SingleRootFileViewProvider.isTooLargeForContentLoading(file);
  }

  @RequiredUIAccess
  @Override
  @Nonnull
  public FileEditor createEditor(@Nonnull Project project, @Nonnull final VirtualFile file) {
    return new LargeFileEditor(file);
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

    public LargeFileEditor(VirtualFile file) {
      myFile = file;
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
