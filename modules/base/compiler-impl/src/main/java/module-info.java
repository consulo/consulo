/**
 * @author VISTALL
 * @since 08-Aug-22
 */
module consulo.compiler.impl {
    requires transitive consulo.compiler.api;
    requires transitive consulo.compiler.artifact.api;
    requires transitive consulo.project.ui.api;
    requires transitive consulo.build.ui.api;

    requires consulo.local.history.api;

    requires consulo.application.impl;
    requires consulo.compiler.artifact.impl;
    requires consulo.build.ui.impl;

    exports consulo.compiler.impl.internal.artifact to consulo.ide.impl;
    exports consulo.compiler.impl.internal.action to consulo.ide.impl;
    exports consulo.compiler.impl.internal to consulo.ide.impl;

    opens consulo.compiler.impl.internal.action to consulo.component.impl;

    opens consulo.compiler.impl.internal to consulo.component.impl, consulo.util.xml.serializer;
}