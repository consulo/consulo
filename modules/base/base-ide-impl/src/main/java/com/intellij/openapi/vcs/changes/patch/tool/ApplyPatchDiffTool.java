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
package com.intellij.openapi.vcs.changes.patch.tool;

import com.intellij.diff.DiffContext;
import com.intellij.diff.FrameDiffTool;
import com.intellij.diff.requests.DiffRequest;
import javax.annotation.Nonnull;

public class ApplyPatchDiffTool implements FrameDiffTool {
  @Nonnull
  @Override
  public DiffViewer createComponent(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    return new MyApplyPatchViewer(context, (ApplyPatchDiffRequest)request);
  }

  @Override
  public boolean canShow(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    return request instanceof ApplyPatchDiffRequest;
  }

  @Nonnull
  @Override
  public String getName() {
    return "Apply patch somehow";
  }

  private static class MyApplyPatchViewer extends ApplyPatchViewer implements DiffViewer {
    public MyApplyPatchViewer(@Nonnull DiffContext context, @Nonnull ApplyPatchDiffRequest request) {
      super(context, request);
    }

    @Nonnull
    @Override
    public ToolbarComponents init() {
      initPatchViewer();

      ToolbarComponents components = new ToolbarComponents();
      components.statusPanel = getStatusPanel();
      components.toolbarActions = createToolbarActions();

      return components;
    }
  }
}
