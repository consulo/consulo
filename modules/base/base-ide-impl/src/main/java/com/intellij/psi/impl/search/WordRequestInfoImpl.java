package com.intellij.psi.impl.search;

import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.SearchSession;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Objects;

public final class WordRequestInfoImpl implements WordRequestInfo {
  private String word;

  private SearchScope searchScope;

  private boolean caseSensitive;

  private short searchContext;

  private String containerName;

  public WordRequestInfoImpl(@Nonnull String word, @Nonnull SearchScope searchScope, boolean caseSensitive, short searchContext, @Nullable String containerName) {
    this.word = word;
    this.searchScope = searchScope;
    this.caseSensitive = caseSensitive;
    this.searchContext = searchContext;
    this.containerName = containerName;
  }

  @Nonnull
  public String getWord() {
    return word;
  }

  @Nonnull
  public SearchScope getSearchScope() {
    return searchScope;
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public short getSearchContext() {
    return searchContext;
  }

  @Nullable
  public String getContainerName() {
    return containerName;
  }

  @Nonnull
  public SearchSession getSearchSession() {
    return new SearchSession();
  }

  @Nonnull
  private String component1() {
    return word;
  }

  @Nonnull
  private SearchScope component2() {
    return searchScope;
  }

  private boolean component3() {
    return caseSensitive;
  }

  private short component4() {
    return searchContext;
  }

  @Nullable
  private String component5() {
    return containerName;
  }

  @Nonnull
  public final WordRequestInfoImpl copy(@Nonnull String word, @Nonnull SearchScope searchScope, boolean caseSensitive, short searchContext, @Nullable String containerName) {
    return new WordRequestInfoImpl(word, searchScope, caseSensitive, searchContext, containerName);
  }

  @Nonnull
  public String toString() {
    StringBuilder __builder = new StringBuilder();
    __builder.append("WordRequestInfoImpl(");
    __builder.append("word=").append(word).append(",");
    __builder.append("searchScope=").append(searchScope).append(",");
    __builder.append("caseSensitive=").append(caseSensitive).append(",");
    __builder.append("searchContext=").append(searchContext).append(",");
    __builder.append("containerName=").append(containerName);
    __builder.append(")");
    return __builder.toString();
  }

  public int hashCode() {
    return Objects.hash(word, searchScope, caseSensitive, searchContext, containerName);
  }

  public boolean equals(@Nullable Object other) {
    if (other == this) return true;
    if (other == null || other.getClass() != this.getClass()) return false;
    if (!Objects.equals(word, ((WordRequestInfoImpl)other).word)) return false;
    if (!Objects.equals(searchScope, ((WordRequestInfoImpl)other).searchScope)) return false;
    if (!Objects.equals(caseSensitive, ((WordRequestInfoImpl)other).caseSensitive)) return false;
    if (!Objects.equals(searchContext, ((WordRequestInfoImpl)other).searchContext)) return false;
    if (!Objects.equals(containerName, ((WordRequestInfoImpl)other).containerName)) return false;
    return true;
  }
}
