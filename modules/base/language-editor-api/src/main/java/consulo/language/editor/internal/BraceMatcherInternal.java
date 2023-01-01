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
package consulo.language.editor.internal;

import consulo.language.editor.highlight.BraceMatcher;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * FIXME [VISTALL] we need it? remove?
 *
 * @author VISTALL
 * @since 05-Sep-22
 */
public class BraceMatcherInternal {
  private static final Map<FileType, BraceMatcher> BRACE_MATCHERS = new HashMap<>();

  public static void registerBraceMatcher(@Nonnull FileType fileType, @Nonnull BraceMatcher braceMatcher) {
    BRACE_MATCHERS.put(fileType, braceMatcher);
  }

  @Nullable
  public static BraceMatcher getMatcher(FileType fileType) {
    return BRACE_MATCHERS.get(fileType);
  }
}
