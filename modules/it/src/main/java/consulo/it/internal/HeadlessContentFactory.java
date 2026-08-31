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
import consulo.component.ComponentManager;
import consulo.it.internal.ui.HeadlessContent;
import consulo.it.internal.ui.HeadlessContentManager;
import consulo.ui.Component;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.ContentUI;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * Headless {@code ContentFactory}: the production impls live in {@code ide-impl}
 * ({@code UnifiedContentFactoryImpl}) and {@code desktop-awt-ide-impl}. Needed at project startup
 * because {@code UnifiedToolWindowImpl} creates a {@link ContentManager} for every registered
 * tool window. Bound only under the {@link ComponentProfiles#INTEGRATION_TEST} profile.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessContentFactory implements ContentFactory {
    @Override
    public ContentManager createContentManager(ContentUI contentUI, boolean canCloseContents, ComponentManager project) {
        return new HeadlessContentManager(contentUI, canCloseContents);
    }

    @Override
    public ContentManager createContentManager(boolean canCloseContents, ComponentManager project) {
        return new HeadlessContentManager(null, canCloseContents);
    }

    @Override
    public Content createUIContent(@Nullable Component component, String displayName, boolean isLockable) {
        return new HeadlessContent(component, displayName, isLockable);
    }
}
