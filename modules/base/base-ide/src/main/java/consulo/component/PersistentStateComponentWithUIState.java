/*
 * Copyright 2013-2018 consulo.io
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
package consulo.component;

import com.intellij.openapi.components.PersistentStateComponent;
import consulo.annotation.access.RequiredWriteAction;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-12-30
 */
public interface PersistentStateComponentWithUIState<S, UIState> extends PersistentStateComponent<S> {
  @Nullable
  @RequiredUIAccess
  UIState getStateFromUI();

  @RequiredWriteAction
  @Nullable
  @Override
  default S getState() {
    throw new IllegalStateException("We don't need call this method anymore");
  }

  @Nullable
  @RequiredWriteAction
  S getState(UIState uiState);
}