// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.filters;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public abstract class HyperlinkInfoFactory {
  @Nonnull
  public static HyperlinkInfoFactory getInstance() {
    return Application.get().getInstance(HyperlinkInfoFactory.class);
  }

  @Nonnull
  public abstract HyperlinkInfo createMultipleFilesHyperlinkInfo(@Nonnull List<? extends VirtualFile> files, int line, @Nonnull Project project);

  /**
   * Creates a hyperlink which points to several files with ability to calculate a position inside line
   *
   * @param files   list of files to navigate to (will be suggested to user)
   * @param line    line number to navigate to
   * @param project a project
   * @param action  an action to be performed once editor is opened
   * @return newly created HyperlinkInfo which navigates to given line and column
   */
  @Nonnull
  public abstract HyperlinkInfo createMultipleFilesHyperlinkInfo(@Nonnull List<? extends VirtualFile> files, int line, @Nonnull Project project, @Nullable HyperlinkHandler action);

  /**
   * Creates a hyperlink that points to elements with ability to navigate to specific element within the file
   *
   * @param elements elements list
   * @return newly create HyperlinkInfo that navigates to given psi elements
   */
  @Nonnull
  public abstract HyperlinkInfo createMultiplePsiElementHyperlinkInfo(@Nonnull Collection<? extends PsiElement> elements);

  public interface HyperlinkHandler {
    void onLinkFollowed(@Nonnull Project project, @Nonnull VirtualFile psiFile, @Nonnull Editor targetEditor, @Nullable Editor originalEditor);
  }
}
