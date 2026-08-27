// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

/*
 * Copyright 2013-2026 consulo.io
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
package consulo.navigation;

import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * Every method returns a new instance, so a builder may be safely shared and branched from.
 */
public interface TargetPresentationBuilder {
    TargetPresentation build();

    /**
     * @see TargetPresentation#getBackgroundColor()
     */
    TargetPresentationBuilder withBackgroundColor(@Nullable ColorValue color);

    /**
     * @see TargetPresentation#getIcon()
     */
    TargetPresentationBuilder withIcon(@Nullable Image icon);

    /**
     * @see TargetPresentation#getPresentableText()
     */
    TargetPresentationBuilder withPresentableText(LocalizeValue text);

    /**
     * @see TargetPresentation#getPresentableTextAttribute()
     */
    TargetPresentationBuilder withPresentableTextAttribute(@Nullable TextAttribute attribute);

    /**
     * @see TargetPresentation#getContainerText()
     */
    TargetPresentationBuilder withContainerText(LocalizeValue text);

    /**
     * @see TargetPresentation#getContainerText()
     * @see TargetPresentation#getContainerTextAttribute()
     */
    TargetPresentationBuilder withContainerText(LocalizeValue text, @Nullable TextAttribute attribute);

    /**
     * @see TargetPresentation#getContainerTextAttribute()
     */
    TargetPresentationBuilder withContainerTextAttribute(@Nullable TextAttribute attribute);

    /**
     * @see TargetPresentation#getLocationText()
     */
    TargetPresentationBuilder withLocationText(LocalizeValue text);

    /**
     * @see TargetPresentation#getLocationText()
     * @see TargetPresentation#getLocationIcon()
     */
    TargetPresentationBuilder withLocationText(LocalizeValue text, @Nullable Image icon);
}
