/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.compiler.artifact.internal.InvalidArtifact;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;

/**
 * @author nik
 */
public class InvalidArtifactConfigurable extends ArtifactConfigurableBase {
    private final String myErrorMessage;

    public InvalidArtifactConfigurable(InvalidArtifact originalArtifact, ArtifactsStructureConfigurableContextImpl artifactsStructureContext) {
        super(originalArtifact, artifactsStructureContext);
        myErrorMessage = originalArtifact.getErrorMessage();
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentUIDisposable) {
        Label errorLabel = Label.create(LocalizeValue.of(myErrorMessage));
        errorLabel.setImage(PlatformIconGroup.generalError());

        DockLayout panel = DockLayout.create();
        panel.top(errorLabel);
        return panel;
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public void apply() {
    }
}
