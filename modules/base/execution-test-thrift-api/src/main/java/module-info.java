/**
 * @author VISTALL
 * @since 21-Jul-22
 */
module consulo.execution.test.thrift.api {
  requires consulo.execution.test.sm.api;
  requires org.apache.thrift;
  requires org.slf4j;

  exports consulo.execution.test.thrift.runner;
}