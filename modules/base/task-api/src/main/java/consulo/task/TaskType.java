/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

package consulo.task;

import consulo.application.AllIcons;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

/**
 * @author Dmitry Avdeev
 */
public enum TaskType {
  BUG,
  EXCEPTION,
  FEATURE,
  OTHER;

  public Image getIcon(boolean issue) {
    switch (this) {
      case BUG:
        return TasksIcons.Bug;
      case EXCEPTION:
        return TasksIcons.Exception;
      case FEATURE:
        return PlatformIconGroup.nodesFavorite();
      default:
      case OTHER:
        return issue ? AllIcons.FileTypes.Any_type : AllIcons.FileTypes.Unknown;
    }
  }
}
