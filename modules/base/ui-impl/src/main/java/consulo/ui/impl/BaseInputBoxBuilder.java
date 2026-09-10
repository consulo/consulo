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
package consulo.ui.impl;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputValidator;
import consulo.ui.MessageSeverity;
import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Holds what an input box was asked for. A frontend subclass only has to render it and answer.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public abstract class BaseInputBoxBuilder<V, C extends Component> implements InputBoxBuilder<V, C> {
    protected LocalizeValue myTitle = LocalizeValue.empty();
    protected LocalizeValue myText = LocalizeValue.empty();
    protected MessageSeverity mySeverity = MessageSeverity.QUESTION;
    protected @Nullable Image myIcon;
    protected @Nullable V myInitialValue;
    protected @Nullable InputValidator<V> myValidator;
    protected LocalizeValue myConfirmText = LocalizeValue.empty();
    protected LocalizeValue myCancelText = LocalizeValue.empty();
    protected @Nullable Runnable myHelpAction;
    protected final List<Consumer<C>> mySetups = new ArrayList<>();

    @Override
    public InputBoxBuilder<V, C> setupComponent(Consumer<C> setup) {
        mySetups.add(setup);
        return this;
    }

    @Override
    public InputBoxBuilder<V, C> title(LocalizeValue title) {
        myTitle = title;
        return this;
    }

    @Override
    public InputBoxBuilder<V, C> text(LocalizeValue text) {
        myText = text;
        return this;
    }

    @Override
    public InputBoxBuilder<V, C> asPlain() {
        mySeverity = MessageSeverity.NONE;
        return this;
    }

    @Override
    public InputBoxBuilder<V, C> asInfo() {
        mySeverity = MessageSeverity.INFO;
        return this;
    }

    @Override
    public InputBoxBuilder<V, C> asWarning() {
        mySeverity = MessageSeverity.WARNING;
        return this;
    }

    @Override
    public InputBoxBuilder<V, C> asError() {
        mySeverity = MessageSeverity.ERROR;
        return this;
    }

    @Override
    public InputBoxBuilder<V, C> asQuestion() {
        mySeverity = MessageSeverity.QUESTION;
        return this;
    }

    @Override
    public InputBoxBuilder<V, C> icon(Image icon) {
        myIcon = icon;
        return this;
    }

    @Override
    public InputBoxBuilder<V, C> value(V initialValue) {
        myInitialValue = initialValue;
        return this;
    }

    @Override
    public InputBoxBuilder<V, C> validator(InputValidator<V> validator) {
        myValidator = validator;
        return this;
    }

    @Override
    public InputBoxBuilder<V, C> confirmText(LocalizeValue text) {
        myConfirmText = text;
        return this;
    }

    @Override
    public InputBoxBuilder<V, C> cancelText(LocalizeValue text) {
        myCancelText = text;
        return this;
    }

    @Override
    public InputBoxBuilder<V, C> help(Runnable action) {
        myHelpAction = action;
        return this;
    }

    /**
     * Hands everything this was asked for to another builder, for a frontend which decides at show
     * time that it cannot represent the request itself.
     */
    public void copyInto(BaseInputBoxBuilder<V, C> target) {
        target.myTitle = myTitle;
        target.myText = myText;
        target.mySeverity = mySeverity;
        target.myIcon = myIcon;
        target.myInitialValue = myInitialValue;
        target.myValidator = myValidator;
        target.myConfirmText = myConfirmText;
        target.myCancelText = myCancelText;
        target.myHelpAction = myHelpAction;
        target.mySetups.addAll(mySetups);
    }

    /**
     * Applies everything {@link #setupComponent} was given, in the order it was given.
     */
    protected void applySetup(C component) {
        for (Consumer<C> setup : mySetups) {
            setup.accept(component);
        }
    }

    /**
     * A text value is trimmed; anything else is answered as entered.
     */
    @SuppressWarnings("unchecked")
    protected @Nullable V normalize(@Nullable V value) {
        return value instanceof String text ? (V)text.trim() : value;
    }
}
