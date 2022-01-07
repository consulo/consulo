package com.intellij.psi.impl.search;

import com.intellij.psi.PsiElement;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Objects;

// from kotlin
public final class LeafOccurrence {
  private PsiElement scope;

  private PsiElement start;

  private int offsetInStart;

  public LeafOccurrence(@Nonnull PsiElement scope, @Nonnull PsiElement start, int offsetInStart) {
    this.scope = scope;
    this.start = start;
    this.offsetInStart = offsetInStart;
  }

  @Nonnull
  public final PsiElement getScope() {
    return scope;
  }

  @Nonnull
  public final PsiElement getStart() {
    return start;
  }

  public final int getOffsetInStart() {
    return offsetInStart;
  }

  @Nonnull
  public final PsiElement component1() {
    return scope;
  }

  @Nonnull
  public final PsiElement component2() {
    return start;
  }

  public final int component3() {
    return offsetInStart;
  }

  @Nonnull
  public final LeafOccurrence copy(@Nonnull PsiElement scope, @Nonnull PsiElement start, int offsetInStart) {
    return new LeafOccurrence(scope, start, offsetInStart);
  }

  @Nonnull
  public String toString() {
    StringBuilder __builder = new StringBuilder();
    __builder.append("LeafOccurrence(");
    __builder.append("scope=").append(scope).append(",");
    __builder.append("start=").append(start).append(",");
    __builder.append("offsetInStart=").append(offsetInStart);
    __builder.append(")");
    return __builder.toString();
  }

  public int hashCode() {
    return Objects.hash(scope, start, offsetInStart);
  }

  public boolean equals(@Nullable Object other) {
    if (other == this) return true;
    if (other == null || other.getClass() != this.getClass()) return false;
    if (!Objects.equals(scope, ((LeafOccurrence)other).scope)) return false;
    if (!Objects.equals(start, ((LeafOccurrence)other).start)) return false;
    if (!Objects.equals(offsetInStart, ((LeafOccurrence)other).offsetInStart)) return false;
    return true;
  }
}
