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
package consulo.compiler.artifact;

import consulo.compiler.artifact.element.ComplexPackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class PackagingElementProcessor<E extends PackagingElement<?>> {
  public boolean shouldProcessSubstitution(ComplexPackagingElement<?> element) {
    return true;
  }

  public boolean shouldProcess(PackagingElement<?> element) {
    return true;
  }

  public abstract boolean process(@Nonnull E element, @Nonnull PackagingElementPath path);
}
