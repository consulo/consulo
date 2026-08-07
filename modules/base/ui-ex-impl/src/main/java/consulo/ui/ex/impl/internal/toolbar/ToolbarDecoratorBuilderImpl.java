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
package consulo.ui.ex.impl.internal.toolbar;

import consulo.dataContext.UiDataProvider;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.ToolbarDecoratorBuilderInternal;
import consulo.ui.ex.internal.ToolbarExecutor;
import consulo.ui.ex.toolbar.*;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-07
 */
public final class ToolbarDecoratorBuilderImpl<E> implements ToolbarDecoratorBuilderInternal<E> {
    private static final class DefaultAddAction<E> extends AddAction<E> {
        @Override
        @RequiredUIAccess
        protected void doAdd(AnActionEvent e) {
            ToolbarExecutor executor = e.getData(ToolbarExecutor.KEY);
            if (executor != null && executor.canAdd()) {
                executor.add();
            }
        }
    }

    private static final class DefaultRemoveAction<E> extends RemoveAction<E> {
        @Override
        @RequiredUIAccess
        protected void doRemove(E value, AnActionEvent e) {
            ToolbarExecutor executor = e.getData(ToolbarExecutor.KEY);
            if (executor != null && executor.canRemove()) {
                executor.remove();
            }
        }
    }

    private static final class DefaultEditAction<E> extends EditAction<E> {
        @Override
        @RequiredUIAccess
        protected void doEdit(E value, AnActionEvent e) {
            ToolbarExecutor executor = e.getData(ToolbarExecutor.KEY);
            if (executor != null && executor.canEdit()) {
                executor.edit();
            }
        }
    }

    private static final class DefaultUpMoveAction<E> extends UpMoveAction<E> {
        @Override
        @RequiredUIAccess
        protected void doUp(E value, AnActionEvent e) {
            ToolbarExecutor executor = e.getData(ToolbarExecutor.KEY);
            if (executor != null && executor.canMoveUp()) {
                executor.moveUp();
            }
        }
    }

    private static final class DefaultDownMoveAction<E> extends DownMoveAction<E> {
        @Override
        @RequiredUIAccess
        protected void doDown(E value, AnActionEvent e) {
            ToolbarExecutor executor = e.getData(ToolbarExecutor.KEY);
            if (executor != null && executor.canMoveDown()) {
                executor.moveDown();
            }
        }
    }

    private final Component myComponent;
    private final @Nullable ToolbarExecutor<E> myExecutor;

    private final List<AnAction> myExtraActions = new ArrayList<>();
    private final Set<Class<? extends ToolbarAction>> myDisabledActions = new HashSet<>();

    private boolean myDisabledAll;

    private String myPlace = ActionPlaces.UNKNOWN;
    private ActionToolbarPosition myToolbarPosition = ActionToolbarPosition.TOP;
    private Function<List<AnAction>, List<AnAction>> myActionSorter = Function.identity();

    private AddAction<E> myAddAction;
    private RemoveAction<E> myRemoveAction;
    private EditAction<E> myEditAction;
    private UpMoveAction<E> myUpAction;
    private DownMoveAction<E> myDownAction;

    public ToolbarDecoratorBuilderImpl(Component component, @Nullable ToolbarExecutor<E> executor) {
        myComponent = component;
        myExecutor = executor;

        if (executor != null) {
            myAddAction = new DefaultAddAction<>();
            myRemoveAction = new DefaultRemoveAction<>();
            myEditAction = new DefaultEditAction<>();
            myUpAction = new DefaultUpMoveAction<>();
            myDownAction = new DefaultDownMoveAction<>();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolbarDecoratorBuilder<E> addOrReplaceAction(ToolbarAction<E> action) {
        if (action instanceof AddAction) {
            myAddAction = (AddAction<E>)action;
        }
        else if (action instanceof RemoveAction) {
            myRemoveAction = (RemoveAction<E>)action;
        }
        else if (action instanceof EditAction) {
            myEditAction = (EditAction<E>)action;
        }
        else if (action instanceof UpMoveAction) {
            myUpAction = (UpMoveAction<E>)action;
        }
        else if (action instanceof DownMoveAction) {
            myDownAction = (DownMoveAction<E>)action;
        }
        return this;
    }

    @Override
    public ToolbarDecoratorBuilder<E> disableAction(Class<? extends ToolbarAction> actionClass) {
        myDisabledActions.add(actionClass);
        return this;
    }

    @Override
    public ToolbarDecoratorBuilder<E> disableAll() {
        myDisabledAll = true;
        return this;
    }

    @Override
    public ToolbarDecoratorBuilder<E> addExtraAction(AnAction action) {
        myExtraActions.add(action);
        return this;
    }

    @Override
    public ToolbarDecoratorBuilder<E> addExtraAction(String actionId) {
        AnAction action = ActionManager.getInstance().getAction(actionId);
        if (action == null) {
            throw new IllegalArgumentException("Unknown action id: " + actionId);
        }
        return addExtraAction(action);
    }

    @Override
    public ToolbarDecoratorBuilder<E> withPlace(String place) {
        myPlace = place;
        return this;
    }

    @Override
    public ToolbarDecoratorBuilder<E> withToolbarPosition(ActionToolbarPosition position) {
        myToolbarPosition = position;
        return this;
    }

    @Override
    public ToolbarDecoratorBuilder<E> withActionSorter(Function<List<AnAction>, List<AnAction>> sorter) {
        myActionSorter = sorter;
        return this;
    }

    @Override
    @RequiredUIAccess
    public Component build() {
        List<AnAction> actions = new ArrayList<>();
        add(actions, myAddAction);
        add(actions, myRemoveAction);
        add(actions, myEditAction);
        add(actions, myUpAction);
        add(actions, myDownAction);
        actions.addAll(myExtraActions);

        actions = myActionSorter.apply(actions);

        if (actions.isEmpty()) {
            throw new IllegalStateException("Toolbar is empty");
        }

        ActionGroup.Builder builder = ActionGroup.newImmutableBuilder();
        actions.forEach(builder::add);

        boolean horizontal = myToolbarPosition == ActionToolbarPosition.TOP || myToolbarPosition == ActionToolbarPosition.BOTTOM;

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(myPlace, builder.build(), horizontal);
        toolbar.setTargetUIComponent(myComponent);

        DockLayout layout = DockLayout.create();
        layout.center(ScrollableLayout.create(myComponent));
        if (myExecutor != null) {
            layout.putUserData(UiDataProvider.KEY, sink -> sink.set(ToolbarExecutor.KEY, myExecutor));
        }

        Component toolbarComponent = toolbar.getUIComponent();
        switch (myToolbarPosition) {
            case TOP -> layout.top(toolbarComponent);
            case BOTTOM -> layout.bottom(toolbarComponent);
            case LEFT -> layout.left(toolbarComponent);
            case RIGHT -> layout.right(toolbarComponent);
        }

        return layout;
    }

    private void add(List<AnAction> actions, @Nullable ToolbarAction<E> action) {
        if (action != null && !myDisabledAll && !isDisabled(action)) {
            actions.add(action);
        }
    }

    private boolean isDisabled(ToolbarAction<E> action) {
        return myDisabledActions.stream().anyMatch(disabled -> disabled.isInstance(action));
    }
}
