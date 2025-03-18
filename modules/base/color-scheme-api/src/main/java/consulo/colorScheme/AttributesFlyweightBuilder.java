/*
 * Copyright 2013-2025 consulo.io
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
package consulo.colorScheme;

import consulo.colorScheme.internal.AttributesFlyweightBuilderImpl;
import consulo.ui.color.ColorValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2025-03-17
 */
public interface AttributesFlyweightBuilder {
    @Nonnull
    static AttributesFlyweightBuilder create() {
        return new AttributesFlyweightBuilderImpl();
    }

    @Nonnull
    AttributesFlyweightBuilder withForeground(@Nonnull ColorValue foreground);

    @Nonnull
    AttributesFlyweightBuilder withBackground(@Nonnull ColorValue background);

    @Nonnull
    AttributesFlyweightBuilder withEffect(@Nonnull EffectType effectType, @Nullable ColorValue effectColor);

    @Nonnull
    AttributesFlyweightBuilder withAdditionalEffect(@Nonnull EffectType effectType, @Nullable ColorValue effectColor);

    @Nonnull
    AttributesFlyweightBuilder withErrorStripeColor(@Nonnull ColorValue errorStripeColor);

    @Nonnull
    AttributesFlyweightBuilder withBoldFont();

    @Nonnull
    AttributesFlyweightBuilder withItalicFont();

    @Nonnull
    AttributesFlyweight build();
}
