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
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.ui.VcsChangesBrowser;
import consulo.versionControlSystem.ui.VcsChangesBrowserBuilder;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedVcsChangesBrowserBuilder implements VcsChangesBrowserBuilder {
    private final Project myProject;
    private final List<AnAction> myToolbarActions = new ArrayList<>();
    private LocalizeValue myEmptyText = LocalizeValue.empty();
    private boolean myWithRollback;
    private @Nullable BiConsumer<Change, TextItemPresentation> myChangeDecorator;
    private @Nullable Consumer<Change> myOpenHandler;
    private @Nullable ActionGroup myViewOptions;
    private @Nullable ActionGroup myPopupGroup;
    private @Nullable Set<String> myGroupingKeys;
    private @Nullable Consumer<Set<String>> myGroupingListener;

    public UnifiedVcsChangesBrowserBuilder(Project project) {
        myProject = project;
    }

    @Override
    public VcsChangesBrowserBuilder emptyText(LocalizeValue text) {
        myEmptyText = text;
        return this;
    }

    @Override
    public VcsChangesBrowserBuilder toolbarAction(AnAction action) {
        myToolbarActions.add(action);
        return this;
    }

    @Override
    public VcsChangesBrowserBuilder viewOptions(ActionGroup group) {
        myViewOptions = group;
        return this;
    }

    @Override
    public VcsChangesBrowserBuilder popupGroup(ActionGroup group) {
        myPopupGroup = group;
        return this;
    }

    @Override
    public VcsChangesBrowserBuilder grouping(Set<String> groupingKeys, Consumer<Set<String>> groupingListener) {
        myGroupingKeys = Set.copyOf(groupingKeys);
        myGroupingListener = groupingListener;
        return this;
    }

    @Override
    public VcsChangesBrowserBuilder withRollback() {
        myWithRollback = true;
        return this;
    }

    @Override
    public VcsChangesBrowserBuilder changeDecorator(BiConsumer<Change, TextItemPresentation> decorator) {
        myChangeDecorator = decorator;
        return this;
    }

    @Override
    public VcsChangesBrowserBuilder openHandler(Consumer<Change> handler) {
        myOpenHandler = handler;
        return this;
    }

    @RequiredUIAccess
    @Override
    public VcsChangesBrowser build(Disposable parentDisposable) {
        UnifiedVcsChangesBrowser browser = new UnifiedVcsChangesBrowser(myProject,
            myEmptyText,
            List.copyOf(myToolbarActions),
            myViewOptions,
            myPopupGroup,
            myWithRollback,
            myChangeDecorator,
            myOpenHandler,
            parentDisposable);
        if (myGroupingKeys != null && myGroupingListener != null) {
            browser.setGrouping(myGroupingKeys, myGroupingListener);
        }
        return browser;
    }
}
