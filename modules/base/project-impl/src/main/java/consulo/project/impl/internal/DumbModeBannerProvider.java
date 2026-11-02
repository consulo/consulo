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
package consulo.project.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.DumbService;
import consulo.project.localize.ProjectLocalize;
import consulo.project.ui.internal.ProjectBannerProvider;
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;

/**
 * Tells why code insight, navigation and search fall short while the project is indexed.
 *
 * @author VISTALL
 * @since 2026-11-02
 */
@ExtensionImpl
public class DumbModeBannerProvider implements ProjectBannerProvider {
    public static final String ID = "banner.dumbMode";

    private final DumbService myDumbService;

    @Inject
    public DumbModeBannerProvider(DumbService dumbService) {
        myDumbService = dumbService;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isVisible() {
        return myDumbService.isDumb();
    }

    @RequiredUIAccess
    @Override
    public void customize(Toast toast) {
        toast.addStyle(ToastStyle.INFO);
        toast.setTitle(ProjectLocalize.dumbModeBannerTitle());
        toast.setText(ProjectLocalize.dumbModeBannerText());
    }
}
