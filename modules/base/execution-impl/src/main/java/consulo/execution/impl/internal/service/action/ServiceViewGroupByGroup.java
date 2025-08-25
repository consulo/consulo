/*
 * Copyright 2013-2024 consulo.io
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
package consulo.execution.impl.internal.service.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.NonEmptyActionGroup;
import consulo.ui.image.Image;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-05-13
 */
@ActionImpl(
    id = "ServiceView.GroupBy",
    children = {
        @ActionRef(type = GroupByContributorAction.class),
        @ActionRef(type = GroupByServiceGroupsAction.class),
        @ActionRef(type = AnSeparator.class)
    }
)
public class ServiceViewGroupByGroup extends NonEmptyActionGroup {
    @Override
    public boolean isPopup() {
        return true;
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return PlatformIconGroup.actionsGroupby();
    }
}
