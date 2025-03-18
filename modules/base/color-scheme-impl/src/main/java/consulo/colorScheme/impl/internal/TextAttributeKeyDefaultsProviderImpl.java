/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.colorScheme.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributeKeyDefaultsProvider;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author Dennis.Ushakov
 */
@Singleton
@ServiceImpl
public class TextAttributeKeyDefaultsProviderImpl implements TextAttributeKeyDefaultsProvider {
  private final EditorColorsManager myEditorColorsManager;

  @Inject
  public TextAttributeKeyDefaultsProviderImpl(EditorColorsManager editorColorsManager) {
    myEditorColorsManager = editorColorsManager;
  }

  @Override
  public TextAttributes getDefaultAttributes(TextAttributesKey key) {
    return ((EditorColorsManagerImpl)myEditorColorsManager).getDefaultAttributes(key);
  }
}
