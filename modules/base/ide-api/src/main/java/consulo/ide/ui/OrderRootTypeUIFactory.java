/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
 * User: anna
 * Date: 26-Dec-2007
 */
package consulo.ide.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.content.OrderRootType;
import consulo.content.bundle.Sdk;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface OrderRootTypeUIFactory {
    ExtensionPointCacheKey<OrderRootTypeUIFactory, Map<String, OrderRootTypeUIFactory>> KEY =
        ExtensionPointCacheKey.groupBy("OrderRootTypeUIFactory", OrderRootTypeUIFactory::getOrderRootTypeId);

    @Nullable
    static OrderRootTypeUIFactory forOrderType(@Nonnull OrderRootType orderRootType) {
        ExtensionPoint<OrderRootTypeUIFactory> point = Application.get().getExtensionPoint(OrderRootTypeUIFactory.class);
        Map<String, OrderRootTypeUIFactory> map = point.getOrBuildCache(KEY);
        return map.get(orderRootType.getId());
    }

    @Nonnull
    String getOrderRootTypeId();

    @Nonnull
    SdkPathEditor createPathEditor(Sdk sdk);

    @Nonnull
    Image getIcon();

    @Nonnull
    String getNodeText();
}
