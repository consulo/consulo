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
package consulo.ide.impl.usage;

import consulo.annotation.component.ServiceImpl;
import consulo.find.FindModel;
import consulo.ide.impl.idea.find.impl.FindInProjectUtil;
import consulo.project.Project;
import consulo.usage.UsagePreviewPanel;
import consulo.usage.UsagePreviewPanelFactory;
import consulo.usage.UsageViewPresentation;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 20-Apr-22
 */
@Singleton
@ServiceImpl
public class UsagePreviewPanelFactoryImpl implements UsagePreviewPanelFactory {
    @Nonnull
    @Override
    public UsagePreviewPanel createPreviewPanel(@Nonnull Project project, @Nonnull UsageViewPresentation presentation, boolean isEditor) {
        return new consulo.ide.impl.idea.usages.impl.UsagePreviewPanel(project, presentation, isEditor);
    }

    @Nonnull
    @Override
    public UsageViewPresentation createEmpUsageViewPresentation() {
        return FindInProjectUtil.setupViewPresentation(false, new FindModel());
    }
}
