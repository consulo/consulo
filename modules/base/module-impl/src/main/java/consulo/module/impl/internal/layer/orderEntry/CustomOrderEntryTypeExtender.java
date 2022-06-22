/*
 * Copyright 2013-2022 consulo.io
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
package consulo.module.impl.internal.layer.orderEntry;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.component.extension.ExtensionExtender;
import consulo.module.content.layer.orderEntry.CustomOrderEntryTypeProvider;
import consulo.module.content.layer.orderEntry.OrderEntryType;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 22-May-22
 */
@ExtensionImpl
public class CustomOrderEntryTypeExtender implements ExtensionExtender<OrderEntryType> {
  @Override
  @SuppressWarnings("unchecked")
  public void extend(@Nonnull ComponentManager componentManager, @Nonnull Consumer<OrderEntryType> consumer) {
    CustomOrderEntryTypeProvider.EP.forEachExtensionSafe((Application)componentManager, it -> {
      consumer.accept(new CustomOrderEntryTypeWrapper<>(it));
    });
  }

  @Override
  public boolean hasAnyExtensions() {
    return CustomOrderEntryTypeProvider.EP.hasAnyExtensions(Application.get());
  }

  @Nonnull
  @Override
  public Class<OrderEntryType> getExtensionClass() {
    return OrderEntryType.class;
  }
}
