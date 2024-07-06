/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.awt.internal.diff;

import consulo.annotation.component.ExtensionImpl;
import consulo.desktop.awt.internal.diff.util.DiffViewerBase;
import consulo.desktop.awt.internal.versionControlSystem.AnnotateDiffViewerAction;
import consulo.diff.DiffContext;
import consulo.diff.DiffExtension;
import consulo.diff.FrameDiffTool;
import consulo.diff.request.DiffRequest;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class AnnotateDiffExtension extends DiffExtension {
  @RequiredUIAccess
  @Override
  public void onViewerCreated(@Nonnull FrameDiffTool.DiffViewer diffViewer, @Nonnull DiffContext context, @Nonnull DiffRequest request) {
    if (diffViewer instanceof DiffViewerBase) {
      DiffViewerBase viewer = (DiffViewerBase)diffViewer;
      viewer.addListener(new AnnotateDiffViewerAction.MyDiffViewerListener(viewer));
    }
  }
}
