/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.desktop.internal.taskBar;

import consulo.annotation.ReviewAfterMigrationToJRE;
import consulo.ui.TaskBar;
import consulo.ui.Window;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2020-10-06
 */
@ReviewAfterMigrationToJRE(0)
public class Java9TaskBarImpl implements TaskBar {
  // TODO [VISTALL] impl via java.desktop API. but not all methods are supported

  private TaskbarWrapper myTaskbarWrapper;
  private Desktop myDesktop;

  public Java9TaskBarImpl() {
    myTaskbarWrapper = TaskbarWrapper.getTaskbar();
  }

  @Override
  public boolean setProgress(@Nonnull Window window, Object processId, ProgressScheme scheme, double value, boolean isOk) {
    if (myTaskbarWrapper.isSupported(TaskbarWrapper.FeatureWrapper.PROGRESS_VALUE)) {
      myTaskbarWrapper.setProgressValue((int)(value * 100.));
    }
    return true;
  }

  @Override
  public boolean hideProgress(@Nonnull Window window, Object processId) {
    if (myTaskbarWrapper.isSupported(TaskbarWrapper.FeatureWrapper.PROGRESS_VALUE)) {
      myTaskbarWrapper.setProgressValue(-1);
    }
    return true;
  }

  @Override
  public void requestAttention(@Nonnull Window window, boolean critical) {
    if (myTaskbarWrapper.isSupported(TaskbarWrapper.FeatureWrapper.USER_ATTENTION)) {
      myTaskbarWrapper.requestUserAttention(true, critical);
    }
  }
}
