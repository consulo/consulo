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

import consulo.annotation.component.ExtensionImpl;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.logging.Logger;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 11-Jun-24
 */
class ExtensionImplOrderable<K> implements LoadingOrder.Orderable {
  private static final Logger LOG = Logger.getInstance(ExtensionImplOrderable.class);

  private final String myOrderId;
  private final Pair<K, PluginDescriptor> myValue;

  private LoadingOrder myLoadingOrder;

  ExtensionImplOrderable(Pair<K, PluginDescriptor> value) {
    myValue = value;

    K extensionImpl = myValue.getFirst();
    Class<?> extensionImplClass = extensionImpl.getClass();

    ExtensionImpl extensionImplAnnotation = extensionImplClass.getAnnotation(ExtensionImpl.class);
    // extension impl can be null if extension added by ExtensionExtender
    if (extensionImplAnnotation != null) {
      myOrderId =
        StringUtil.isEmptyOrSpaces(extensionImplAnnotation.id()) ? extensionImplClass.getSimpleName() : extensionImplAnnotation.id();
      myLoadingOrder = LoadingOrder.readOrder(extensionImplAnnotation.order());
    }
    else {
      myOrderId = extensionImplClass.getSimpleName();
      myLoadingOrder = LoadingOrder.ANY;
    }
  }

  protected void reportFirstLastRestriction(@Nonnull PluginDescriptor apiPlugin) {
    // we allow it in platform impl
    if (PluginIds.isPlatformPlugin(myValue.getValue().getPluginId())) {
      return;
    }

    if (myLoadingOrder == LoadingOrder.FIRST || myLoadingOrder == LoadingOrder.LAST) {
      if (apiPlugin.getPluginId() != myValue.getValue().getPluginId()) {
        LOG.error("Usage order [first, last] is restricted for owner plugin impl only. Class: %s, Plugin: %s, Owner Plugin: %s"
                    .formatted(myValue.getKey().toString(),
                               myValue.getValue().getPluginId().getIdString(),
                               apiPlugin.getPluginId().getIdString()));
        myLoadingOrder = LoadingOrder.ANY;
      }
    }
  }

  public Pair<K, PluginDescriptor> getValue() {
    return myValue;
  }

  @Override
  public Object getObjectValue() {
    return myValue.getKey();
  }

  @Nullable
  @Override
  public String getOrderId() {
    return myOrderId;
  }

  @Override
  public LoadingOrder getOrder() {
    return myLoadingOrder;
  }
}
