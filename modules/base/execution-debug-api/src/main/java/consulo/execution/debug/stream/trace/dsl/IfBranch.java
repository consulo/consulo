// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import java.util.function.Consumer;

/**
 * @author Vitaliy.Bibaev
 */
public interface IfBranch extends Convertable {
  void elseBranch(Consumer<CodeBlock> init);

  IfBranch elseIfBranch(Expression condition, Consumer<CodeBlock> init);
}
