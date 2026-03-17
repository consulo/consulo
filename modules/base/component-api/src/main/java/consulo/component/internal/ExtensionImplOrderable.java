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
package consulo.component.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author VISTALL
 * @since 2024-06-11
 */
class ExtensionImplOrderable<K> implements LoadingOrder.Orderable {
  private static final Logger LOG = LoggerFactory.getLogger(ExtensionImplOrderable.class);

  private final String myOrderId;
  private final ExtensionValue<K> myValue;

  private LoadingOrder myLoadingOrder;

  ExtensionImplOrderable(ExtensionValue<K> value) {
    myValue = value;

    K extensionImpl = myValue.extension();
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

  protected void reportFirstLastRestriction(PluginDescriptor apiPlugin) {
    // we allow it in platform impl
    if (PluginIds.isPlatformPlugin(myValue.pluginDescriptor().getPluginId())) {
      return;
    }

    if (myLoadingOrder == LoadingOrder.FIRST || myLoadingOrder == LoadingOrder.LAST) {
      if (apiPlugin.getPluginId() != myValue.pluginDescriptor().getPluginId()) {
        LOG.error(
            "Usage order [first, last] is restricted for owner plugin impl only. Class: {}, Plugin: {}, Owner Plugin: {}",
            myValue.extension(),
            myValue.pluginDescriptor().getPluginId().getIdString(),
            apiPlugin.getPluginId().getIdString()
        );
        myLoadingOrder = LoadingOrder.ANY;
      }
    }
  }

  public ExtensionValue<K> getValue() {
    return myValue;
  }

  @Override
  public Object getObjectValue() {
    return myValue.extension();
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
