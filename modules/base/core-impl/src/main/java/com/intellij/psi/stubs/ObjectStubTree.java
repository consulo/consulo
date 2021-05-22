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
package com.intellij.psi.stubs;

import com.intellij.util.ArrayUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Dmitry Avdeev
 */
public class ObjectStubTree<T extends Stub> {
  private static final Key<ObjectStubTree> STUB_TO_TREE_REFERENCE = Key.create("stub to tree reference");
  protected final ObjectStubBase myRoot;
  private String myDebugInfo;
  private final List<T> myPlainList;

  public ObjectStubTree(@Nonnull final ObjectStubBase root, final boolean withBackReference) {
    myRoot = root;
    myPlainList = enumerateStubs(root);
    if (withBackReference) {
      myRoot.putUserData(STUB_TO_TREE_REFERENCE, this); // This will prevent soft references to stub tree to be collected before all of the stubs are collected.
    }
  }

  @Nonnull
  public Stub getRoot() {
    return myRoot;
  }

  @Nonnull
  public List<T> getPlainList() {
    return myPlainList;
  }

  @Nonnull
  List<T> getPlainListFromAllRoots() {
    return getPlainList();
  }

  @Deprecated
  @Nonnull
  public Map<StubIndexKey, Map<Object, int[]>> indexStubTree() {
    return indexStubTree(key -> HashingStrategy.canonical());
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public Map<StubIndexKey, Map<Object, int[]>> indexStubTree(@Nonnull Function<StubIndexKey<?, ?>, HashingStrategy<?>> keyHashingStrategyFunction) {
    StubIndexSink sink = new StubIndexSink(keyHashingStrategyFunction);
    final List<T> plainList = getPlainListFromAllRoots();
    for (int i = 0, plainListSize = plainList.size(); i < plainListSize; i++) {
      final Stub stub = plainList.get(i);
      sink.myStubIdx = i;
      StubSerializationUtil.getSerializer(stub).indexStub(stub, sink);
    }

    return sink.getResult();
  }

  @Nonnull
  protected List<T> enumerateStubs(@Nonnull Stub root) {
    List<T> result = new ArrayList<>();
    //noinspection unchecked
    enumerateStubsInto(root, (List)result);
    return result;
  }

  private static void enumerateStubsInto(@Nonnull Stub root, @Nonnull List<? super Stub> result) {
    ((ObjectStubBase)root).id = result.size();
    result.add(root);
    List<? extends Stub> childrenStubs = root.getChildrenStubs();
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < childrenStubs.size(); i++) {
      Stub child = childrenStubs.get(i);
      enumerateStubsInto(child, result);
    }
  }

  public void setDebugInfo(@Nonnull String info) {
    ObjectStubTree ref = getStubTree(myRoot);
    if (ref != null) {
      assert ref == this;
      info += "; with backReference";
    }
    myDebugInfo = info;
  }

  @Nullable
  public static ObjectStubTree getStubTree(@Nonnull ObjectStubBase root) {
    return root.getUserData(STUB_TO_TREE_REFERENCE);
  }

  public String getDebugInfo() {
    return myDebugInfo;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{myDebugInfo='" + myDebugInfo + '\'' + ", myRoot=" + myRoot + '}' + hashCode();
  }

  private static class StubIndexSink implements IndexSink, Consumer<Map<Object, int[]>>, BiConsumer<Object, int[]> {
    private final Map<StubIndexKey, Map<Object, int[]>> myResult = new HashMap<>();
    private final Function<StubIndexKey<?, ?>, HashingStrategy<?>> myHashingStrategyFunction;
    private int myStubIdx;
    private Map<Object, int[]> myProcessingMap;

    private StubIndexSink(@Nonnull Function<StubIndexKey<?, ?>, HashingStrategy<?>> hashingStrategyFunction) {
      myHashingStrategyFunction = hashingStrategyFunction;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void occurrence(@Nonnull final StubIndexKey indexKey, @Nonnull final Object value) {
      Map<Object, int[]> map = myResult.get(indexKey);
      if (map == null) {
        map = Maps.newHashMap((HashingStrategy<Object>)myHashingStrategyFunction.apply(indexKey));
        myResult.put(indexKey, map);
      }

      int[] list = map.get(value);
      if (list == null) {
        map.put(value, new int[]{myStubIdx});
      }
      else {
        int lastNonZero = ArrayUtil.lastIndexOfNot(list, 0);
        if (lastNonZero >= 0 && list[lastNonZero] == myStubIdx) {
          // second and subsequent occurrence calls for the same value are no op
          return;
        }
        int lastZero = lastNonZero + 1;

        if (lastZero == list.length) {
          list = ArrayUtil.realloc(list, Math.max(4, list.length << 1));
          map.put(value, list);
        }
        list[lastZero] = myStubIdx;
      }
    }

    @Nonnull
    public Map<StubIndexKey, Map<Object, int[]>> getResult() {
      myResult.values().forEach(this);
      return myResult;
    }

    @Override
    public void accept(Map<Object, int[]> object) {
      myProcessingMap = object;
      object.forEach(this);
    }

    @Override
    public void accept(Object a, int[] b) {
      if (b.length == 1) return;

      int firstZero = ArrayUtil.indexOf(b, 0);
      if (firstZero != -1) {
        int[] shorterList = ArrayUtil.realloc(b, firstZero);
        myProcessingMap.put(a, shorterList);
      }
    }
  }
}
