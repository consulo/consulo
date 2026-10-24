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
import consulo.application.ApplicationPropertiesComponent;
import consulo.dataContext.DataContext;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.HasFocus;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.internal.UIAccessInternal;
import consulo.ui.WidthAndHeight;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.WindowOwner;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
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
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.ex.dialog.WholeLeftDialogDescriptor;
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

        private static final Space CONTENT_VERTICAL_PADDING = Space.MEDIUM;
        private static final Space CONTENT_HORIZONTAL_PADDING = Space.LARGE;

        private static final int LEFT_PROPORTION = 30;

        private final DialogDescriptor myDescriptor;
        private final Window myWindow;

        private final CompletableFuture<DialogValue> myResult = new CompletableFuture<>();

        private @Nullable Button myDefaultButton;

        private @Nullable UIAccessInternal myModalAccess;

        private DialogImpl(DialogDescriptor descriptor, @Nullable Window owner) {
            myDescriptor = descriptor;

            WindowOptions.Builder options = WindowOptions.builder()
                .owner(owner)
                .dimensionKey(descriptor.getDimensionServiceKey());
            if (descriptor.isReattachable()) {
                options.reattachable();
            }
            if (!descriptor.isModal()) {
                options.disableModal();
            }

            myWindow = Window.create(descriptor.getTitle().get(), options.build());
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

            WidthAndHeight initialSize = myDescriptor.getInitialSize();
            if (initialSize != null) {
                myWindow.setSize(initialSize.width(), initialSize.height());
            }

            // the cross of the window and a close coming from the frontend are a cancel, the same way closing a
            // DialogWrapper is. doOkAction completes the result before it closes, so this only ever answers for a
            // close which no action handled
            myWindow.addCloseListener(event -> {
                leaveModal();

                if (myResult.isDone()) {
                    return;
                }

                myDescriptor.onHandleValue(new DialogCancelAction(), null);

                myResult.completeExceptionally(new IllegalArgumentException("reject"));
            });

            myWindow.addAttachListener(event -> followModality());

            myWindow.setCloseHandler(window -> cancelByUser());

            if (myDescriptor.isModal()) {
                enterModal(UIAccess.current());
            }

            try {
                myWindow.show();
            }
            catch (Throwable e) {
                leaveModal();
                LOG.error("showAsync: cannot show window " + myWindow.getClass().getName(), e);
                throw e;
            }

            if (myDescriptor.getPreferredFocusedComponent() instanceof HasFocus focusTarget) {
                focusTarget.focus();
            }

            return myResult;
        }

        @RequiredUIAccess
        private void enterModal(@Nullable UIAccess uiAccess) {
            if (uiAccess instanceof UIAccessInternal internal) {
                internal.enterModal(this);
                myModalAccess = internal;
            }
        }

        @RequiredUIAccess
        private void leaveModal() {
            UIAccessInternal access = myModalAccess;
            myModalAccess = null;
            if (access != null && access.isValid()) {
                access.leaveModal(this);
            }
        }

        @RequiredUIAccess
        private void followModality() {
            UIAccessInternal entered = myModalAccess;
            UIAccess current = myWindow.getUIAccess();
            if (entered == null || current == null || UIAccessInternal.original(current) == UIAccessInternal.original(entered)) {
                return;
            }

            leaveModal();
            enterModal(current);
        }

        @RequiredUIAccess
        private Component buildContent() {

            DockLayout main = DockLayout.create();
            // the window is disposed when it closes, so anything the center component registers against it lives
            // exactly as long as the dialog does
            main.center(myDescriptor.createCenterComponent(myWindow));

            AnAction[] actions = myDescriptor.createActions(Platform.current().os().isMac());
            if (actions.length != 0) {
                main.bottom(buildButtons(actions));
            }

            if (myDescriptor.hasDefaultContentBorder()) {
                main.paddingBuilder()
                    .topSet(CONTENT_VERTICAL_PADDING)
                    .bottomSet(CONTENT_VERTICAL_PADDING)
                    .leftSet(CONTENT_HORIZONTAL_PADDING)
                    .rightSet(CONTENT_HORIZONTAL_PADDING)
                    .apply();
            }

            Component root = myDescriptor instanceof WholeLeftDialogDescriptor wholeLeft
                ? buildWholeLeft(wholeLeft, wholeLeft.createLeftComponent(myWindow), main)
                : main;

            installDefaultActionKey(root);

            return root;
        }

        @RequiredUIAccess
        private Component buildWholeLeft(WholeLeftDialogDescriptor descriptor, Component left, Component main) {
            TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
            split.setFirstComponent(left);
            split.setSecondComponent(main);

            String splitterKey = descriptor.getLeftSplitterKey();
            if (splitterKey == null) {
                split.setProportion(LEFT_PROPORTION);
            }
            else {
                ApplicationPropertiesComponent properties = ApplicationPropertiesComponent.getInstance();
                split.setProportion(loadLeftProportion(properties, splitterKey));
                split.addSplitProportionChangedListener(event -> saveLeftProportion(properties, splitterKey, event.getProportion()));
            }
            return split;
        }

        private static int loadLeftProportion(ApplicationPropertiesComponent properties, String splitterKey) {
            int percent = Math.round(properties.getFloat(splitterKey, LEFT_PROPORTION / 100f) * 100);
            return percent <= 0 || percent >= 100 ? LEFT_PROPORTION : percent;
        }

        private static void saveLeftProportion(ApplicationPropertiesComponent properties, String splitterKey, int percent) {
            if (percent <= 0 || percent >= 100) {
                return;
            }

            properties.setValue(splitterKey, percent / 100f, LEFT_PROPORTION / 100f);
        }

        @RequiredUIAccess
        private void installDefaultActionKey(Component root) {
            UIAccess uiAccess = UIAccess.current();
            root.addKeyPressedListener(event -> {
                if (!(event.getInputDetails() instanceof KeyboardInputDetails details)
                    || details.getKeyCode() != KeyCode.ENTER
                    || !details.getModifiers().isEmpty()) {
                    return;
                }

                Button defaultButton = myDefaultButton;
                if (defaultButton == null || !defaultButton.isEnabled()) {
                    return;
                }

                uiAccess.execute(() -> {
                    if (!myResult.isDone() && defaultButton.isEnabled()) {
                        defaultButton.invoke(details);
                    }
                });
            });
        }

        @RequiredUIAccess
        private Component buildButtons(AnAction[] actions) {
            ActionGroup group = ActionGroup.newImmutableBuilder().addAll(actions).build();

            UnifiedActionToolbarImpl toolbar = new UnifiedActionToolbarImpl(PLACE, group, ActionToolbar.Style.BUTTON) {
                @Override
                @RequiredUIAccess
                protected void tweakActionComponentUI(AnAction action, Component component) {
                    if (myDescriptor.isDefaultAction(action) && component instanceof Button button) {
                        button.addStyle(ButtonStyle.PRIMARY);
                        myDefaultButton = button;
                    }
                }
            };
            toolbar.setDataContextProvider(() -> DataContext.builder().add(Dialog.KEY, this).build());

            myDescriptor.setOkButtonStateUpdater(toolbar::updateActionsAsync);
            myDescriptor.setSizeUpdater(myWindow::pack);

            toolbar.updateActionsAsync();

            DockLayout buttons = DockLayout.create();

            if (myDescriptor.hasHelp()) {
                buttons.left(DialogHelpButton.create(myDescriptor));
            }

            buttons.right(toolbar.getUIComponent());

            buttons.paddingBuilder()
                .topSet(CONTENT_VERTICAL_PADDING)
                .bottomSet(CONTENT_VERTICAL_PADDING)
                .leftSet(CONTENT_HORIZONTAL_PADDING)
                .rightSet(CONTENT_HORIZONTAL_PADDING)
                .apply();

            if (myDescriptor.hasBorderAtButtonLayout()) {
                buttons.borderBuilder().topSet().apply();
            }
            return buttons;
        }

        @Override
        @RequiredUIAccess
        public void doOkAction(DialogValue value) {
            myResult.complete(value);

            myWindow.close();
        }

        @RequiredUIAccess
        private void cancelByUser() {
            DialogCancelAction action = new DialogCancelAction();
            if (myDescriptor.canHandle(action, null, myWindow)) {
                myDescriptor.onHandleValue(action, null);

                doCancelAction();
            }
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
