/*
 * Copyright 2013-2022 consulo.io
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
package consulo.diff.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationBundle;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorAndFontDescriptorsProvider;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.configurable.localize.ConfigurableLocalize;
import consulo.diff.DiffColors;
import consulo.diff.util.TextDiffTypeFactory;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 23-Jun-22
 */
@ExtensionImpl
public class DiffColorAndFontDescriptorsProvider implements ColorAndFontDescriptorsProvider {
  @Nonnull
  @Override
  public String getDisplayName() {
    return ApplicationBundle.message("title.diff");
  }

  @Override
  @Nonnull
  public AttributesDescriptor[] getAttributeDescriptors() {
    TextDiffTypeFactory.TextDiffTypeImpl[] diffTypes = TextDiffTypeFactory.getInstance().getAllDiffTypes();
    return ContainerUtil.map2Array(
      diffTypes,
      AttributesDescriptor.class,
      type -> new AttributesDescriptor(
        ConfigurableLocalize.optionsGeneralColorDescriptorVcsDiffTypeTagPrefix(type.getName()),
        type.getKey()
      ));
  }

  @Override
  @Nonnull
  public ColorDescriptor[] getColorDescriptors() {
    List<ColorDescriptor> descriptors = new ArrayList<>();

    descriptors.add(new ColorDescriptor(
      ConfigurableLocalize.optionsGeneralColorDescriptorVcsDiffSeparatorBackground(),
      DiffColors.DIFF_SEPARATORS_BACKGROUND,
      ColorDescriptor.Kind.BACKGROUND
    ));

    return descriptors.toArray(ColorDescriptor.EMPTY_ARRAY);
  }
}
