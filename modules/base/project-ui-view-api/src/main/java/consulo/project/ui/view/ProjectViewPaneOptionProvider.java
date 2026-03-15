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
package consulo.project.ui.view;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.util.dataholder.KeyWithDefaultValue;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 09.05.2015
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ProjectViewPaneOptionProvider<T> {
  ExtensionPointName<ProjectViewPaneOptionProvider> EX_NAME = ExtensionPointName.create(ProjectViewPaneOptionProvider.class);

  abstract class BoolValue implements ProjectViewPaneOptionProvider<Boolean> {
    
    @Override
    public Boolean parseValue(String value) {
      return Boolean.parseBoolean(value);
    }

    @Nullable
    @Override
    public String toString(@Nullable Boolean value) {
      if(value == null || Objects.equals(getKey().getDefaultValue(), value)) {
        return null;
      }
      return value.toString();
    }
  }

  
  KeyWithDefaultValue<T> getKey();

  void addToolbarActions(ProjectViewPane pane, DefaultActionGroup actionGroup);

  
  T parseValue(String value);

  @Nullable
  String toString(@Nullable T value);
}
