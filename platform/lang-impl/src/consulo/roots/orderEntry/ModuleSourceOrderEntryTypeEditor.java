/*
 * Copyright 2013-2016 must-be.org
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
package consulo.roots.orderEntry;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.roots.impl.ModuleSourceOrderEntryImpl;
import com.intellij.openapi.roots.ui.CellAppearanceEx;
import com.intellij.openapi.roots.ui.util.SimpleTextCellAppearance;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 06-Jun-16
 */
public class ModuleSourceOrderEntryTypeEditor implements OrderEntryTypeEditor<ModuleSourceOrderEntryImpl> {
  @NotNull
  @Override
  public CellAppearanceEx getCellAppearance(@NotNull ModuleSourceOrderEntryImpl orderEntry) {
    return SimpleTextCellAppearance.synthetic(orderEntry.getPresentableName(), AllIcons.Nodes.Module);
  }
}
