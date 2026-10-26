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
package consulo.versionControlSystem.ui;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.versionControlSystem.change.Change;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public interface VcsChangesBrowserBuilder {
    VcsChangesBrowserBuilder emptyText(LocalizeValue text);

    VcsChangesBrowserBuilder toolbarAction(AnAction action);

    /**
     * Replaces the grouping button with a view options popup, presented as the given group, that lists the grouping toggle
     * followed by the group's actions.
     */
    VcsChangesBrowserBuilder viewOptions(ActionGroup group);

    /**
     * The context menu of the tree. Its actions read the selected changes from the data context, the
     * {@link ChangesGroupingSupport#KEY grouping} and the tree expander.
     */
    VcsChangesBrowserBuilder popupGroup(ActionGroup group);

    /**
     * Starts the tree with the given {@link ChangesGroupingSupport} keys and reports every change of them. Keys the tree does
     * not support are kept as they are.
     */
    VcsChangesBrowserBuilder grouping(Set<String> groupingKeys, Consumer<Set<String>> groupingListener);

    /**
     * Adds the rollback action; rolled back changes leave the list.
     */
    VcsChangesBrowserBuilder withRollback();

    /**
     * Adds to the row of every change, after the file presentation.
     */
    VcsChangesBrowserBuilder changeDecorator(BiConsumer<Change, TextItemPresentation> decorator);

    /**
     * Replaces the diff opened by a double click or Enter on a change.
     */
    VcsChangesBrowserBuilder openHandler(Consumer<Change> handler);

    @RequiredUIAccess
    VcsChangesBrowser build(Disposable parentDisposable);
}
