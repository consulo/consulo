/*
 * Copyright 2013-2018 consulo.io
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
package consulo.options;

import consulo.ide.impl.idea.openapi.options.ConfigurableEP;
import consulo.configurable.UnnamedConfigurable;
import consulo.project.Project;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Property;
import consulo.util.xml.serializer.annotation.Tag;

import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2018-08-24
 */
@Tag("configurable")
public class ProjectConfigurableEP<T extends UnnamedConfigurable> extends ConfigurableEP<T> {
  private Project myProject;

  @Property(surroundWithTag = false)
  @AbstractCollection(surroundWithTag = false)
  public ProjectConfigurableEP[] children;

  /**
   * Marks project level configurables that do not apply to the default project.
   */
  @Attribute("nonDefaultProject")
  public boolean nonDefaultProject;

  @Override
  public ProjectConfigurableEP[] getChildren() {
    if (children == null) {
      return null;
    }

    for (ProjectConfigurableEP child : children) {
      child.myContainerOwner = myContainerOwner;
      child.myPluginDescriptor = myPluginDescriptor;
      child.myProject = myProject;
    }
    return children;
  }

  // used for children serialization
  private ProjectConfigurableEP() {
    super(null);
  }

  @Inject
  public ProjectConfigurableEP(Project project) {
    super(project);
    myProject = project;
  }

  @Override
  public boolean isAvailable() {
    return !nonDefaultProject || !(myProject != null && myProject.isDefault());
  }

  public Project getProject() {
    return myProject;
  }
}
