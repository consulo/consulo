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
package consulo.web.internal.credential;

import consulo.annotation.component.ExtensionImpl;
import consulo.container.boot.ContainerPathManager;
import consulo.credentialStorage.CredentialStore;
import consulo.credentialStorage.internal.CredentialStoreFactory;
import consulo.platform.Platform;
import jakarta.inject.Inject;

import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2026-10-13
 */
@ExtensionImpl(order = "first")
public class WebBrowserCredentialStoreFactory implements CredentialStoreFactory {
    private static final String FILE_NAME = "browser-credentials.properties";

    private final WebBrowserCrypto myCrypto;

    @Inject
    public WebBrowserCredentialStoreFactory(WebBrowserCrypto crypto) {
        myCrypto = crypto;
    }

    @Override
    public CredentialStore create(Platform platform) {
        return new WebBrowserCredentialStore(myCrypto, Path.of(ContainerPathManager.get().getConfigPath()).resolve(FILE_NAME));
    }
}
