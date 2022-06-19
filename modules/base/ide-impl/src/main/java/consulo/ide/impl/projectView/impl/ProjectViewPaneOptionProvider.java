/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.projectView.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.ide.impl.idea.ide.projectView.impl.AbstractProjectViewPane;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.component.extension.ExtensionPointName;
import consulo.util.dataholder.KeyWithDefaultValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 09.05.2015
 */
@Extension(ComponentScope.APPLICATION)
public interface ProjectViewPaneOptionProvider<T> {
  ExtensionPointName<ProjectViewPaneOptionProvider> EX_NAME = ExtensionPointName.create(ProjectViewPaneOptionProvider.class);

  abstract class BoolValue implements ProjectViewPaneOptionProvider<Boolean> {
    @Nonnull
    @Override
    public Boolean parseValue(@Nonnull String value) {
      return Boolean.parseBoolean(value);
    }

    @Nullable
    @Override
    public String toString(@Nullable Boolean value) {
      if(value == null || getKey().getDefaultValue() == value) {
        return null;
      }
      return value.toString();
    }
  }

  @Nonnull
  KeyWithDefaultValue<T> getKey();

  void addToolbarActions(@Nonnull AbstractProjectViewPane pane, @Nonnull DefaultActionGroup actionGroup);

  @Nonnull
  T parseValue(@Nonnull String value);

  @Nullable
  String toString(@Nullable T value);
}
