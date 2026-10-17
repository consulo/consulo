/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.impl.internal.push;

import consulo.annotation.component.ActionImpl;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.distributed.push.PushActionBase;
import consulo.versionControlSystem.distributed.push.VcsPushUi;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

@ActionImpl(id = "Vcs.Push.Simple")
public final class SimplePushAction extends PushActionBase {
    private @Nullable Predicate<? super VcsPushUi> myCondition;

    public SimplePushAction() {
        super(DistributedVcsLocalize.actionComplexPush());
    }

    @RequiredUIAccess
    @Override
    protected boolean isEnabled(VcsPushUi dialog) {
        return dialog.canPush();
    }

    @RequiredUIAccess
    @Override
    protected LocalizeValue getText(VcsPushUi dialog, boolean enabled) {
        if (dialog.hasWarnings()) {
            return DistributedVcsLocalize.actionPushAnyway();
        }
        return super.getText(dialog, enabled);
    }

    @RequiredUIAccess
    @Override
    protected LocalizeValue getDescription(VcsPushUi dialog, boolean enabled) {
        return LocalizeValue.empty();
    }

    @RequiredUIAccess
    @Override
    protected void actionPerformed(Project project, VcsPushUi dialog) {
        if (myCondition == null || myCondition.test(dialog)) {
            dialog.push(false);
        }
    }

    void setCondition(@Nullable Predicate<? super VcsPushUi> condition) {
        myCondition = condition;
    }
}
