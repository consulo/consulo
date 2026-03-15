// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.wrapper;

import consulo.document.util.TextRange;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public interface MethodCall  {
  
  String getName();

  /**
   * @return Returns a string, representing all generic arguments of a function in a chain surrounded by brackets. In C# there are calls, that look like
   * collection.Cast&lt;int&gt;()`, so for this call this method should return `&lt;int&gt;`.  This string is necessary for a code generator to recreate
   * a method call for evaluation.
   */
  
 String getGenericArguments();

  
  List<CallArgument> getArguments();

  
  TextRange getTextRange();

  
  default String getTabTitle() {
    return getName().replace(" ", "") + getGenericArguments();
  }

  
  default String getTabTooltip() {
    return TraceUtil.formatWithArguments(this);
  }
}
