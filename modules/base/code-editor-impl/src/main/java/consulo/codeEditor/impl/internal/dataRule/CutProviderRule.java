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
package consulo.codeEditor.impl.internal.dataRule;

import consulo.annotation.component.ExtensionImpl;
import consulo.ui.ex.CutProvider;
import consulo.dataContext.DataProvider;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.dataContext.GetDataRule;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class CutProviderRule implements GetDataRule<CutProvider> {
  @Nonnull
  @Override
  public Key<CutProvider> getKey() {
    return CutProvider.KEY;
  }

  @Override
  public CutProvider getData(@Nonnull DataProvider dataProvider) {
    Editor editor = dataProvider.getDataUnchecked(Editor.KEY);
    return editor instanceof EditorEx editorEx ? editorEx.getCutProvider() : null;
  }
}
