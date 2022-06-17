/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ui.ex;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.component.util.BusyObject;
import consulo.ui.ModalityState;

import javax.annotation.Nonnull;

@Service(ComponentScope.APPLICATION)
public abstract class UiActivityMonitor {

  public abstract BusyObject getBusy(@Nonnull ComponentManager project, UiActivity... toWatch);

  public abstract BusyObject getBusy(UiActivity ... toWatch);

  public abstract void addActivity(@Nonnull ComponentManager project, @Nonnull UiActivity activity);

  public abstract void addActivity(@Nonnull ComponentManager project, @Nonnull UiActivity activity, @Nonnull ModalityState effectiveModalityState);

  public abstract void addActivity(@Nonnull UiActivity activity);

  public abstract void addActivity(@Nonnull UiActivity activity, @Nonnull ModalityState effectiveModalityState);

  public abstract void removeActivity(@Nonnull ComponentManager project, @Nonnull UiActivity activity);

  public abstract void removeActivity(@Nonnull UiActivity activity);

  public abstract void clear();

  public abstract void setActive(boolean active);

  public static UiActivityMonitor getInstance() {
    return Application.get().getInstance(UiActivityMonitor.class);
  }
}
