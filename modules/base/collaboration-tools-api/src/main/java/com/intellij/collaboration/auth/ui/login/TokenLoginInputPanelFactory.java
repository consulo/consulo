// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.auth.ui.login;

import com.intellij.collaboration.async.LaunchNowKt;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPanelFactory;
import com.intellij.collaboration.ui.codereview.list.error.ErrorStatusPresenter;
import com.intellij.collaboration.util.URIUtil;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.ui.ex.awt.AnimatedIcon;
import consulo.ui.ex.awt.ValidationInfo;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.FlowKt;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.function.Consumer;

/**
 * Basic token login interface with validation and progress indication.
 * <p>
 * To save data in {@code model} one should call {@link DialogPanel#apply()}.
 */
public final class TokenLoginInputPanelFactory {
    private final TokenLoginPanelModel model;

    public TokenLoginInputPanelFactory(@Nonnull TokenLoginPanelModel model) {
        this.model = model;
    }

    @Nonnull
    public DialogPanel createIn(
        @Nonnull CoroutineScope cs,
        boolean serverFieldDisabled,
        @Nullable @NlsContexts.DetailedDescription String tokenNote,
        @Nullable ErrorStatusPresenter<Throwable> errorPresenter
    ) {
        return createIn(cs, serverFieldDisabled, null, tokenNote, errorPresenter, null);
    }

    @Nonnull
    public DialogPanel createIn(
        @Nonnull CoroutineScope cs,
        boolean serverFieldDisabled,
        @Nullable @NlsContexts.DetailedDescription String serverNote,
        @Nullable @NlsContexts.DetailedDescription String tokenNote,
        @Nullable ErrorStatusPresenter<Throwable> errorPresenter
    ) {
        return createIn(cs, serverFieldDisabled, serverNote, tokenNote, errorPresenter, null);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public DialogPanel createIn(
        @Nonnull CoroutineScope cs,
        boolean serverFieldDisabled,
        @Nullable @NlsContexts.DetailedDescription String serverNote,
        @Nullable @NlsContexts.DetailedDescription String tokenNote,
        @Nullable ErrorStatusPresenter<Throwable> errorPresenter,
        @Nullable Consumer<Panel> footer
    ) {
        ExtendableTextField serverTextField = new ExtendableTextField();
        ExtendableTextComponent.Extension progressExtension = ExtendableTextComponent.Extension
            .create(AnimatedIcon.Default.INSTANCE, CollaborationToolsLocalize.loginProgress().get(), null);

        SingleValueModel<Boolean> progressModel = new SingleValueModel<>(false);
        progressModel.addAndInvokeListener(inProgress -> {
            if (inProgress) {
                serverTextField.addExtension(progressExtension);
            }
            else {
                serverTextField.removeExtension(progressExtension);
            }
        });

        LaunchNowKt.launchNow(
            cs,
            (scope, cont) -> FlowKt.collectLatest(
                model.getLoginState(),
                (state, innerCont) -> {
                    progressModel.setValue(state instanceof LoginModel.LoginState.Connecting);
                    return kotlin.Unit.INSTANCE;
                },
                cont
            )
        );

        return BuilderKt.panel(p -> {
            p.row(
                CollaborationToolsLocalize.loginFieldServer().get(),
                r -> {
                    r.cell(serverTextField)
                        .bind(
                            () -> model.getServerUri(),
                            v -> {
                                model.setServerUri(v);
                            },
                            new MutableProperty<>() {
                                public String get() {
                                    return model.getServerUri();
                                }

                                public void set(String value) {
                                    model.setServerUri(value);
                                }
                            }
                        )
                        .align(AlignX.FILL)
                        .resizableColumn()
                        .comment(serverNote, MAX_LINE_LENGTH_WORD_WRAP)
                        .enabledIf(toComponentPredicate(progressModel, !serverFieldDisabled))
                        .validationOnApply(field -> {
                            if (field.getText().isBlank()) {
                                return new com.intellij.openapi.ui.ValidationInfo(
                                    CollaborationToolsLocalize.loginServerEmpty().get(),
                                    field
                                );
                            }
                            if (!URIUtil.isValidHttpUri(field.getText())) {
                                return new com.intellij.openapi.ui.ValidationInfo(
                                    CollaborationToolsLocalize.loginServerInvalid().get(),
                                    field
                                );
                            }
                            return null;
                        });
                    return kotlin.Unit.INSTANCE;
                }
            );
            p.row(
                CollaborationToolsLocalize.loginFieldToken().get(),
                r -> {
                    Cell<JPasswordField> tokenCell = r.passwordField()
                        .bind(
                            () -> model.getToken(),
                            v -> model.setToken(v),
                            new MutableProperty<>() {
                                public String get() {
                                    return model.getToken();
                                }

                                public void set(String value) {
                                    model.setToken(value);
                                }
                            }
                        )
                        .align(AlignX.FILL)
                        .resizableColumn()
                        .comment(tokenNote, MAX_LINE_LENGTH_WORD_WRAP)
                        .enabledIf(toComponentPredicate(progressModel, true))
                        .validationOnApply(field -> {
                            if (field.getPassword().length == 0) {
                                return new ValidationInfo(
                                    CollaborationToolsLocalize.loginTokenEmpty(),
                                    field
                                );
                            }
                            return null;
                        })
                        .focused();
                    tokenCell.onReset(c -> {
                        c.setText("");
                        return kotlin.Unit.INSTANCE;
                    });
                    JPasswordField tokenField = tokenCell.getComponent();

                    if (model instanceof LoginTokenGenerator generator) {
                        r.button(
                            CollaborationToolsLocalize.loginTokenGenerate().get(),
                            e -> {
                                generator.generateToken(serverTextField.getText());
                                IdeFocusManager.findInstanceByComponent(tokenField).requestFocus(tokenField, false);
                            }
                        ).enabledIf(new TokenGeneratorPredicate(generator, serverTextField));
                    }
                    return kotlin.Unit.INSTANCE;
                }
            );
            p.row(r -> {
                if (errorPresenter != null) {
                    JComponent errorPanel = ErrorStatusPanelFactory.create(cs, LoginModel.getErrorFlow(model), errorPresenter,
                        ErrorStatusPanelFactory.Alignment.LEFT
                    );
                    r.cell(errorPanel);
                }
                return kotlin.Unit.INSTANCE;
            });
            if (footer != null) {
                footer.accept(p);
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    @Nonnull
    private static ComponentPredicate toComponentPredicate(@Nonnull SingleValueModel<Boolean> progressModel, boolean defaultState) {
        return new ComponentPredicate() {
            @Override
            public boolean invoke() {
                return !progressModel.getValue() && defaultState;
            }

            @Override
            public void addListener(@Nonnull kotlin.jvm.functions.Function1<? super Boolean, kotlin.Unit> listener) {
                progressModel.addListener(value -> listener.invoke(!value && defaultState));
            }
        };
    }

    private static final class TokenGeneratorPredicate extends ComponentPredicate {
        private final LoginTokenGenerator generator;
        private final ExtendableTextField serverTextField;

        TokenGeneratorPredicate(@Nonnull LoginTokenGenerator generator, @Nonnull ExtendableTextField serverTextField) {
            this.generator = generator;
            this.serverTextField = serverTextField;
        }

        @Override
        public boolean invoke() {
            return generator.canGenerateToken(serverTextField.getText());
        }

        @Override
        public void addListener(@Nonnull kotlin.jvm.functions.Function1<? super Boolean, kotlin.Unit> listener) {
            serverTextField.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(@Nonnull DocumentEvent e) {
                    listener.invoke(invoke());
                }
            });
        }
    }
}
