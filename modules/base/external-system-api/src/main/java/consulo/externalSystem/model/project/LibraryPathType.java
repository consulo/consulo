package consulo.externalSystem.model.project;

import consulo.content.OrderRootType;

/**
 * Same as  {@link OrderRootType} but without dep
 *
 * @author Denis Zhdanov
 * @since 8/10/11 6:37 PM
 */
public interface LibraryPathType {
    String BINARY = "binaries";
    String SOURCE = "sources";
    String DOC = "documentation";
}
