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
package consulo.language.editor.inspection.reference;

import jakarta.annotation.Nonnull;

/**
 * @author anna
 * @since 2007-12-18
 */
public class RefVisitor {
  public void visitElement(@Nonnull RefEntity elem) {}

  public void visitFile(@Nonnull RefFile file) {
    visitElement(file);
  }

  public void visitModule(@Nonnull RefModule module){
    visitElement(module);
  }

  public void visitDirectory(@Nonnull RefDirectory directory) {
    visitElement(directory);
  }
}