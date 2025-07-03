/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.language.psi.stub;

import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.ObjectUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.function.IntFunction;

/**
 * @author max
 */
public abstract class StubBase<T extends PsiElement> extends ObjectStubBase<StubElement> implements StubElement<T> {
  StubList myStubList;
  private volatile T myPsi;

  private static final VarHandle ourPsiUpdater;

  static {
    try {
      ourPsiUpdater = MethodHandles.lookup().findVarHandle(StubBase.class, "myPsi", PsiElement.class);
    }
    catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  protected StubBase(StubElement parent, IStubElementType elementType) {
    super(parent);
    myStubList = parent == null ? new MaterialStubList(10) : ((StubBase<?>)parent).myStubList;
    myStubList.addStub(this, (StubBase<?>)parent, elementType);
  }

  public StubList getStubList() {
    return myStubList;
  }

  @Override
  public StubElement getParentStub() {
    return myParent;
  }

  @Override
  public PsiFileStub<?> getContainingFileStub() {
    StubBase<?> rootStub = myStubList.get(0);
    if (!(rootStub instanceof PsiFileStub)) {
      return null;
    }
    return (PsiFileStub<?>)rootStub;
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public List<StubElement> getChildrenStubs() {
    return (List)myStubList.getChildrenStubs(id);
  }

  @Override
  @Nullable
  public <P extends PsiElement, S extends StubElement<P>> S findChildStubByType(@Nonnull IStubElementType<S, P> elementType) {
    return myStubList.findChildStubByType(id, elementType);
  }

  public void setPsi(@Nonnull T psi) {
    assert myPsi == null || myPsi == psi;
    myPsi = psi;
  }

  @Nullable
  public final T getCachedPsi() {
    return myPsi;
  }

  @Override
  public T getPsi() {
    T psi = myPsi;
    if (psi != null) return psi;

    //noinspection unchecked
    psi = (T)getStubType().createPsi(this);
    return ourPsiUpdater.compareAndSet(this, null, psi) ? psi : ObjectUtil.assertNotNull(myPsi);
  }

  @Nonnull
  @Override
  public <E extends PsiElement> E[] getChildrenByType(@Nonnull final IElementType elementType, E[] array) {
    List<StubElement> childrenStubs = getChildrenStubs();
    int count = countChildren(elementType, childrenStubs);

    array = ArrayUtil.ensureExactSize(count, array);
    if (count == 0) return array;
    fillFilteredChildren(elementType, array, childrenStubs);

    return array;
  }

  @Nonnull
  @Override
  public <E extends PsiElement> E[] getChildrenByType(@Nonnull final TokenSet filter, E[] array) {
    List<StubElement> childrenStubs = getChildrenStubs();
    int count = countChildren(filter, childrenStubs);

    array = ArrayUtil.ensureExactSize(count, array);
    if (count == 0) return array;
    fillFilteredChildren(filter, array, childrenStubs);

    return array;
  }

  @Nonnull
  @Override
  public <E extends PsiElement> E[] getChildrenByType(@Nonnull final IElementType elementType, @Nonnull final IntFunction<E[]> f) {
    List<StubElement> childrenStubs = getChildrenStubs();
    int count = countChildren(elementType, childrenStubs);

    E[] result = f.apply(count);
    if (count > 0) fillFilteredChildren(elementType, result, childrenStubs);

    return result;
  }

  private static int countChildren(IElementType elementType, List<? extends StubElement> childrenStubs) {
    int count = 0;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, childrenStubsSize = childrenStubs.size(); i < childrenStubsSize; i++) {
      StubElement<?> childStub = childrenStubs.get(i);
      if (childStub.getStubType() == elementType) count++;
    }

    return count;
  }

  private static int countChildren(TokenSet types, List<? extends StubElement> childrenStubs) {
    int count = 0;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, childrenStubsSize = childrenStubs.size(); i < childrenStubsSize; i++) {
      StubElement<?> childStub = childrenStubs.get(i);
      if (types.contains(childStub.getStubType())) count++;
    }

    return count;
  }

  private static <E extends PsiElement> void fillFilteredChildren(IElementType type, E[] result, List<? extends StubElement> childrenStubs) {
    int count = 0;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, childrenStubsSize = childrenStubs.size(); i < childrenStubsSize; i++) {
      StubElement<?> childStub = childrenStubs.get(i);
      if (childStub.getStubType() == type) {
        //noinspection unchecked
        result[count++] = (E)childStub.getPsi();
      }
    }

    assert count == result.length;
  }

  private static <E extends PsiElement> void fillFilteredChildren(TokenSet set, E[] result, List<? extends StubElement> childrenStubs) {
    int count = 0;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, childrenStubsSize = childrenStubs.size(); i < childrenStubsSize; i++) {
      StubElement<?> childStub = childrenStubs.get(i);
      if (set.contains(childStub.getStubType())) {
        //noinspection unchecked
        result[count++] = (E)childStub.getPsi();
      }
    }

    assert count == result.length;
  }

  @Nonnull
  @Override
  public <E extends PsiElement> E[] getChildrenByType(@Nonnull final TokenSet filter, @Nonnull final IntFunction<E[]> f) {
    List<StubElement> childrenStubs = getChildrenStubs();
    int count = countChildren(filter, childrenStubs);

    E[] array = f.apply(count);
    if (count == 0) return array;

    fillFilteredChildren(filter, array, childrenStubs);

    return array;
  }

  @Override
  @Nullable
  public <E extends PsiElement> E getParentStubOfType(@Nonnull final Class<E> parentClass) {
    StubElement<?> parent = myParent;
    while (parent != null) {
      PsiElement psi = parent.getPsi();
      if (parentClass.isInstance(psi)) {
        //noinspection unchecked
        return (E)psi;
      }
      parent = parent.getParentStub();
    }
    return null;
  }

  @Override
  public IStubElementType getStubType() {
    return myStubList.getStubType(id);
  }

  public Project getProject() {
    return getPsi().getProject();
  }

  public String printTree() {
    StringBuilder builder = new StringBuilder();
    printTree(builder, 0);
    return builder.toString();
  }

  private void printTree(StringBuilder builder, int nestingLevel) {
    for (int i = 0; i < nestingLevel; i++) builder.append("  ");
    builder.append(this).append('\n');
    for (StubElement<?> child : getChildrenStubs()) {
      ((StubBase<?>)child).printTree(builder, nestingLevel + 1);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  /**
   * @return comparison result (as in {@link Comparable}) of this stub with {@code another},
   * where "a<b" means that "a" occurs before "b" in the deep-first traversal of the stub tree,
   * and the same holds for their AST equivalents.
   */
  public int compareByOrderWith(ObjectStubBase<?> another) {
    return Integer.compare(getStubId(), another.getStubId());
  }
}
