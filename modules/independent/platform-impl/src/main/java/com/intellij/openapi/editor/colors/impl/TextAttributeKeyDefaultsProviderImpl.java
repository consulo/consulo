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
package com.intellij.openapi.editor.colors.impl;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Dennis.Ushakov
 */
@Singleton
public class TextAttributeKeyDefaultsProviderImpl implements TextAttributesKey.TextAttributeKeyDefaultsProvider {
  private EditorColorsManagerImpl myEditorColorsManager;

  @Inject
  public TextAttributeKeyDefaultsProviderImpl(EditorColorsManager editorColorsManager) {
    myEditorColorsManager = (EditorColorsManagerImpl)editorColorsManager;
  }

  @Override
  public TextAttributes getDefaultAttributes(TextAttributesKey key) {
    return myEditorColorsManager.getDefaultAttributes(key);
  }
}
