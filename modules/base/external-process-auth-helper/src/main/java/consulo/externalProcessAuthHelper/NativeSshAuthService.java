// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.externalProcessAuthHelper;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import externalApp.nativessh.NativeSshAskPassApp;
import externalApp.nativessh.NativeSshAskPassAppHandler;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class NativeSshAuthService extends ExternalProcessHandlerService<NativeSshAskPassAppHandler> {
    public static NativeSshAuthService getInstance() {
        return Application.get().getInstance(NativeSshAuthService.class);
    }

    public NativeSshAuthService() {
        super("intellij-ssh-askpass", NativeSshAskPassApp.class);
    }

    @Override
    protected @Nullable String handleRequest(NativeSshAskPassAppHandler handler, String requestBody) {
        return handler.handleInput(requestBody);
    }
}
