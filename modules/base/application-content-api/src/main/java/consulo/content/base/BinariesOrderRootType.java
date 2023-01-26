/*
 * Copyright 2013-2016 consulo.io
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
package consulo.content.base;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.extension.ExtensionInstance;
import consulo.content.OrderRootType;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 17.08.14
 */
@ExtensionImpl
public class BinariesOrderRootType extends OrderRootType {
  private static final Supplier<BinariesOrderRootType> INSTANCE = ExtensionInstance.of();

  @Nonnull
  public static BinariesOrderRootType getInstance() {
    return INSTANCE.get();
  }

  public BinariesOrderRootType() {
    super("binaries");
  }
}
