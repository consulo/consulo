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
package com.intellij.usages;

import com.intellij.openapi.editor.markup.AttributesFlyweight;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.usages.impl.rules.UsageType;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TextChunk {
  public static final TextChunk[] EMPTY_ARRAY = new TextChunk[0];

  private final AttributesFlyweight myAttributes;
  private final String myText;
  private final UsageType myType;

  public TextChunk(@Nonnull TextAttributes attributes, @Nonnull String text) {
    this(attributes, text, null);
  }

  public TextChunk(@Nonnull TextAttributes attributes, @Nonnull String text, @Nullable UsageType type) {
    myAttributes = attributes.getFlyweight();
    myText = text;
    myType = type;
  }

  @Nonnull
  public TextAttributes getAttributes() {
    return TextAttributes.fromFlyweight(myAttributes);
  }

  @Nonnull
  public String getText() {
    return myText;
  }

  public String toString() {
    return getText();
  }

  public @Nullable UsageType getType() {
    return myType;
  }

  @Nonnull
  public SimpleTextAttributes getSimpleAttributesIgnoreBackground() {
    SimpleTextAttributes simples = SimpleTextAttributes.fromTextAttributes(getAttributes());
    simples = new SimpleTextAttributes(null, simples.getFgColor(), simples.getWaveColor(), simples.getStyle());
    return simples;
  }
}
