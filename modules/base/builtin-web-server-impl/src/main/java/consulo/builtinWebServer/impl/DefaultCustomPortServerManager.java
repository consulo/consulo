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
package consulo.builtinWebServer.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.builtinWebServer.custom.CustomPortServerManagerBase;
import consulo.builtinWebServer.localize.BuiltInServerLocalize;
import consulo.project.ui.notification.NotificationService;
import jakarta.inject.Inject;

@ExtensionImpl
public final class DefaultCustomPortServerManager extends CustomPortServerManagerBase {
    private final Application myApplication;
    private final NotificationService myNotificationService;

    @Inject
    public DefaultCustomPortServerManager(Application application, NotificationService notificationService) {
        myApplication = application;
        myNotificationService = notificationService;
    }

    @Override
    public void cannotBind(Exception e, int port) {
        myNotificationService.newError(BuiltInServerManagerImpl.NOTIFICATION_GROUP)
            .content(BuiltInServerLocalize.notificationContentCannotStartBuiltInHttpServerOnCustomPort(port, myApplication.getName()))
            .notify(null);
    }

    @Override
    public int getPort() {
        int port = BuiltInServerOptions.getInstance().getBuiltInServerPort();
        return port == BuiltInServerOptions.DEFAULT_PORT ? -1 : port;
    }

    @Override
    public boolean isAvailableExternally() {
        return BuiltInServerOptions.getInstance().isBuiltInServerAvailableExternally();
    }
}
