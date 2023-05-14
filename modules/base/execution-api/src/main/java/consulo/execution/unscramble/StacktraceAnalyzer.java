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
package consulo.execution.unscramble;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 01-Sep-22
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface StacktraceAnalyzer {
  @Nonnull
  LocalizeValue getName();

  boolean isPreferredForProject(@Nonnull Project project);

  boolean isStacktrace(@Nonnull String trace);

  @Nonnull
  default String normalizeStacktrace(@Nonnull String rawStacktrace) {
    return rawStacktrace;
  }

  /**
   * Parse stacktrace as thread dump, and will show as thread dump if list not empty
   */
  @Nonnull
  default List<ThreadState> parseAsThreadDump(@Nonnull String stacktrace) {
    return List.of();
  }

  /**
   * Try parse stacktrace as exception trace, and return ExceptionName if its exception trace
   */
  @Nullable
  default String parseAsException(@Nonnull String stacktrace) {
    return null;
  }
}
