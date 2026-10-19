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

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import consulo.ui.impl.ToastPresentation;
import consulo.ui.internal.ToastInternal;
import consulo.ui.style.ComponentColors;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.WebInputDetails;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.image.WebImageConverter;
import consulo.web.ui.impl.internal.vaadin.SimpleComponent;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-16
 */
public class WebToastImpl extends VaadinComponentDelegate<WebToastImpl.Vaadin> implements ToastInternal {
    @Tag("div")
    @StyleSheet("/toast/webToast.css")
    public class Vaadin extends SimpleComponent implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebToastImpl.this;
        }
    }

    private final Div myIconHolder = new Div();
    private final Span myTitleLabel = new Span();
    private final Span myTextLabel = new Span();
    private final Div myHtmlLabel = new Div();
    private final Div myActions = new Div();
    private final Div myToolbarHolder = new Div();

    private @Nullable ToastStyle myStyle;
    private boolean myGlassy;
    private boolean myLeftLine;
    private boolean myShadow;
    private @Nullable Image myIcon;
    private @Nullable Component myToolbar;

    public WebToastImpl() {
        Vaadin vaadin = getVaadinComponent();
        vaadin.addClassName("consulo-toast");

        myIconHolder.addClassName("consulo-toast-icon");
        myTitleLabel.addClassName("consulo-toast-title");
        myTextLabel.addClassName("consulo-toast-text");
        myHtmlLabel.addClassName("consulo-toast-text");
        myHtmlLabel.addClassName("consulo-toast-html");
        WebInputDetails.addClickEventListener(myHtmlLabel.getElement(), "click", (event, details) -> {
            String href = event.getEventData().path("event.target.closest('a').getAttribute('href')").asString("");
            getListenerDispatcher(HyperlinkEvent.class).onEvent(new HyperlinkEvent(this, href, details));
        })
            .setFilter("event.target.closest('a') != null")
            .addEventData("event.target.closest('a').getAttribute('href')")
            .preventDefault();
        myActions.addClassName("consulo-toast-actions");
        myToolbarHolder.addClassName("consulo-toast-toolbar");

        Div center = new Div(myTitleLabel, myTextLabel, myHtmlLabel, myActions);
        center.addClassName("consulo-toast-center");

        vaadin.getElement().appendChild(myIconHolder.getElement(), center.getElement(), myToolbarHolder.getElement());

        myTitleLabel.setVisible(false);
        myTextLabel.setVisible(false);
        myHtmlLabel.setVisible(false);
        myActions.setVisible(false);
        myToolbarHolder.setVisible(false);

        updateColors();
        updateIcon();
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public void addStyle(ToastStyle style) {
        if (style == ToastStyle.GLASSY) {
            myGlassy = true;
        }
        else if (style == ToastStyle.LEFT_LINE) {
            myLeftLine = true;
            getVaadinComponent().addClassName("consulo-toast-left-line");
            updateIcon();
        }
        else {
            myStyle = style;
            updateIcon();
        }
        updateColors();
    }

    @RequiredUIAccess
    @Override
    public Toast setTitle(LocalizeValue title) {
        myTitleLabel.setText(title.get());
        myTitleLabel.setVisible(title != LocalizeValue.empty());
        return this;
    }

    @RequiredUIAccess
    @Override
    public Toast setText(LocalizeValue text) {
        myHtmlLabel.setVisible(false);
        myTextLabel.setText(text.get());
        myTextLabel.setVisible(text != LocalizeValue.empty());
        return this;
    }

    @RequiredUIAccess
    @Override
    public Toast setHtmlText(LocalizeValue html) {
        myTextLabel.setVisible(false);
        myHtmlLabel.getElement().setProperty("innerHTML", html == LocalizeValue.empty() ? "" : html.get());
        myHtmlLabel.setVisible(html != LocalizeValue.empty());
        return this;
    }

    @RequiredUIAccess
    @Override
    public Toast setIcon(@Nullable Image icon) {
        myIcon = icon;
        updateIcon();
        return this;
    }

    @RequiredUIAccess
    @Override
    public Toast addAction(LocalizeValue text, @RequiredUIAccess Runnable action) {
        Span link = new Span(text.get());
        link.addClassName("consulo-toast-link");
        link.getStyle().set("color", WebColors.toStyleCssColor(ComponentColors.LINK_FOREGROUND));
        link.addClickListener(event -> action.run());
        myActions.add(link);
        myActions.setVisible(true);
        return this;
    }

    @RequiredUIAccess
    @Override
    public Toast setToolbarComponent(@Nullable Component toolbar) {
        myToolbar = toolbar;
        myToolbarHolder.removeAll();
        if (toolbar != null) {
            myToolbarHolder.add(TargetVaadin.to(toolbar));
        }
        myToolbarHolder.setVisible(toolbar != null);
        return this;
    }

    @RequiredUIAccess
    @Override
    public void setShadow(boolean shadow) {
        myShadow = shadow;
        getVaadinComponent().setClassName("consulo-toast-shadow", shadow);
        updateColors();
    }

    private void updateColors() {
        Vaadin vaadin = getVaadinComponent();
        String background = WebColors.toStyleCssColor(ToastPresentation.background(myStyle));
        if (myLeftLine) {
            vaadin.getStyle().set("background-color", "transparent");
            vaadin.getStyle().set("--consulo-toast-line-color", WebColors.toStyleCssColor(ToastPresentation.border(myStyle)));
            return;
        }
        if (myGlassy && myShadow) {
            int opacity = Math.round(ToastPresentation.GLASSY_OPACITY * 100);
            String blur = "blur(" + ToastPresentation.GLASSY_BLUR + "px)";
            vaadin.getStyle().set("background-color", "color-mix(in srgb, " + background + " " + opacity + "%, transparent)");
            vaadin.getStyle().set("backdrop-filter", blur);
            vaadin.getStyle().set("-webkit-backdrop-filter", blur);
        }
        else {
            vaadin.getStyle().set("background-color", background);
            vaadin.getStyle().remove("backdrop-filter");
            vaadin.getStyle().remove("-webkit-backdrop-filter");
        }
        vaadin.getStyle().set("border-color", WebColors.toStyleCssColor(ToastPresentation.border(myStyle)));
    }

    private void updateIcon() {
        myIconHolder.removeAll();
        Image icon = myIcon != null ? myIcon : myLeftLine ? null : ToastPresentation.icon(myStyle);
        if (icon != null) {
            myIconHolder.add(WebImageConverter.getImage(icon));
        }
        myIconHolder.setVisible(icon != null);
    }
}
