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
package consulo.execution.impl.internal.unscramble;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.unscramble.StacktraceAnalyzer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

import javax.annotation.Nonnull;

/**
 * Default impl of stacktrace analyzer. Not support clipboard sync, and preferring for projects
 *
 * @author VISTALL
 * @since 01-Sep-22
 */
@ExtensionImpl(order = "first")
public class RawStacktraceAnalyzer implements StacktraceAnalyzer {
  @Nonnull
  @Override
  public LocalizeValue getName() {
    return LocalizeValue.localizeTODO("<raw>");
  }

  @Override
  public boolean isPreferredForProject(@Nonnull Project project) {
    return false;
  }

  @Override
  public boolean isStacktrace(@Nonnull String trace) {
    return false;
  }
}
