/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.psi.stub;

import consulo.language.Language;
import consulo.language.ast.IFileElementType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Base class for file element types having stubs.
 *
 * @author Konstantin.Ulitin
 */
public abstract class StubFileElementType<T extends PsiFileStub> extends IFileElementType implements StubSerializer<T> {

  public static final String DEFAULT_EXTERNAL_ID = "psi.file";

  public StubFileElementType(@Nullable Language language) {
    super(language);
  }

  public StubFileElementType(@Nonnull String debugName, @Nullable Language language) {
    super(debugName, language);
  }
}
