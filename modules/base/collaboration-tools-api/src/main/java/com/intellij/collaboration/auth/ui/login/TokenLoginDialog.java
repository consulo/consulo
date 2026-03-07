// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.auth.ui.login;

import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public final class TokenLoginDialog extends DialogWrapper {

    private final LoginModel model;
    @Nullable
    private final Flow<kotlin.Unit> userCustomExitSignal;
    private final Function<CoroutineScope, DialogPanel> centerPanelSupplier;
    private final CoroutineScope uiScope;

    public TokenLoginDialog(
        @Nullable Project project,
        @Nonnull CoroutineScope parentCs,
        @Nullable Component parent,
        @Nonnull LoginModel model,
        @NlsContexts.DialogTitle @Nonnull String title,
        @Nullable Flow<kotlin.Unit> userCustomExitSignal,
        @Nonnull Function<CoroutineScope, DialogPanel> centerPanelSupplier
    ) {
        super(project, parent, false, IdeModalityType.IDE);
        this.model = model;
        this.userCustomExitSignal = userCustomExitSignal;
        this.centerPanelSupplier = centerPanelSupplier;

        this.uiScope = ChildScopeKt.childScope(parentCs, getClass().getName(),
            Dispatchers.getEDT().plus(ModalityKt.asContextElement(Dialog.ModalityType.APPLICATION_MODAL))
        );

        setOKButtonText(CollaborationToolsLocalize.loginButton());
        setTitle(title);
        init();

        kotlinx.coroutines.BuildersKt.launch(
            uiScope,
            null,
            null,
            (scope, cont) -> FlowKt.collectLatest(
                model.getLoginState(),
                (state, innerCont) -> {
                    setOKActionEnabled(!(state instanceof LoginModel.LoginState.Connecting));

                    if (state instanceof LoginModel.LoginState.Failed) {
                        startTrackingValidation();
                    }
                    if (state instanceof LoginModel.LoginState.Connected) {
                        close(OK_EXIT_CODE);
                    }

                    return kotlin.Unit.INSTANCE;
                },
                cont
            )
        );

        if (userCustomExitSignal != null) {
            kotlinx.coroutines.BuildersKt.launch(
                uiScope,
                null,
                null,
                (scope, cont) -> FlowKt.collectLatest(
                    userCustomExitSignal,
                    (unit, innerCont) -> {
                        close(NEXT_USER_EXIT_CODE);
                        return kotlin.Unit.INSTANCE;
                    },
                    cont
                )
            );
        }
    }

    public TokenLoginDialog(
        @Nullable Project project,
        @Nonnull CoroutineScope parentCs,
        @Nullable Component parent,
        @Nonnull LoginModel model,
        @Nonnull Function<CoroutineScope, DialogPanel> centerPanelSupplier
    ) {
        this(
            project,
            parentCs,
            parent,
            model,
            CollaborationToolsLocalize.loginDialogTitle().get(),
            null,
            centerPanelSupplier
        );
    }

    @Override
    protected JComponent createCenterPanel() {
        return centerPanelSupplier.apply(uiScope);
    }

    @Override
    protected void doOKAction() {
        applyFields();
        if (!isOKActionEnabled()) {
            return;
        }

        kotlinx.coroutines.BuildersKt.launch(
            uiScope,
            null,
            null,
            (scope, cont) -> {
                Object result = model.login(cont);
                initValidation();
                return kotlin.Unit.INSTANCE;
            }
        );
    }
}
