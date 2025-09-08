/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * @author Konstantin Bulenkov
 */
public class CommonActionsPanel extends JPanel {
    public enum Buttons {
        ADD(PlatformIconGroup.generalAdd(), Listener::doAdd),
        REMOVE(PlatformIconGroup.actionsEdit(), Listener::doEdit),
        EDIT(PlatformIconGroup.generalRemove(), Listener::doRemove),
        UP(PlatformIconGroup.actionsMoveup(), Listener::doUp),
        DOWN(PlatformIconGroup.actionsMovedown(), Listener::doDown);

        public static Buttons[] ALL = {ADD, REMOVE, EDIT, UP, DOWN};

        private final Image myIcon;
        private final BiConsumer<Listener, AnActionEvent> myActionPerformer;

        Buttons(Image icon, @RequiredUIAccess BiConsumer<Listener, AnActionEvent> actionPerformer) {
            myIcon = icon;
            myActionPerformer = actionPerformer;
        }

        public Image getIcon() {
            return myIcon;
        }

        MyActionButton createButton(Listener listener, String name, Image icon) {
            return new MyActionButton(this, listener, name == null ? StringUtil.capitalize(name().toLowerCase()) : name, icon);
        }

        public String getText() {
            return StringUtil.capitalize(name().toLowerCase());
        }

        public void performAction(Listener listener, @Nonnull AnActionEvent event) {
            myActionPerformer.accept(listener, event);
        }
    }

    public interface Listener {
        @RequiredUIAccess
        default void doAdd(@Nullable AnActionEvent e) {
        }

        @RequiredUIAccess
        default void doRemove(@Nullable AnActionEvent e) {
        }

        @RequiredUIAccess
        default void doUp(@Nullable AnActionEvent e) {
        }

        @RequiredUIAccess
        default void doDown(@Nullable AnActionEvent e) {
        }

        @RequiredUIAccess
        default void doEdit(@Nullable AnActionEvent e) {
        }
    }

    private Map<Buttons, MyActionButton> myButtons = new HashMap<>();
    private final AnAction[] myActions;

    CommonActionsPanel(
        ListenerFactory factory,
        @Nullable JComponent contextComponent,
        ActionToolbarPosition position,
        @Nullable AnAction[] additionalActions,
        @Nullable Comparator<AnAction> buttonComparator,
        String addName,
        String removeName,
        String moveUpName,
        String moveDownName,
        String editName,
        Image addIcon,
        Buttons... buttons
    ) {
        super(new BorderLayout());
        Listener listener = factory.createListener(this);
        AnAction[] actions = new AnAction[buttons.length];
        for (int i = 0; i < buttons.length; i++) {
            Buttons button = buttons[i];
            String name = switch (button) {
                case ADD -> addName;
                case EDIT -> editName;
                case REMOVE -> removeName;
                case UP -> moveUpName;
                case DOWN -> moveDownName;
            };
            MyActionButton b =
                button.createButton(listener, name, button == Buttons.ADD && addIcon != null ? addIcon : button.getIcon());
            actions[i] = b;
            myButtons.put(button, b);
        }
        if (additionalActions != null && additionalActions.length > 0) {
            ArrayList<AnAction> allActions = new ArrayList<>(Arrays.asList(actions));
            allActions.addAll(Arrays.asList(additionalActions));
            actions = allActions.toArray(new AnAction[allActions.size()]);
        }
        myActions = actions;
        for (AnAction action : actions) {
            if (action instanceof AnActionButton actionButton) {
                actionButton.setContextComponent(contextComponent);
            }
        }
        if (buttonComparator != null) {
            Arrays.sort(myActions, buttonComparator);
        }
        ArrayList<AnAction> toolbarActions = new ArrayList<>(Arrays.asList(myActions));
        for (int i = 0; i < toolbarActions.size(); i++) {
            if (toolbarActions.get(i) instanceof AnActionButton.CheckedAnActionButton actionButton) {
                toolbarActions.set(i, actionButton.getDelegate());
            }
        }

        ActionManager mgr = ActionManager.getInstance();
        ActionToolbar toolbar = mgr.createActionToolbar(
            ActionPlaces.UNKNOWN,
            new DefaultActionGroup(toolbarActions.toArray(new AnAction[toolbarActions.size()])),
            position == ActionToolbarPosition.BOTTOM || position == ActionToolbarPosition.TOP
        );
        toolbar.getComponent().setOpaque(false);
        toolbar.setTargetComponent(contextComponent);
        toolbar.getComponent().setBorder(null);
        add(toolbar.getComponent(), BorderLayout.CENTER);
    }

    public AnActionButton getAnActionButton(Buttons button) {
        return myButtons.get(button);
    }

    @Override
    public void addNotify() {
        JRootPane pane = getRootPane();
        for (AnAction button : myActions) {
            ShortcutSet shortcut = button instanceof AnActionButton actionButton ? actionButton.getShortcut() : null;
            if (shortcut != null) {
                if (button instanceof MyActionButton actionButton
                    && actionButton.isAddButton()
                    && UIUtil.isDialogRootPane(pane)) {
                    button.registerCustomShortcutSet(shortcut, pane);
                }
                else {
                    button.registerCustomShortcutSet(shortcut, ((AnActionButton) button).getContextComponent());
                }
                if (button instanceof MyActionButton actionButton && actionButton.isRemoveButton()) {
                    registerDeleteHook(actionButton);
                }
            }
        }

        super.addNotify(); // call after all to construct actions tooltips properly
    }

    private static void registerDeleteHook(final MyActionButton removeButton) {
        new AnAction(LocalizeValue.localizeTODO("Delete Hook")) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                removeButton.actionPerformed(e);
            }

            @Override
            public void update(@Nonnull AnActionEvent e) {
                JComponent contextComponent = removeButton.getContextComponent();
                if (contextComponent instanceof JTable table && table.isEditing()) {
                    e.getPresentation().setEnabled(false);
                    return;
                }
                SpeedSearchSupply supply = SpeedSearchSupply.getSupply(contextComponent);
                if (supply != null && supply.isPopupActive()) {
                    e.getPresentation().setEnabled(false);
                    return;
                }
                removeButton.update(e);
            }
        }.registerCustomShortcutSet(CustomShortcutSet.fromString("DELETE", "BACK_SPACE"), removeButton.getContextComponent());
    }

    public void setEnabled(Buttons button, boolean enabled) {
        MyActionButton b = myButtons.get(button);
        if (b != null) {
            b.setEnabled(enabled);
        }
    }

    static class MyActionButton extends AnActionButton implements DumbAware {
        private final Buttons myButton;
        private final Listener myListener;

        MyActionButton(Buttons button, Listener listener, String name, Image icon) {
            super(name, name, icon);
            myButton = button;
            myListener = listener;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            myButton.performAction(myListener, e);
        }

        @Override
        public ShortcutSet getShortcut() {
            return getCommonShortcut(myButton);
        }

        @Override
        public void updateButton(@Nonnull AnActionEvent e) {
            super.updateButton(e);
            if (!e.getPresentation().isEnabled()) {
                return;
            }

            JComponent c = getContextComponent();
            if (c instanceof JTable || c instanceof JList) {
                ListSelectionModel model = c instanceof JTable jTable ? jTable.getSelectionModel() : ((JList) c).getSelectionModel();
                int size = c instanceof JTable jTable ? jTable.getRowCount() : ((JList) c).getModel().getSize();
                int min = model.getMinSelectionIndex();
                int max = model.getMaxSelectionIndex();

                if ((myButton == Buttons.UP && min < 1)
                    || (myButton == Buttons.DOWN && max == size - 1)
                    || (myButton != Buttons.ADD && size == 0)
                    || (myButton == Buttons.EDIT && (min != max || min == -1))) {
                    e.getPresentation().setEnabled(false);
                }
                else {
                    e.getPresentation().setEnabled(isEnabled());
                }
            }
        }

        //@Override
        //public boolean isEnabled() {
        //    if (myButton == Buttons.REMOVE) {
        //        final JComponent c = getContextComponent();
        //        if (c instanceof JTable && ((JTable)c).isEditing()) return false;
        //    }
        //    return super.isEnabled();
        //}

        boolean isAddButton() {
            return myButton == Buttons.ADD;
        }

        boolean isRemoveButton() {
            return myButton == Buttons.REMOVE;
        }
    }

    public static ShortcutSet getCommonShortcut(Buttons button) {
        return switch (button) {
            case ADD -> CommonShortcuts.getNewForDialogs();
            case EDIT -> CustomShortcutSet.fromString("ENTER");
            case REMOVE -> CustomShortcutSet.fromString(Platform.current().os().isMac() ? "meta BACK_SPACE" : "alt DELETE");
            case UP -> CommonShortcuts.MOVE_UP;
            case DOWN -> CommonShortcuts.MOVE_DOWN;
        };
    }

    public interface ListenerFactory {
        Listener createListener(CommonActionsPanel panel);
    }
}
