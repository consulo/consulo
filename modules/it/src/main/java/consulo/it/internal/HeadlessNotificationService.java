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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * Headless {@code NotificationService}: the production impls live in the AWT and web front ends, so nothing
 * bound this service in the headless application and every consumer received {@code null} instead - a
 * constructor annotated with {@code @Inject} is taken without a dependency check, so an unbound parameter is
 * injected as null rather than reported. {@code IdeStorageNotificationService} then failed with a
 * {@code NullPointerException} whenever a storage could not be read, hiding the actual read error.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessNotificationService implements NotificationService {
    private static final Logger LOG = Logger.getInstance(HeadlessNotificationService.class);

    @Override
    public void notify(Notification notification, @Nullable Project project) {
        LOG.info("notification [" + notification.getType() + "] " + notification.getTitle() + ": " + notification.getContent());
    }
}
