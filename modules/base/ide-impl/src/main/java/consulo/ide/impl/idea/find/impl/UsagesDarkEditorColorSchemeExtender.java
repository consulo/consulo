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
package consulo.ide.impl.idea.find.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.AttributesFlyweightBuilder;
import consulo.colorScheme.EditorColorSchemeExtender;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.EffectType;
import consulo.ui.color.RGBColor;
import consulo.usage.UsageTreeColors;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 23-Jun-22
 */
@ExtensionImpl
public class UsagesDarkEditorColorSchemeExtender implements EditorColorSchemeExtender {
    @Override
    public void extend(Builder builder) {
        builder.add(UsageTreeColors.INVALID_PREFIX, AttributesFlyweightBuilder.create()
            .withForeground(new RGBColor(0xFF, 0, 0))
            .build());

        builder.add(UsageTreeColors.READONLY_PREFIX, AttributesFlyweightBuilder.create()
            .withForeground(new RGBColor(0xFF, 0, 0))
            .build());

        builder.add(UsageTreeColors.HAS_READ_ONLY_CHILD, AttributesFlyweightBuilder.create()
            .withForeground(new RGBColor(0xFF, 0, 0))
            .build());

        builder.add(UsageTreeColors.TEXT_NODE, AttributesFlyweightBuilder.create()
            .withBoldFont()
            .build());

        builder.add(UsageTreeColors.NUMBER_OF_USAGES, AttributesFlyweightBuilder.create()
            .withForeground(new RGBColor(0x80, 0x80, 0x80))
            .build());

        builder.add(UsageTreeColors.OCCURENCE, AttributesFlyweightBuilder.create()
            .withBoldFont()
            .build());

        builder.add(UsageTreeColors.SELECTED_OCCURENCE, AttributesFlyweightBuilder.create()
            .withBoldFont()
            .build());

        builder.add(UsageTreeColors.EXCLUDED_NODE, AttributesFlyweightBuilder.create()
            .withEffect(EffectType.STRIKEOUT, new RGBColor(0x80, 0x80, 0x80))
            .build());
    }

    @Nonnull
    @Override
    public String getColorSchemeId() {
        return EditorColorsScheme.DARCULA_SCHEME_NAME;
    }
}
