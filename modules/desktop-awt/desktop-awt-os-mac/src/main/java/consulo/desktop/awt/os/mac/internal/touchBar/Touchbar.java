// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import consulo.application.ApplicationManager;
import consulo.application.util.SystemInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.ClientProperty;
import consulo.ui.ex.awt.JBOptionButton;
import consulo.ui.ex.awt.OptionAction;
import consulo.ui.util.TextWithMnemonic;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;

//
// Public API for assigning touchbar actions to ui-component
//
public final class Touchbar {
    public static @Nullable ActionGroup getActions(@Nonnull JComponent component) {
        return ClientProperty.get(component, ACTION_GROUP_KEY);
    }

    //
    // Use setActions to link action-group with JComponent.
    // Linked group will be shown in touchbar when JComponent gains focus.
    //

    public static void setActions(@Nonnull JComponent component, @Nullable ActionGroup group) {
        if (!SystemInfo.isMac || ApplicationManager.getApplication() == null) {
            return;
        }
        component.putClientProperty(ACTION_GROUP_KEY, group);
    }

    public static void setActions(@Nonnull JComponent component, @Nullable AnAction action) {
        setActions(component, action == null ? null : new DefaultActionGroup(action));
    }

    public static void setActions(@Nonnull JComponent component, @Nonnull String actionId) {
        setActions(component, ActionManager.getInstance().getAction(actionId));
    }

    public static void addActions(@Nonnull JComponent component, @Nullable ActionGroup group) {
        if (!SystemInfo.isMac || ApplicationManager.getApplication() == null) {
            return;
        }

        ActionGroup old = ClientProperty.get(component, ACTION_GROUP_KEY);
        if (old == null) {
            setActions(component, group);
        }
        else if (old instanceof DefaultActionGroup && group != null) {
            ((DefaultActionGroup) old).addAll(group);
        }
    }

    public static void setButtonActions(@Nonnull JComponent component,
                                        Collection<? extends JButton> buttons,
                                        Collection<? extends JButton> principal,
                                        JButton defaultButton) {
        setButtonActions(component, buttons, principal, defaultButton, null);
    }

    public static void setButtonActions(@Nonnull JComponent component,
                                        Collection<? extends JButton> buttons,
                                        Collection<? extends JButton> principal,
                                        JButton defaultButton,
                                        @Nullable ActionGroup extraActions) {
        if (!SystemInfo.isMac || ApplicationManager.getApplication() == null) {
            return;
        }

        DefaultActionGroup result = new DefaultActionGroup();
        if (buttons != null) {
            result.add(buildActionsFromButtons(buttons, defaultButton, false));
        }
        if (extraActions != null) {
            result.add(extraActions);
        }
        if (principal != null) {
            result.add(buildActionsFromButtons(principal, defaultButton, true));
        }
        setActions(component, result);
    }

    //
    // Private
    //

    private static final Key<ActionGroup> ACTION_GROUP_KEY = Key.create("Touchbar.ActionGroup.key");
    private static final boolean EXPAND_OPTION_BUTTONS = Boolean.getBoolean("Touchbar.expand.option.button");

    private static @Nonnull DefaultActionGroup buildActionsFromButtons(Collection<? extends JButton> buttons,
                                                                       JButton defaultButton,
                                                                       boolean isPrincipal) {
        final DefaultActionGroup result = new DefaultActionGroup();
        if (EXPAND_OPTION_BUTTONS) {
            DefaultActionGroup options = null;
            for (JButton jb : buttons) {
                if (jb instanceof JBOptionButton ob) {
                    final Action[] opts = ob.getOptions();
                    if (opts != null) {
                        for (Action a : opts) {
                            if (a == null) {
                                continue;
                            }
                            AnAction anAct = _createActionFromButton(a, ob, true);
                            if (anAct == null) {
                                continue;
                            }
                            if (options == null) {
                                options = new DefaultActionGroup();
                            }
                            options.add(anAct);
                        }
                    }
                }
            }
            if (options != null) {
                result.add(options);
            }
        }

        for (JButton jb : buttons) {
            final AnAction anAct = _createActionFromButton(jb.getAction(), jb, false);
            if (anAct != null) {
                if (jb == defaultButton) {
                    TouchbarActionCustomizations.setDefault(anAct, true);
                }
                result.add(anAct);
            }
        }

        if (isPrincipal) {
            TouchbarActionCustomizations.setPrincipal(result, true);
        }

        return result;
    }

    private static AnAction _createActionFromButton(@Nullable Action action,
                                                    @Nonnull JButton button,
                                                    boolean useTextFromAction /*for optional buttons*/) {
        Object anAct = action == null ? null : action.getValue(OptionAction.AN_ACTION);
        if (anAct == null) {
            anAct = new MyAction(useTextFromAction, action, button);
        }
        if (!(anAct instanceof AnAction)) {
            return null;
        }
        TouchbarActionCustomizations.setComponent((AnAction) anAct, button).setShowText(true).setShowImage(false);
        return (AnAction) anAct;
    }

    private static class MyAction extends DumbAwareAction implements ActionWithDelegate<Object> {

        final boolean useTextFromAction;
        final @Nullable Action action;
        final @Nonnull JButton button;

        MyAction(boolean useTextFromAction, @Nullable Action action, @Nonnull JButton button) {
            this.useTextFromAction = useTextFromAction;
            this.action = action;
            this.button = button;
            setEnabledInModalContext(true);
            if (useTextFromAction) {
                Object name = action == null ? button.getText() : action.getValue(Action.NAME);
                getTemplatePresentation().setText(name instanceof String ? (String) name : "");
            }
        }

        @Override
        public @Nonnull Object getDelegate() {
            return action == null ? button : action;
        }

        @Override
        public @Nonnull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            if (action == null) {
                button.doClick();
                return;
            }
            // also can be used something like: ApplicationManager.getApplication().invokeLater(() -> jb.doClick(), ms)
            action.actionPerformed(new ActionEvent(button, ActionEvent.ACTION_PERFORMED, null));
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabled(action == null ? button.isEnabled() : action.isEnabled());
            if (!useTextFromAction) {
                e.getPresentation().setTextValue(LocalizeValue.of(TextWithMnemonic.parse(button.getText()).getText()));
            }
        }
    }
}
