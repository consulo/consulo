/**
 * @author VISTALL
 * @since 2026-03-20
 */
module consulo.external.system.thrift.api {
  requires consulo.external.system.api;
  requires transitive org.apache.thrift;
  requires jakarta.annotation;
  requires org.slf4j;

  exports consulo.externalSystem.thrift;
  exports consulo.externalSystem.thrift.client;
  exports consulo.externalSystem.thrift.converter;
  exports consulo.externalSystem.thrift.server;
}
