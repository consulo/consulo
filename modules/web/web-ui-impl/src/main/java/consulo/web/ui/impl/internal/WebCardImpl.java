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

import com.vaadin.flow.component.card.CardVariant;
import consulo.localize.LocalizeValue;
import consulo.ui.Card;
import consulo.ui.CardStyle;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LayoutConstraint;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.vaadin.VaadinSizeUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class WebCardImpl extends WebLayoutImpl<WebCardImpl.Vaadin, LayoutConstraint> implements Card {
    public class Vaadin extends com.vaadin.flow.component.card.Card implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebCardImpl.this;
        }
    }

    private @Nullable Component myMedia;
    private @Nullable Component myHeader;
    private @Nullable Component myHeaderPrefix;
    private @Nullable Component myHeaderSuffix;
    private final List<Component> myFooter = new ArrayList<>();

    public WebCardImpl() {
        VaadinSizeUtil.setWidthFull(this);
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    protected @Nullable String clickFilter() {
        return "!event.target.closest('vaadin-checkbox, vaadin-radio-button, vaadin-button, input, button, a')";
    }

    @Override
    public void addStyle(CardStyle style) {
        switch (style) {
            case ELEVATED -> toVaadinComponent().addThemeVariants(CardVariant.ELEVATED);
            case OUTLINED -> toVaadinComponent().addThemeVariants(CardVariant.OUTLINED);
            case HORIZONTAL -> toVaadinComponent().addThemeVariants(CardVariant.HORIZONTAL);
            case STRETCH_MEDIA -> toVaadinComponent().addThemeVariants(CardVariant.STRETCH_MEDIA);
            case COVER_MEDIA -> toVaadinComponent().addThemeVariants(CardVariant.COVER_MEDIA);
            // the room the desktop cards keep when compact - Space.MEDIUM
            case COMPACT -> toVaadinComponent().getStyle().set("--vaadin-card-padding", "6px");
            case SELECTED -> toVaadinComponent().getElement().getClassList().add("consulo-card-selected");
            case CENTERED -> toVaadinComponent().getElement().getClassList().add("consulo-card-centered");
        }
    }

    @Override
    public void removeStyle(CardStyle style) {
        switch (style) {
            case ELEVATED -> toVaadinComponent().removeThemeVariants(CardVariant.ELEVATED);
            case OUTLINED -> toVaadinComponent().removeThemeVariants(CardVariant.OUTLINED);
            case HORIZONTAL -> toVaadinComponent().removeThemeVariants(CardVariant.HORIZONTAL);
            case STRETCH_MEDIA -> toVaadinComponent().removeThemeVariants(CardVariant.STRETCH_MEDIA);
            case COVER_MEDIA -> toVaadinComponent().removeThemeVariants(CardVariant.COVER_MEDIA);
            case COMPACT -> toVaadinComponent().getStyle().remove("--vaadin-card-padding");
            case SELECTED -> toVaadinComponent().getElement().getClassList().remove("consulo-card-selected");
            case CENTERED -> toVaadinComponent().getElement().getClassList().remove("consulo-card-centered");
        }
    }

    @RequiredUIAccess
    @Override
    public Card setMedia(@Nullable Component media) {
        myMedia = media;
        toVaadinComponent().setMedia(media == null ? null : TargetVaadin.to(media));
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card setTitle(LocalizeValue title) {
        toVaadinComponent().setTitle(title == LocalizeValue.empty() ? null : title.get());
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card setSubtitle(LocalizeValue subtitle) {
        toVaadinComponent().setSubtitle(subtitle == LocalizeValue.empty() ? null : subtitle.get());
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card setHeader(@Nullable Component header) {
        myHeader = header;
        toVaadinComponent().setHeader(header == null ? null : TargetVaadin.to(header));
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card setHeaderPrefix(@Nullable Component prefix) {
        myHeaderPrefix = prefix;
        toVaadinComponent().setHeaderPrefix(prefix == null ? null : TargetVaadin.to(prefix));
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card setHeaderSuffix(@Nullable Component suffix) {
        myHeaderSuffix = suffix;
        toVaadinComponent().setHeaderSuffix(suffix == null ? null : TargetVaadin.to(suffix));
        return this;
    }

    @Override
    public Layout<LayoutConstraint> add(Component component, LayoutConstraint constraint) {
        toVaadinComponent().add(TargetVaadin.to(component));
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card addToFooter(Component component) {
        myFooter.add(component);
        toVaadinComponent().addToFooter(TargetVaadin.to(component));
        return this;
    }

    @Override
    public void forEachChild(Consumer<Component> consumer) {
        for (Component slot : new Component[]{myMedia, myHeaderPrefix, myHeader, myHeaderSuffix}) {
            if (slot != null) {
                consumer.accept(slot);
            }
        }
        super.forEachChild(consumer);
        myFooter.forEach(consumer);
    }
}
