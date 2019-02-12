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
package com.intellij.execution.testframework;

import com.intellij.icons.AllIcons;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

public interface PoolOfTestIcons {
  Image SKIPPED_ICON = AllIcons.RunConfigurations.TestSkipped;
  Image PASSED_ICON = AllIcons.RunConfigurations.TestPassed;
  Image FAILED_ICON = AllIcons.RunConfigurations.TestFailed;
  Image ERROR_ICON = AllIcons.RunConfigurations.TestError;
  Image NOT_RAN = AllIcons.RunConfigurations.TestNotRan;
  Image LOADING_ICON = AllIcons.RunConfigurations.LoadingTree;
  Image TERMINATED_ICON = AllIcons.RunConfigurations.TestTerminated;
  Image IGNORED_ICON = AllIcons.RunConfigurations.TestIgnored;
  Image ERROR_ICON_MARK = AllIcons.Nodes.ErrorMark;
  Image TEAR_DOWN_FAILURE = ImageEffects.layered(PASSED_ICON, ERROR_ICON_MARK);
}
