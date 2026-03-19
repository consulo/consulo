package consulo.versionControlSystem.log.impl.internal.data;

import consulo.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.versionControlSystem.log.*;
import consulo.virtualFileSystem.VirtualFile;
import gnu.trove.TIntObjectHashMap;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RefsModel implements VcsLogRefs {
  private static final Logger LOG = Logger.getInstance(RefsModel.class);

  
  private final VcsLogStorage myHashMap;
  
  private final Map<VirtualFile, CompressedRefs> myRefs;
  
  private final TIntObjectHashMap<VcsRef> myBestRefForHead;
  
  private final TIntObjectHashMap<VirtualFile> myRootForHead;

  public RefsModel(Map<VirtualFile, CompressedRefs> refs,
                   Set<Integer> heads,
                   VcsLogStorage hashMap,
                   Map<VirtualFile, VcsLogProvider> providers) {
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

  public @Nullable VcsRef bestRefToHead(int headIndex) {
    return myBestRefForHead.get(headIndex);
  }

  
  public VirtualFile rootAtHead(int headIndex) {
    return myRootForHead.get(headIndex);
  }

  
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
  
  public Collection<VcsRef> getBranches() {
    return myRefs.values().stream().flatMap(CompressedRefs::streamBranches).collect(Collectors.toList());
  }

  
  public Stream<VcsRef> stream() {
    assert !ApplicationManager.getApplication().isDispatchThread();
    return myRefs.values().stream().flatMap(CompressedRefs::stream);
  }
}
