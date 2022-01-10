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
package com.intellij.ide;

import com.intellij.openapi.actionSystem.AsyncDataContext;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.util.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.concurrency.Promise;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class DataManager {
  @Nonnull
  public static DataManager getInstance() {
    return Application.get().getComponent(DataManager.class);
  }

  /**
   * @return {@link DataContext} constructed by the current focused component
   * @deprecated use either {@link #getDataContext(consulo.ui.Component)} or {@link #getDataContextFromFocus()}
   */
  @Nonnull
  public abstract DataContext getDataContext();

  @Nonnull
  public abstract AsyncDataContext createAsyncDataContext(@Nonnull DataContext dataContext);

  /**
   * @return {@link DataContext} constructed by the currently focused component.
   */
  @Nonnull
  public abstract Promise<DataContext> getDataContextFromFocusAsync();

  @Nonnull
  public abstract AsyncResult<DataContext> getDataContextFromFocus();

  /**
   * @return {@link DataContext} constructed by the specified <code>component</code>
   */
  @Nonnull
  public abstract DataContext getDataContext(@Nullable consulo.ui.Component component);

  /**
   * @param dataContext should be instance of {@link UserDataHolder}
   * @param dataKey     key to store value
   * @param data        value to store
   */
  public abstract <T> void saveInDataContext(@Nullable DataContext dataContext, @Nonnull Key<T> dataKey, @Nullable T data);

  /**
   * @param dataContext find by key if instance of {@link UserDataHolder}
   * @param dataKey     key to find value by
   * @return value stored by {@link #saveInDataContext(com.intellij.openapi.actionSystem.DataContext, Key, Object)}
   */
  @Nullable
  public abstract <T> T loadFromDataContext(@Nonnull DataContext dataContext, @Nonnull Key<T> dataKey);

  // TODO [VISTALL] region AWT & Swing dependency

  // region AWT & Swing dependency

  /**
   * @return {@link DataContext} constructed be the specified <code>component</code>
   * and the point specified by <code>x</code> and <code>y</code> coordinate inside the
   * component.
   * @throws java.lang.IllegalArgumentException if point <code>(x, y)</code> is not inside
   *                                            component's bounds
   */
  public DataContext getDataContext(@Nonnull java.awt.Component component, int x, int y) {
    throw new UnsupportedOperationException();
  }

  @NonNls
  public static final String CLIENT_PROPERTY_DATA_PROVIDER = "DataProvider";

  public static void registerDataProvider(@Nonnull javax.swing.JComponent component, @Nonnull DataProvider provider) {
    component.putClientProperty(CLIENT_PROPERTY_DATA_PROVIDER, provider);
  }

  /**
   * @return {@link DataContext} constructed by the specified <code>component</code>
   */
  public DataContext getDataContext(@Nullable java.awt.Component component) {
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
