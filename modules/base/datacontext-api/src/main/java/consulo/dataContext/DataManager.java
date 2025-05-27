/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.dataContext;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.internal.RootComponentHolder;
import consulo.util.concurrent.AsyncResult;
import consulo.util.concurrent.Promise;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ServiceAPI(ComponentScope.APPLICATION)
public interface DataManager {
  @Nonnull
  public static DataManager getInstance() {
    return RootComponentHolder.getRootComponent().getInstance(DataManager.class);
  }

  /**
   * @return {@link DataContext} constructed by the current focused component
   * @deprecated use either {@link #getDataContext(consulo.ui.Component)} or {@link #getDataContextFromFocus()}
   */
  @Nonnull
  DataContext getDataContext();

  @Nonnull
  AsyncDataContext createAsyncDataContext(@Nonnull DataContext dataContext);

  /**
   * @return {@link DataContext} constructed by the currently focused component.
   */
  @Nonnull
  Promise<DataContext> getDataContextFromFocusAsync();

  @Nonnull
  AsyncResult<DataContext> getDataContextFromFocus();

  /**
   * @return {@link DataContext} constructed by the specified <code>component</code>
   */
  @Nonnull
  DataContext getDataContext(@Nullable consulo.ui.Component component);

  /**
   * @param dataContext should be instance of {@link UserDataHolder}
   * @param dataKey     key to store value
   * @param data        value to store
   */
  <T> void saveInDataContext(@Nullable DataContext dataContext, @Nonnull Key<T> dataKey, @Nullable T data);

  /**
   * @param dataContext find by key if instance of {@link UserDataHolder}
   * @param dataKey     key to find value by
   * @return value stored by {@link #saveInDataContext(DataContext, Key, Object)}
   */
  @Nullable
  <T> T loadFromDataContext(@Nonnull DataContext dataContext, @Nonnull Key<T> dataKey);

  // TODO [VISTALL] region AWT & Swing dependency

  // region AWT & Swing dependency

  /**
   * @return {@link DataContext} constructed be the specified <code>component</code>
   * and the point specified by <code>x</code> and <code>y</code> coordinate inside the
   * component.
   * @throws java.lang.IllegalArgumentException if point <code>(x, y)</code> is not inside
   *                                            component's bounds
   */
  default DataContext getDataContext(@Nonnull java.awt.Component component, int x, int y) {
    throw new UnsupportedOperationException();
  }

  public static final String CLIENT_PROPERTY_DATA_PROVIDER = "DataProvider";

  public static void registerDataProvider(@Nonnull javax.swing.JComponent component, @Nonnull DataProvider provider) {
    component.putClientProperty(CLIENT_PROPERTY_DATA_PROVIDER, provider);
  }

  /**
   * @return {@link DataContext} constructed by the specified <code>component</code>
   */
  default DataContext getDataContext(@Nullable java.awt.Component component) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  public static DataProvider getDataProvider(@Nonnull javax.swing.JComponent component) {
    return (DataProvider)component.getClientProperty(CLIENT_PROPERTY_DATA_PROVIDER);
  }

  public static void removeDataProvider(@Nonnull javax.swing.JComponent component) {
    component.putClientProperty(CLIENT_PROPERTY_DATA_PROVIDER, null);
  }
  // endregion
}
