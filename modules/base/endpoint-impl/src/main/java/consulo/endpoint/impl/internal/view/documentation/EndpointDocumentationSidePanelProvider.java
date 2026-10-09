package consulo.endpoint.impl.internal.view.documentation;

import consulo.annotation.component.ExtensionImpl;
import consulo.endpoint.EndpointSidePanel;
import consulo.endpoint.EndpointSidePanelProvider;
import consulo.language.editor.internal.DocumentationViewFactory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;

@ExtensionImpl(id = "documentation")
public final class EndpointDocumentationSidePanelProvider implements EndpointSidePanelProvider {
    private final Project myProject;
    private final DocumentationViewFactory myDocumentationViewFactory;

    @Inject
    public EndpointDocumentationSidePanelProvider(Project project, DocumentationViewFactory documentationViewFactory) {
        myProject = project;
        myDocumentationViewFactory = documentationViewFactory;
    }

    @RequiredUIAccess
    @Override
    public EndpointSidePanel create() {
        return new EndpointDocumentationSidePanel(myProject, myDocumentationViewFactory);
    }
}
