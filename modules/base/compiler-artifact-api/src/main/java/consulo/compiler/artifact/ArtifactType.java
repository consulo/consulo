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

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.compiler.artifact.element.*;
import consulo.compiler.artifact.ui.ArtifactProblemsHolder;
import consulo.compiler.artifact.ui.PackagingSourceItem;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModulesProvider;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author nik
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ArtifactType {
    private static final ExtensionPointCacheKey<ArtifactType, Map<String, ArtifactType>> GROUP =
        ExtensionPointCacheKey.groupBy("GroupArtifactType", ArtifactType::getId);

    @Nullable
    public static ArtifactType findById(String id) {
        Map<String, ArtifactType> map = Application.get().getExtensionPoint(ArtifactType.class).getOrBuildCache(GROUP);
        return map.get(id);
    }

    private final String myId;
    
    private final LocalizeValue myTitle;

    protected ArtifactType(String id, LocalizeValue title) {
        myId = id;
        myTitle = title;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    protected ArtifactType(String id, String title) {
        myId = id;
        myTitle = LocalizeValue.ofNullable(title);
    }

    public final String getId() {
        return myId;
    }

    
    public LocalizeValue getPresentableName() {
        return myTitle;
    }

    
    public abstract Image getIcon();

    @Nullable
    public String getDefaultPathFor(PackagingSourceItem sourceItem) {
        return getDefaultPathFor(sourceItem.getKindOfProducedElements());
    }

    @Nullable
    public abstract String getDefaultPathFor(PackagingElementOutputKind kind);

    public boolean isSuitableItem(PackagingSourceItem sourceItem) {
        return true;
    }

    public boolean isAvailableForAdd(ModulesProvider modulesProvider) {
        return true;
    }

    
    public abstract CompositePackagingElement<?> createRootElement(
        PackagingElementFactory packagingElementFactory,
        String artifactName
    );

    
    public List<? extends ArtifactTemplate> getNewArtifactTemplates(PackagingElementResolvingContext context) {
        return Collections.emptyList();
    }

    public void checkRootElement(
        CompositePackagingElement<?> rootElement,
        Artifact artifact,
        ArtifactProblemsHolder manager
    ) {
    }

    @Nullable
    public List<? extends PackagingElement<?>> getSubstitution(
        Artifact artifact,
        PackagingElementResolvingContext context,
        ArtifactType parentType
    ) {
        return null;
    }
}
