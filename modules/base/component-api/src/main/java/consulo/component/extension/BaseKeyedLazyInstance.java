// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.component.extension;

import consulo.util.xml.serializer.annotation.Transient;
import consulo.component.ComponentManager;
import consulo.container.plugin.PluginDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BaseKeyedLazyInstance<T> extends LazyExtensionInstance<T> implements PluginAware, ComponentAware {
  private PluginDescriptor myPluginDescriptor;
  private ComponentManager myComponentManager;

  protected BaseKeyedLazyInstance() {
  }

  protected BaseKeyedLazyInstance(@Nonnull T instance) {
    super(instance);
  }

  @Transient
  @Nonnull
  public final PluginDescriptor getPluginDescriptor() {
    return myPluginDescriptor;
  }

  @Override
  public void setComponent(@Nonnull ComponentManager component) {
    myComponentManager = component;
  }

  @Override
  public final void setPluginDescriptor(@Nonnull PluginDescriptor value) {
    myPluginDescriptor = value;
  }

  @Override
  @Nullable
  protected abstract String getImplementationClassName();

  @Nonnull
  public final T getInstance() {
    return getInstance(myComponentManager, myPluginDescriptor);
  }

  @Nonnull
  public ClassLoader getLoaderForClass() {
    assert myPluginDescriptor != null;
    return myPluginDescriptor.getPluginClassLoader();
  }
}
