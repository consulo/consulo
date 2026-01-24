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
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface LibrarySupportProvider {
  ExtensionPointName<LibrarySupportProvider> EP_NAME = ExtensionPointName.create(LibrarySupportProvider.class);

  static @Nonnull List<LibrarySupportProvider> getList() {
    return EP_NAME.getExtensionList();
  }

  @Nonnull
  String getLanguageId();

  @Nonnull
  StreamChainBuilder getChainBuilder();

  @Nonnull
  TraceExpressionBuilder getExpressionBuilder(@Nonnull Project project);

  @Nonnull
  XValueInterpreter getXValueInterpreter(@Nonnull Project project);

  @Nonnull
  CollectionTreeBuilder getCollectionTreeBuilder(@Nonnull Project project);

  @Nonnull
  LibrarySupport getLibrarySupport();

  @Nonnull
  DebuggerCommandLauncher getDebuggerCommandLauncher(@Nonnull XDebugSession session);
}
