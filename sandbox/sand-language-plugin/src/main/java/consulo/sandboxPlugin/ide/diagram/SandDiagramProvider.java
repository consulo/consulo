/*
 * Copyright 2013-2026 consulo.io
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
package consulo.sandboxPlugin.ide.diagram;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.util.ModificationTracker;
import consulo.diagram.AbstractDiagramNodeContentManager;
import consulo.diagram.AbstractDiagramVisibilityManager;
import consulo.diagram.DiagramCategory;
import consulo.diagram.DiagramDataModel;
import consulo.diagram.DiagramDeleteProvider;
import consulo.diagram.DiagramEdgeCreationPolicy;
import consulo.diagram.DiagramElementManager;
import consulo.diagram.DiagramExtras;
import consulo.diagram.DiagramNode;
import consulo.diagram.DiagramNodeContentManager;
import consulo.diagram.DiagramProvider;
import consulo.diagram.DiagramRelationshipInfo;
import consulo.diagram.DiagramRelationshipManager;
import consulo.diagram.DiagramVfsResolver;
import consulo.diagram.DiagramVisibilityManager;
import consulo.diagram.VisibilityLevel;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
@ExtensionImpl
public class SandDiagramProvider extends DiagramProvider<String> {
    private final SandDiagramElementManager myElementManager = new SandDiagramElementManager();
    private final SandDiagramVfsResolver myVfsResolver = new SandDiagramVfsResolver();
    private final SandDiagramDeleteProvider myDeleteProvider = new SandDiagramDeleteProvider();
    private final SandDiagramExtras myExtras = new SandDiagramExtras();

    private final DiagramEdgeCreationPolicy<String> myEdgeCreationPolicy = new DiagramEdgeCreationPolicy<>() {
        @Override
        public boolean acceptSource(DiagramNode<String> source) {
            return true;
        }

        @Override
        public boolean acceptTarget(DiagramNode<String> target) {
            return true;
        }
    };

    private final DiagramVisibilityManager myVisibilityManager = new AbstractDiagramVisibilityManager() {
        @Override
        public VisibilityLevel[] getVisibilityLevels() {
            return VisibilityLevel.EMPTY_ARRAY;
        }

        @Override
        public @Nullable VisibilityLevel getVisibilityLevel(Object element) {
            return null;
        }

        @Override
        public Comparator<VisibilityLevel> getComparator() {
            return VisibilityLevel.DUMMY_COMPARATOR;
        }
    };

    private final AbstractDiagramNodeContentManager myNodeContentManager = new AbstractDiagramNodeContentManager() {
        @Override
        public boolean isInCategory(Object element, DiagramCategory category) {
            return element instanceof SandDiagramMember member && member.category().equals(category);
        }

        @Override
        public DiagramCategory[] getContentCategories() {
            return new DiagramCategory[]{SandDiagramMember.FIELDS, SandDiagramMember.METHODS};
        }
    };

    private final DiagramRelationshipManager<String> myRelationshipManager = new DiagramRelationshipManager<>() {
        @Override
        public @Nullable DiagramRelationshipInfo getDependencyInfo(String e1, String e2, DiagramCategory category) {
            return null;
        }

        @Override
        public DiagramCategory[] getContentCategories() {
            return DiagramCategory.EMPTY_ARRAY;
        }
    };

    public SandDiagramProvider() {
        myElementManager.setDiagramProvider(this);
        myNodeContentManager.setEnabled(SandDiagramMember.FIELDS, true);
        myNodeContentManager.setEnabled(SandDiagramMember.METHODS, true);
    }

    @Override
    public String getID() {
        return "sand";
    }

    @Override
    public DiagramVisibilityManager getVisibilityManager() {
        return myVisibilityManager;
    }

    @Override
    public DiagramNodeContentManager getNodeContentManager() {
        return myNodeContentManager;
    }

    @Override
    public DiagramElementManager<String> getElementManager() {
        return myElementManager;
    }

    @Override
    public DiagramVfsResolver<String> getVfsResolver() {
        return myVfsResolver;
    }

    @Override
    public DiagramRelationshipManager<String> getRelationshipManager() {
        return myRelationshipManager;
    }

    @Override
    public DiagramDataModel<String> createDataModel(Project project, @Nullable String element, @Nullable VirtualFile file) {
        return new SandDiagramDataModel(this, myDeleteProvider);
    }

    @Override
    public DiagramExtras<String> getExtras() {
        return myExtras;
    }

    @Override
    public DiagramEdgeCreationPolicy<String> getEdgeCreationPolicy() {
        return myEdgeCreationPolicy;
    }

    @Override
    public DiagramDeleteProvider<String> getDeleteProvider() {
        return myDeleteProvider;
    }

    @Override
    public ModificationTracker getModificationTracker(Project project) {
        return ModificationTracker.NEVER_CHANGED;
    }
}
