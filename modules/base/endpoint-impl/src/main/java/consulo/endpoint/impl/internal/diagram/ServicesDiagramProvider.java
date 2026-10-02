package consulo.endpoint.impl.internal.diagram;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.component.util.ModificationTracker;
import consulo.diagram.AbstractDiagramNodeContentManager;
import consulo.diagram.AbstractDiagramVisibilityManager;
import consulo.diagram.DiagramCategory;
import consulo.diagram.DiagramDataModel;
import consulo.diagram.DiagramElementManager;
import consulo.diagram.DiagramNodeContentManager;
import consulo.diagram.DiagramProvider;
import consulo.diagram.DiagramRelationshipInfo;
import consulo.diagram.DiagramRelationshipManager;
import consulo.diagram.DiagramVfsResolver;
import consulo.diagram.DiagramVisibilityManager;
import consulo.diagram.VisibilityLevel;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.language.psi.PsiModificationTracker;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

@ExtensionImpl
public final class ServicesDiagramProvider extends DiagramProvider<ServicesDiagramElement> {
    public static final String ID = "EndpointServices";

    private final DiagramCategory myServerCategory =
        new DiagramCategory(EndpointLocalize.servicesDiagramCategoryServer().get(), PlatformIconGroup.webreferencesServer());
    private final DiagramCategory myClientCategory =
        new DiagramCategory(EndpointLocalize.servicesDiagramCategoryClient().get(), PlatformIconGroup.javaeeWebserviceclient());

    private final ServicesDiagramElementManager myElementManager = new ServicesDiagramElementManager();
    private final ServicesDiagramVfsResolver myVfsResolver = new ServicesDiagramVfsResolver();

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
            return element instanceof ServicesDiagramMember member
                && (member.client() ? myClientCategory : myServerCategory).equals(category);
        }

        @Override
        public DiagramCategory[] getContentCategories() {
            return new DiagramCategory[]{myServerCategory, myClientCategory};
        }
    };

    private final DiagramRelationshipManager<ServicesDiagramElement> myRelationshipManager = new DiagramRelationshipManager<>() {
        @Override
        public @Nullable DiagramRelationshipInfo getDependencyInfo(
            ServicesDiagramElement e1,
            ServicesDiagramElement e2,
            DiagramCategory category
        ) {
            return null;
        }

        @Override
        public DiagramCategory[] getContentCategories() {
            return DiagramCategory.EMPTY_ARRAY;
        }
    };

    public ServicesDiagramProvider() {
        myElementManager.setDiagramProvider(this);
        myNodeContentManager.setEnabled(myServerCategory, true);
        myNodeContentManager.setEnabled(myClientCategory, true);
    }

    @Override
    public String getID() {
        return ID;
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
    public DiagramElementManager<ServicesDiagramElement> getElementManager() {
        return myElementManager;
    }

    @Override
    public DiagramVfsResolver<ServicesDiagramElement> getVfsResolver() {
        return myVfsResolver;
    }

    @Override
    public DiagramRelationshipManager<ServicesDiagramElement> getRelationshipManager() {
        return myRelationshipManager;
    }

    @RequiredReadAction
    @Override
    public DiagramDataModel<ServicesDiagramElement> createDataModel(
        Project project,
        @Nullable ServicesDiagramElement element,
        @Nullable VirtualFile file
    ) {
        return new ServicesDiagramDataModel(this, ServicesDiagramGraphBuilder.getGraph(project));
    }

    @Override
    public ModificationTracker getModificationTracker(Project project) {
        return PsiModificationTracker.getInstance(project);
    }
}
