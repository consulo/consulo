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
package consulo.start;

import com.intellij.openapi.components.ServiceManager;
import consulo.ui.RequiredUIAccess;
import consulo.ui.shared.Size;
import consulo.ui.Window;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 23-Sep-17
 */
public interface WelcomeFrameManager {
  @Nonnull
  static Size getDefaultWindowSize() {
    return new Size(777, 460);
  }

  final String DIMENSION_KEY = "WELCOME_SCREEN";

  @Nonnull
  @Deprecated
  static WelcomeFrameManager getInstance() {
    return ServiceManager.getService(WelcomeFrameManager.class);
  }

  @Nonnull
  @RequiredUIAccess
  Window createFrame();
}
