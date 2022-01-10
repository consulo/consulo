/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.application.options.colors.highlighting;

import com.intellij.codeInsight.daemon.impl.ParameterHintsPresentationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;

import javax.annotation.Nonnull;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.intellij.openapi.editor.colors.CodeInsightColors.BLINKING_HIGHLIGHTS_ATTRIBUTES;

public class InlineElementData extends HighlightData {
  private final String myText;
  private boolean myAddBorder;

  public InlineElementData(int offset, TextAttributesKey attributesKey, String text) {
    this(offset, attributesKey, text, false);
  }

  private InlineElementData(int offset, TextAttributesKey attributesKey, String text, boolean highlighted) {
    super(offset, offset, attributesKey);
    myText = text;
    myAddBorder = highlighted;
  }

  public String getText() {
    return myText;
  }

  @Override
  public void addHighlToView(Editor view, EditorColorsScheme scheme, Map<TextAttributesKey, LocalizeValue> displayText) {
    int offset = getStartOffset();
    ParameterHintsPresentationManager.getInstance().addHint(view, offset, null, myText, false);
    List<Inlay> inlays = view.getInlayModel().getInlineElementsInRange(offset, offset);
    for (Inlay inlay : inlays) {
      EditorCustomElementRenderer renderer = inlay.getRenderer();
      if (!(renderer instanceof RendererWrapper)) {
        Disposer.dispose(inlay);
        RendererWrapper wrapper = new RendererWrapper(renderer);
        wrapper.drawBorder = myAddBorder;
        view.getInlayModel().addInlineElement(offset, wrapper);
      }
    }
  }

  @Override
  public void addToCollection(@Nonnull Collection<HighlightData> list, boolean highlighted) {
    list.add(new InlineElementData(getStartOffset(), getHighlightKey(), myText, highlighted));
  }

  public static class RendererWrapper implements EditorCustomElementRenderer {
    private final EditorCustomElementRenderer myDelegate;
    private boolean drawBorder;

    public RendererWrapper(EditorCustomElementRenderer delegate) {
      myDelegate = delegate;
    }

    @Override
    public int calcWidthInPixels(@Nonnull Inlay inlay) {
      return myDelegate.calcWidthInPixels(inlay);
    }

    @Override
    public void paint(@Nonnull Inlay inlay, @Nonnull Graphics g, @Nonnull Rectangle r, @Nonnull TextAttributes textAttributes) {
      myDelegate.paint(inlay, g, r, textAttributes);
      if (drawBorder) {
        TextAttributes attributes = inlay.getEditor().getColorsScheme().getAttributes(BLINKING_HIGHLIGHTS_ATTRIBUTES);
        if (attributes != null && attributes.getEffectColor() != null) {
          g.setColor(TargetAWT.to(attributes.getEffectColor()));
          g.drawRect(r.x, r.y, r.width, r.height);
        }
      }
    }
  }

}
