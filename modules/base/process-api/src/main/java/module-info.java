/**
 * @author VISTALL
 * @since 05-Feb-22
 */
module consulo.process.api {
  requires transitive consulo.application.api;
  requires transitive consulo.virtual.file.system.api;
  requires transitive consulo.util.dataholder;

  requires consulo.util.jna;

  exports consulo.process;
  exports consulo.process.cmd;
  exports consulo.process.io;
  exports consulo.process.event;
  exports consulo.process.local;
  exports consulo.process.localize;
  exports consulo.process.util;

  exports consulo.process.internal to
      consulo.ide.impl,
      consulo.process.impl,
      consulo.execution.debug.api,
      consulo.execution.test.api,
      consulo.execution.test.sm.api,
      consulo.remote.server.impl,
      consulo.execution.impl,
      consulo.virtual.file.system.impl;
}