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
package consulo.component.impl.internal.extension;

import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 11-Jun-24
 */
public class InvalidOrderable<K> implements LoadingOrder.Orderable {
  private final K myValue;

  public InvalidOrderable(K value) {
    myValue = value;
  }

  public K getValue() {
    return myValue;
  }

  @Override
  public Object getObjectValue() {
    return myValue;
  }

  @Nullable
  @Override
  public String getOrderId() {
    return null;
  }

  @Override
  public LoadingOrder getOrder() {
    return LoadingOrder.ANY;
  }
}
