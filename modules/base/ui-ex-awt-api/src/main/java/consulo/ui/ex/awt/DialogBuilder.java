/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.annotation.DeprecationInfo;
import consulo.application.HelpManager;
import consulo.component.ComponentManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.AsyncResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.MagicConstant;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

/**
 * The DialogBuilder is a simpler alternative to {@link DialogWrapper}.
 * There is no need to create a subclass (which is needed in the DialogWrapper), which can be nice for simple dialogs.
 */
public class DialogBuilder implements Disposable {
    public static final String REQUEST_FOCUS_ENABLED = "requestFocusEnabled";

    private JComponent myCenterPanel;
    private JComponent myNorthPanel;
    private LocalizeValue myTitle;
    private JComponent myPreferedFocusComponent;
    private String myDimensionServiceKey;
    private ArrayList<ActionDescriptor> myActions = null;
    private final MyDialogWrapper myDialogWrapper;
    private Runnable myCancelOperation = null;
    private Runnable myOkOperation = null;

    @RequiredUIAccess
    public int show() {
        return showImpl(true).getExitCode();
    }

    @RequiredUIAccess
    public boolean showAndGet() {
        return showImpl(true).isOK();
    }

    @Nonnull
    @RequiredUIAccess
    public AsyncResult<Void> showAsync() {
        return showAsync(true);
    }

    @RequiredUIAccess
    public void showNotModal() {
        showImpl(false);
    }

    public DialogBuilder(@Nullable ComponentManager project) {
        myDialogWrapper = new MyDialogWrapper(project, true);
        Disposer.register(myDialogWrapper.getDisposable(), this);
    }

    public DialogBuilder(@Nullable Component parent) {
        myDialogWrapper = new MyDialogWrapper(parent, true);
        Disposer.register(myDialogWrapper.getDisposable(), this);
    }

    public DialogBuilder() {
        this(((ComponentManager)null));
    }

    @Override
    public void dispose() {
    }

    @RequiredUIAccess
    private AsyncResult<Void> showAsync(boolean isModal) {
        myDialogWrapper.setTitle(myTitle);
        myDialogWrapper.init();
        myDialogWrapper.setModal(isModal);
        return myDialogWrapper.showAsync();
    }

    @RequiredUIAccess
    private MyDialogWrapper showImpl(boolean isModal) {
        myDialogWrapper.setTitle(myTitle);
        myDialogWrapper.init();
        myDialogWrapper.setModal(isModal);
        myDialogWrapper.show();
        if (isModal) {
            myDialogWrapper.dispose();
        }
        return myDialogWrapper;
    }

    public void setCenterPanel(JComponent centerPanel) {
        myCenterPanel = centerPanel;
    }

    @Nonnull
    public DialogBuilder centerPanel(@Nonnull JComponent centerPanel) {
        myCenterPanel = centerPanel;
        return this;
    }

    @Nonnull
    public DialogBuilder setNorthPanel(@Nonnull JComponent northPanel) {
        myNorthPanel = northPanel;
        return this;
    }

    public void setTitle(@Nonnull LocalizeValue title) {
        myTitle = title;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void setTitle(String title) {
        myTitle = LocalizeValue.ofNullable(title);
    }

    @Nonnull
    public DialogBuilder title(@Nonnull LocalizeValue title) {
        myTitle = title;
        return this;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @Nonnull
    public DialogBuilder title(@Nonnull String title) {
        myTitle = LocalizeValue.of(title);
        return this;
    }

    public void setPreferredFocusComponent(JComponent component) {
        myPreferedFocusComponent = component;
    }

    public void setDimensionServiceKey(String dimensionServiceKey) {
        myDimensionServiceKey = dimensionServiceKey;
    }

    public DialogBuilder dimensionKey(@Nonnull String dimensionServiceKey) {
        myDimensionServiceKey = dimensionServiceKey;
        return this;
    }

    public void addAction(Action action) {
        addActionDescriptor(new CustomActionDescriptor(action));
    }

    public <T extends ActionDescriptor> T addActionDescriptor(T actionDescriptor) {
        getActionDescriptors().add(actionDescriptor);
        return actionDescriptor;
    }

    private ArrayList<ActionDescriptor> getActionDescriptors() {
        if (myActions == null) {
            removeAllActions();
        }
        return myActions;
    }

    public void setActionDescriptors(ActionDescriptor... descriptors) {
        removeAllActions();
        ContainerUtil.addAll(myActions, descriptors);
    }

    public void removeAllActions() {
        myActions = new ArrayList<>();
    }

    public Window getWindow() {
        return myDialogWrapper.getWindow();
    }

    public CustomizableAction addOkAction() {
        return addActionDescriptor(new OkActionDescriptor());
    }

    public CustomizableAction addCancelAction() {
        return addActionDescriptor(new CancelActionDescriptor());
    }

    public CustomizableAction addCloseButton() {
        CustomizableAction closeAction = addOkAction();
        closeAction.setText(CommonLocalize.buttonClose());
        return closeAction;
    }

    public void addDisposable(@Nonnull Disposable disposable) {
        Disposer.register(this, disposable);
    }

    public void setButtonsAlignment(@MagicConstant(intValues = {SwingConstants.CENTER, SwingConstants.RIGHT}) int alignment) {
        myDialogWrapper.setButtonsAlignment(alignment);
    }

    public DialogWrapper getDialogWrapper() {
        return myDialogWrapper;
    }

    @RequiredUIAccess
    public void showModal(boolean modal) {
        if (modal) {
            show();
        }
        else {
            showNotModal();
        }
    }

    public void setHelpId(String helpId) {
        myDialogWrapper.setHelpId(helpId);
    }

    public void setCancelOperation(Runnable runnable) {
        myCancelOperation = runnable;
    }

    public void setOkOperation(Runnable runnable) {
        myOkOperation = runnable;
    }

    public void setOkActionEnabled(final boolean isEnabled) {
        myDialogWrapper.setOKActionEnabled(isEnabled);
    }

    @Nonnull
    public DialogBuilder okActionEnabled(boolean isEnabled) {
        myDialogWrapper.setOKActionEnabled(isEnabled);
        return this;
    }

    @Nonnull
    public DialogBuilder resizable(boolean resizable) {
        myDialogWrapper.setResizable(resizable);
        return this;
    }

    public CustomizableAction getOkAction() {
        return get(getActionDescriptors(), OkActionDescriptor.class);
    }

    private static CustomizableAction get(final ArrayList<ActionDescriptor> actionDescriptors, final Class aClass) {
        for (ActionDescriptor actionDescriptor : actionDescriptors) {
            if (actionDescriptor.getClass().isAssignableFrom(aClass)) {
                return (CustomizableAction)actionDescriptor;
            }
        }
        return null;
    }

    public CustomizableAction getCancelAction() {
        return get(getActionDescriptors(), CancelActionDescriptor.class);
    }

    public Component getCenterPanel() {
        return myCenterPanel;
    }

    public interface ActionDescriptor {
        Action getAction(DialogWrapper dialogWrapper);
    }

    public abstract static class DialogActionDescriptor implements ActionDescriptor {
        private final String myName;
        private final Object myMnemonicChar;
        private boolean myIsDefault = false;

        protected DialogActionDescriptor(String name, int mnemonicChar) {
            myName = name;
            myMnemonicChar = mnemonicChar == -1 ? null : mnemonicChar;
        }

        @Override
        public Action getAction(DialogWrapper dialogWrapper) {
            Action action = createAction(dialogWrapper);
            action.putValue(Action.NAME, myName);
            if (myMnemonicChar != null) {
                action.putValue(Action.MNEMONIC_KEY, myMnemonicChar);
            }
            if (myIsDefault) {
                action.putValue(Action.DEFAULT, Boolean.TRUE);
            }
            return action;
        }

        public void setDefault(boolean isDefault) {
            myIsDefault = isDefault;
        }

        protected abstract Action createAction(DialogWrapper dialogWrapper);
    }

    public static class CloseDialogAction extends DialogActionDescriptor {
        private final int myExitCode;

        public CloseDialogAction() {
            this(CommonLocalize.buttonClose().get(), -1, DialogWrapper.CLOSE_EXIT_CODE);
        }

        public CloseDialogAction(String name, int mnemonicChar, int exitCode) {
            super(name, mnemonicChar);
            myExitCode = exitCode;
        }

        public static CloseDialogAction createDefault(String name, int mnemonicChar, int exitCode) {
            CloseDialogAction closeDialogAction = new CloseDialogAction(name, mnemonicChar, exitCode);
            closeDialogAction.setDefault(true);
            return closeDialogAction;
        }

        @Override
        protected Action createAction(final DialogWrapper dialogWrapper) {
            return new AbstractAction() {
                @Override
                public void actionPerformed(@Nonnull ActionEvent e) {
                    dialogWrapper.close(myExitCode);
                }
            };
        }
    }

    public interface CustomizableAction {
        void setText(LocalizeValue text);
    }

    public static class CustomActionDescriptor implements ActionDescriptor {
        private final Action myAction;

        public CustomActionDescriptor(Action action) {
            myAction = action;
        }

        @Override
        public Action getAction(DialogWrapper dialogWrapper) {
            return myAction;
        }
    }

    private abstract static class BuiltinAction implements ActionDescriptor, CustomizableAction {
        protected LocalizeValue myText = null;

        @Override
        public void setText(@Nonnull LocalizeValue text) {
            myText = text;
        }

        @Override
        public Action getAction(DialogWrapper dialogWrapper) {
            LocalizeAction builtinAction = getBuiltinAction((MyDialogWrapper)dialogWrapper);
            if (myText != LocalizeValue.empty()) {
                builtinAction.setText(myText);
            }
            return builtinAction;
        }

        protected abstract LocalizeAction getBuiltinAction(MyDialogWrapper dialogWrapper);
    }

    public static class OkActionDescriptor extends BuiltinAction {
        @Override
        protected LocalizeAction getBuiltinAction(MyDialogWrapper dialogWrapper) {
            return dialogWrapper.getOKAction();
        }
    }

    public static class CancelActionDescriptor extends BuiltinAction {
        @Override
        protected LocalizeAction getBuiltinAction(MyDialogWrapper dialogWrapper) {
            return dialogWrapper.getCancelAction();
        }
    }

    private class MyDialogWrapper extends DialogWrapper {
        private String myHelpId = null;

        private MyDialogWrapper(@Nullable ComponentManager project, boolean canBeParent) {
            super(project, canBeParent);
        }

        private MyDialogWrapper(Component parent, boolean canBeParent) {
            super(parent, canBeParent);
        }

        public void setHelpId(String helpId) {
            myHelpId = helpId;
        }

        @Nullable
        @Override
        protected String getHelpId() {
            return myHelpId;
        }

        @Override
        public void init() {
            super.init();
        }

        @Override
        @Nonnull
        public LocalizeAction getOKAction() {
            return super.getOKAction();
        }

        @Override
        @Nonnull
        public LocalizeAction getCancelAction() {
            return super.getCancelAction();
        }

        @Override
        protected JComponent createCenterPanel() {
            return myCenterPanel;
        }

        @Override
        protected JComponent createNorthPanel() {
            return myNorthPanel;
        }

        @Override
        public void dispose() {
            myPreferedFocusComponent = null;
            super.dispose();
        }

        @Override
        @RequiredUIAccess
        public JComponent getPreferredFocusedComponent() {
            if (myPreferedFocusComponent != null) {
                return myPreferedFocusComponent;
            }
            FocusTraversalPolicy focusTraversalPolicy = null;
            Container container = myCenterPanel;
            while (container != null && (focusTraversalPolicy =
                container.getFocusTraversalPolicy()) == null && !(container instanceof Window)) {
                container = container.getParent();
            }
            if (focusTraversalPolicy == null) {
                return null;
            }
            Component component = focusTraversalPolicy.getDefaultComponent(myCenterPanel);
            while (!(component instanceof JComponent) && component != null) {
                component = focusTraversalPolicy.getComponentAfter(myCenterPanel, component);
            }
            return (JComponent)component;
        }

        @Override
        protected String getDimensionServiceKey() {
            return myDimensionServiceKey;
        }

        @Override
        protected JButton createJButtonForAction(Action action) {
            JButton button = super.createJButtonForAction(action);
            if (action.getValue(REQUEST_FOCUS_ENABLED) instanceof Boolean requestFocusEnabled) {
                button.setRequestFocusEnabled(requestFocusEnabled);
            }
            return button;
        }

        @Override
        public void doCancelAction() {
            if (!getCancelAction().isEnabled()) {
                return;
            }
            if (myCancelOperation != null) {
                myCancelOperation.run();
            }
            else {
                super.doCancelAction();
            }
        }

        @Override
        protected void doOKAction() {
            if (myOkOperation != null) {
                myOkOperation.run();
            }
            else {
                super.doOKAction();
            }
        }

        @Override
        @RequiredUIAccess
        protected void doHelpAction() {
            if (myHelpId == null) {
                super.doHelpAction();
                return;
            }

            HelpManager.getInstance().invokeHelp(myHelpId);
        }

        @Override
        @Nonnull
        protected Action[] createActions() {
            if (myActions == null) {
                return super.createActions();
            }
            ArrayList<Action> actions = new ArrayList<>(myActions.size());
            for (ActionDescriptor actionDescriptor : myActions) {
                actions.add(actionDescriptor.getAction(this));
            }
            if (myHelpId != null) {
                actions.add(getHelpAction());
            }
            return actions.toArray(new Action[actions.size()]);
        }
    }

    public void setErrorText(@Nullable final String text) {
        myDialogWrapper.setErrorText(text);
    }

    public void setErrorText(@Nullable final String text, @Nullable JComponent component) {
        myDialogWrapper.setErrorText(text, component);
    }
}
