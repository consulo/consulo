/**
 * @author VISTALL
 * @since 15/01/2022
 */
module consulo.sand.language.plugin {
  requires java.desktop;

  requires org.slf4j;
  
  requires consulo.virtual.file.watcher.api;

  requires consulo.ai.api;

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

  requires consulo.module.creation.api;
  requires consulo.module.ui.api;

  requires consulo.language.copyright.api;

  requires consulo.builtin.web.server.api;

  requires consulo.version.control.system.api;

  requires consulo.language.impl;

  opens consulo.sandboxPlugin.lang.inspection to consulo.util.xml.serializer;

  opens consulo.sandboxPlugin.colorScheme to consulo.ide.impl;

  requires org.apache.thrift;

  opens consulo.enviroment.remoteAgent to consulo.util.xml.serializer;

  exports consulo.cpp.api.icon;
  exports consulo.cpp.impl;
  exports consulo.cpp.impl.projectView;
  exports consulo.cpp.impl.projectView.node;
  exports consulo.cpp.lang;
  exports consulo.cpp.lang.editor.highlight;
  exports consulo.cpp.lang.psi.impl;
  exports consulo.cpp.localize;
  exports consulo.cpp.module.extension;
  exports consulo.cpp.moduleAware;
  exports consulo.cpp.preprocessor;
  exports consulo.cpp.preprocessor.expand;
  exports consulo.cpp.preprocessor.fileProvider;
  exports consulo.cpp.preprocessor.lexer;
  exports consulo.cpp.preprocessor.parser;
  exports consulo.cpp.preprocessor.psi;
  exports consulo.cpp.preprocessor.psi.impl;
  exports consulo.cpp.preprocessor.psi.impl.visitor;
  exports consulo.cpp.preprocessor.psi.stub;
  exports consulo.cpp.preprocessor.psi.stub.impl;
  exports org.napile.cpp4idea;
  exports org.napile.cpp4idea.config;
  exports org.napile.cpp4idea.config.sdk;
  exports org.napile.cpp4idea.config.sdk.sdkdialect;
  exports org.napile.cpp4idea.config.sdk.sdkdialect.impl;
  exports org.napile.cpp4idea.ide.highlight;
  exports org.napile.cpp4idea.lang;
  exports org.napile.cpp4idea.lang.lexer;
  exports org.napile.cpp4idea.lang.parser;
  exports org.napile.cpp4idea.lang.parser.parsingMain;
  exports org.napile.cpp4idea.lang.psi;
  exports org.napile.cpp4idea.lang.psi.impl;
  exports org.napile.cpp4idea.lang.psi.visitors;
  exports org.napile.cpp4idea.util;
}
