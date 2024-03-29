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

package consulo.execution.internal;

import consulo.project.ProjectPropertiesComponent;

public class RunManagerConfig {
  private final ProjectPropertiesComponent myPropertiesComponent;
  public static final int MIN_RECENT_LIMIT = 0;

  private static final String RECENTS_LIMIT = "recentsLimit";
  private static final String RESTART_REQUIRES_CONFIRMATION = "restartRequiresConfirmation";

  public RunManagerConfig(ProjectPropertiesComponent propertiesComponent) {
    myPropertiesComponent = propertiesComponent;
  }

  public int getRecentsLimit() {
    try {
      return Math.max(MIN_RECENT_LIMIT, Integer.valueOf(myPropertiesComponent.getValue(RECENTS_LIMIT, "5")).intValue());
    }
    catch (NumberFormatException e) {
      return 5;
    }
  }

  public void setRecentsLimit(int recentsLimit) {
    myPropertiesComponent.setValue(RECENTS_LIMIT, Integer.toString(recentsLimit));
  }

  public boolean isRestartRequiresConfirmation() {
    return myPropertiesComponent.getBoolean(RESTART_REQUIRES_CONFIRMATION, true);
  }

  public void setRestartRequiresConfirmation(boolean restartRequiresConfirmation) {
    myPropertiesComponent.setValue(RESTART_REQUIRES_CONFIRMATION, String.valueOf(restartRequiresConfirmation));
  }
}
