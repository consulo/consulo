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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.compiler.artifact.Artifact;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author nik
 */
public class ArtifactConfigurable extends ArtifactConfigurableBase {
    private final Consumer<Artifact> myNameChanged;

    public ArtifactConfigurable(
        Artifact originalArtifact,
        ArtifactsStructureConfigurableContextImpl artifactsStructureContext,
        Consumer<Artifact> nameChanged
    ) {
        super(originalArtifact, artifactsStructureContext);
        myNameChanged = nameChanged;
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentUIDisposable) {
        return getEditor().createMainComponent(parentUIDisposable, () -> myNameChanged.accept(myOriginalArtifact));
    }

    @RequiredUIAccess
    @Override
    public void updateName() {
        getEditor().updateNameField();
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        return getEditor().isModified();
    }

    @Override
    @RequiredUIAccess
    public void apply() {
        getEditor().apply();
    }

    @Override
    public @Nullable String getHelpTopic() {
        return getEditor().getHelpTopic();
    }

    private ArtifactEditorImpl getEditor() {
        return myArtifactsStructureContext.getOrCreateEditor(myOriginalArtifact);
    }
}
