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
package com.intellij.diff.tools.util;

import com.intellij.diff.DiffContext;
import com.intellij.diff.FrameDiffTool;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.merge.MergeTool;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.util.LineRange;
import com.intellij.openapi.editor.Editor;
import consulo.util.dataholder.Key;
import com.intellij.pom.Navigatable;

public interface DiffDataKeys {
  Key<Navigatable> NAVIGATABLE = Key.create("diff_navigatable");
  Key<Navigatable[]> NAVIGATABLE_ARRAY = Key.create("diff_navigatable_array");

  Key<Editor> CURRENT_EDITOR = Key.create("diff_current_editor");
  Key<DiffContent> CURRENT_CONTENT = Key.create("diff_current_content");
  Key<LineRange> CURRENT_CHANGE_RANGE = Key.create("diff_current_change_range");

  Key<DiffRequest> DIFF_REQUEST = Key.create("diff_request");
  Key<DiffContext> DIFF_CONTEXT = Key.create("diff_context");
  Key<FrameDiffTool.DiffViewer> DIFF_VIEWER = Key.create("diff_frame_viewer");
  Key<FrameDiffTool.DiffViewer> WRAPPING_DIFF_VIEWER = Key.create("main_diff_frame_viewer"); // if DiffViewerWrapper is used

  Key<MergeTool.MergeViewer> MERGE_VIEWER = Key.create("merge_viewer");

  Key<PrevNextDifferenceIterable> PREV_NEXT_DIFFERENCE_ITERABLE = Key.create("prev_next_difference_iterable");
}
