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
package com.intellij.diff.tools.dir;

import com.intellij.diff.DiffContext;
import com.intellij.diff.FrameDiffTool;
import com.intellij.diff.requests.ContentDiffRequest;
import com.intellij.diff.requests.DiffRequest;
import javax.annotation.Nonnull;

public class DirDiffTool implements FrameDiffTool {
  public static final DirDiffTool INSTANCE = new DirDiffTool();

  @Nonnull
  @Override
  public DiffViewer createComponent(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    return new DirDiffViewer(context, (ContentDiffRequest)request);
  }

  @Override
  public boolean canShow(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
    return DirDiffViewer.canShowRequest(context, request);
  }

  @Nonnull
  @Override
  public String getName() {
    return "Directory viewer";
  }
}
