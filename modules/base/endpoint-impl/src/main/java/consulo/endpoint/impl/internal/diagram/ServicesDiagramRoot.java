package consulo.endpoint.impl.internal.diagram;

import consulo.util.dataholder.Key;

public record ServicesDiagramRoot(String projectName) implements ServicesDiagramElement {
    public static final Key<ServicesDiagramRoot> KEY = Key.create("ServicesDiagramRoot");

    static final String QUALIFIED_NAME = "root";

    @Override
    public String getName() {
        return projectName;
    }

    @Override
    public String getQualifiedName() {
        return QUALIFIED_NAME;
    }
}
