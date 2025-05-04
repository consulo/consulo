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
package consulo.web.internal.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.TextWithMnemonic;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.vaadin.WebBooleanValueComponentBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public class WebCheckBoxImpl extends WebBooleanValueComponentBase<WebCheckBoxImpl.Vaadin> implements CheckBox {
    public class Vaadin extends Checkbox implements FromVaadinComponentWrapper {
        private LocalizeValue myLabelText = LocalizeValue.empty();

        public void setLabelText(LocalizeValue labelText) {
            myLabelText = labelText;
            updateLabelText();
        }

        public LocalizeValue getLabelText() {
            return myLabelText;
        }

        private void updateLabelText() {
            TextWithMnemonic textWithMnemonic = TextWithMnemonic.parse(myLabelText.get());

            setLabel(textWithMnemonic.getText());
        }

        @Nullable
        @Override
        public Component toUIComponent() {
            return WebCheckBoxImpl.this;
        }
    }

    public WebCheckBoxImpl() {
        super(false);
    }

    @Nonnull
    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    @Nonnull
    public LocalizeValue getLabelText() {
        return toVaadinComponent().getLabelText();
    }

    @RequiredUIAccess
    @Override
    public void setLabelText(@Nonnull LocalizeValue textValue) {
        UIAccess.assertIsUIThread();

        toVaadinComponent().setLabelText(textValue);
    }
}
