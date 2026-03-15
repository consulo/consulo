// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;


/**
 * @author Vitaliy.Bibaev
 */
public interface Convertable {
  
  
  default String toCode() {
    return toCode(0);
  }

  
  
  String toCode(int indent);

  
  default String withIndent(String text, int indent) {
    return "  ".repeat(indent) + text;
  }
}
