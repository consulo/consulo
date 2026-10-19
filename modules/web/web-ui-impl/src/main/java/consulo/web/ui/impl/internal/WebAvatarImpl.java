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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.avatar.AvatarVariant;
import consulo.localize.LocalizeValue;
import consulo.ui.Avatar;
import consulo.ui.AvatarStyle;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.impl.avatar.AvatarPresentation;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.image.WebImageUrl;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class WebAvatarImpl extends VaadinComponentDelegate<WebAvatarImpl.Vaadin> implements Avatar {
    public class Vaadin extends com.vaadin.flow.component.avatar.Avatar implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebAvatarImpl.this;
        }
    }

    private LocalizeValue myName;
    private @Nullable Image myImage;

    public WebAvatarImpl(LocalizeValue name, @Nullable Image image) {
        myName = name;
        myImage = image;
        Vaadin component = toVaadinComponent();
        component.getElement().getThemeList().remove("small");
        component.getElement().getThemeList().add("medium");
        component.setTooltipEnabled(true);
        updateName();
        updateImage();
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public void addStyle(AvatarStyle style) {
        Vaadin component = toVaadinComponent();
        component.getElement().getThemeList().remove("medium");
        component.removeThemeVariants(AvatarVariant.XSMALL, AvatarVariant.SMALL, AvatarVariant.LARGE, AvatarVariant.XLARGE);
        component.addThemeVariants(switch (style) {
            case X_SMALL -> AvatarVariant.XSMALL;
            case SMALL -> AvatarVariant.SMALL;
            case LARGE -> AvatarVariant.LARGE;
            case X_LARGE -> AvatarVariant.XLARGE;
        });
    }

    @Override
    public LocalizeValue getName() {
        return myName;
    }

    @RequiredUIAccess
    @Override
    public void setName(LocalizeValue name) {
        myName = name;
        updateName();
        updateImage();
    }

    @Override
    public @Nullable Image getImage() {
        return myImage;
    }

    @RequiredUIAccess
    @Override
    public void setImage(@Nullable Image image) {
        myImage = image;
        updateImage();
    }

    private void updateName() {
        Vaadin component = toVaadinComponent();
        String name = myName.get();
        component.setName(name.isEmpty() ? null : name);
        component.setAbbreviation(name.isEmpty() ? null : AvatarPresentation.initialsOf(name));
        updateColor();
    }

    private void updateImage() {
        Image image = AvatarPresentation.imageOf(myName.get(), myImage);
        toVaadinComponent().setImage(image == null ? null : WebImageUrl.toURL(image));
        updateColor();
    }

    private void updateColor() {
        Vaadin component = toVaadinComponent();
        String name = myName.get();
        if (name.isEmpty() || AvatarPresentation.imageOf(name, myImage) != null) {
            component.getElement().removeProperty("colorIndex");
        }
        else {
            component.setColorIndex(AvatarPresentation.colorIndexOf(name));
        }
    }
}
