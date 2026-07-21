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

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Headless {@link WelcomeFrameManager}: the harness never shows a welcome frame, so {@code createFrame}
 * returns a headless frame only to satisfy the contract; {@code closeFrame} short-circuits on a null
 * current frame.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl
public class HeadlessWelcomeFrameManager extends WelcomeFrameManager {
    @Inject
    public HeadlessWelcomeFrameManager(Application application) {
        super(application);
    }

    @RequiredUIAccess
    @Override
    protected IdeFrame createFrame() {
        return new HeadlessIdeFrame(null);
    }
}
