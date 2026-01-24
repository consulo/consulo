// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import java.util.function.Consumer;

/**
 * @author Vitaliy.Bibaev
 */
public interface Dsl extends DslFactory {
  Expression getNullExpression();

  Expression getThisExpression();

  Types getTypes();

  CodeBlock block(Consumer<CodeContext> init);

  String code(Consumer<CodeContext> init);
}
