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
 * Represents presentation in target popup as follows:
 * <pre>
 * | $icon$ $presentableText$ $containerText$ spacer $locationText$ $locationIcon$ |
 * </pre>
 * Elements before spacer are aligned to the left, right text and right icon are aligned to the right.
 * <p>
 * An instance is fully computed before it reaches the UI - see {@link NavigationService#presentationBuilder(LocalizeValue)}.
 * Nothing here may be resolved lazily, since a renderer is not allowed to read the model.
 */
public interface TargetPresentation {
    @Nullable ColorValue getBackgroundColor();

    @Nullable Image getIcon();

    LocalizeValue getPresentableText();

    /**
     * Attribute to highlight {@link #getPresentableText()}
     */
    @Nullable TextAttribute getPresentableTextAttribute();

    /**
     * Presentable text of a container, e.g. containing class name for a method, or a parent directory name for a file
     */
    LocalizeValue getContainerText();

    /**
     * Attribute to highlight {@link #getContainerText()}
     */
    @Nullable TextAttribute getContainerTextAttribute();

    /**
     * Presentable text of a location, e.g. a containing module, or a library, or an SDK
     */
    LocalizeValue getLocationText();

    /**
     * Icon of a location, e.g. a containing module, or a library, or an SDK
     */
    @Nullable Image getLocationIcon();
}
