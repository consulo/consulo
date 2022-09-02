/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.externalSystem.service.settings;

import consulo.externalSystem.ExternalSystemManager;
import consulo.ide.impl.idea.openapi.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.project.Project;
import consulo.util.lang.function.Condition;
import javax.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 5/11/13 5:44 PM
 */
public abstract class AbstractExternalSystemToolWindowCondition implements Condition<Project> {
  
  @Nonnull
  private final ProjectSystemId myExternalSystemId;

  protected AbstractExternalSystemToolWindowCondition(@Nonnull ProjectSystemId externalSystemId) {
    myExternalSystemId = externalSystemId;
  }

  @Override
  public boolean value(Project project) {
    if (project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT) == Boolean.TRUE) {
      return true;
    }
    ExternalSystemManager<?,?,?,?,?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
    if (manager == null) {
      return false;
    }
    AbstractExternalSystemSettings<?, ?,?> settings = manager.getSettingsProvider().apply(project);
    return settings != null && !settings.getLinkedProjectsSettings().isEmpty();
  }
}
