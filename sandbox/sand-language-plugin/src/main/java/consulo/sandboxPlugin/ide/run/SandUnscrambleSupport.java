/*
 * Copyright 2013-2022 consulo.io
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
package consulo.sandboxPlugin.ide.run;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.unscramble.StacktraceAnalyzer;
import consulo.execution.unscramble.UnscrambleSupport;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 01-Sep-22
 */
@ExtensionImpl
public class SandUnscrambleSupport implements UnscrambleSupport {
  @Nonnull
  @Override
  public String getId() {
    return "sand";
  }

  @Nonnull
  @Override
  public LocalizeValue getName() {
    return LocalizeValue.localizeTODO("Sand");
  }

  @Override
  public boolean isAvailable(@Nonnull StacktraceAnalyzer analyzer) {
    return true;
  }

  @Nullable
  @Override
  public String unscramble(Project project, String text, String logName) {
    return text;
  }
}
