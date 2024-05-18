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
package consulo.execution.impl.internal.configuration;

import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationType;
import consulo.project.Project;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 14.05.2024
 */
public class ConfigurationTypeSelector {
  public static List<ConfigurationType> getTypesToShow(Project project, boolean showApplicableTypesOnly, List<ConfigurationType> allTypes) {
    if (showApplicableTypesOnly) {
      List<ConfigurationType> applicableTypes = new ArrayList<>();
      for (ConfigurationType type : allTypes) {
        if (isApplicable(project, type)) {
          applicableTypes.add(type);
        }
      }
      return applicableTypes;
    }
    return new ArrayList<>(allTypes);
  }

  public static boolean isApplicable(Project project, ConfigurationType type) {
    for (ConfigurationFactory factory : type.getConfigurationFactories()) {
      if (factory.isApplicable(project)) {
        return true;
      }
    }
    return false;
  }
}
