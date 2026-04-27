/*
 * Copyright 2013-2026 consulo.io
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
package consulo.http.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.component.ComponentManager;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.http.HttpProxySettingService;
import consulo.http.impl.internal.proxy.HttpProxyConfigurable;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-04-27
 */
@Singleton
@ServiceImpl
public class HttpProxySettingServiceImpl implements HttpProxySettingService {
    private final ShowConfigurableService myShowConfigurableService;

    @Inject
    public HttpProxySettingServiceImpl(ShowConfigurableService showConfigurableService) {
        myShowConfigurableService = showConfigurableService;
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<?> showSettings(ComponentManager project) {
        return myShowConfigurableService.show((Project) project, HttpProxyConfigurable.class);
    }
}
