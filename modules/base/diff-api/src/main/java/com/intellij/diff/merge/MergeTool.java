/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.diff.merge;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.BooleanGetter;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;

import javax.swing.*;
import java.util.List;

public interface MergeTool {
  ExtensionPointName<MergeTool> EP_NAME = ExtensionPointName.create("com.intellij.diff.merge.MergeTool");

  /**
   * Creates viewer for the given request. Clients should call {@link #canShow(MergeContext, MergeRequest)} first.
   */
  @RequiredUIAccess
  @Nonnull
  MergeViewer createComponent(@Nonnull MergeContext context, @Nonnull MergeRequest request);

  boolean canShow(@Nonnull MergeContext context, @Nonnull MergeRequest request);

  /**
   * Merge viewer should call {@link MergeContext#finishMerge(MergeResult)} when processing is over.
   *
   * {@link MergeRequest#applyResult(MergeResult)} will be performed by the caller, so it shouldn't be called by MergeViewer directly.
   */
  interface MergeViewer extends Disposable {
    @Nonnull
    JComponent getComponent();

    @javax.annotation.Nullable
    JComponent getPreferredFocusedComponent();

    /**
     * @return Action that should be triggered on the corresponding action.
     * <p/>
     * Typical implementation can perform some checks and either call finishMerge(result) or do nothing
     * <p/>
     * return null if action is not available
     */
    @Nullable
    Action getResolveAction(@Nonnull MergeResult result);

    /**
     * Should be called after adding {@link #getComponent()} to the components hierarchy.
     */
    @Nonnull
    @RequiredUIAccess
    ToolbarComponents init();

    @Override
    @RequiredUIAccess
    void dispose();
  }

  class ToolbarComponents {
    @Nullable public List<AnAction> toolbarActions;
    @javax.annotation.Nullable
    public JComponent statusPanel;

    /**
     * return false if merge window should be prevented from closing and canceling resolve.
     */
    @Nullable public BooleanGetter closeHandler;
  }
}
