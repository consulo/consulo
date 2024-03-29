/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.designer.propertyTable;

import consulo.ide.impl.idea.designer.model.PropertiesContainer;
import consulo.ide.impl.idea.designer.model.PropertyContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author Alexander Lobas
 */
public interface PropertyRenderer {
  @Nonnull
  JComponent getComponent(@Nullable PropertiesContainer container,
                          PropertyContext context, @Nullable Object value, boolean selected, boolean hasFocus);

  void updateUI();
}