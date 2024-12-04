// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.runtime.deployment;

import consulo.application.AllIcons;
import consulo.localize.LocalizeValue;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

public class DeploymentStatus {

    public static final DeploymentStatus DEPLOYED = new DeploymentStatus(AllIcons.RunConfigurations.TestPassed,
        RemoteServerLocalize.deploymentstatusDeployed(),
        false);

    public static final DeploymentStatus NOT_DEPLOYED = new DeploymentStatus(AllIcons.RunConfigurations.TestIgnored,
        RemoteServerLocalize.deploymentstatusNotDeployed(),
        false);

    public static final DeploymentStatus DEPLOYING = new DeploymentStatus(AllIcons.Process.Step_4,
        RemoteServerLocalize.deploymentstatusDeploying(),
        true);

    public static final DeploymentStatus UNDEPLOYING = new DeploymentStatus(AllIcons.Process.Step_4,
        RemoteServerLocalize.deploymentstatusUndeploying(),
        true);

    private final Image myIcon;
    private final LocalizeValue myPresentableText;
    private final boolean myTransition;

    public DeploymentStatus(Image icon, @Nonnull LocalizeValue presentableText, boolean transition) {
        myIcon = icon;
        myPresentableText = presentableText;
        myTransition = transition;
    }

    public Image getIcon() {
        return myIcon;
    }

    public final LocalizeValue getPresentableText() {
        return myPresentableText;
    }

    public boolean isTransition() {
        return myTransition;
    }
}
