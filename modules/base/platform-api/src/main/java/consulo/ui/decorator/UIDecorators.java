/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.decorator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import consulo.container.plugin.PluginManager;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-11-03
 */
public class UIDecorators {
  private static final ExtensionPointName<UIDecorator> EP_NAME = ExtensionPointName.create("com.intellij.uiDecorator");

  @Nonnull
  public static List<UIDecorator> getDecorators() {
    if (PluginManager.isInitialized()) {
      if (ApplicationManager.getApplication() == null) {
        throw new IllegalArgumentException("Application is not initialized");
      }

      return EP_NAME.getExtensionList();
    }
    else {
      return Collections.emptyList();
    }
  }
}
