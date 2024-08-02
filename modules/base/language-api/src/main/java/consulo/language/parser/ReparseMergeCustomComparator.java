/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.parser;

import consulo.language.ast.ASTNode;
import consulo.language.ast.LighterASTNode;
import consulo.language.util.FlyweightCapableTreeStructure;
import consulo.util.lang.ThreeState;

import jakarta.annotation.Nonnull;

/**
 * Comparator which called during reparse when merge algorithm is not sure what to merge
 */
public interface ReparseMergeCustomComparator {
    @Nonnull
    ThreeState compare(@Nonnull ASTNode node, LighterASTNode lighterASTNode, FlyweightCapableTreeStructure<LighterASTNode> structure);
}
