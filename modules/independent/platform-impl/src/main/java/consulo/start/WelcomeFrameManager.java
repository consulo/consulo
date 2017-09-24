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
import consulo.ui.Size;
import consulo.ui.Window;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 23-Sep-17
 *
 * TODO [VISTALL] migrate desktop frame to this class
 */
public interface WelcomeFrameManager {
  @NotNull
  public static Size getDefaultWindowSize() {
    return new Size(777, 460);
  }

  @NotNull
  static WelcomeFrameManager getInstance() {
    return ServiceManager.getService(WelcomeFrameManager.class);
  }

  @NotNull
  @RequiredUIAccess
  Window openFrame();
}
