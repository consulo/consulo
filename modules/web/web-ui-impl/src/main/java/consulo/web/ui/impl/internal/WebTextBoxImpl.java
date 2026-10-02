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

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.ToVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebTextBoxImpl extends VaadinComponentDelegate<WebTextBoxImpl.Vaadin> implements TextBox {
    // served straight from META-INF/resources - the theme goes through the vite bundle, which skips rebuilding
    // on css only changes
    @StyleSheet("/textBox/webTextBox.css")
    public class Vaadin extends TextField implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebTextBoxImpl.this;
        }
    }

    private final List<Validator<String>> myValidators = new CopyOnWriteArrayList<>();

    private @Nullable Component mySuffixComponent;
    private @Nullable Component myPrefixComponent;

    private boolean myFireListeners = true;

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public WebTextBoxImpl(String text) {
        setValue(text, false);

        Vaadin field = getVaadinComponent();

        // a field which reports only when it is left keeps the typed name from the server until then, and the
        // stroke which acts on that name - enter in a popup - arrives before it
        field.setValueChangeMode(ValueChangeMode.EAGER);

        field.addValueChangeListener(event -> {
            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, event.getValue()));
            }
        });
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;

        updateSuffixSlot();
    }

    protected void updateSuffixSlot() {
        toVaadinComponent().setSuffixComponent(toVaadinOrNull(mySuffixComponent));
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @Override
    @RequiredUIAccess
    public void setVisibleLength(int columns) {
        toVaadinComponent().getStyle().set("width", columns + "ch");
    }

    @Override
    public void setPrefixComponent(@Nullable Component prefixComponent) {
        myPrefixComponent = prefixComponent;

        toVaadinComponent().setPrefixComponent(toVaadinOrNull(prefixComponent));
    }

    @Override
    public @Nullable Component getPrefixComponent() {
        return myPrefixComponent;
    }

    protected static com.vaadin.flow.component.@Nullable Component toVaadinOrNull(@Nullable Component component) {
        if (component == null) {
            return null;
        }

        com.vaadin.flow.component.Component vaadinComponent = ((ToVaadinComponentWrapper) component).toVaadinComponent();
        vaadinComponent.getElement().getStyle().set("width", "auto");
        return vaadinComponent;
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public @Nullable String getValue() {
        return getVaadinComponent().getValue();
    }

    @Override
    @RequiredUIAccess
    public void setValue(@Nullable String value, boolean fireListeners) {
        boolean previous = myFireListeners;
        myFireListeners = fireListeners;
        try {
            getVaadinComponent().setValue(StringUtil.notNullize(value));
        }
        finally {
            myFireListeners = previous;
        }
    }

    @Override
    @RequiredUIAccess
    public void selectAll() {
        getVaadinComponent().getElement().executeJs("if (this.inputElement) { this.inputElement.select(); }");
    }

    @Override
    @RequiredUIAccess
    public void setEditable(boolean editable) {
        getVaadinComponent().setReadOnly(!editable);
    }

    @Override
    public boolean isEditable() {
        return !getVaadinComponent().isReadOnly();
    }

    @Override
    public Disposable addValidator(Validator<String> validator) {
        myValidators.add(validator);
        return () -> myValidators.remove(validator);
    }

    @Override
    @RequiredUIAccess
    public boolean validate() {
        Vaadin field = getVaadinComponent();
        String value = getValue();
        for (Validator<String> validator : myValidators) {
            ValidationInfo info = validator.validateValue(value);
            if (info != null) {
                field.setErrorMessage(info.getMessage());
                field.setInvalid(true);
                return false;
            }
        }

        field.setInvalid(false);
        field.setErrorMessage(null);
        return true;
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        getVaadinComponent().setPlaceholder(text.isEmpty() ? null : text.get());
    }
}
