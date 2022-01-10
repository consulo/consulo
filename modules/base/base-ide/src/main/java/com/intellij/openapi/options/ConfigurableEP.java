/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.options;

import com.intellij.AbstractBundle;
import com.intellij.CommonBundle;
import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.util.AtomicNotNullLazyValue;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import consulo.injecting.InjectingContainerOwner;
import consulo.localize.LocalizeManager;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ResourceBundle;

/**
 * @author nik
 * @see Configurable
 */
@Tag("configurable")
public abstract class ConfigurableEP<T extends UnnamedConfigurable> extends AbstractExtensionPointBean {
  private static final Logger LOG = Logger.getInstance(ConfigurableEP.class);

  @Attribute("displayName")
  public String displayName;

  @Attribute("key")
  public String key;

  @Attribute("bundle")
  public String bundle;

  public String getDisplayName() {
    if (displayName != null) {
      if (displayName.contains("@")) {
        return LocalizeManager.get().fromStringKey(displayName).get();
      }
      return displayName;
    }

    LOG.assertTrue(bundle != null, "Bundle missed for " + instanceClass);
    final ResourceBundle resourceBundle = AbstractBundle.getResourceBundle(bundle, myPluginDescriptor.getPluginClassLoader());
    return displayName = CommonBundle.message(resourceBundle, key);
  }

  /**
   * Extension point of ConfigurableEP type to calculate children
   */
  @Attribute("childrenEPName")
  public String childrenEPName;

  /**
   * Indicates that configurable has dynamically calculated children.
   * {@link com.intellij.openapi.options.Configurable.Composite#getConfigurables()} will be called for such configurables.
   */
  @Attribute("dynamic")
  public boolean dynamic;

  @Attribute("parentId")
  public String parentId;

  public abstract ConfigurableEP[] getChildren();

  @Attribute("id")
  public String id;


  public boolean isAvailable() {
    return true;
  }

  /**
   * @deprecated use '{@link #instanceClass instance}' or '{@link #providerClass provider}' attribute instead
   */
  @Attribute("implementation")
  public String implementationClass;
  @Attribute("instance")
  public String instanceClass;
  @Attribute("provider")
  public String providerClass;

  private final AtomicNotNullLazyValue<ConfigrableFactory<T>> myFactory;
  protected InjectingContainerOwner myContainerOwner;

  protected ConfigurableEP(InjectingContainerOwner containerOwner) {
    myContainerOwner = containerOwner;
    myFactory = new AtomicNotNullLazyValue<ConfigrableFactory<T>>() {
      @Nonnull
      @Override
      protected ConfigrableFactory<T> compute() {
        if (providerClass != null) {
          return new InstanceFromProviderFactory();
        }
        else if (instanceClass != null) {
          return new NewInstanceFactory();
        }
        else if (implementationClass != null) {
          return new ImplementationFactory();
        }
        throw new RuntimeException();
      }
    };
  }

  @Nullable
  public T createConfigurable() {
    try {
      return myFactory.getValue().create();
    }
    catch (LinkageError | AssertionError | Exception e) {
      LOG.error(e);
    }
    return null;
  }

  @Override
  public String toString() {
    return getDisplayName();
  }

  private interface ConfigrableFactory<K extends UnnamedConfigurable> {
    K create();
  }

  private class InstanceFromProviderFactory extends AtomicNotNullLazyValue<ConfigurableProvider> implements ConfigrableFactory<T> {
    @Override
    public T create() {
      return (T)getValue().createConfigurable();
    }

    @Nonnull
    @Override
    protected ConfigurableProvider compute() {
      try {
        return instantiate(providerClass, myContainerOwner.getInjectingContainer());
      }
      catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private class NewInstanceFactory extends NotNullLazyValue<Class<? extends T>> implements ConfigrableFactory<T> {
    @Override
    public T create() {
      return instantiate(getValue(), myContainerOwner.getInjectingContainer());
    }

    @Nonnull
    @Override
    protected Class<? extends T> compute() {
      try {
        return findClass(instanceClass);
      }
      catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private class ImplementationFactory extends AtomicNotNullLazyValue<T> implements ConfigrableFactory<T> {
    @Override
    public T create() {
      return compute();
    }

    @Nonnull
    @Override
    protected T compute() {
      try {
        final Class<T> aClass = findClass(implementationClass);
        return instantiate(aClass, myContainerOwner.getInjectingContainer());
      }
      catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
