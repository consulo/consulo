package consulo.versionControlSystem.log.impl.internal;

import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.VcsRefType;
import consulo.util.interner.Interner;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author erokhins
 */
public final class VcsRefImpl implements VcsRef {
  private static final Interner<String> ourNames = Interner.createStringInterner();
  
  private final Hash myCommitHash;
  
  private final String myName;
  
  private final VcsRefType myType;
  
  private final VirtualFile myRoot;

  public VcsRefImpl(Hash commitHash, String name, VcsRefType type, VirtualFile root) {
    myCommitHash = commitHash;
    myType = type;
    myRoot = root;
    synchronized (ourNames) {
      myName = ourNames.intern(name);
    }
  }

  @Override
  
  public VcsRefType getType() {
    return myType;
  }

  @Override
  
  public Hash getCommitHash() {
    return myCommitHash;
  }

  @Override
  
  public String getName() {
    return myName;
  }

  @Override
  
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
