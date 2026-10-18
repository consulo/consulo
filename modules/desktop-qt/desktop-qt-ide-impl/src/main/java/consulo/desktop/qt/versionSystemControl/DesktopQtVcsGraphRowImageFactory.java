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
package consulo.desktop.qt.versionSystemControl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.ui.image.Image;
import consulo.versionControlSystem.log.graph.paint.VcsGraphRow;
import consulo.versionControlSystem.log.graph.paint.VcsGraphRowImageFactory;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
@Singleton
public class DesktopQtVcsGraphRowImageFactory implements VcsGraphRowImageFactory {
    @Override
    public Image createImage(VcsGraphRow row) {
        return new DesktopQtVcsGraphRowImage(row);
    }
}
