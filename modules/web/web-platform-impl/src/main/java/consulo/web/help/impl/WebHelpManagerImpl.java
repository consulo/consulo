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
package consulo.web.help.impl;

import com.intellij.openapi.help.HelpManager;
import org.jetbrains.annotations.NonNls;

import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 18-Oct-17
 */
@Singleton
public class WebHelpManagerImpl extends HelpManager {
  @Override
  public void invokeHelp(@javax.annotation.Nullable @NonNls String id) {
    // TODO [VISTALL] stub
  }
}
