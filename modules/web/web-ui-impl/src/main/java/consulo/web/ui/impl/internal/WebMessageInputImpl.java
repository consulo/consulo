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

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.messages.MessageInputI18n;
import com.vaadin.flow.dom.Element;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.messageList.MessageInput;
import consulo.ui.messageList.MessageInputStyle;
import consulo.ui.messageList.MessageInputSubmitEvent;
import consulo.ui.messageList.MessageInputTextChangeEvent;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.image.WebImageUrl;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-13
 */
public class WebMessageInputImpl extends VaadinComponentDelegate<WebMessageInputImpl.Vaadin> implements MessageInput {
    @StyleSheet("/messageList/webMessageInput.css")
    public class Vaadin extends com.vaadin.flow.component.messages.MessageInput implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebMessageInputImpl.this;
        }
    }

    private static final int TEXT_CHANGE_DEBOUNCE_MS = 250;

    private LocalizeValue myPlaceholder = LocalizeValue.empty();
    private LocalizeValue mySendText = LocalizeValue.empty();
    private @Nullable Image mySendIcon;
    private @Nullable Component mySuffixComponent;

    public WebMessageInputImpl() {
        toVaadinComponent().addAttachListener(event -> placeSuffix());
        toVaadinComponent().addSubmitListener(event -> {
            fireTextChange("");
            getListenerDispatcher(MessageInputSubmitEvent.class).onEvent(new MessageInputSubmitEvent(this, event.getValue()));
        });
        toVaadinComponent().getElement()
            .addEventListener("input", event -> fireTextChange(event.getEventData().get("event.target.value").asString()))
            .addEventData("event.target.value")
            .debounce(TEXT_CHANGE_DEBOUNCE_MS);
    }

    private void fireTextChange(@Nullable String text) {
        getListenerDispatcher(MessageInputTextChangeEvent.class).onEvent(new MessageInputTextChangeEvent(this, text == null ? "" : text));
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @RequiredUIAccess
    @Override
    public void setPlaceholder(LocalizeValue placeholder) {
        myPlaceholder = placeholder;
        updateI18n();
    }

    @RequiredUIAccess
    @Override
    public void setSendText(LocalizeValue sendText) {
        mySendText = sendText;
        updateI18n();
    }

    @RequiredUIAccess
    @Override
    public void insertText(String text) {
        toVaadinComponent().getElement().executeJs(
            "const input = this.querySelector('vaadin-text-area')?.inputElement;"
                + " if (!input) { this.value = (this.value || '') + $0; return; }"
                + " input.focus();"
                + " input.setRangeText($0, input.selectionStart, input.selectionEnd, 'end');"
                + " input.dispatchEvent(new Event('input', {bubbles: true}));",
            text
        );
    }

    @RequiredUIAccess
    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        Component old = mySuffixComponent;
        if (old != null) {
            toVaadinComponent().getElement().removeChild(TargetVaadin.to(old).getElement());
        }
        mySuffixComponent = suffixComponent;
        if (suffixComponent != null) {
            toVaadinComponent().getElement().appendChild(TargetVaadin.to(suffixComponent).getElement());
            placeSuffix();
        }
    }

    /**
     * Vaadin keeps its own send button only while nothing else fills the button slot, so the suffix goes into the
     * suffix slot of the text area instead - and again on every attach, since the client builds that area anew.
     */
    private void placeSuffix() {
        Component suffix = mySuffixComponent;
        if (suffix == null) {
            return;
        }
        Element element = TargetVaadin.to(suffix).getElement();
        toVaadinComponent().getElement().executeJs(
            "const area = this.querySelector('vaadin-text-area'); if (area && $0) { $0.setAttribute('slot', 'suffix');"
                + " $0.style.width = 'auto'; $0.style.flex = 'none'; area.appendChild($0); }",
            element
        );
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @Override
    public void addStyle(MessageInputStyle style) {
        switch (style) {
            case INPLACE -> toVaadinComponent().getElement().getThemeList().add("consulo-inplace");
        }
    }

    @RequiredUIAccess
    @Override
    public void setSendIcon(@Nullable Image icon) {
        mySendIcon = icon;
        updateI18n();
    }

    private void updateI18n() {
        MessageInputI18n i18n = new MessageInputI18n();
        if (myPlaceholder != LocalizeValue.empty()) {
            i18n.setMessage(myPlaceholder.get());
        }
        if (mySendText != LocalizeValue.empty()) {
            i18n.setSend(mySendText.get());
        }
        toVaadinComponent().setI18n(i18n);

        Vaadin vaadin = toVaadinComponent();
        Image icon = mySendIcon;
        String url = icon == null ? null : WebImageUrl.toURL(icon);
        if (url != null) {
            vaadin.getElement().getThemeList().add("consulo-send-icon");
            vaadin.getStyle().set("--consulo-message-input-send-icon", "url('" + url + "')");
            vaadin.setTooltipText(mySendText == LocalizeValue.empty() ? null : mySendText.get());
        }
        else {
            vaadin.getElement().getThemeList().remove("consulo-send-icon");
            vaadin.getStyle().remove("--consulo-message-input-send-icon");
            vaadin.setTooltipText(null);
        }
    }

    @Override
    public void focus() {
        toVaadinComponent().getElement().executeJs("const field = this.querySelector('textarea, vaadin-message-input-text-area, vaadin-text-area'); (field || this).focus();");
    }
}
