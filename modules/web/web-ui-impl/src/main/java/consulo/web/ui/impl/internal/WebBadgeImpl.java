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

import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.html.Span;
import consulo.localize.LocalizeValue;
import consulo.ui.Badge;
import consulo.ui.BadgeStyle;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.impl.BadgePresentation;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.image.WebImageConverter;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public class WebBadgeImpl extends VaadinComponentDelegate<WebBadgeImpl.Vaadin> implements Badge {
    public class Vaadin extends com.vaadin.flow.component.badge.Badge implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebBadgeImpl.this;
        }
    }

    private final Set<BadgeStyle> myStyles = EnumSet.noneOf(BadgeStyle.class);
    private LocalizeValue myText;
    private @Nullable Image myIcon;
    private @Nullable Component myPrefixComponent;
    private @Nullable Component mySuffixComponent;

    public WebBadgeImpl(LocalizeValue text) {
        myText = text;
        updateContent();
        updateFontSize();
    }

    private void updateContent() {
        Vaadin badge = toVaadinComponent();
        badge.setContent(null);
        if (myPrefixComponent == null && mySuffixComponent == null) {
            badge.setText(myText.get());
            return;
        }

        badge.setText("");
        Span container = new Span();
        container.getStyle().set("display", "inline-flex").set("align-items", "center").set("gap", "0.25em");
        Component prefix = myPrefixComponent;
        if (prefix != null) {
            container.add(TargetVaadin.to(prefix));
        }
        container.add(new Span(myText.get()));
        Component suffix = mySuffixComponent;
        if (suffix != null) {
            container.add(TargetVaadin.to(suffix));
        }
        badge.setContent(container);
    }

    private void updateFontSize() {
        int delta = BadgePresentation.fontPointDelta(myStyles);
        toVaadinComponent().getStyle().set("font-size", delta == 0 ? "1em" : "calc(1em " + (delta < 0 ? "- " : "+ ") + Math.abs(delta) + "pt)");
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public void addStyle(BadgeStyle style) {
        Vaadin component = toVaadinComponent();
        if (style != BadgeStyle.FILLED && style != BadgeStyle.SMALL) {
            component.removeThemeVariants(BadgeVariant.SUCCESS, BadgeVariant.WARNING, BadgeVariant.ERROR, BadgeVariant.CONTRAST);
            component.getStyle().remove("--vaadin-badge-background").remove("--vaadin-badge-border-color");
            myStyles.removeIf(each -> each != BadgeStyle.FILLED && each != BadgeStyle.SMALL);
        }
        myStyles.add(style);
        switch (style) {
            case SUCCESS -> component.addThemeVariants(BadgeVariant.SUCCESS);
            case WARNING -> component.addThemeVariants(BadgeVariant.WARNING);
            case ERROR -> component.addThemeVariants(BadgeVariant.ERROR);
            case CONTRAST -> component.addThemeVariants(BadgeVariant.CONTRAST);
            case NEUTRAL -> component.getStyle()
                .set("--vaadin-badge-background", "var(--vaadin-background-container)")
                .set("--vaadin-badge-border-color", "var(--vaadin-border-color)");
            case FILLED -> component.addThemeVariants(BadgeVariant.FILLED);
            case SMALL -> updateFontSize();
        }
    }

    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @RequiredUIAccess
    @Override
    public void setText(LocalizeValue text) {
        myText = text;
        updateContent();
    }

    @Override
    public @Nullable Image getIcon() {
        return myIcon;
    }

    @RequiredUIAccess
    @Override
    public void setIcon(@Nullable Image icon) {
        myIcon = icon;
        toVaadinComponent().setIcon(icon == null ? null : WebImageConverter.getImage(icon));
    }

    @Override
    public void setPrefixComponent(@Nullable Component prefixComponent) {
        myPrefixComponent = prefixComponent;
        updateContent();
    }

    @Override
    public @Nullable Component getPrefixComponent() {
        return myPrefixComponent;
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;
        updateContent();
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }
}
