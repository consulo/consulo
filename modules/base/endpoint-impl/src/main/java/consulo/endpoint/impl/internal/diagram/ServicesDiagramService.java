package consulo.endpoint.impl.internal.diagram;

import java.util.List;

public record ServicesDiagramService(String moduleName, List<ServicesDiagramMember> members) implements ServicesDiagramElement {
    static final String QUALIFIED_NAME_PREFIX = "service:";

    @Override
    public String getName() {
        return moduleName;
    }

    @Override
    public String getQualifiedName() {
        return qualifiedName(moduleName);
    }

    static String qualifiedName(String moduleName) {
        return QUALIFIED_NAME_PREFIX + moduleName;
    }
}
