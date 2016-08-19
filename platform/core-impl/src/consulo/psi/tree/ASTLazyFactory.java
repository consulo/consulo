/*
 * Copyright 2013 must-be.org
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
package consulo.psi.tree;

import com.intellij.psi.impl.source.tree.LazyParseableElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import com.intellij.util.containers.Predicate;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 2:10/02.04.13
 */
public interface ASTLazyFactory extends Predicate<IElementType> {
  ElementTypeEntryExtensionCollector<ASTLazyFactory> EP = ElementTypeEntryExtensionCollector.create("com.intellij.lang.ast.lazyFactory");

  @NotNull
  LazyParseableElement createLazy(final ILazyParseableElementType type, final CharSequence text);
}
