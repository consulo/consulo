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
package consulo.desktop.awt.ui.dialog;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.action.toolbar.ActionButtonToolbarImpl;
import consulo.platform.Platform;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Size;
import consulo.ui.Window;
import consulo.ui.WindowOwner;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.BorderLayoutPanel;
import consulo.ui.ex.awt.CustomLineBorder;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.util.concurrent.AsyncResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 13/12/2021
 */
@Singleton
@ServiceImpl
public class DesktopAwtDialogService implements DialogService {
    private static class DialogImpl implements Dialog {
        private final DialogWrapperImpl myDialogWrapper;
        private final DialogDescriptor myDescriptor;

        private DialogValue myValue;

        public DialogImpl(DialogDescriptor descriptor) {
            myDescriptor = descriptor;
            myDialogWrapper = new DialogWrapperImpl(descriptor, this);
        }

        public DialogImpl(@Nonnull Project project, DialogDescriptor descriptor) {
            myDescriptor = descriptor;
            myDialogWrapper = new DialogWrapperImpl(project, descriptor, this);
        }

        public DialogImpl(@Nonnull java.awt.Component component, DialogDescriptor descriptor) {
            myDescriptor = descriptor;
            myDialogWrapper = new DialogWrapperImpl(component, descriptor, this);
        }

        @RequiredUIAccess
        @Nonnull
        @Override
        public CompletableFuture<DialogValue> showAsync() {
            CompletableFuture<DialogValue> result = new CompletableFuture<>();

            AsyncResult<Void> showAsync = myDialogWrapper.showAsync();
            showAsync.doWhenDone(() -> result.complete(myValue));
            showAsync.doWhenRejected(() -> result.completeExceptionally(new IllegalArgumentException("reject")));
            return result;
        }

        @Override
        public void doOkAction(@Nonnull DialogValue value) {
            myValue = value;

            myDialogWrapper.close(DialogWrapper.OK_EXIT_CODE);
        }

        @Override
        public void doCancelAction() {
            myDialogWrapper.close(DialogWrapper.CANCEL_EXIT_CODE);
        }

        @Nonnull
        @Override
        public DialogDescriptor getDescriptor() {
            return myDescriptor;
        }

        @Nonnull
        @Override
        public Window getWindow() {
            return TargetAWT.from(myDialogWrapper.getWindow());
        }
    }

    private static class DialogWrapperImpl extends DialogWrapper {
        private final DialogDescriptor myDescriptor;
        private final Dialog myDialog;

        private String myHelpId;

        protected DialogWrapperImpl(DialogDescriptor descriptor, Dialog dialog) {
            super(true);
            myDescriptor = descriptor;
            myDialog = dialog;

            initImpl();
        }

        protected DialogWrapperImpl(Project project, DialogDescriptor descriptor, Dialog dialog) {
            super(project, true);
            myDescriptor = descriptor;
            myDialog = dialog;

            initImpl();
        }

        protected DialogWrapperImpl(java.awt.Component parent, DialogDescriptor descriptor, Dialog dialog) {
            super(parent, true);
            myDescriptor = descriptor;
            myDialog = dialog;

            initImpl();
        }

        private void initImpl() {
            myHelpId = myDescriptor.getHelpId();

            setTitle(myDescriptor.getTitle());

            init();

            Size size = myDescriptor.getInitialSize();
            if (size != null) {
                setScalableSize(size.getWidth(), size.getHeight());
            }
        }

        @Nonnull
        @Override
        protected Action[] createActions() {
            throw new UnsupportedOperationException();
        }

        @RequiredUIAccess
        @Nullable
        @Override
        public JComponent getPreferredFocusedComponent() {
            return (JComponent) TargetAWT.to(myDescriptor.getPreferredFocusedComponent());
        }

        @Nullable
        @Override
        protected JComponent createSouthPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            DataManager.registerDataProvider(panel, dataId -> {
                if (dataId == Dialog.KEY) {
                    return myDialog;
                }

                return null;
            });

            AnAction[] actions = myDescriptor.createActions(Platform.current().os().isMac());

            ActionGroup group = ActionGroup.newImmutableBuilder().addAll(actions).build();


            if (myHelpId != null) {
                final Insets insets = Platform.current().os().isMac() ? JBUI.emptyInsets() : JBUI.insetsTop(8);

                JButton helpButton = new JButton(getHelpAction());
                helpButton.putClientProperty("JButton.buttonType", "help");
                helpButton.setText("");
                helpButton.setMargin(insets);
                helpButton.setToolTipText(ActionLocalize.actionHelptopicsDescription().get());

                panel.add(helpButton, BorderLayout.WEST);
            }

            ActionToolbar toolbar = new ActionButtonToolbarImpl("DialogRightButtons",
                group,
                Application.get(),
                KeymapManager.getInstance(),
                ActionManager.getInstance(),
                DataManager.getInstance()) {
                @Override
                protected void tweakActionComponentUI(@Nonnull AnAction action, java.awt.Component component) {
                    super.tweakActionComponentUI(action, component);

                    if (myDescriptor.isDefaultAction(action)) {
                        getPeer().getRootPane().setDefaultButton((JButton) component);
                    }
                }
            };

            toolbar.setTargetComponent(panel);

            toolbar.updateActionsImmediately();

            panel.add(toolbar.getComponent(), BorderLayout.EAST);

            panel.setBorder(JBUI.Borders.empty(ourDefaultBorderInsets));

            if (myDescriptor.hasBorderAtButtonLayout()) {
                BorderLayoutPanel borderLayoutPanel = JBUI.Panels.simplePanel(panel);
                borderLayoutPanel.setBorder(new CustomLineBorder(JBUI.scale(1), 0, 0, 0));
                return borderLayoutPanel;
            }
            else {
                return panel;
            }
        }

        @Nullable
        @Override
        public String getHelpId() {
            return myHelpId;
        }

        @Nullable
        @Override
        protected Border createContentPaneBorder() {
            if (!myDescriptor.hasDefaultContentBorder()) {
                return JBUI.Borders.empty();
            }
            return super.createContentPaneBorder();
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            return (JComponent) TargetAWT.to(myDescriptor.createCenterComponent(getDisposable()));
        }
    }

    @Nonnull
    @Override
    public Dialog build(@Nonnull DialogDescriptor descriptor) {
        return new DialogImpl(descriptor);
    }

    @Nonnull
    @Override
    public Dialog build(@Nonnull Component parent, @Nonnull DialogDescriptor descriptor) {
        return new DialogImpl(TargetAWT.to(parent), descriptor);
    }

    @Nonnull
    @Override
    public Dialog build(@Nonnull WindowOwner windowOwner, @Nonnull DialogDescriptor descriptor) {
        if (!(windowOwner instanceof Project project)) {
            throw new IllegalArgumentException("Expecte instance of Project");
        }
        return new DialogImpl(project, descriptor);
    }
}
