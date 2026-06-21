/*
 * Copyright 2013-2024 consulo.io
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
package consulo.diff.util;

import consulo.diff.DiffUserDataKeys;
import consulo.diff.internal.GenericDataProvider;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 06-Jul-24
 */
public class DiffUtil {

  // TODO: DiffUserDataKeys.DATA_PROVIDER is Key<DataProvider>, but GenericDataProvider now implements UiDataProvider.
  //  This method needs to be reworked as part of the DiffUserDataKeys migration to support UiDataProvider pattern.
  public static <T> void putDataKey(UserDataHolder holder, Key<T> key, @Nullable T value) {
    Object dataProvider = holder.getUserData(DiffUserDataKeys.DATA_PROVIDER);
    GenericDataProvider genericDataProvider;
    if (dataProvider instanceof GenericDataProvider gdp) {
      genericDataProvider = gdp;
    }
    else {
      genericDataProvider = new GenericDataProvider();
      //noinspection unchecked
      holder.putUserData((Key) DiffUserDataKeys.DATA_PROVIDER, genericDataProvider);
    }
    genericDataProvider.putData(key, value);
  }
}
