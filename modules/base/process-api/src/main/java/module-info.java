/**
 * @author VISTALL
 * @since 05-Feb-22
 */
module consulo.process.api {
  requires transitive consulo.application.api;
  requires transitive consulo.util.dataholder;

  requires consulo.util.jna;

  exports consulo.process;
  exports consulo.process.cmd;
  exports consulo.process.io;
  exports consulo.process.event;
  exports consulo.process.local;

  exports consulo.process.local.internal to consulo.ide.impl;
}