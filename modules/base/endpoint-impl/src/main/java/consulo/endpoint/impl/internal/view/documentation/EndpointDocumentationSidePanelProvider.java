package consulo.endpoint.impl.internal.view.documentation;

import consulo.annotation.component.ExtensionImpl;
import consulo.endpoint.EndpointSidePanel;
import consulo.endpoint.EndpointSidePanelProvider;
import consulo.language.editor.documentation.DocumentationManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;

@ExtensionImpl(id = "documentation")
public final class EndpointDocumentationSidePanelProvider implements EndpointSidePanelProvider {
    private final Project myProject;
    private final DocumentationManager myDocumentationManager;

    @Inject
    public EndpointDocumentationSidePanelProvider(Project project, DocumentationManager documentationManager) {
        myProject = project;
        myDocumentationManager = documentationManager;
    }

    @RequiredUIAccess
    @Override
    public EndpointSidePanel create() {
        return new EndpointDocumentationSidePanel(myProject, myDocumentationManager);
    }
}
