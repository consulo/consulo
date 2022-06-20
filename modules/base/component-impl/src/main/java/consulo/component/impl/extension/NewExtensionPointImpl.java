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
package consulo.component.impl.extension;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.ComponentManager;
import consulo.component.bind.InjectingBinding;
import consulo.component.extension.ExtensionPoint;
import consulo.injecting.InjectingContainer;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 17-Jun-22
 */
public class NewExtensionPointImpl<T> implements ExtensionPoint<T> {
  private static final Logger LOG = Logger.getInstance(NewExtensionPointImpl.class);

  private static class ExtensionImplOrderable<K> implements LoadingOrder.Orderable {
    private final String myOrderId;
    private final K myValue;
    private final LoadingOrder myLoadingOrder;

    private ExtensionImplOrderable(K value) {
      myValue = value;
      Class<?> valueClass = value.getClass();
      ExtensionImpl extensionImpl = valueClass.getAnnotation(ExtensionImpl.class);
      assert extensionImpl != null;

      myOrderId = StringUtil.isEmptyOrSpaces(extensionImpl.id()) ? valueClass.getSimpleName() : extensionImpl.id();
      myLoadingOrder = LoadingOrder.readOrder(extensionImpl.order());
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

  private AtomicBoolean myInitialized = new AtomicBoolean();

  private Class<T> myApiClass;

  private final String myApiClassName;
  private final List<InjectingBinding> myInjectingBindings;
  private final ComponentManager myComponentManager;

  private List<T> myExtensions;

  public NewExtensionPointImpl(String apiClassName, List<InjectingBinding> bindings, ComponentManager componentManager) {
    myApiClassName = apiClassName;
    myInjectingBindings = bindings;
    myComponentManager = componentManager;
  }

  public void initialize(Class<T> extensionClass) {
    if (myInitialized.compareAndSet(false, true)) {
      myApiClass = extensionClass;
    }
  }

  @SuppressWarnings("unchecked")
  private List<T> getOrInit() {
    if (myExtensions != null) {
      return myExtensions;
    }

    InjectingContainer injectingContainer = myComponentManager.getInjectingContainer();

    List<T> extensions = new ArrayList<>(myInjectingBindings.size());
    for (InjectingBinding binding : myInjectingBindings) {
      T extensionInstance = null;
      try {
        extensionInstance = (T) injectingContainer.getUnbindedInstance(binding.getImplClass(), binding.getParameterTypes(), binding::create);
        extensions.add(extensionInstance);
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }

    // prepare for sorting
    List<ExtensionImplOrderable<T>> orders = new ArrayList<>(extensions.size());
    for (T extension : extensions) {
      orders.add(new ExtensionImplOrderable<>(extension));
    }

    LoadingOrder.sort(orders);

    // set new order
    for (int i = 0; i < orders.size(); i++) {
      extensions.set(i, orders.get(i).myValue);
    }

    myExtensions = extensions;
    return extensions;
  }

  @Nonnull
  @Override
  public String getName() {
    return myApiClass.getName();
  }

  @Nonnull
  @Override
  public List<T> getExtensionList() {
    return getOrInit();
  }

  @Nonnull
  @Override
  public Class<T> getExtensionClass() {
    if (!myInitialized.get()) {
      throw new IllegalArgumentException("not initialized");
    }
    return myApiClass;
  }

  @Nonnull
  @Override
  public Kind getKind() {
    return myApiClass.isInterface() ? Kind.INTERFACE : Kind.BEAN_CLASS;
  }

  @Nonnull
  @Override
  public String getClassName() {
    return myApiClassName;
  }
}
