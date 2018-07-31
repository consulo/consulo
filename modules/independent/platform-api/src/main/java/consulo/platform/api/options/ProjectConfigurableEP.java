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
package consulo.platform.api.options;

import com.intellij.openapi.options.ConfigurableEP;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.Tag;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * @author VISTALL
 * @since 2018-07-31
 */
@Tag("configurable")
public class ProjectConfigurableEP<T extends UnnamedConfigurable> extends ConfigurableEP<T> {
  @Inject
  public ProjectConfigurableEP(@Nonnull Project project) {
    super(project, project);
  }
}
