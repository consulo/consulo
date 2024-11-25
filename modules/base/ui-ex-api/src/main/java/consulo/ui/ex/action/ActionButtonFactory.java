/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.ui.Size;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22-Jul-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ActionButtonFactory {
  @Nonnull
  static ActionButtonFactory getInstance() {
    return Application.get().getInstance(ActionButtonFactory.class);
  }

  @Nonnull
  ActionButton create(@Nonnull AnAction action, Presentation presentation, String place, @Nonnull Size minimumSize);
}
