/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.application.impl;

import consulo.application.impl.internal.UnifiedApplication;
import consulo.application.impl.internal.start.StartupProgress;
import consulo.desktop.swt.ui.impl.DesktopSwtUIAccess;
import consulo.ui.UIAccess;
import consulo.util.lang.ref.SimpleReference;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtApplicationImpl extends UnifiedApplication {
  public DesktopSwtApplicationImpl(@Nonnull SimpleReference<? extends StartupProgress> splashRef) {
    super(splashRef);
  }

  @Nonnull
  @Override
  public UIAccess getLastUIAccess() {
    return DesktopSwtUIAccess.INSTANCE;
  }
}
