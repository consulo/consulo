/**
 * @author VISTALL
 * @since 15/01/2022
 */
module consulo.sand.language.plugin {
  requires java.desktop;

  requires consulo.virtual.file.watcher.api;

  requires consulo.ide.api;

  requires consulo.compiler.api;

  requires consulo.compiler.artifact.api;

  requires consulo.file.template.api;

  requires consulo.language.api;

  requires consulo.language.diagram.api;

  requires consulo.language.editor.refactoring.api;

  requires consulo.color.scheme.api;

  requires consulo.repository.ui.api;

  requires consulo.task.api;

  requires consulo.execution.coverage.api;

  requires consulo.remote.server.api;

  requires consulo.module.ui.api;

  requires consulo.language.copyright.api;

  requires consulo.builtin.web.server.api;

  requires consulo.language.impl;

  opens consulo.sandboxPlugin.lang.inspection to consulo.util.xml.serializer;

  opens consulo.sandboxPlugin.colorScheme to consulo.ide.impl;
}