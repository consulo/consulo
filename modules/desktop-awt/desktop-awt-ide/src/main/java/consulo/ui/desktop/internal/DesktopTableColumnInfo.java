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
import consulo.ui.TableColumn;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2020-09-15
 */
public class DesktopTableColumnInfo<Value, Item> extends ColumnInfo<Item, Value> implements TableColumn<Value, Item> {
  private final Function<Item, Value> myConverter;

  public DesktopTableColumnInfo(String name, Function<Item, Value> converter) {
    super(name);
    myConverter = converter;
  }

  @Nullable
  @Override
  public Value valueOf(Item value) {
    return myConverter.apply(value);
  }
}
