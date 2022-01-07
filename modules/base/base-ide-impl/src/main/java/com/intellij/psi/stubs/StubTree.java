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
package com.intellij.psi.stubs;

import com.intellij.psi.impl.source.StubbedSpine;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import java.util.List;

public class StubTree extends ObjectStubTree<StubElement<?>> {
  private final StubSpine mySpine = new StubSpine(this);

  public StubTree(@Nonnull final PsiFileStub root) {
    this(root, true);
  }

  public StubTree(@Nonnull final PsiFileStub root, final boolean withBackReference) {
    super((ObjectStubBase)root, withBackReference);
  }

  @Nonnull
  @Override
  protected List<StubElement<?>> enumerateStubs(@Nonnull Stub root) {
    return ((StubBase)root).myStubList.finalizeLoadingStage().toPlainList();
  }

  @Nonnull
  @Override
  final List<StubElement<?>> getPlainListFromAllRoots() {
    PsiFileStub[] roots = ((PsiFileStubImpl<?>)getRoot()).getStubRoots();
    if (roots.length == 1) return super.getPlainListFromAllRoots();

    return ContainerUtil.concat(roots, stub -> ((PsiFileStubImpl)stub).myStubList.toPlainList());
  }

  @Nonnull
  @Override
  public PsiFileStub getRoot() {
    return (PsiFileStub)myRoot;
  }

  @Nonnull
  public StubbedSpine getSpine() {
    return mySpine;
  }
}
