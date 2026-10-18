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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.CommitSession;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Asks for the options of a commit session (or any other options form) before the action runs: OK follows the
 * form's own check, and its validation message is shown under it.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public class SessionDialogDescriptor extends DialogDescriptor {
    private final BooleanSupplier myCanExecute;
    private final Supplier<LocalizeValue> myValidation;
    private final @Nullable String myHelpId;
    private final Label myErrorLabel = Label.create();
    private @Nullable Component myComponent;
    private boolean myShown;

    public SessionDialogDescriptor(LocalizeValue title,
                                   BooleanSupplier canExecute,
                                   Supplier<LocalizeValue> validation,
                                   @Nullable String helpId) {
        super(title);
        myCanExecute = canExecute;
        myValidation = validation;
        myHelpId = helpId;
        myErrorLabel.setForegroundColor(ComponentColors.ERROR_FOREGROUND);
    }

    /**
     * Shows the session options, if it has any. A cancelled dialog cancels the session.
     *
     * @return whether the session is to be executed
     */
    @RequiredUIAccess
    public static CompletableFuture<Boolean> configureCommitSession(Project project,
                                                                    LocalizeValue title,
                                                                    CommitSession session,
                                                                    Collection<Change> changes,
                                                                    String commitMessage) {
        LocalizeValue sessionTitle = session.getAdditionalConfigurationTitle();
        SessionDialogDescriptor descriptor = new SessionDialogDescriptor(
            sessionTitle.isEmpty() ? title.removeEllipsis() : sessionTitle,
            () -> session.canExecute(changes, commitMessage),
            session::validateFields,
            session.getHelpId()
        );
        Component component = session.getAdditionalConfigurationUI(changes, commitMessage, descriptor::refresh);
        if (component == null) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }
        return descriptor.show(project, component).thenApply(ok -> {
            if (!ok) {
                session.executionCanceled();
            }
            return ok;
        });
    }

    @RequiredUIAccess
    public CompletableFuture<Boolean> show(Project project, Component component) {
        myComponent = component;
        Dialog dialog = project.getApplication().getInstance(DialogService.class).build(project, this);
        return dialog.showAsync().handleAsync((value, error) -> error == null && value != null, UIAccess.current());
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        VerticalLayout layout = VerticalLayout.create();
        if (myComponent != null) {
            layout.add(myComponent);
        }
        layout.add(myErrorLabel);
        myShown = true;
        myErrorLabel.setText(myValidation.get());
        return layout;
    }

    @Override
    public @Nullable String getHelpId() {
        return myHelpId;
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return myCanExecute.getAsBoolean();
    }

    @RequiredUIAccess
    public void refresh() {
        if (!myShown) {
            return;
        }
        myErrorLabel.setText(myValidation.get());
        updateOkButtonState();
    }
}
