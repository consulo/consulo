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
package consulo.ide.projectView.impl;

import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.KeyWithDefaultValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 09.05.2015
 */
public interface ProjectViewPaneOptionProvider<T> {
  ExtensionPointName<ProjectViewPaneOptionProvider> EX_NAME = ExtensionPointName.create("com.intellij.projectViewOptionProvider");

  abstract class BoolValue implements ProjectViewPaneOptionProvider<Boolean> {
    @NotNull
    @Override
    public Boolean parseValue(@NotNull String value) {
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

  @NotNull
  KeyWithDefaultValue<T> getKey();

  void addToolbarActions(@NotNull AbstractProjectViewPane pane, @NotNull DefaultActionGroup actionGroup);

  @NotNull
  T parseValue(@NotNull String value);

  @Nullable
  String toString(@Nullable T value);
}
