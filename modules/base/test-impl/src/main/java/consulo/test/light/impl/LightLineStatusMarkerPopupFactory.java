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
package consulo.test.light.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.versionControlSystem.internal.LineStatusMarkerPopup;
import consulo.versionControlSystem.internal.LineStatusMarkerPopupFactory;
import consulo.versionControlSystem.internal.LineStatusTrackerI;
import consulo.versionControlSystem.internal.VcsRange;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-11
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.LIGHT_TEST)
public class LightLineStatusMarkerPopupFactory implements LineStatusMarkerPopupFactory {
    @Override
    public LineStatusMarkerPopup create(LineStatusTrackerI tracker, Editor editor, VcsRange range) {
        return new LineStatusMarkerPopup() {
            @Override
            @RequiredUIAccess
            public void showHintAt(@Nullable InputDetails details) {
            }

            @Override
            @RequiredUIAccess
            public void scrollAndShow() {
            }

            @Override
            @RequiredUIAccess
            public void showAfterScroll() {
            }
        };
    }
}
