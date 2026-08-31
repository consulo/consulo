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
package consulo.ide.impl.navigation;

import consulo.localize.LocalizeValue;
import consulo.navigation.TargetPresentation;
import consulo.navigation.TargetPresentationBuilder;
import consulo.ui.TextAttribute;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * The presentation is its own builder, so branching one costs a copy and never a rebuild.
 *
 * @author VISTALL
 * @since 2026-08-27
 */
final class TargetPresentationImpl implements TargetPresentation, TargetPresentationBuilder {
    private final @Nullable ColorValue myBackgroundColor;
    private final @Nullable Image myIcon;
    private final LocalizeValue myPresentableText;
    private final @Nullable TextAttribute myPresentableTextAttribute;
    private final LocalizeValue myContainerText;
    private final @Nullable TextAttribute myContainerTextAttribute;
    private final LocalizeValue myLocationText;
    private final @Nullable Image myLocationIcon;

    static TargetPresentationImpl create(LocalizeValue presentableText) {
        return new TargetPresentationImpl(null, null, presentableText, null, LocalizeValue.empty(), null, LocalizeValue.empty(), null);
    }

    private TargetPresentationImpl(
        @Nullable ColorValue backgroundColor,
        @Nullable Image icon,
        LocalizeValue presentableText,
        @Nullable TextAttribute presentableTextAttribute,
        LocalizeValue containerText,
        @Nullable TextAttribute containerTextAttribute,
        LocalizeValue locationText,
        @Nullable Image locationIcon
    ) {
        myBackgroundColor = backgroundColor;
        myIcon = icon;
        myPresentableText = presentableText;
        myPresentableTextAttribute = presentableTextAttribute;
        myContainerText = containerText;
        myContainerTextAttribute = containerTextAttribute;
        myLocationText = locationText;
        myLocationIcon = locationIcon;
    }

    @Override
    public @Nullable ColorValue getBackgroundColor() {
        return myBackgroundColor;
    }

    @Override
    public @Nullable Image getIcon() {
        return myIcon;
    }

    @Override
    public LocalizeValue getPresentableText() {
        return myPresentableText;
    }

    @Override
    public @Nullable TextAttribute getPresentableTextAttribute() {
        return myPresentableTextAttribute;
    }

    @Override
    public LocalizeValue getContainerText() {
        return myContainerText;
    }

    @Override
    public @Nullable TextAttribute getContainerTextAttribute() {
        return myContainerTextAttribute;
    }

    @Override
    public LocalizeValue getLocationText() {
        return myLocationText;
    }

    @Override
    public @Nullable Image getLocationIcon() {
        return myLocationIcon;
    }

    @Override
    public TargetPresentation build() {
        return this;
    }

    @Override
    public TargetPresentationBuilder withBackgroundColor(@Nullable ColorValue color) {
        return new TargetPresentationImpl(color, myIcon, myPresentableText, myPresentableTextAttribute, myContainerText,
            myContainerTextAttribute, myLocationText, myLocationIcon);
    }

    @Override
    public TargetPresentationBuilder withIcon(@Nullable Image icon) {
        return new TargetPresentationImpl(myBackgroundColor, icon, myPresentableText, myPresentableTextAttribute, myContainerText,
            myContainerTextAttribute, myLocationText, myLocationIcon);
    }

    @Override
    public TargetPresentationBuilder withPresentableText(LocalizeValue text) {
        return new TargetPresentationImpl(myBackgroundColor, myIcon, text, myPresentableTextAttribute, myContainerText,
            myContainerTextAttribute, myLocationText, myLocationIcon);
    }

    @Override
    public TargetPresentationBuilder withPresentableTextAttribute(@Nullable TextAttribute attribute) {
        return new TargetPresentationImpl(myBackgroundColor, myIcon, myPresentableText, attribute, myContainerText,
            myContainerTextAttribute, myLocationText, myLocationIcon);
    }

    @Override
    public TargetPresentationBuilder withContainerText(LocalizeValue text) {
        return new TargetPresentationImpl(myBackgroundColor, myIcon, myPresentableText, myPresentableTextAttribute, text,
            myContainerTextAttribute, myLocationText, myLocationIcon);
    }

    @Override
    public TargetPresentationBuilder withContainerText(LocalizeValue text, @Nullable TextAttribute attribute) {
        return new TargetPresentationImpl(myBackgroundColor, myIcon, myPresentableText, myPresentableTextAttribute, text,
            attribute, myLocationText, myLocationIcon);
    }

    @Override
    public TargetPresentationBuilder withContainerTextAttribute(@Nullable TextAttribute attribute) {
        return new TargetPresentationImpl(myBackgroundColor, myIcon, myPresentableText, myPresentableTextAttribute, myContainerText,
            attribute, myLocationText, myLocationIcon);
    }

    @Override
    public TargetPresentationBuilder withLocationText(LocalizeValue text) {
        return new TargetPresentationImpl(myBackgroundColor, myIcon, myPresentableText, myPresentableTextAttribute, myContainerText,
            myContainerTextAttribute, text, myLocationIcon);
    }

    @Override
    public TargetPresentationBuilder withLocationText(LocalizeValue text, @Nullable Image icon) {
        return new TargetPresentationImpl(myBackgroundColor, myIcon, myPresentableText, myPresentableTextAttribute, myContainerText,
            myContainerTextAttribute, text, icon);
    }
}
