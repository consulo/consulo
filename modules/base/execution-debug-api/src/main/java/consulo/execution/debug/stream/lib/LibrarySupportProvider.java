// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.lib;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.execution.debug.stream.trace.CollectionTreeBuilder;
import consulo.execution.debug.stream.trace.DebuggerCommandLauncher;
import consulo.execution.debug.stream.trace.TraceExpressionBuilder;
import consulo.execution.debug.stream.trace.XValueInterpreter;
import consulo.execution.debug.stream.wrapper.StreamChainBuilder;
import consulo.component.extension.ExtensionPointName;
import consulo.execution.debug.XDebugSession;
import consulo.project.Project;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface LibrarySupportProvider {
  ExtensionPointName<LibrarySupportProvider> EP_NAME = ExtensionPointName.create(LibrarySupportProvider.class);

  static List<LibrarySupportProvider> getList() {
    return EP_NAME.getExtensionList();
  }

  
  String getLanguageId();

  
  StreamChainBuilder getChainBuilder();

  
  TraceExpressionBuilder getExpressionBuilder(Project project);

  
  XValueInterpreter getXValueInterpreter(Project project);

  
  CollectionTreeBuilder getCollectionTreeBuilder(Project project);

  
  LibrarySupport getLibrarySupport();

  
  DebuggerCommandLauncher getDebuggerCommandLauncher(XDebugSession session);
}
