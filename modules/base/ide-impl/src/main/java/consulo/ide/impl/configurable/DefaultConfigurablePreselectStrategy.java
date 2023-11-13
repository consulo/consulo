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
package consulo.ide.impl.configurable;

import consulo.component.PropertiesComponent;
import consulo.configurable.Configurable;
import consulo.ide.impl.idea.ide.ui.search.SearchUtil;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 14/05/2023
 */
public class DefaultConfigurablePreselectStrategy implements ConfigurablePreselectStrategy {
  private static final String LAST_SELECTED_CONFIGURABLE = "options.lastSelected";

  private final PropertiesComponent myPropertiesComponent;

  public DefaultConfigurablePreselectStrategy(Project project) {
    myPropertiesComponent = ProjectPropertiesComponent.getInstance(project);
  }

  @Override
  public Configurable get(@Nonnull Configurable[] configurables) {
    return findLastSavedConfigurable(configurables, myPropertiesComponent);
  }

  @Nullable
  private static Configurable findLastSavedConfigurable(Configurable[] configurables, PropertiesComponent propertiesComponent) {
    final String id = propertiesComponent.getValue(LAST_SELECTED_CONFIGURABLE);
    if (id == null) return null;

    return findConfigurableInGroups(id, configurables);
  }

  @Nullable
  private static Configurable findConfigurableInGroups(String id, Configurable[] configurables) {
    // avoid unnecessary group expand: check top-level configurables in all groups before looking at children
    for (Configurable c : configurables) {
      if (id.equals(c.getId())) {
        return c;
      }
      else if (id.equals(c.getClass().getName())) {
        return c;
      }
    }

    for (Configurable c : configurables) {
      if (c instanceof Configurable.Composite) {
        Configurable result = findConfigurableInGroups(id, ((Configurable.Composite)c).getConfigurables());
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }


  @Nullable
  public static Configurable getPreselectedByDisplayName(final Configurable[] configurables,
                                                         final String preselectedConfigurableDisplayName,
                                                         final Project project) {
    Configurable result = findPreselectedByDisplayName(preselectedConfigurableDisplayName, configurables);

    return result == null ? findLastSavedConfigurable(configurables, ProjectPropertiesComponent.getInstance(project)) : result;
  }

  @Nullable
  private static Configurable findPreselectedByDisplayName(final String preselectedConfigurableDisplayName, Configurable[] configurables) {
    final List<Configurable> all = SearchUtil.expand(configurables);
    for (Configurable each : all) {
      if (preselectedConfigurableDisplayName.equals(each.getDisplayName())) return each;
    }
    return null;
  }

  @Override
  public void save(@Nonnull Configurable configurable) {
    myPropertiesComponent.setValue(LAST_SELECTED_CONFIGURABLE, configurable.getId());
  }
}
