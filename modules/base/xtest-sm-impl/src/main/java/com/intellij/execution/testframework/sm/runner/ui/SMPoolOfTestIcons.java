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
package com.intellij.execution.testframework.sm.runner.ui;

import com.intellij.execution.testframework.PoolOfTestIcons;
import com.intellij.execution.testframework.ui.TestsProgressAnimator;
import com.intellij.icons.AllIcons;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;

/**
 * @author Roman.Chernyatchik
 */
public class SMPoolOfTestIcons implements PoolOfTestIcons {
  // Error flag icon

  public static final Image SKIPPED_E_ICON = addErrorMarkTo(SKIPPED_ICON);
  public static final Image PASSED_E_ICON = addErrorMarkTo(PASSED_ICON);
  public static final Image FAILED_E_ICON = addErrorMarkTo(FAILED_ICON);
  public static final Image TERMINATED_E_ICON = addErrorMarkTo(TERMINATED_ICON);
  public static final Image IGNORED_E_ICON = addErrorMarkTo(IGNORED_ICON);

  // Test Progress
  public static final Image PAUSED_E_ICON = addErrorMarkTo(AllIcons.RunConfigurations.TestPaused);
  public static final Image[] FRAMES_E = new Image[TestsProgressAnimator.FRAMES.length];

  static {
    for (int i = 0, length = FRAMES_E.length; i < length; i++) {
      FRAMES_E[i] = addErrorMarkTo(TestsProgressAnimator.FRAMES[i]);
    }
  }

  @Nonnull
  public static Image addErrorMarkTo(@Nonnull Image baseIcon) {
    return ImageEffects.layered(baseIcon, ERROR_ICON_MARK);
  }
}
