package com.intellij.vcs.log.impl;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.Interner;
import com.intellij.vcs.log.Hash;
import com.intellij.vcs.log.VcsRef;
import com.intellij.vcs.log.VcsRefType;

import javax.annotation.Nonnull;

/**
 * @author erokhins
 */
public final class VcsRefImpl implements VcsRef {
  private static final Interner<String> ourNames = Interner.createStringInterner();
  @Nonnull
  private final Hash myCommitHash;
  @Nonnull
  private final String myName;
  @Nonnull
  private final VcsRefType myType;
  @Nonnull
  private final VirtualFile myRoot;

  public VcsRefImpl(@Nonnull Hash commitHash, @Nonnull String name, @Nonnull VcsRefType type, @Nonnull VirtualFile root) {
    myCommitHash = commitHash;
    myType = type;
    myRoot = root;
    synchronized (ourNames) {
      myName = ourNames.intern(name);
    }
  }

  @Override
  @Nonnull
  public VcsRefType getType() {
    return myType;
  }

  @Override
  @Nonnull
  public Hash getCommitHash() {
    return myCommitHash;
  }

  @Override
  @Nonnull
  public String getName() {
    return myName;
  }

  @Override
  @Nonnull
  public VirtualFile getRoot() {
    return myRoot;
  }

  @Override
  public String toString() {
    return String.format("%s:%s(%s|%s)", myRoot.getName(), myName, myCommitHash, myType);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VcsRefImpl ref = (VcsRefImpl)o;

    if (!myCommitHash.equals(ref.myCommitHash)) return false;
    if (!myName.equals(ref.myName)) return false;
    if (!myRoot.equals(ref.myRoot)) return false;
    if (myType != ref.myType) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myCommitHash.hashCode();
    result = 31 * result + (myName.hashCode());
    result = 31 * result + (myRoot.hashCode());
    result = 31 * result + (myType.hashCode());
    return result;
  }
}
