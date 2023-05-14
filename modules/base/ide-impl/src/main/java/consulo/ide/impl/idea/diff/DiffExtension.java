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
package consulo.ide.impl.idea.diff;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.diff.request.DiffRequest;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class DiffExtension {
  public static final ExtensionPointName<DiffExtension> EP_NAME = ExtensionPointName.create(DiffExtension.class);

  @RequiredUIAccess
  public abstract void onViewerCreated(@Nonnull FrameDiffTool.DiffViewer viewer,
                                       @Nonnull DiffContext context,
                                       @Nonnull DiffRequest request);
}
