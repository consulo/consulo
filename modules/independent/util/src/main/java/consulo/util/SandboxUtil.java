/*
 * Copyright 2013-2016 consulo.io
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
package consulo.util;

import com.intellij.icons.AllIcons;
import consulo.annotations.DeprecationInfo;
import consulo.application.ApplicationProperties;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 15.09.14
 */
@Deprecated
@DeprecationInfo("Check method descriptors")
public class SandboxUtil {
  @NotNull
  @DeprecationInfo("Use Application.get().getIcon()")
  public static Icon getAppIcon() {
    return ApplicationProperties.isInSandbox() ? AllIcons.Icon16_Sandbox : AllIcons.Icon16;
  }

  @Deprecated
  @DeprecationInfo("Use ApplicationProperties#isInSandbox()")
  public static boolean isInsideSandbox() {
    return ApplicationProperties.isInSandbox();
  }
}
