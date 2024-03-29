/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.ui.ex.awt.ItemRemovable;

/**
 * @author Konstantin Bulenkov
 * @since 11.0
 */
public interface EditableModel extends ItemRemovable {
  void addRow();

  void exchangeRows(int oldIndex, int newIndex);

  boolean canExchangeRows(int oldIndex, int newIndex);
}
