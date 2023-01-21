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
package consulo.ide.impl.idea.vcs.log.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.versionControlSystem.log.VcsLogData;
import consulo.versionControlSystem.log.VcsLogHighlighter;
import consulo.versionControlSystem.log.VcsLogHighlighterFactory;
import consulo.versionControlSystem.log.VcsLogUi;

import javax.annotation.Nonnull;

@ExtensionImpl
public class MyCommitsHighlighterFactory implements VcsLogHighlighterFactory {
  @Nonnull
  public static final String ID = "MY_COMMITS";

  @Nonnull
  @Override
  public VcsLogHighlighter createHighlighter(@Nonnull VcsLogData logData, @Nonnull VcsLogUi logUi) {
    return new MyCommitsHighlighter(logData, logUi);
  }

  @Nonnull
  @Override
  public String getId() {
    return ID;
  }

  @Nonnull
  @Override
  public String getTitle() {
    return "My Commits";
  }

  @Override
  public boolean showMenuItem() {
    return true;
  }
}
