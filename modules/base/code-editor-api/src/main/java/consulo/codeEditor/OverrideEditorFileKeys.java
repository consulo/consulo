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
package consulo.codeEditor;

import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

/**
 * Special keys which allow redefine editor settings. Must be set into file {@link VirtualFile}
 *
 * @author VISTALL
 * @since 24/05/2023
 */
public interface OverrideEditorFileKeys {
  /**
   * @see PersistentEditorSettings#getStripTrailingSpaces()
   */
  public static final Key<String> OVERRIDE_STRIP_TRAILING_SPACES_KEY = Key.create("OVERRIDE_TRIM_TRAILING_SPACES_KEY");

  /**
   * @see PersistentEditorSettings#isEnsureNewLineAtEOF
   */
  public static final Key<Boolean> OVERRIDE_ENSURE_NEWLINE_KEY = Key.create("OVERRIDE_ENSURE_NEWLINE_KEY");
}
