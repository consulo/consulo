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
package com.intellij.ide.impl.dataRules;

import com.intellij.ide.PasteProvider;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;

public class PasteProviderRule implements GetDataRule<PasteProvider> {
  @Nonnull
  @Override
  public Key<PasteProvider> getKey() {
    return PlatformDataKeys.PASTE_PROVIDER;
  }

  @Override
  public PasteProvider getData(@Nonnull DataProvider dataProvider) {
    final Editor editor = dataProvider.getDataUnchecked(PlatformDataKeys.EDITOR);
    if (editor instanceof EditorEx) {
      return ((EditorEx) editor).getPasteProvider();
    }
    return null;
  }
}
