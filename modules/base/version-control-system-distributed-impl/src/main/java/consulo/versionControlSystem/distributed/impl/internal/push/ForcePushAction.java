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
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageBoxRemember;
import consulo.ui.MessageButtonRole;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.distributed.push.PushActionBase;
import consulo.versionControlSystem.distributed.push.PushInfo;
import consulo.versionControlSystem.distributed.push.PushSource;
import consulo.versionControlSystem.distributed.push.PushSupport;
import consulo.versionControlSystem.distributed.push.PushTarget;
import consulo.versionControlSystem.distributed.push.VcsPushUi;
import consulo.versionControlSystem.distributed.repository.Repository;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ActionImpl(id = "Vcs.Push.Force")
public final class ForcePushAction extends PushActionBase {
    public ForcePushAction() {
        super(DistributedVcsLocalize.actionForcePush());
    }

    @RequiredUIAccess
    @Override
    protected void actionPerformed(Project project, VcsPushUi ui) {
        confirmForcePush(ui);
    }

    @RequiredUIAccess
    @Override
    protected boolean isEnabled(VcsPushUi ui) {
        return ui.canPush() && getProhibitedTarget(ui) == null;
    }

    @RequiredUIAccess
    @Override
    protected LocalizeValue getDescription(VcsPushUi ui, boolean enabled) {
        PushTarget prohibitedTarget = getProhibitedTarget(ui);
        LocalizeValue configurablePath = getProhibitedTargetConfigurablePath(ui);
        if (!enabled && prohibitedTarget != null) {
            LocalizeValue message = DistributedVcsLocalize.actionForcePushIsProhibitedDescription(prohibitedTarget.getPresentation());
            if (configurablePath.isNotEmpty()) {
                return LocalizeValue.join(message, LocalizeValue.of("\n"), DistributedVcsLocalize.actionForcePushIsProhibitedSettingsLink(configurablePath));
            }
            return message;
        }
        if (configurablePath.isNotEmpty()) {
            return DistributedVcsLocalize.actionForcePushIsProhibitedSettingsLink(configurablePath);
        }
        return LocalizeValue.empty();
    }

    @RequiredUIAccess
    private static void confirmForcePush(VcsPushUi ui) {
        Map<PushSupport<Repository, PushSource, PushTarget>, List<PushInfo>> silentForcePushIsNotAllowed = new LinkedHashMap<>();
        for (Map.Entry<PushSupport<Repository, PushSource, PushTarget>, Collection<PushInfo>> entry : ui.getSelectedPushSpecs().entrySet()) {
            List<PushInfo> infos = new ArrayList<>();
            for (PushInfo info : entry.getValue()) {
                if (!entry.getKey().isSilentForcePushAllowed(info.getPushSpec().getTarget())) {
                    infos.add(info);
                }
            }
            if (!infos.isEmpty()) {
                silentForcePushIsNotAllowed.put(entry.getKey(), infos);
            }
        }

        if (silentForcePushIsNotAllowed.isEmpty()) {
            ui.push(true);
            return;
        }

        PushTarget commonTarget = null;
        PushSupport<Repository, PushSource, PushTarget> support = null;
        if (silentForcePushIsNotAllowed.size() == 1) {
            support = silentForcePushIsNotAllowed.keySet().iterator().next();
            Set<PushTarget> targets = new LinkedHashSet<>();
            for (PushInfo info : silentForcePushIsNotAllowed.get(support)) {
                targets.add(info.getPushSpec().getTarget());
            }
            commonTarget = targets.size() == 1 ? targets.iterator().next() : null;
        }

        LocalizeValue message = commonTarget != null
            ? DistributedVcsLocalize.actionForcePushToConfirmationText(commonTarget.getPresentation())
            : DistributedVcsLocalize.actionForcePushConfirmationText();

        MessageBoxBuilder<Boolean> builder = MessageBoxBuilder.<Boolean>create()
            .asWarning()
            .title(DistributedVcsLocalize.forcePushDialogTitle())
            .text(message)
            .button(MessageButtonRole.YES, DistributedVcsLocalize.actionForcePush(), Boolean.TRUE)
            .asDefaultButton()
            .button(MessageButtonRole.CANCEL, CommonLocalize.buttonCancel(), Boolean.FALSE)
            .asExitButton();
        if (commonTarget != null) {
            builder.remember(new DoNotWarnAboutTarget(support, commonTarget));
        }

        builder.showAsync().whenComplete((confirmed, error) -> {
            if (Boolean.TRUE.equals(confirmed)) {
                ui.push(true);
            }
        });
    }

    static @Nullable PushTarget getProhibitedTarget(VcsPushUi ui) {
        for (Map.Entry<PushSupport<Repository, PushSource, PushTarget>, Collection<PushInfo>> entry : ui.getSelectedPushSpecs().entrySet()) {
            for (PushInfo info : entry.getValue()) {
                if (!entry.getKey().isForcePushAllowed(info.getRepository(), info.getPushSpec().getTarget())) {
                    return info.getPushSpec().getTarget();
                }
            }
        }
        return null;
    }

    static LocalizeValue getProhibitedTargetConfigurablePath(VcsPushUi ui) {
        Map<PushSupport<Repository, PushSource, PushTarget>, Collection<PushInfo>> specs = ui.getSelectedPushSpecs();
        for (Map.Entry<PushSupport<Repository, PushSource, PushTarget>, Collection<PushInfo>> entry : specs.entrySet()) {
            for (PushInfo info : entry.getValue()) {
                if (!entry.getKey().isForcePushAllowed(info.getRepository(), info.getPushSpec().getTarget())) {
                    return entry.getKey().getForcePushConfigurablePath();
                }
            }
        }
        for (PushSupport<Repository, PushSource, PushTarget> support : specs.keySet()) {
            return support.getForcePushConfigurablePath();
        }
        return LocalizeValue.empty();
    }

    private record DoNotWarnAboutTarget(PushSupport<Repository, PushSource, PushTarget> support, PushTarget target)
        implements MessageBoxRemember<Boolean> {
        @Override
        public void setValue(Boolean value) {
            if (Boolean.TRUE.equals(value)) {
                support.saveSilentForcePushTarget(target);
            }
        }

        @Override
        public @Nullable Boolean getValue() {
            return null;
        }

        @Override
        public LocalizeValue getMessageText() {
            return DistributedVcsLocalize.actionForcePushDonTWarnAboutThisTarget();
        }
    }
}
