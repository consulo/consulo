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
package consulo.compiler.artifact;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.compiler.artifact.element.ArtifactRootElementImpl;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.element.PackagingElementOutputKind;
import consulo.compiler.localize.CompilerLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
@ExtensionImpl(order = "last")
public class PlainArtifactType extends ArtifactType {
    public static final String ID = "plain";

    public static PlainArtifactType getInstance() {
        return Application.get().getExtensionPoint(ArtifactType.class).findExtension(PlainArtifactType.class);
    }

    public PlainArtifactType() {
        super(ID, CompilerLocalize.artifactTypePlain());
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesArtifact();
    }

    @Override
    public String getDefaultPathFor(@Nonnull PackagingElementOutputKind kind) {
        return "/";
    }

    @Override
    @Nonnull
    public CompositePackagingElement<?> createRootElement(@Nonnull PackagingElementFactory elementFactory, @Nonnull String artifactName) {
        return new ArtifactRootElementImpl();
    }
}
