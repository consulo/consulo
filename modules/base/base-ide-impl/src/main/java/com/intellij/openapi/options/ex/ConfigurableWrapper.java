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

import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtil;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.options.ProjectConfigurableEP;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;

/**
 * @author Dmitry Avdeev
 * Date: 9/17/12
 */
public class ConfigurableWrapper implements SearchableConfigurable {

  private static final ConfigurableWrapper[] EMPTY_ARRAY = new ConfigurableWrapper[0];
  private static final NullableFunction<ConfigurableEP<Configurable>, Configurable> CONFIGURABLE_FUNCTION = ConfigurableWrapper::wrapConfigurable;
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
    return ContainerUtil.mapNotNull(name.getExtensionList(), ConfigurableWrapper::wrapConfigurable);
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
    if (configurable instanceof ConfigurableWrapper) {
      ConfigurableEP ep = ((ConfigurableWrapper)configurable).myEp;
      if (ep instanceof ProjectConfigurableEP && ((ProjectConfigurableEP)ep).nonDefaultProject) {
        return true;
      }
    }
    return false;
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

  public ConfigurableEP getEp() {
    return myEp;
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

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent createComponent(@Nonnull Disposable parentUIDisposable) {
    return getConfigurable().createComponent(parentUIDisposable);
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public Component createUIComponent(@Nonnull Disposable parentUIDisposable) {
    return getConfigurable().createUIComponent(parentUIDisposable);
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return getConfigurable().isModified();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    getConfigurable().apply();
  }

  @RequiredUIAccess
  @Override
  public void initialize() {
    getConfigurable().initialize();
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    getConfigurable().reset();
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    getConfigurable().disposeUIResources();
  }

  @Nonnull
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

  @Nonnull
  @Override
  public Class<?> getOriginalClass() {
    final UnnamedConfigurable configurable = getConfigurable();
    return configurable instanceof SearchableConfigurable ? ((SearchableConfigurable)configurable).getOriginalClass() : configurable.getClass();
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
      else if (ep.getChildren() != null) {
        kids = ContainerUtil.mapNotNull(ep.getChildren(), ep1 -> ep1.isAvailable() ? (ConfigurableWrapper)wrapConfigurable(ep1) : null, EMPTY_ARRAY);
      }
      if (ep.childrenEPName != null) {
        ExtensionPoint<Object> childrenEP = getProject(ep).getExtensionPoint(ExtensionPointName.create(ep.childrenEPName));
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

    private Project getProject(ConfigurableEP<?> ep) {
      if (ep instanceof ProjectConfigurableEP) {
        return ((ProjectConfigurableEP)ep).getProject();
      }
      return null;
    }

    @Nonnull
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
