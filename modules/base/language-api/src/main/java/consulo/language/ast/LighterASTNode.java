/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.ast;

import consulo.util.collection.ArrayFactory;

/**
 * @author max
 */
public interface LighterASTNode {
  LighterASTNode[] EMPTY_ARRAY = new LighterASTNode[0];

  ArrayFactory<LighterASTNode> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new LighterASTNode[count];

  IElementType getTokenType();

  int getStartOffset();

  int getEndOffset();

  default int getTextLength() {
    return getEndOffset() - getStartOffset();
  }
}