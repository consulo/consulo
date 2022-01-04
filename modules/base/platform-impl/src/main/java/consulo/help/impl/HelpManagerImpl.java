/*
 * Copyright 2013-2022 consulo.io
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
package consulo.help.impl;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.help.HelpManager;
import consulo.externalService.impl.WebServiceApi;
import consulo.util.lang.StringUtil;
import jakarta.inject.Singleton;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 04/01/2022
 */
@Singleton
public class HelpManagerImpl implements HelpManager {
  @Override
  public void invokeHelp(@Nullable String id) {
    BrowserUtil.browse(WebServiceApi.HELP.buildUrl(StringUtil.notNullize(id)));
  }
}
