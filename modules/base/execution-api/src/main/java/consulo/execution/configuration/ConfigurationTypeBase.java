/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.execution.configuration;

import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
public abstract class ConfigurationTypeBase implements ConfigurationType {
  private static final ConfigurationFactory[] EMPTY_FACTORIES = new ConfigurationFactory[0];

  private final String myId;
  private final LocalizeValue myDisplayName;
  private final LocalizeValue myDescription;
  private final Image myIcon;
  private ConfigurationFactory[] myFactories;

  protected ConfigurationTypeBase(@Nonnull String id, @Nonnull LocalizeValue displayName, @Nonnull Image icon) {
    this(id, displayName, displayName, icon);
  }

  protected ConfigurationTypeBase(@Nonnull String id,
                                  @Nonnull LocalizeValue displayName,
                                  @Nonnull LocalizeValue description,
                                  @Nonnull Image icon) {
    myId = id;
    myDisplayName = displayName;
    myDescription = description;
    myIcon = icon;
    myFactories = EMPTY_FACTORIES;
  }

  protected void addFactory(ConfigurationFactory factory) {
    myFactories = ArrayUtil.append(myFactories, factory);
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return myDisplayName;
  }

  @Nonnull
  @Override
  public LocalizeValue getConfigurationTypeDescription() {
    return myDescription;
  }

  @Override
  public Image getIcon() {
    return myIcon;
  }

  @Override
  @Nonnull
  public String getId() {
    return myId;
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return myFactories;
  }
}
