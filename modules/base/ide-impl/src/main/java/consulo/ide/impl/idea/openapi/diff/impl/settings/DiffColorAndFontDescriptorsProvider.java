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
package consulo.ide.impl.idea.openapi.diff.impl.settings;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorAndFontDescriptorsProvider;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.configurable.OptionsBundle;
import consulo.ide.impl.idea.diff.util.DiffLineSeparatorRenderer;
import consulo.ide.impl.idea.diff.util.TextDiffTypeFactory;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static consulo.ide.impl.idea.openapi.diff.impl.settings.DiffColorAndFontPanelFactory.getDiffGroup;

/**
 * @author VISTALL
 * @since 23-Jun-22
 */
@ExtensionImpl
public class DiffColorAndFontDescriptorsProvider implements ColorAndFontDescriptorsProvider {
  @Nonnull
  @Override
  public String getDisplayName() {
    return getDiffGroup();
  }

  @Override
  @Nonnull
  public AttributesDescriptor[] getAttributeDescriptors() {
    TextDiffTypeFactory.TextDiffTypeImpl[] diffTypes = TextDiffTypeFactory.getInstance().getAllDiffTypes();
    return ContainerUtil.map2Array(diffTypes, AttributesDescriptor.class,
                                   type -> new AttributesDescriptor(OptionsBundle.message("options.general.color.descriptor.vcs.diff.type.tag.prefix") + type.getName(), type.getKey()));
  }

  @Override
  @Nonnull
  public ColorDescriptor[] getColorDescriptors() {
    List<ColorDescriptor> descriptors = new ArrayList<>();

    descriptors
            .add(new ColorDescriptor(OptionsBundle.message("options.general.color.descriptor.vcs.diff.separator.background"), DiffLineSeparatorRenderer.BACKGROUND, ColorDescriptor.Kind.BACKGROUND));

    return descriptors.toArray(ColorDescriptor.EMPTY_ARRAY);
  }
}
