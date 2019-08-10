package com.intellij.vcs.log.data;

import com.intellij.openapi.application.ApplicationManager;
import consulo.logging.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.log.*;
import gnu.trove.TIntObjectHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RefsModel implements VcsLogRefs {
  private static final Logger LOG = Logger.getInstance(RefsModel.class);

  @Nonnull
  private final VcsLogStorage myHashMap;
  @Nonnull
  private final Map<VirtualFile, CompressedRefs> myRefs;
  @Nonnull
  private final TIntObjectHashMap<VcsRef> myBestRefForHead;
  @Nonnull
  private final TIntObjectHashMap<VirtualFile> myRootForHead;

  public RefsModel(@Nonnull Map<VirtualFile, CompressedRefs> refs,
                   @Nonnull Set<Integer> heads,
                   @Nonnull VcsLogStorage hashMap,
                   @Nonnull Map<VirtualFile, VcsLogProvider> providers) {
    myRefs = refs;
    myHashMap = hashMap;

    myBestRefForHead = new TIntObjectHashMap<>();
    myRootForHead = new TIntObjectHashMap<>();
    for (int head : heads) {
      CommitId commitId = myHashMap.getCommitId(head);
      if (commitId != null) {
        VirtualFile root = commitId.getRoot();
        myRootForHead.put(head, root);
        Optional<VcsRef> bestRef =
          myRefs.get(root).refsToCommit(head).stream().min(providers.get(root).getReferenceManager().getBranchLayoutComparator());
        if (bestRef.isPresent()) {
          myBestRefForHead.put(head, bestRef.get());
        }
        else {
          LOG.warn("No references at head " + commitId);
        }
      }
    }
  }

  @Nullable
  public VcsRef bestRefToHead(int headIndex) {
    return myBestRefForHead.get(headIndex);
  }

  @Nonnull
  public VirtualFile rootAtHead(int headIndex) {
    return myRootForHead.get(headIndex);
  }

  @Nonnull
  public Map<VirtualFile, CompressedRefs> getAllRefsByRoot() {
    return myRefs;
  }

  public Collection<VcsRef> refsToCommit(int index) {
    CommitId id = myHashMap.getCommitId(index);
    if (id == null) return Collections.emptyList();
    VirtualFile root = id.getRoot();
    return myRefs.get(root).refsToCommit(index);
  }

  @Override
  @Nonnull
  public Collection<VcsRef> getBranches() {
    return myRefs.values().stream().flatMap(CompressedRefs::streamBranches).collect(Collectors.toList());
  }

  @Nonnull
  public Stream<VcsRef> stream() {
    assert !ApplicationManager.getApplication().isDispatchThread();
    return myRefs.values().stream().flatMap(CompressedRefs::stream);
  }
}
