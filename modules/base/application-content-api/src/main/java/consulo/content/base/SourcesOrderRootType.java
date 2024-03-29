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

import jakarta.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 17.08.14
 */
@ExtensionImpl
public class SourcesOrderRootType extends OrderRootType {
  private static final Supplier<SourcesOrderRootType> INSTANCE = ExtensionInstance.from(OrderRootType.class);

  @Nonnull
  public static SourcesOrderRootType getInstance() {
    return INSTANCE.get();
  }

  public SourcesOrderRootType() {
    super("sources");
  }
}
