/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.configurable;

import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.contentAnnotation.VcsContentAnnotationSettings;
import consulo.versionControlSystem.impl.internal.contentAnnotation.VcsContentAnnotationSettingsState;
import consulo.versionControlSystem.localize.VcsLocalize;

/**
 * @author Irina.Chernushina
 * @since 2011-08-04
 */
public class VcsContentAnnotationConfigurable extends SimpleConfigurableByProperties {
    private final Project myProject;

    public VcsContentAnnotationConfigurable(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        VcsContentAnnotationSettings settings = VcsContentAnnotationSettings.getInstance(myProject);

        CheckBox showBox = CheckBox.create(VcsLocalize.settingsShowChangedInLast(), settings.isShow());
        propertyBuilder.add(showBox, settings::isShow, settings::setShow);

        IntBox daysBox = IntBox.create(settings.getLimitDays()).withRange(1, VcsContentAnnotationSettingsState.ourMaxDays);
        propertyBuilder.add(daysBox, settings::getLimitDays, settings::setLimit);

        return VcsSettingsRows.gated(showBox, daysBox, VcsLocalize.settingsDays());
    }
}
