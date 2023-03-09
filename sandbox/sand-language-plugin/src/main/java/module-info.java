/**
 * @author VISTALL
 * @since 15/01/2022
 */
module consulo.sand.language.plugin {
  requires java.desktop;

  requires consulo.virtual.file.watcher.api;

  requires consulo.ide.impl;

  opens consulo.sandboxPlugin.lang.inspection to consulo.util.xml.serializer;
}