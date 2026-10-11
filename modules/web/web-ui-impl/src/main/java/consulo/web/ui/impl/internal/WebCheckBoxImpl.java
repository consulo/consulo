/*
 * Copyright 2013-2019 consulo.io
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

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.CheckBoxStyle;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.internal.LocalizeValueWithMnemonic;
import consulo.ui.internal.SearchHighlightUtil;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.vaadin.WebBooleanValueComponentBase;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public class WebCheckBoxImpl extends WebBooleanValueComponentBase<WebCheckBoxImpl.Vaadin> implements CheckBox {
    public class Vaadin extends Checkbox implements FromVaadinComponentWrapper {
        private LocalizeValue myLabelText = LocalizeValue.empty();

        private @Nullable String mySearchHighlight;

        public Vaadin() {
            getStyle().set("white-space", "nowrap");
        }

        public void setLabelText(LocalizeValue labelText) {
            myLabelText = labelText;
            updateLabelText();
        }

        public LocalizeValue getLabelText() {
            return myLabelText;
        }

        public void setSearchHighlight(@Nullable String query) {
            mySearchHighlight = query;
            updateLabelText();
        }

        private void updateLabelText() {
            String text = LocalizeValueWithMnemonic.get(myLabelText).getText();
            if (SearchHighlightUtil.matches(text, mySearchHighlight)) {
                Span label = new Span();
                WebSearchHighlight.appendText(label.getElement(), text, mySearchHighlight);
                setLabelComponent(label);
            }
            else {
                setLabel(text);
            }
        }

        @Override
        public @Nullable Component toUIComponent() {
            return WebCheckBoxImpl.this;
        }
    }

    public WebCheckBoxImpl() {
        super(false);
    }

    @Override
    public void addStyle(CheckBoxStyle style) {
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public LocalizeValue getLabelText() {
        return toVaadinComponent().getLabelText();
    }

    @Override
    @RequiredUIAccess
    public void setLabelText(LocalizeValue textValue) {
        UIAccess.assertIsUIThread();

        toVaadinComponent().setLabelText(textValue);
    }

    @Override
    @RequiredUIAccess
    public void setSearchHighlight(@Nullable String query) {
        toVaadinComponent().setSearchHighlight(SearchHighlightUtil.normalizeQuery(query));
    }

    @Override
    public @Nullable String getSearchHighlight() {
        return toVaadinComponent().mySearchHighlight;
    }
}
