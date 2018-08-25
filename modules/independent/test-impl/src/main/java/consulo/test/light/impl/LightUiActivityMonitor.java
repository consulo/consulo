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
package consulo.test.light.impl;

import com.intellij.ide.UiActivity;
import com.intellij.ide.UiActivityMonitor;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.BusyObject;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightUiActivityMonitor extends UiActivityMonitor {
  @Override
  public BusyObject getBusy(@Nonnull Project project, UiActivity... toWatch) {
    return null;
  }

  @Override
  public BusyObject getBusy(UiActivity... toWatch) {
    return null;
  }

  @Override
  public void addActivity(@Nonnull Project project, @Nonnull UiActivity activity) {

  }

  @Override
  public void addActivity(@Nonnull Project project, @Nonnull UiActivity activity, @Nonnull ModalityState effectiveModalityState) {

  }

  @Override
  public void addActivity(@Nonnull UiActivity activity) {

  }

  @Override
  public void addActivity(@Nonnull UiActivity activity, @Nonnull ModalityState effectiveModalityState) {

  }

  @Override
  public void removeActivity(@Nonnull Project project, @Nonnull UiActivity activity) {

  }

  @Override
  public void removeActivity(@Nonnull UiActivity activity) {

  }

  @Override
  public void clear() {

  }

  @Override
  public void setActive(boolean active) {

  }
}
