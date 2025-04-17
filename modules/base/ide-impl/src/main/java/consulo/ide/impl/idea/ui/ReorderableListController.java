/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.ScrollingUtil;
import consulo.ui.ex.awt.SortedListModel;
import consulo.ui.ex.awt.util.ListUtil;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author dyoma
 */
public abstract class ReorderableListController<T> {
    private final JList<T> myList;
    private static final Image REMOVE_ICON = PlatformIconGroup.generalRemove();

    protected ReorderableListController(JList<T> list) {
        myList = list;
    }

    public JList getList() {
        return myList;
    }

    public RemoveActionDescription addRemoveAction(String actionName) {
        RemoveActionDescription description = new RemoveActionDescription(actionName);
        addActionDescription(description);
        return description;
    }

    protected abstract void addActionDescription(ActionDescription description);

    public AddActionDescription addAddAction(String actionName, Supplier<T> creator, boolean createShortcut) {
        AddActionDescription description = new AddActionDescription(actionName, creator, createShortcut);
        addActionDescription(description);
        return description;
    }

    public AddMultipleActionDescription addAddMultipleAction(
        String actionName,
        Supplier<Collection<T>> creator,
        boolean createShortcut
    ) {
        AddMultipleActionDescription description = new AddMultipleActionDescription(actionName, creator, createShortcut);
        addActionDescription(description);
        return description;
    }

    public CopyActionDescription addCopyAction(String actionName, Function<T, T> copier, Predicate<T> enableCondition) {
        CopyActionDescription description = new CopyActionDescription(actionName, copier, enableCondition);
        addActionDescription(description);
        return description;
    }

    public void addMoveUpAction() {
        addAction(new AnAction(UILocalize.moveUpActionName(), LocalizeValue.empty(), PlatformIconGroup.actionsMoveup()) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                ListUtil.moveSelectedItemsUp(myList);
            }

            @Override
            @RequiredUIAccess
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setEnabled(ListUtil.canMoveSelectedItemsUp(myList));
            }
        });
    }

    public void addMoveDownAction() {
        addAction(new AnAction(UILocalize.moveDownActionName(), LocalizeValue.empty(), PlatformIconGroup.actionsMovedown()) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                ListUtil.moveSelectedItemsDown(myList);
            }

            @Override
            @RequiredUIAccess
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setEnabled(ListUtil.canMoveSelectedItemsDown(myList));
            }
        });
    }

    public void addAction(AnAction action) {
        addActionDescription(new FixedActionDescription(action));
    }

    @SuppressWarnings("unchecked")
    private void handleNewElement(T element) {
        ListModel listModel = myList.getModel();
        if (listModel instanceof SortedListModel) {
            ((SortedListModel<T>)listModel).add(element);
        }
        else {
            ((DefaultListModel)listModel).addElement(element);
        }
        myList.clearSelection();
        ScrollingUtil.selectItem(myList, element);
    }

    public static <T> ReorderableListController<T> create(JList<T> list, DefaultActionGroup actionGroup) {
        return new ReorderableListController<>(list) {
            @Override
            protected void addActionDescription(ActionDescription description) {
                actionGroup.add(description.createAction(list));
            }
        };
    }

    protected static abstract class ActionDescription {
        public abstract AnAction createAction(JComponent component);
    }

    public interface ActionNotification<T> {
        void afterActionPerformed(T change);
    }

    public static abstract class CustomActionDescription<V> extends ActionDescription {
        private final ArrayList<ActionNotification<V>> myPostHandlers = new ArrayList<>(1);
        private boolean myShowText = false;

        public void addPostHandler(ActionNotification<V> runnable) {
            myPostHandlers.add(runnable);
        }

        protected void runPostHandlers(V change) {
            for (ActionNotification<V> runnable : myPostHandlers) {
                runnable.afterActionPerformed(change);
            }
        }

        @Override
        public abstract CustomActionDescription.BaseAction createAction(JComponent component);

        BaseAction<V> createAction(ActionBehaviour<V> behaviour) {
            return myShowText
                ? new ActionWithText<>(this, getActionName(), null, getActionIcon(), behaviour)
                : new BaseAction<>(this, getActionName(), null, getActionIcon(), behaviour);
        }

        protected abstract Image getActionIcon();

        protected abstract String getActionName();

        public void setShowText(boolean showText) {
            myShowText = showText;
        }

        protected static class BaseAction<V> extends DumbAwareAction {
            private final ActionBehaviour<V> myBehaviour;
            private final CustomActionDescription<V> myCustomActionDescription;

            public BaseAction(
                CustomActionDescription<V> customActionDescription,
                String text,
                String description,
                Image icon,
                ActionBehaviour<V> behaviour
            ) {
                super(text, description, icon);
                myBehaviour = behaviour;
                this.myCustomActionDescription = customActionDescription;
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                V change = myBehaviour.performAction(e);
                if (change == null) {
                    return;
                }
                myCustomActionDescription.runPostHandlers(change);
            }

            @Override
            @RequiredUIAccess
            public void update(@Nonnull AnActionEvent e) {
                myBehaviour.updateAction(e);
            }
        }

        private static class ActionWithText<V> extends BaseAction<V> {
            public ActionWithText(
                CustomActionDescription<V> customActionDescription,
                String text,
                String description,
                Image icon,
                ActionBehaviour<V> behaviour
            ) {
                super(customActionDescription, text, description, icon, behaviour);
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        }
    }

    static interface ActionBehaviour<T> {
        T performAction(AnActionEvent e);

        void updateAction(AnActionEvent e);
    }

    public class RemoveActionDescription extends CustomActionDescription<List<T>> {
        private final String myActionName;
        private Predicate<List<T>> myConfirmation;
        private Predicate<T> myEnableCondition;

        public RemoveActionDescription(String actionName) {
            myActionName = actionName;
        }

        @Override
        public BaseAction createAction(JComponent component) {
            ActionBehaviour<List<T>> behaviour = new ActionBehaviour<>() {
                @Override
                @SuppressWarnings("unchecked")
                public List<T> performAction(AnActionEvent e) {
                    if (myConfirmation != null && !myConfirmation.test((List<T>)myList.getSelectedValuesList())) {
                        return Collections.emptyList();
                    }
                    return ListUtil.removeSelectedItems(myList, myEnableCondition);
                }

                @Override
                public void updateAction(AnActionEvent e) {
                    e.getPresentation().setEnabled(ListUtil.canRemoveSelectedItems(myList, myEnableCondition));
                }
            };
            BaseAction action = createAction(behaviour);
            action.registerCustomShortcutSet(CommonShortcuts.getDelete(), component);
            return action;
        }

        @Override
        protected Image getActionIcon() {
            return REMOVE_ICON;
        }

        @Override
        protected String getActionName() {
            return myActionName;
        }

        public void setConfirmation(Predicate<List<T>> confirmation) {
            myConfirmation = confirmation;
        }

        public void setEnableCondition(Predicate<T> enableCondition) {
            myEnableCondition = enableCondition;
        }

        public JList getList() {
            return myList;
        }
    }

    public abstract class AddActionDescriptionBase<V> extends CustomActionDescription<V> {
        private final String myActionDescription;
        private final Supplier<V> myAddHandler;
        private final boolean myCreateShortcut;
        private Image myIcon = PlatformIconGroup.generalAdd();

        public AddActionDescriptionBase(String actionDescription, Supplier<V> addHandler, boolean createShortcut) {
            myActionDescription = actionDescription;
            myAddHandler = addHandler;
            myCreateShortcut = createShortcut;
        }

        @Override
        public BaseAction createAction(JComponent component) {
            ActionBehaviour<V> behaviour = new ActionBehaviour<>() {
                @Override
                public V performAction(AnActionEvent e) {
                    return addInternal(myAddHandler.get());
                }

                @Override
                public void updateAction(AnActionEvent e) {
                }
            };
            BaseAction action = createAction(behaviour);
            if (myCreateShortcut) {
                action.registerCustomShortcutSet(CommonShortcuts.getInsert(), component);
            }
            return action;
        }

        @Nullable
        protected abstract V addInternal(V v);

        @Override
        public Image getActionIcon() {
            return myIcon;
        }

        @Override
        public String getActionName() {
            return myActionDescription;
        }

        public void setIcon(Image icon) {
            myIcon = icon;
        }
    }

    public class AddActionDescription extends AddActionDescriptionBase<T> {
        public AddActionDescription(String actionDescription, Supplier<T> addHandler, boolean createShortcut) {
            super(actionDescription, addHandler, createShortcut);
        }

        @Override
        protected T addInternal(T t) {
            if (t != null) {
                handleNewElement(t);
            }
            return t;
        }
    }

    public class AddMultipleActionDescription extends AddActionDescriptionBase<Collection<T>> {
        public AddMultipleActionDescription(
            String actionDescription,
            Supplier<Collection<T>> addHandler,
            boolean createShortcut
        ) {
            super(actionDescription, addHandler, createShortcut);
        }

        @Override
        protected Collection<T> addInternal(Collection<T> t) {
            if (t != null) {
                for (T element : t) {
                    handleNewElement(element);
                }
            }
            return t;
        }
    }

    public class CopyActionDescription extends CustomActionDescription<T> {
        private final Function<T, T> myCopier;
        private final Predicate<T> myEnabled;
        private final String myActionName;
        private boolean myVisibleWhenDisabled;

        public CopyActionDescription(String actionName, Function<T, T> copier, Predicate<T> enableCondition) {
            myActionName = actionName;
            myCopier = copier;
            myEnabled = enableCondition;
            myVisibleWhenDisabled = true;
        }

        @Override
        public BaseAction createAction(JComponent component) {
            ActionBehaviour<T> behaviour = new ActionBehaviour<>() {
                @Override
                @SuppressWarnings("unchecked")
                public T performAction(AnActionEvent e) {
                    T newElement = myCopier.apply((T)myList.getSelectedValue());
                    handleNewElement(newElement);
                    return newElement;
                }

                @Override
                @SuppressWarnings("unchecked")
                public void updateAction(AnActionEvent e) {
                    boolean applicable = myList.getSelectedIndices().length == 1;
                    Presentation presentation = e.getPresentation();
                    if (!applicable) {
                        presentation.setEnabled(applicable);
                        return;
                    }
                    boolean enabled = myEnabled.test((T)myList.getSelectedValue());
                    presentation.setEnabled(enabled);
                    presentation.setVisible(enabled || myVisibleWhenDisabled);
                }
            };
            return createAction(behaviour);
        }

        @Override
        public Image getActionIcon() {
            return PlatformIconGroup.actionsCopy();
        }

        @Override
        public String getActionName() {
            return myActionName;
        }

        public void setVisibleWhenDisabled(boolean visible) {
            myVisibleWhenDisabled = visible;
        }
    }

    private static class FixedActionDescription extends ActionDescription {
        private final AnAction myAction;

        public FixedActionDescription(AnAction action) {
            myAction = action;
        }

        @Override
        public AnAction createAction(JComponent component) {
            return myAction;
        }
    }
}
