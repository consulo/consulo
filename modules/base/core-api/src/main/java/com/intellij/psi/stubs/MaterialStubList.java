// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.stubs;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

class MaterialStubList extends StubList {
  /**
   * The list of all stubs ordered by id. The order is DFS (except maybe temporarily during construction, fixed by {@link #finalizeLoadingStage()} later)
   */
  private final ArrayList<StubBase<?>> myPlainList;

  MaterialStubList(int initialCapacity) {
    super(initialCapacity);
    myPlainList = new ArrayList<>(initialCapacity);
  }

  @Override
  void addStub(@Nonnull StubBase<?> stub, @Nullable StubBase<?> parent, @Nullable IStubElementType<?, ?> type) {
    super.addStub(stub, parent, type);
    myPlainList.add(stub);
  }

  @Nonnull
  @Override
  StubList finalizeLoadingStage() {
    if (!isChildrenLayoutOptimal()) {
      return createOptimizedCopy();
    }

    myPlainList.trimToSize();
    return super.finalizeLoadingStage();
  }

  @Nonnull
  private StubList createOptimizedCopy() {
    MaterialStubList copy = new MaterialStubList(size());
    new Object() {
      void visitStub(StubBase<?> stub, int parentId) {
        int idInCopy = copy.size();
        copy.addStub(idInCopy, parentId, getStubTypeIndex(stub.id));
        copy.myPlainList.add(stub);

        List<StubBase<?>> children = getChildrenStubs(stub.id);
        copy.prepareForChildren(idInCopy, children.size());

        for (StubBase<?> child : children) {
          visitStub(child, idInCopy);
        }
      }
    }.visitStub(get(0), -1);

    assert copy.isChildrenLayoutOptimal();

    for (int i = 0; i < copy.size(); i++) {
      StubBase<?> stub = copy.get(i);
      stub.myStubList = copy;
      stub.id = i;
    }

    return copy.finalizeLoadingStage();
  }

  @Override
  public int size() {
    return myPlainList.size();
  }

  @Override
  public StubBase<?> get(int id) {
    return myPlainList.get(id);
  }

  @Nullable
  @Override
  StubBase<?> getCachedStub(int index) {
    return get(index);
  }
}
