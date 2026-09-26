/**
 * @author VISTALL
 * @since 2022-01-15
 */
@SuppressWarnings("module")
module consulo.sand.language.plugin {
    requires java.desktop;

    requires org.apache.thrift;
    requires org.slf4j;

    requires consulo.ai.api;
    requires consulo.builtin.web.server.api;
    requires consulo.color.scheme.api;
    requires consulo.compiler.api;
    requires consulo.compiler.artifact.api;
    requires consulo.execution.coverage.api;
    requires consulo.external.system.api;
    requires consulo.file.template.api;
    requires consulo.ide.api;
    requires consulo.language.api;
    requires consulo.language.copyright.api;
    requires consulo.language.diagram.api;
    requires consulo.language.editor.refactoring.api;
    requires consulo.language.impl;
    requires consulo.module.creation.api;
    requires consulo.module.ui.api;
    requires consulo.remote.server.api;
    requires consulo.repository.ui.api;
    requires consulo.task.api;
    requires consulo.version.control.system.api;
    requires consulo.virtual.file.watcher.api;

    opens consulo.enviroment.remoteAgent to consulo.util.xml.serializer;
    opens consulo.sandboxPlugin.colorScheme to consulo.ide.impl;
    opens consulo.sandboxPlugin.lang.inspection to consulo.util.xml.serializer;
}
