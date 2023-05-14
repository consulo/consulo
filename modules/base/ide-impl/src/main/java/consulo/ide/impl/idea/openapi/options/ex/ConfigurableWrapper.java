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
package consulo.ide.impl.idea.openapi.options.ex;

import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ArrayUtil;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

/**
 * @author Dmitry Avdeev
 * Date: 9/17/12
 */
public class ConfigurableWrapper implements SearchableConfigurable {
  public static ConfigurableWrapper wrapConfigurable(Configurable configurable) {
    return new CompositeWrapper(configurable);
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
    return false;
  }

  private final Configurable myConfigurable;

  public ConfigurableWrapper(Configurable configurable) {
    myConfigurable = configurable;
  }

  public Configurable getConfigurable() {
    return myConfigurable;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return myConfigurable.getDisplayName();
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return myConfigurable.getHelpTopic();
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
  public JComponent createComponent() {
    return getConfigurable().createComponent();
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public Component createUIComponent() {
    return getConfigurable().createUIComponent();
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
    return myConfigurable.getId();
  }

  @Override
  public String getParentId() {
    return myConfigurable.getParentId();
  }

  public ConfigurableWrapper addChild(Configurable configurable) {
    return new CompositeWrapper(myConfigurable, configurable);
  }

  @Nonnull
  @Override
  public Class<?> getOriginalClass() {
    final UnnamedConfigurable configurable = getConfigurable();
    return configurable instanceof SearchableConfigurable ? ((SearchableConfigurable)configurable).getOriginalClass() : configurable.getClass();
  }

  @Override
  public String toString() {
    return myConfigurable.toString();
  }

  @Nullable
  @Override
  public Runnable enableSearch(String option) {
    final UnnamedConfigurable configurable = getConfigurable();
    return configurable instanceof SearchableConfigurable ? ((SearchableConfigurable)configurable).enableSearch(option) : null;
  }

  private static class CompositeWrapper extends ConfigurableWrapper implements Configurable.Composite {
    private Configurable[] myKids;

    public CompositeWrapper(Configurable configurable, Configurable... kids) {
      super(configurable);
      if (configurable instanceof Composite c) {
        kids = ArrayUtil.mergeArrays(c.getConfigurables(), kids);
      }
      myKids = kids;
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
