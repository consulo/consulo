/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.xdebugger.impl.ui;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.OptionsBundle;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.ui.DebuggerColors;
import consulo.preferences.internal.ConfigurableWeight;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author max
 */
public class DebuggerColorsPage implements ColorSettingsPage, ConfigurableWeight {
  @Override
  @Nonnull
  public String getDisplayName() {
    return XDebuggerBundle.message("xdebugger.colors.page.name");
  }

  @Override
  @Nonnull
  public AttributesDescriptor[] getAttributeDescriptors() {
    return new AttributesDescriptor[] {
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.breakpoint.line"), DebuggerColors.BREAKPOINT_ATTRIBUTES),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.execution.point"), DebuggerColors.EXECUTIONPOINT_ATTRIBUTES),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.not.top.frame"), DebuggerColors.NOT_TOP_FRAME_ATTRIBUTES),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.inlined.values"), DebuggerColors.INLINED_VALUES),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.inlined.values.modified"), DebuggerColors.INLINED_VALUES_MODIFIED),
            new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.inlined.values.execution.line"), DebuggerColors.INLINED_VALUES_EXECUTION_LINE),
    };
  }

  @Override
  @Nonnull
  public ColorDescriptor[] getColorDescriptors() {
    return new ColorDescriptor[] {
            new ColorDescriptor(OptionsBundle.message("options.java.attribute.descriptor.recursive.call"), DebuggerColors.RECURSIVE_CALL_ATTRIBUTES, ColorDescriptor.Kind.BACKGROUND)
    };
  }

  @Override
  @Nonnull
  public SyntaxHighlighter getHighlighter() {
    return new PlainSyntaxHighlighter();
  }

  @Override
  @Nonnull
  public String getDemoText() {
    return " ";
  }

  @Override
  @Nullable
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return null;
  }

  @Override
  public int getConfigurableWeight() {
    return Integer.MAX_VALUE - 1;
  }
}
