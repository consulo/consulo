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

/*
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 25.10.2006
 * Time: 17:24:41
 */
package com.intellij.ide.impl;

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.TypeSafeDataProvider;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TypeSafeDataProviderAdapter implements DataProvider, DataSink {
  private final TypeSafeDataProvider myProvider;
  private Key<?> myLastKey = null;
  private Object myValue = null;

  public TypeSafeDataProviderAdapter(final TypeSafeDataProvider provider) {
    myProvider = provider;
  }

  @Override
  @Nullable
  public synchronized Object getData(@Nonnull Key<?> dataId) {
    myValue = null;
    myLastKey = dataId;
    myProvider.calcData(myLastKey, this);
    return myValue;
  }

  @Override
  public synchronized <T> void put(Key<T> key, T data) {
    if (key == myLastKey) {
      myValue = data;
    }
  }

  @Override
  public String toString() {
    return super.toString() + '(' + myProvider + ')';
  }
}
