/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.idea.find;

import consulo.application.Application;
import consulo.dataContext.DataProvider;
import consulo.ide.impl.idea.openapi.util.BooleanGetter;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public class SearchReplaceComponentBuilder {
  private final Project myProject;
  private final JComponent myTargetComponent;

  private DataProvider myDataProvider;

  private Runnable myReplaceAction;
  private Runnable myCloseAction;

  private final DefaultActionGroup mySearchActions = DefaultActionGroup.createFlatGroup(() -> "search bar 1");
  private final DefaultActionGroup myExtraSearchActions = DefaultActionGroup.createFlatGroup(() -> "search bar 2");
  private final DefaultActionGroup mySearchFieldActions = DefaultActionGroup.createFlatGroup(() -> "search field actions");
  private BooleanGetter mySearchToolbarModifiedFlagGetter = BooleanGetter.FALSE;

  private final DefaultActionGroup myReplaceActions = DefaultActionGroup.createFlatGroup(() -> "replace bar 1");
  private final DefaultActionGroup myExtraReplaceActions = DefaultActionGroup.createFlatGroup(() -> "replace bar 1");
  private final DefaultActionGroup myReplaceFieldActions = DefaultActionGroup.createFlatGroup(() -> "replace field actions");

  public SearchReplaceComponentBuilder(@Nullable Project project, @Nonnull JComponent component) {
    myProject = project;
    myTargetComponent = component;
  }

  @Nonnull
  public SearchReplaceComponentBuilder withDataProvider(@Nonnull DataProvider provider) {
    myDataProvider = provider;
    return this;
  }

  @Nonnull
  public SearchReplaceComponentBuilder withReplaceAction(@Nonnull Runnable action) {
    myReplaceAction = action;
    return this;
  }

  @Nonnull
  public SearchReplaceComponentBuilder withCloseAction(@Nonnull Runnable action) {
    myCloseAction = action;
    return this;
  }

  @Nonnull
  public SearchReplaceComponentBuilder addSearchFieldActions(AnAction... actions) {
    mySearchFieldActions.addAll(actions);
    return this;
  }

  @Nonnull
  public SearchReplaceComponentBuilder addReplaceFieldActions(AnAction... actions) {
    myReplaceFieldActions.addAll(actions);
    return this;
  }

  @Nonnull
  public SearchReplaceComponentBuilder addPrimarySearchActions(AnAction... actions) {
    mySearchActions.addAll(actions);
    return this;
  }

  @Nonnull
  public SearchReplaceComponentBuilder addSecondarySearchActions(AnAction... actions) {
    for (AnAction action : actions) {
      mySearchActions.addAction(action).setAsSecondary(true);
    }
    return this;
  }

  @Nonnull
  public SearchReplaceComponentBuilder withSecondarySearchActionsIsModifiedGetter(@Nonnull BooleanGetter getter) {
    mySearchToolbarModifiedFlagGetter = getter;
    return this;
  }

  @Nonnull
  public SearchReplaceComponentBuilder addExtraSearchActions(AnAction... actions) {
    myExtraSearchActions.addAll(actions);
    return this;
  }

  @Nonnull
  public SearchReplaceComponentBuilder addPrimaryReplaceActions(AnAction... actions) {
    myReplaceActions.addAll(actions);
    return this;
  }

  @Nonnull
  public SearchReplaceComponentBuilder addExtraReplaceAction(AnAction... actions) {
    myExtraReplaceActions.addAll(actions);
    return this;
  }

  @Nonnull
  public SearchReplaceComponent build() {
    SearchReplaceComponentFactory factory = Application.get().getInstance(SearchReplaceComponentFactory.class);
    return factory.create(myProject,
                          myTargetComponent,
                          mySearchActions,
                          mySearchToolbarModifiedFlagGetter,
                          myExtraSearchActions,
                          mySearchFieldActions,
                          myReplaceActions,
                          myExtraReplaceActions,
                          myReplaceFieldActions,
                          myReplaceAction,
                          myCloseAction,
                          myDataProvider);
  }
}
