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

package consulo.language.psi.stub;

import consulo.index.io.StringRef;
import consulo.language.psi.PsiNamedElement;

import jakarta.annotation.Nullable;

/**
 * @author yole
 */
public abstract class NamedStubBase<T extends PsiNamedElement> extends StubBase<T> implements NamedStub<T> {
  private final String myName;

  protected NamedStubBase(StubElement parent, IStubElementType elementType, @Nullable StringRef name) {
    this(parent, elementType, StringRef.toString(name));
  }

  protected NamedStubBase(StubElement parent, IStubElementType elementType, @Nullable String name) {
    super(parent, elementType);
    myName = name;
  }

  @Override
  public String getName() {
    return myName;
  }
}
