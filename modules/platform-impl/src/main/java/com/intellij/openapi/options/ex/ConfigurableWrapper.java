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
package com.intellij.openapi.options.ex;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.options.*;
import com.intellij.util.ArrayUtil;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author Dmitry Avdeev
 *         Date: 9/17/12
 */
public class ConfigurableWrapper implements SearchableConfigurable {

  private static final ConfigurableWrapper[] EMPTY_ARRAY = new ConfigurableWrapper[0];
  private static final NullableFunction<ConfigurableEP<Configurable>, Configurable> CONFIGURABLE_FUNCTION =
          new NullableFunction<ConfigurableEP<Configurable>, Configurable>() {
            @Override
            public Configurable fun(ConfigurableEP<Configurable> ep) {
              return wrapConfigurable(ep);
            }
          };
  private static final Logger LOG = Logger.getInstance(ConfigurableWrapper.class);

  @Nullable
  @SuppressWarnings("unchecked")
  public static <T extends UnnamedConfigurable> T wrapConfigurable(ConfigurableEP<T> ep) {
    if (ep.displayName != null || ep.key != null) {
      return (T)new CompositeWrapper(ep);
    }
    else {
      return ep.createConfigurable();
    }
  }

  public static <T extends UnnamedConfigurable> List<T> createConfigurables(ExtensionPointName<? extends ConfigurableEP<T>> name) {
    return ContainerUtil.mapNotNull(Extensions.getExtensions(name), new NullableFunction<ConfigurableEP<T>, T>() {
      @Override
      public T fun(ConfigurableEP<T> ep) {
        return wrapConfigurable(ep);
      }
    });
  }

  public static boolean isNoScroll(Configurable configurable) {
    return cast(configurable, NoScroll.class) != null;
  }

  public static boolean isNoMargin(Configurable configurable) {
    return cast(configurable, NoMargin.class) != null;
  }

  public static boolean hasOwnContent(Configurable configurable) {
    Parent asParent = cast(configurable, Parent.class);
    return asParent != null && asParent.hasOwnContent();
  }

  public static <T> T cast(Configurable configurable, Class<T> clazz) {
    if (configurable == null) {
      return null;
    }

    if (clazz.isInstance(configurable)) {
      return clazz.cast(configurable);
    }

    if (configurable instanceof ConfigurableWrapper) {
      UnnamedConfigurable unnamedConfigurable = ((ConfigurableWrapper)configurable).getConfigurable();
      if (clazz.isInstance(unnamedConfigurable)) {
        return clazz.cast(unnamedConfigurable);
      }
    }
    return null;
  }

  public static boolean isNonDefaultProject(Configurable configurable) {
    if (cast(configurable, NonDefaultProjectConfigurable.class) != null) {
      return true;
    }
    return configurable instanceof ConfigurableWrapper && ((ConfigurableWrapper)configurable).myEp.nonDefaultProject;
  }

  private final ConfigurableEP myEp;

  public ConfigurableWrapper(ConfigurableEP ep) {
    myEp = ep;
  }

  private UnnamedConfigurable myConfigurable;

  public UnnamedConfigurable getConfigurable() {
    if (myConfigurable == null) {
      myConfigurable = myEp.createConfigurable();
      if (myConfigurable == null) {
        LOG.error("Can't instantiate configurable for " + myEp);
      }
    }
    return myConfigurable;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return myEp.getDisplayName();
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    UnnamedConfigurable configurable = getConfigurable();
    return configurable instanceof Configurable ? ((Configurable)configurable).getHelpTopic() : null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return getConfigurable().createComponent();
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public Component createUIComponent() {
    return getConfigurable().createUIComponent();
  }

  @Override
  public boolean isModified() {
    return getConfigurable().isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    getConfigurable().apply();
  }

  @Override
  public void reset() {
    getConfigurable().reset();
  }

  @Override
  public void disposeUIResources() {
    getConfigurable().disposeUIResources();
  }

  @NotNull
  @Override
  public String getId() {
    return myEp.id == null ? myEp.instanceClass : myEp.id;
  }


  public String getParentId() {
    return myEp.parentId;
  }

  public ConfigurableWrapper addChild(Configurable configurable) {
    return new CompositeWrapper(myEp, configurable);
  }

  @Override
  public String toString() {
    return getDisplayName();
  }

  @Nullable
  @Override
  public Runnable enableSearch(String option) {
    final UnnamedConfigurable configurable = getConfigurable();
    return configurable instanceof SearchableConfigurable ? ((SearchableConfigurable)configurable).enableSearch(option) : null;
  }

  private static class CompositeWrapper extends ConfigurableWrapper implements Configurable.Composite {

    private Configurable[] myKids;

    public CompositeWrapper(ConfigurableEP ep, Configurable... kids) {
      super(ep);
      if (ep.dynamic) {
        kids = ((Composite)getConfigurable()).getConfigurables();
      }
      else if (ep.children != null) {
        kids = ContainerUtil.mapNotNull(ep.getChildren(), ep1 -> ep1.isAvailable() ? (ConfigurableWrapper)wrapConfigurable(ep1) : null, EMPTY_ARRAY);
      }
      if (ep.childrenEPName != null) {
        ExtensionPoint<Object> childrenEP = Extensions.getArea(ep.getProject()).getExtensionPoint(ep.childrenEPName);
        Object[] extensions = childrenEP.getExtensions();
        if (extensions.length > 0) {
          if (extensions[0] instanceof ConfigurableEP) {
            Configurable[] children = ContainerUtil.mapNotNull(((ConfigurableEP<Configurable>[])extensions), CONFIGURABLE_FUNCTION, new Configurable[0]);
            kids = ArrayUtil.mergeArrays(kids, children);
          }
          else {
            kids = ArrayUtil.mergeArrays(kids, ((Composite)getConfigurable()).getConfigurables());
          }
        }
      }
      myKids = kids;
    }

    @NotNull
    @Override
    public Configurable[] getConfigurables() {
      return myKids;
    }

    @Override
    public ConfigurableWrapper addChild(Configurable configurable) {
      myKids = ArrayUtil.append(myKids, configurable);
      return this;
    }
  }
}
