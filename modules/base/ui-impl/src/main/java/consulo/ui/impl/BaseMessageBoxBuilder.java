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
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageBoxRemember;
import consulo.ui.MessageButtonRole;
import consulo.ui.MessageLinkHandler;
import consulo.ui.MessageSeverity;
import consulo.ui.MessageTextFormat;
import consulo.ui.RememberScope;
import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Holds what a message box was asked for. A frontend subclass only has to render it and answer.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public abstract class BaseMessageBoxBuilder<V> implements MessageBoxBuilder<V> {
    protected static class ButtonImpl<V> {
        public final MessageButtonRole myRole;
        public final @Nullable LocalizeValue myText;
        public final Supplier<V> myValue;
        public boolean myDefault;
        public boolean myExit;

        ButtonImpl(MessageButtonRole role, @Nullable LocalizeValue text, Supplier<V> value) {
            myRole = role;
            myText = text;
            myValue = value;
        }

        public LocalizeValue label() {
            return myText != null ? myText : MessagePresentation.label(myRole);
        }
    }

    protected LocalizeValue myTitle = LocalizeValue.empty();
    protected LocalizeValue myText = LocalizeValue.empty();
    protected LocalizeValue myDetail = LocalizeValue.empty();
    protected MessageSeverity mySeverity = MessageSeverity.INFO;
    protected MessageTextFormat myTextFormat = MessageTextFormat.PLAIN;
    protected @Nullable Image myIcon;
    protected @Nullable MessageLinkHandler myLinkHandler;
    protected final List<ButtonImpl<V>> myButtons = new ArrayList<>();
    protected @Nullable MessageBoxRemember<V> myRemember;
    protected @Nullable Runnable myHelpAction;
    protected @Nullable Supplier<V> myExitValue;

    @Override
    public MessageBoxBuilder<V> title(LocalizeValue title) {
        myTitle = title;
        return this;
    }

    @Override
    public MessageBoxBuilder<V> text(LocalizeValue text) {
        myText = text;
        return this;
    }

    @Override
    public MessageBoxBuilder<V> detail(LocalizeValue detail) {
        myDetail = detail;
        return this;
    }

    @Override
    public MessageBoxBuilder<V> asPlain() {
        mySeverity = MessageSeverity.NONE;
        return this;
    }

    @Override
    public MessageBoxBuilder<V> asInfo() {
        mySeverity = MessageSeverity.INFO;
        return this;
    }

    @Override
    public MessageBoxBuilder<V> asWarning() {
        mySeverity = MessageSeverity.WARNING;
        return this;
    }

    @Override
    public MessageBoxBuilder<V> asError() {
        mySeverity = MessageSeverity.ERROR;
        return this;
    }

    @Override
    public MessageBoxBuilder<V> asQuestion() {
        mySeverity = MessageSeverity.QUESTION;
        return this;
    }

    @Override
    public MessageBoxBuilder<V> icon(Image icon) {
        myIcon = icon;
        return this;
    }

    @Override
    public MessageBoxBuilder<V> richText() {
        myTextFormat = MessageTextFormat.RICH;
        return this;
    }

    @Override
    public MessageBoxBuilder<V> richText(MessageLinkHandler linkHandler) {
        myTextFormat = MessageTextFormat.RICH;
        myLinkHandler = linkHandler;
        return this;
    }

    @Override
    public MessageBoxBuilder<V> button(MessageButtonRole role, Supplier<V> valueGetter) {
        return addButton(role, null, valueGetter);
    }

    @Override
    public MessageBoxBuilder<V> button(MessageButtonRole role, LocalizeValue text, Supplier<V> valueGetter) {
        return addButton(role, text, valueGetter);
    }

    private MessageBoxBuilder<V> addButton(MessageButtonRole role, @Nullable LocalizeValue text, Supplier<V> valueGetter) {
        for (ButtonImpl<V> button : myButtons) {
            if (button.myRole == role) {
                throw new IllegalArgumentException("duplicate button role: " + role);
            }
        }
        myButtons.add(new ButtonImpl<>(role, text, valueGetter));
        return this;
    }

    @Override
    public MessageBoxBuilder<V> asDefaultButton() {
        if (!myButtons.isEmpty()) {
            for (ButtonImpl<V> button : myButtons) {
                button.myDefault = false;
            }
            myButtons.get(myButtons.size() - 1).myDefault = true;
        }
        return this;
    }

    @Override
    public MessageBoxBuilder<V> asExitButton() {
        if (!myButtons.isEmpty()) {
            for (ButtonImpl<V> button : myButtons) {
                button.myExit = false;
            }
            ButtonImpl<V> last = myButtons.get(myButtons.size() - 1);
            last.myExit = true;
            myExitValue = last.myValue;
        }
        return this;
    }

    @Override
    public MessageBoxBuilder<V> exitValue(Supplier<V> valueGetter) {
        myExitValue = valueGetter;
        return this;
    }

    @Override
    public MessageBoxBuilder<V> help(Runnable action) {
        myHelpAction = action;
        return this;
    }

    @Override
    public MessageBoxBuilder<V> remember(MessageBoxRemember<V> remember) {
        myRemember = remember;
        return this;
    }

    /**
     * Hands everything this was asked for to another builder, for a frontend which decides at show
     * time that it cannot represent the request itself.
     */
    public void copyInto(BaseMessageBoxBuilder<V> target) {
        target.myTitle = myTitle;
        target.myText = myText;
        target.myDetail = myDetail;
        target.mySeverity = mySeverity;
        target.myTextFormat = myTextFormat;
        target.myIcon = myIcon;
        target.myLinkHandler = myLinkHandler;
        target.myButtons.addAll(myButtons);
        target.myRemember = myRemember;
        target.myHelpAction = myHelpAction;
        target.myExitValue = myExitValue;
    }

    /**
     * A box with nothing to press still has to be answerable, and a help affordance is an answer
     * like any other so that every frontend places it the way its platform does.
     */
    protected void prepare() {
        if (myHelpAction != null && !hasRole(MessageButtonRole.HELP)) {
            addButton(MessageButtonRole.HELP, null, () -> exitValueOrNull());
        }

        if (myButtons.isEmpty()) {
            addButton(MessageButtonRole.OK, null, () -> exitValueOrNull());
            asDefaultButton();
            asExitButton();
        }
    }

    protected boolean hasRole(MessageButtonRole role) {
        for (ButtonImpl<V> button : myButtons) {
            if (button.myRole == role) {
                return true;
            }
        }
        return false;
    }

    /**
     * Runs the help affordance, if this is the button carrying it.
     */
    protected boolean runHelpIfNeeded(@Nullable ButtonImpl<V> pressed) {
        Runnable helpAction = myHelpAction;
        if (helpAction != null && pressed != null && pressed.myRole == MessageButtonRole.HELP) {
            helpAction.run();
            return true;
        }
        return false;
    }

    /**
     * The answer already remembered, or null when the box has to be shown.
     */
    protected @Nullable V rememberedValue() {
        return myRemember != null ? myRemember.getValue() : null;
    }

    /**
     * Records the answer if the checkbox was ticked and the scope allows it.
     */
    protected void storeRemembered(@Nullable ButtonImpl<V> pressed, boolean checked, V value) {
        MessageBoxRemember<V> remember = myRemember;
        if (remember == null || !checked) {
            return;
        }

        RememberScope scope = remember.getScope();
        boolean allowed = switch (scope) {
            case ON_ANY_ANSWER -> true;
            case ON_ANSWER -> pressed != null;
            case ON_ACCEPT -> pressed != null && MessagePresentation.isAccept(pressed.myRole);
        };

        if (allowed) {
            remember.setValue(value);
        }
    }

    @SuppressWarnings("unchecked")
    protected V exitValueOrNull() {
        Supplier<V> exitValue = myExitValue;
        return exitValue != null ? exitValue.get() : (V)null;
    }
}
