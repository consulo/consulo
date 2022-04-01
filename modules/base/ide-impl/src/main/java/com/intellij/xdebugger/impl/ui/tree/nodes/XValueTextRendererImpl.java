/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.xdebugger.impl.ui.tree.nodes;

import consulo.debugger.ui.XValuePresentationUtil;
import consulo.debugger.ui.XValueTextRendererBase;
import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.ui.ex.ColoredTextContainer;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.TextAttributes;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.ui.ex.SimpleTextAttributes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
* @author nik
*/
public class XValueTextRendererImpl extends XValueTextRendererBase {
  private final ColoredTextContainer myText;

  public XValueTextRendererImpl(ColoredTextContainer text) {
    myText = text;
  }

  @Override
  public void renderValue(@Nonnull String value) {
    XValuePresentationUtil.renderValue(value, myText, SimpleTextAttributes.REGULAR_ATTRIBUTES, -1, null);
  }

  @Override
  protected void renderRawValue(@Nonnull String value, @Nonnull TextAttributesKey key) {
    TextAttributes textAttributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(key);
    SimpleTextAttributes attributes = TextAttributesUtil.fromTextAttributes(textAttributes);
    myText.append(value, attributes);
  }

  @Override
  public void renderStringValue(@Nonnull String value, @Nullable String additionalSpecialCharsToHighlight, char quoteChar, int maxLength) {
    TextAttributes textAttributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(DefaultLanguageHighlighterColors.STRING);
    SimpleTextAttributes attributes = TextAttributesUtil.fromTextAttributes(textAttributes);
    myText.append(String.valueOf(quoteChar), attributes);
    XValuePresentationUtil.renderValue(value, myText, attributes, maxLength, additionalSpecialCharsToHighlight);
    myText.append(String.valueOf(quoteChar), attributes);
  }

  @Override
  public void renderError(@Nonnull String error) {
    myText.append(error, SimpleTextAttributes.ERROR_ATTRIBUTES);
  }

  @Override
  public void renderComment(@Nonnull String comment) {
    myText.append(comment, SimpleTextAttributes.GRAY_ATTRIBUTES);
  }

  @Override
  public void renderSpecialSymbol(@Nonnull String symbol) {
    myText.append(symbol, SimpleTextAttributes.REGULAR_ATTRIBUTES);
  }
}
