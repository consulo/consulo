/**
 * @author VISTALL
 * @since 12-Feb-22
 */
module consulo.compiler.api {
    requires transitive consulo.execution.api;
    requires transitive consulo.index.io;
    requires transitive consulo.module.api;
    requires transitive consulo.module.content.api;
    requires transitive consulo.navigation.api;
    requires transitive consulo.project.ui.api;

    exports consulo.compiler;
    exports consulo.compiler.action;
    exports consulo.compiler.event;
    exports consulo.compiler.execution;
    exports consulo.compiler.generic;
    exports consulo.compiler.localize;
    exports consulo.compiler.resourceCompiler;
    exports consulo.compiler.scope;
    exports consulo.compiler.setting;
    exports consulo.compiler.util;

    exports consulo.compiler.internal to
        consulo.compiler.impl;

    opens consulo.compiler.resourceCompiler to
        consulo.util.xml.serializer;
}