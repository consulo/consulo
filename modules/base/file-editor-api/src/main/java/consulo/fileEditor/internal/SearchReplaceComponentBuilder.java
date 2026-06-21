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
package consulo.fileEditor.internal;

import consulo.application.Application;
import consulo.dataContext.UiDataProvider;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.function.BooleanSupplier;

public class SearchReplaceComponentBuilder {
  private final Project myProject;
  private final JComponent myTargetComponent;

  private UiDataProvider myDataProvider;

  private Runnable myReplaceAction;
  private Runnable myCloseAction;

  private final DefaultActionGroup mySearchActions = DefaultActionGroup.createFlatGroup(() -> "search bar 1");
  private final DefaultActionGroup myExtraSearchActions = DefaultActionGroup.createFlatGroup(() -> "search bar 2");
  private final DefaultActionGroup mySearchFieldActions = DefaultActionGroup.createFlatGroup(() -> "search field actions");
  private BooleanSupplier mySearchToolbarModifiedFlagGetter = () -> false;

  private final DefaultActionGroup myReplaceActions = DefaultActionGroup.createFlatGroup(() -> "replace bar 1");
  private final DefaultActionGroup myExtraReplaceActions = DefaultActionGroup.createFlatGroup(() -> "replace bar 1");
  private final DefaultActionGroup myReplaceFieldActions = DefaultActionGroup.createFlatGroup(() -> "replace field actions");

  public SearchReplaceComponentBuilder(@Nullable Project project, JComponent component) {
    myProject = project;
    myTargetComponent = component;
  }

  public SearchReplaceComponentBuilder withDataProvider(UiDataProvider provider) {
    myDataProvider = provider;
    return this;
  }

  public SearchReplaceComponentBuilder withReplaceAction(Runnable action) {
    myReplaceAction = action;
    return this;
  }

  public SearchReplaceComponentBuilder withCloseAction(Runnable action) {
    myCloseAction = action;
    return this;
  }

  public SearchReplaceComponentBuilder addSearchFieldActions(AnAction... actions) {
    mySearchFieldActions.addAll(actions);
    return this;
  }

  public SearchReplaceComponentBuilder addReplaceFieldActions(AnAction... actions) {
    myReplaceFieldActions.addAll(actions);
    return this;
  }

  public SearchReplaceComponentBuilder addPrimarySearchActions(AnAction... actions) {
    mySearchActions.addAll(actions);
    return this;
  }

  public SearchReplaceComponentBuilder addSecondarySearchActions(AnAction... actions) {
    for (AnAction action : actions) {
      mySearchActions.addAction(action).setAsSecondary(true);
    }
    return this;
  }

  public SearchReplaceComponentBuilder withSecondarySearchActionsIsModifiedGetter(BooleanSupplier getter) {
    mySearchToolbarModifiedFlagGetter = getter;
    return this;
  }

  public SearchReplaceComponentBuilder addExtraSearchActions(AnAction... actions) {
    myExtraSearchActions.addAll(actions);
    return this;
  }

  public SearchReplaceComponentBuilder addPrimaryReplaceActions(AnAction... actions) {
    myReplaceActions.addAll(actions);
    return this;
  }

  public SearchReplaceComponentBuilder addExtraReplaceAction(AnAction... actions) {
    myExtraReplaceActions.addAll(actions);
    return this;
  }

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
