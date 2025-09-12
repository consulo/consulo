/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.configurable.UnifiedShowSettingsUtil;
import consulo.ide.internal.IdeInternal;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.internal.DefaultProjectFactory;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2025-09-12
 */
@Singleton
@ServiceImpl
public class IdeInternalImpl implements IdeInternal {
    @Override
    public ShowSettingsUtil createUnifiedSettingsUtil() {
        return new UnifiedShowSettingsUtil(DefaultProjectFactory.getInstance());
    }
}
