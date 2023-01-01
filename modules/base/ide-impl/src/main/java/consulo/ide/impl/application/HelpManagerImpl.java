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
package consulo.ide.impl.application;

import consulo.annotation.component.ServiceImpl;
import consulo.application.HelpManager;
import consulo.ide.impl.externalService.impl.WebServiceApi;
import consulo.platform.Platform;
import consulo.util.lang.StringUtil;
import jakarta.inject.Singleton;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 04/01/2022
 */
@Singleton
@ServiceImpl
public class HelpManagerImpl implements HelpManager {
  @Override
  public void invokeHelp(@Nullable String id) {
    Platform.current().openInBrowser(WebServiceApi.HELP.buildUrl(StringUtil.notNullize(id)));
  }
}
