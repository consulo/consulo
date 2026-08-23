/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ui.ex.impl.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.dataContext.DataContext;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.ui.Component;
import consulo.ui.Size2D;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.WindowOwner;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogCancelAction;
import consulo.ui.ex.impl.internal.action.UnifiedActionToolbarImpl;
import consulo.ui.layout.DockLayout;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * The counterpart of {@code DesktopAwtDialogService} for the frontends which have no {@code DialogWrapper} - the
 * dialog is a {@link Window} whose content is the center component of the descriptor over a row of buttons built
 * from its actions.
 *
 * @author VISTALL
 * @since 2021-12-14
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedDialogServiceImpl implements DialogService {
    private static final Logger LOG = Logger.getInstance(UnifiedDialogServiceImpl.class);

    private static class DialogImpl implements Dialog {
        private static final String PLACE = "DialogRightButtons";

        private final DialogDescriptor myDescriptor;
        private final Window myWindow;

        private final CompletableFuture<DialogValue> myResult = new CompletableFuture<>();

        private DialogImpl(DialogDescriptor descriptor, @Nullable Window owner) {
            myDescriptor = descriptor;

            WindowOptions options = WindowOptions.builder().owner(owner).build();

            myWindow = Window.create(descriptor.getTitle().get(), options);
        }

        @Override
        @RequiredUIAccess
        public CompletableFuture<DialogValue> showAsync() {
            Component content;
            try {
                content = buildContent();
            }
            catch (Throwable e) {
                LOG.error("showAsync: cannot build content of " + myDescriptor.getClass().getName(), e);
                throw e;
            }

            myWindow.setContent(content);

            Size2D initialSize = myDescriptor.getInitialSize();
            if (initialSize != null) {
                myWindow.setSize(initialSize);
            }

            // the cross of the window and a close coming from the frontend are a cancel, the same way closing a
            // DialogWrapper is. doOkAction completes the result before it closes, so this only ever answers for a
            // close which no action handled
            myWindow.addCloseListener(event -> {
                if (myResult.isDone()) {
                    return;
                }

                myDescriptor.onHandleValue(new DialogCancelAction(), null);

                myResult.completeExceptionally(new IllegalArgumentException("reject"));
            });

            try {
                myWindow.show();
            }
            catch (Throwable e) {
                LOG.error("showAsync: cannot show window " + myWindow.getClass().getName(), e);
                throw e;
            }

            return myResult;
        }

        @RequiredUIAccess
        private Component buildContent() {
            DockLayout root = DockLayout.create();
            // the window is disposed when it closes, so anything the center component registers against it lives
            // exactly as long as the dialog does
            root.center(myDescriptor.createCenterComponent(myWindow));

            AnAction[] actions = myDescriptor.createActions(Platform.current().os().isMac());
            if (actions.length != 0) {
                root.bottom(buildButtons(actions));
            }

            return root;
        }

        @RequiredUIAccess
        private Component buildButtons(AnAction[] actions) {
            ActionGroup group = ActionGroup.newImmutableBuilder().addAll(actions).build();

            UnifiedActionToolbarImpl toolbar = new UnifiedActionToolbarImpl(PLACE, group, ActionToolbar.Style.BUTTON);
            // the ok and cancel actions read the dialog they belong to out of the context, and there is no component
            // to hang a provider off - the row is expanded off the ui thread, and a built context is already a
            // snapshot, so it needs no wrapping
            toolbar.setDataContextSupplier(() -> DataContext.builder().add(Dialog.KEY, this).build());

            myDescriptor.setOkButtonStateUpdater(toolbar::updateActionsAsync);

            toolbar.updateActionsAsync();

            DockLayout buttons = DockLayout.create();
            buttons.right(toolbar.getUIComponent());
            return buttons;
        }

        @Override
        @RequiredUIAccess
        public void doOkAction(DialogValue value) {
            myResult.complete(value);

            myWindow.close();
        }

        @Override
        @RequiredUIAccess
        public void doCancelAction() {
            myResult.completeExceptionally(new IllegalArgumentException("reject"));

            myWindow.close();
        }

        @Override
        public DialogDescriptor getDescriptor() {
            return myDescriptor;
        }

        @Override
        public Window getWindow() {
            return myWindow;
        }
    }

    @Override
    public Dialog build(DialogDescriptor descriptor) {
        return new DialogImpl(descriptor, null);
    }

    @Override
    public Dialog build(Component parent, DialogDescriptor descriptor) {
        return new DialogImpl(descriptor, findWindow(parent));
    }

    @Override
    public Dialog build(WindowOwner windowOwner, DialogDescriptor descriptor) {
        return new DialogImpl(descriptor, windowOwner.getWindow());
    }

    private static @Nullable Window findWindow(@Nullable Component component) {
        for (Component each = component; each != null; each = each.getParent()) {
            if (each instanceof Window window) {
                return window;
            }
        }
        return null;
    }
}
