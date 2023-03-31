/**
 * @author VISTALL
 * @since 02-Sep-22
 */
module consulo.external.system.api {
  requires transitive consulo.project.api;
  requires transitive consulo.configurable.api;
  requires transitive consulo.file.chooser.api;
  requires transitive consulo.process.api;
  requires transitive consulo.module.api;
  requires transitive consulo.module.content.api;
  requires transitive consulo.execution.api;
  requires transitive consulo.execution.debug.api;

  requires transitive consulo.external.system.rt;

  requires consulo.util.rmi;
  requires consulo.util.nodep;

  opens consulo.externalSystem.model.execution to consulo.util.xml.serializer;
  opens consulo.externalSystem.setting to consulo.util.xml.serializer;

  exports consulo.externalSystem;
  exports consulo.externalSystem.model;
  exports consulo.externalSystem.model.execution;
  exports consulo.externalSystem.model.setting;
  exports consulo.externalSystem.model.task;
  exports consulo.externalSystem.service;
  exports consulo.externalSystem.service.execution;
  exports consulo.externalSystem.service.notification;
  exports consulo.externalSystem.service.setting;
  exports consulo.externalSystem.service.project;
  exports consulo.externalSystem.service.project.manage;
  exports consulo.externalSystem.setting;
  exports consulo.externalSystem.task;
  exports consulo.externalSystem.model.project;
  exports consulo.externalSystem.service.module.extension;
  exports consulo.externalSystem.ui;
  exports consulo.externalSystem.util;

  // TODO remove this dependency in future
  requires java.desktop;
  // TODO remove this dependency in future
  exports consulo.externalSystem.ui.awt;

  exports consulo.externalSystem.internal to consulo.ide.impl;
  exports consulo.externalSystem.internal.ui to consulo.ide.impl;
}