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

/*
 * @author max
 */
package consulo.language.psi.stub;

import consulo.language.psi.PsiFile;
import consulo.util.dataholder.UserDataHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface PsiFileStub<T extends PsiFile> extends StubElement<T>, UserDataHolder {
  PsiFileStub[] EMPTY_ARRAY = new PsiFileStub[0];

  StubFileElementType getType();

  @Nonnull
  PsiFileStub[] getStubRoots();

  @Nullable
  String getInvalidationReason();
}
