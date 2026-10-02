package consulo.endpoint.impl.internal.view.example;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.EditorFactory;
import consulo.endpoint.EndpointSidePanel;
import consulo.endpoint.EndpointSidePanelProvider;
import consulo.endpoint.client.generator.ClientGenerator;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@ExtensionImpl(id = "examples", order = "last")
public final class EndpointExampleSidePanelProvider implements EndpointSidePanelProvider {
    private final Project myProject;
    private final EditorFactory myEditorFactory;
    private final EditorBoxBuilderFactory myEditorBoxBuilderFactory;

    @Inject
    public EndpointExampleSidePanelProvider(Project project, EditorFactory editorFactory, EditorBoxBuilderFactory editorBoxBuilderFactory) {
        myProject = project;
        myEditorFactory = editorFactory;
        myEditorBoxBuilderFactory = editorBoxBuilderFactory;
    }

    @RequiredUIAccess
    @Override
    public @Nullable EndpointSidePanel create() {
        if (!myProject.getExtensionPoint(ClientGenerator.class).hasAnyExtensions()) {
            return null;
        }

        List<ClientGenerator> generators = new ArrayList<>();
        myProject.getExtensionPoint(ClientGenerator.class).forEach(generators::add);
        return new EndpointExampleSidePanel(myProject, myEditorFactory, myEditorBoxBuilderFactory, generators);
    }
}
