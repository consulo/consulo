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
package consulo.execution.debug.impl.internal.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.configurable.internal.ConfigurableWeight;
import consulo.configurable.localize.ConfigurableLocalize;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.ui.DebuggerColors;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.DefaultSyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.internal.ColorPageWeights;
import jakarta.annotation.Nonnull;

/**
 * @author max
 */
@ExtensionImpl
public class DebuggerColorsPage implements ColorSettingsPage, ConfigurableWeight {
  @Override
  @Nonnull
  public String getDisplayName() {
    return XDebuggerLocalize.xdebuggerColorsPageName().get();
  }

  @Override
  @Nonnull
  public AttributesDescriptor[] getAttributeDescriptors() {
    return new AttributesDescriptor[] {
      new AttributesDescriptor(
        ConfigurableLocalize.optionsJavaAttributeDescriptorBreakpointLine(),
        DebuggerColors.BREAKPOINT_ATTRIBUTES
      ),
      new AttributesDescriptor(
        ConfigurableLocalize.optionsJavaAttributeDescriptorExecutionPoint(),
        DebuggerColors.EXECUTIONPOINT_ATTRIBUTES
      ),
      new AttributesDescriptor(
        ConfigurableLocalize.optionsJavaAttributeDescriptorNotTopFrame(),
        DebuggerColors.NOT_TOP_FRAME_ATTRIBUTES
      ),
      new AttributesDescriptor(
        ConfigurableLocalize.optionsJavaAttributeDescriptorInlinedValues(),
        DebuggerColors.INLINED_VALUES
      ),
      new AttributesDescriptor(
        ConfigurableLocalize.optionsJavaAttributeDescriptorInlinedValuesModified(),
        DebuggerColors.INLINED_VALUES_MODIFIED
      ),
      new AttributesDescriptor(
        ConfigurableLocalize.optionsJavaAttributeDescriptorInlinedValuesExecutionLine(),
        DebuggerColors.INLINED_VALUES_EXECUTION_LINE
      ),
    };
  }

  @Override
  @Nonnull
  public ColorDescriptor[] getColorDescriptors() {
    return new ColorDescriptor[] {
      new ColorDescriptor(
        ConfigurableLocalize.optionsJavaAttributeDescriptorRecursiveCall(),
        DebuggerColors.RECURSIVE_CALL_ATTRIBUTES,
        ColorDescriptor.Kind.BACKGROUND
      )
    };
  }

  @Override
  @Nonnull
  public SyntaxHighlighter getHighlighter() {
    return new DefaultSyntaxHighlighter();
  }

  @Override
  @Nonnull
  public String getDemoText() {
    return " ";
  }

  @Override
  public int getConfigurableWeight() {
    return ColorPageWeights.DEBUGGER;
  }
}
