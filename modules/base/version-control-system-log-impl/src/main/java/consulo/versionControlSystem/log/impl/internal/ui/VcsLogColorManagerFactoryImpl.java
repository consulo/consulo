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
package consulo.versionControlSystem.log.impl.internal.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.versionControlSystem.log.ui.VcsLogColorManager;
import consulo.versionControlSystem.log.ui.VcsLogColorManagerFactory;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import java.util.Collection;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
@ServiceImpl
@Singleton
public class VcsLogColorManagerFactoryImpl implements VcsLogColorManagerFactory {
    @Override
    public VcsLogColorManager create(Collection<VirtualFile> roots) {
        return new VcsLogColorManagerImpl(roots);
    }
}
