/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import consulo.util.dataholder.Key;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.List;

public interface QuickActionProvider {
  Key<QuickActionProvider> KEY = Key.create(QuickActionProvider.class);

  String getName();

  List<AnAction> getActions(boolean originalProvider);

  default boolean isCycleRoot() {
    return false;
  }

  @Nullable
  JComponent getComponent();
}
