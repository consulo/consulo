/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.editor.structureView;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.component.extension.ByClassGrouper;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.extension.ExtensionPointName;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface StructureViewExtension {
  ExtensionPointName<StructureViewExtension> EXTENSION_POINT_NAME = ExtensionPointName.create(StructureViewExtension.class);

  Class<? extends PsiElement> getType();

  StructureViewTreeElement[] getChildren(PsiElement parent);

  @Nullable
  Object getCurrentEditorElement(Editor editor, PsiElement parent);

  default void filterChildren(@Nonnull Collection<StructureViewTreeElement> baseChildren, @Nonnull List<StructureViewTreeElement> extensionChildren) {
  }
}
