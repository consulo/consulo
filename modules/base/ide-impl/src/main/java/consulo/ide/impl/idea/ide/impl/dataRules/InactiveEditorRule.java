/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author max
 */
package consulo.ide.impl.idea.ide.impl.dataRules;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.dataContext.DataProvider;
import consulo.dataContext.GetDataRule;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class InactiveEditorRule implements GetDataRule<Editor> {
  @Nonnull
  @Override
  public Key<Editor> getKey() {
    return EditorKeys.EDITOR_EVEN_IF_INACTIVE;
  }

  @Override
  @Nullable
  public Editor getData(@Nonnull final DataProvider dataProvider) {
    return dataProvider.getDataUnchecked(Editor.KEY);
  }
}