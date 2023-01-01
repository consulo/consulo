/**
 * @author VISTALL
 * @since 21-Jul-22
 */
module consulo.external.service.impl {
  requires consulo.application.api;
  requires consulo.external.service.api;
  requires java.prefs;

  exports consulo.externalService.impl.internal.statistic to consulo.ide.impl;
  exports consulo.externalService.impl.internal to consulo.ide.impl;

  opens consulo.externalService.impl.internal.statistic to consulo.util.xml.serializer;
}