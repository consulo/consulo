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
package consulo.compiler.artifact.element;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingElementPropertiesPanel;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author nik
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class PackagingElementType<E extends PackagingElement<?>> {
    private final String myId;
    private final LocalizeValue myPresentableName;

    protected PackagingElementType(String id, LocalizeValue presentableName) {
        myId = id;
        myPresentableName = presentableName;
    }

    public final String getId() {
        return myId;
    }

    
    public LocalizeValue getPresentableName() {
        return myPresentableName;
    }

    
    public abstract Image getIcon();

    public boolean isAvailableForAdd(ArtifactEditorContext context, Artifact artifact) {
        return true;
    }

    
    public abstract List<? extends PackagingElement<?>> chooseAndCreate(
        ArtifactEditorContext context,
        Artifact artifact,
        CompositePackagingElement<?> parent
    );

    
    public abstract E createEmpty(Project project);

    protected static <T extends PackagingElementType<?>> T getInstance(Class<T> aClass) {
        return Application.get().getExtensionPoint(PackagingElementType.class).findExtensionOrFail(aClass);
    }

    @Nullable
    public PackagingElementPropertiesPanel createElementPropertiesPanel(E element, ArtifactEditorContext context) {
        return null;
    }
}
