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
package consulo.desktop.qt.platform;

import consulo.desktop.qt.ui.impl.DesktopQtUIAccess;
import consulo.platform.PlatformUser;
import consulo.platform.impl.PlatformBase;
import consulo.ui.UIAccess;
import io.qt.core.QUrl;
import io.qt.gui.QDesktopServices;

import java.io.File;
import java.net.URL;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtPlatformImpl extends PlatformBase {
    public DesktopQtPlatformImpl() {
        super(LOCAL, LOCAL, getSystemJvmProperties());
    }

    @Override
    protected PlatformUser createUser(Map<String, String> jvmProperties) {
        return new DesktopQtPlatformUserImpl(this, jvmProperties);
    }

    @Override
    public void openInBrowser(URL url) {
        DesktopQtUIAccess.INSTANCE.give(() -> QDesktopServices.openUrl(new QUrl(url.toString())));
    }

    @Override
    public void openFileInFileManager(File file, UIAccess uiAccess) {
        openDirectoryInFileManager(file.getParentFile(), uiAccess);
    }

    @Override
    public void openDirectoryInFileManager(File file, UIAccess uiAccess) {
        DesktopQtUIAccess.INSTANCE.give(() -> QDesktopServices.openUrl(QUrl.fromLocalFile(file.getAbsolutePath())));
    }
}
