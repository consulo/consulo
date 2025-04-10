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
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingElementPropertiesPanel;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author nik
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class PackagingElementType<E extends PackagingElement<?>> {
    public static final ExtensionPointName<PackagingElementType> EP_NAME = ExtensionPointName.create(PackagingElementType.class);
    private final String myId;
    private final String myPresentableName;

    protected PackagingElementType(@Nonnull String id, @Nonnull String presentableName) {
        myId = id;
        myPresentableName = presentableName;
    }

    public final String getId() {
        return myId;
    }

    public String getPresentableName() {
        return myPresentableName;
    }

    @Nonnull
    public abstract Image getIcon();

    public boolean isAvailableForAdd(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact) {
        return true;
    }

    @Nonnull
    public abstract List<? extends PackagingElement<?>> chooseAndCreate(
        @Nonnull ArtifactEditorContext context,
        @Nonnull Artifact artifact,
        @Nonnull CompositePackagingElement<?> parent
    );

    @Nonnull
    public abstract E createEmpty(@Nonnull Project project);

    protected static <T extends PackagingElementType<?>> T getInstance(Class<T> aClass) {
        for (PackagingElementType type : EP_NAME.getExtensionList()) {
            if (aClass.isInstance(type)) {
                return aClass.cast(type);
            }
        }
        throw new AssertionError();
    }

    @Nullable
    public PackagingElementPropertiesPanel createElementPropertiesPanel(@Nonnull E element, @Nonnull ArtifactEditorContext context) {
        return null;
    }
}
