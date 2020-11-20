/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.impl;

import com.intellij.notification.Notification;
import com.intellij.ui.BalloonLayout;
import com.intellij.ui.BalloonLayoutData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 14-Oct-17
 */
public interface BalloonLayoutEx extends BalloonLayout {
  @Nullable
  Component getTopBalloonComponent();

  void addListener(Runnable listener);

  void removeListener(Runnable listener);

  void remove(@Nonnull Notification notification);

  @Nullable
  BalloonLayoutData.MergeInfo preMerge(@Nonnull Notification notification);
}
