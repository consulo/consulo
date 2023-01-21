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
package consulo.ide.impl.idea.vcs.log.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.versionControlSystem.log.VcsLogHighlighter;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.ide.impl.idea.vcs.log.data.VcsLogData;
import javax.annotation.Nonnull;

@ExtensionAPI(ComponentScope.PROJECT)
public interface VcsLogHighlighterFactory {
  @Nonnull
  VcsLogHighlighter createHighlighter(@Nonnull VcsLogData logDataHolder, @Nonnull VcsLogUi logUi);

  @Nonnull
  String getId();

  @Nonnull
  String getTitle();

  boolean showMenuItem();
}
