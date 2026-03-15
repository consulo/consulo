/*
 * Copyright 2013-2023 consulo.io
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

/**
 * @author VISTALL
 * @since 08/07/2023
 */
public abstract class SimpleConfigurationType extends ConfigurationFactory implements ConfigurationType {
  private final String myId;
  private final LocalizeValue myDisplayName;
  private final LocalizeValue myDescription;
  private final Image myIcon;

  public SimpleConfigurationType(String id,
                                 LocalizeValue displayName,
                                 Image icon)  {
    this(id, displayName, displayName, icon);
  }

  public SimpleConfigurationType(String id,
                                 LocalizeValue displayName,
                                 LocalizeValue description,
                                 Image icon) {
    myId = id;
    myDisplayName = displayName;
    myDescription = description;
    myIcon = icon;
  }

  
  @Override
  public final ConfigurationType getType() {
    return this;
  }

  
  @Override
  public final LocalizeValue getDisplayName() {
    return myDisplayName;
  }

  
  @Override
  public final LocalizeValue getConfigurationTypeDescription() {
    return myDescription;
  }

  
  @Override
  public final Image getIcon() {
    return myIcon;
  }

  
  @Override
  public final String getId() {
    return myId;
  }

  @Override
  public final ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{this};
  }
}
