/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.desktop.internal;

import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import consulo.ui.model.TableModel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 2020-11-23
 */
class DesktopTableModelImpl<Item> extends ListTableModel<Item> implements TableModel<Item> {
  public DesktopTableModelImpl(Collection<? extends Item> items) {
    super(new ColumnInfo[0], new ArrayList<>(items));
  }

  @Override
  public int getSize() {
    return getRowCount();
  }

  @Nonnull
  @Override
  public Item get(int index) {
    return getItem(index);
  }
}
