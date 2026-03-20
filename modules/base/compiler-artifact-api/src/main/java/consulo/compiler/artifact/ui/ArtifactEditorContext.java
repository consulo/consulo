/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler.artifact.ui;

import consulo.annotation.DeprecationInfo;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.ModifiableArtifactModel;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElementResolvingContext;
import consulo.content.library.Library;
import consulo.localize.LocalizeValue;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.content.layer.ModifiableRootModel;

import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author nik
 */
public interface ArtifactEditorContext extends PackagingElementResolvingContext {
    void queueValidation();

    
    ArtifactType getArtifactType();

    
    ModifiableArtifactModel getOrCreateModifiableArtifactModel();

    @Nullable ModifiableModuleModel getModifiableModuleModel();

    
    ModifiableRootModel getOrCreateModifiableRootModel(Module module);

    CompositePackagingElement<?> getRootElement(Artifact artifact);

    void editLayout(Artifact artifact, Runnable runnable);

    ArtifactEditor getOrCreateEditor(Artifact originalArtifact);

    ArtifactEditor getThisArtifactEditor();

    void selectArtifact(Artifact artifact);

    void selectModule(Module module);

    void selectLibrary(Library library);

    @RequiredUIAccess
    List<Artifact> chooseArtifacts(List<? extends Artifact> artifacts, LocalizeValue title);

    @RequiredUIAccess
    List<Module> chooseModules(List<Module> modules, LocalizeValue title);

    @RequiredUIAccess
    List<Library> chooseLibraries(LocalizeValue title);

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @RequiredUIAccess
    default List<Artifact> chooseArtifacts(List<? extends Artifact> artifacts, String title) {
        return chooseArtifacts(artifacts, LocalizeValue.ofNullable(title));
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @RequiredUIAccess
    default List<Module> chooseModules(List<Module> modules, String title) {
        return chooseModules(modules, LocalizeValue.ofNullable(title));
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @RequiredUIAccess
    default List<Library> chooseLibraries(String title) {
        return chooseLibraries(LocalizeValue.ofNullable(title));
    }

    Artifact getArtifact();
}
