// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.breadcrumbs;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.breadcrumbs.Crumb;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Allows to replace the mechanism of gathering breadcrumbs for a file.
 */
public abstract class FileBreadcrumbsCollector {

  public static final ExtensionPointName<FileBreadcrumbsCollector> EP_NAME = ExtensionPointName.create("com.intellij.fileBreadcrumbsCollector");

  /**
   * Checks if this collector handles the given file.
   */
  public abstract boolean handlesFile(@Nonnull VirtualFile virtualFile);

  /**
   * Checks if the breadcrumbs should be shown for the given file.
   */
  public boolean isShownForFile(@Nonnull Editor editor, @Nonnull VirtualFile file) {
    return true;
  }

  /**
   * Adds event listeners required to redraw the breadcrumbs when the contents of the file changes.
   *
   * @param file           the file to watch
   * @param editor         current editor
   * @param disposable     the disposable used to detach listeners when the file is closed.
   * @param changesHandler the callback to be called when any changes are detected.
   */
  public abstract void watchForChanges(@Nonnull VirtualFile file, @Nonnull Editor editor, @Nonnull Disposable disposable, @Nonnull Runnable changesHandler);

  @Nonnull
  public abstract Iterable<? extends Crumb> computeCrumbs(@Nonnull VirtualFile virtualFile, @Nonnull Document document, int offset, @Nullable Boolean forcedShown);

  public static FileBreadcrumbsCollector findBreadcrumbsCollector(Project project, VirtualFile file) {
    if (file != null) {
      for (FileBreadcrumbsCollector extension : EP_NAME.getExtensions(project)) {
        if (extension.handlesFile(file)) {
          return extension;
        }
      }
    }
    return ContainerUtil.getLastItem(EP_NAME.getExtensionList(project));
  }
}
