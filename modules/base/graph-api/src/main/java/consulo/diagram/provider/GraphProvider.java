/*
 * Copyright 2013-2016 consulo.io
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
package consulo.diagram.provider;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import consulo.diagram.builder.GraphBuilder;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 23:48/15.10.13
 */
public interface GraphProvider {
  ExtensionPointName<GraphProvider> EP_NAME = ExtensionPointName.create("com.intellij.graphProvider");

  @Nonnull
  GraphBuilder createBuilder(@Nonnull PsiElement element);

  boolean isSupported(@Nonnull PsiElement element);
}
