package consulo.endpoint.impl.internal.diagram;

import consulo.endpoint.localize.EndpointLocalize;

import java.util.List;

public record ServicesDiagramExternal(String authority, List<ServicesDiagramMember> members) implements ServicesDiagramElement {
    static final String QUALIFIED_NAME_PREFIX = "external:";

    @Override
    public String getName() {
        return authority.isEmpty() ? EndpointLocalize.frameworksFiltersModuleExternal().get() : authority;
    }

    @Override
    public String getQualifiedName() {
        return qualifiedName(authority);
    }

    static String qualifiedName(String authority) {
        return QUALIFIED_NAME_PREFIX + authority;
    }
}
