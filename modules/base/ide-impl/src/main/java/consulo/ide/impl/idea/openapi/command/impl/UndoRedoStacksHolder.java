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
package consulo.ide.impl.idea.openapi.command.impl;

import consulo.document.DocumentReference;
import consulo.document.DocumentReferenceManager;
import consulo.document.Document;
import consulo.util.collection.WeakList;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import java.util.*;

class UndoRedoStacksHolder {
  private final Key<LinkedList<UndoableGroup>> STACK_IN_DOCUMENT_KEY = Key.create("STACK_IN_DOCUMENT_KEY");

  private final boolean myUndo;

  private final LinkedList<UndoableGroup> myGlobalStack = new LinkedList<UndoableGroup>();
  private final Map<DocumentReference, LinkedList<UndoableGroup>> myDocumentStacks = new HashMap<DocumentReference, LinkedList<UndoableGroup>>();
  private final WeakList<Document> myDocumentsWithStacks = new WeakList<Document>();

  public UndoRedoStacksHolder(boolean isUndo) {
    myUndo = isUndo;
  }

  @Nonnull
  LinkedList<UndoableGroup> getStack(@Nonnull DocumentReference r) {
    return r.getFile() != null ? doGetStackForFile(r) : doGetStackForDocument(r);
  }

  @Nonnull
  private LinkedList<UndoableGroup> doGetStackForFile(@Nonnull DocumentReference r) {
    LinkedList<UndoableGroup> result = myDocumentStacks.get(r);
    if (result == null) {
      result = new LinkedList<UndoableGroup>();
      myDocumentStacks.put(r, result);
    }
    return result;
  }

  @Nonnull
  private LinkedList<UndoableGroup> doGetStackForDocument(@Nonnull DocumentReference r) {
    // If document is not associated with file, we have to store its stack in document
    // itself to avoid memory leaks caused by holding stacks of all documents, ever created, here.
    // And to know, what documents do exist now, we have to maintain weak reference list of them.

    Document d = r.getDocument();
    LinkedList<UndoableGroup> result = d.getUserData(STACK_IN_DOCUMENT_KEY);
    if (result == null) {
      result = new LinkedList<UndoableGroup>();
      d.putUserData(STACK_IN_DOCUMENT_KEY, result);
      myDocumentsWithStacks.add(d);
    }
    return result;
  }

  public boolean canBeUndoneOrRedone(@Nonnull Collection<DocumentReference> refs) {
    if (refs.isEmpty()) return !myGlobalStack.isEmpty() && myGlobalStack.getLast().isValid();
    for (DocumentReference each : refs) {
      if (!getStack(each).isEmpty() && getStack(each).getLast().isValid()) return true;
    }
    return false;
  }

  @Nonnull
  public UndoableGroup getLastAction(Collection<DocumentReference> refs) {
    if (refs.isEmpty()) return myGlobalStack.getLast();

    UndoableGroup mostRecentAction = null;
    int mostRecentDocTimestamp = myUndo ? -1 : Integer.MAX_VALUE;

    for (DocumentReference each : refs) {
      LinkedList<UndoableGroup> stack = getStack(each);
      // the stack for a document can be empty in case of compound editors with several documents
      if (stack.isEmpty()) continue;
      UndoableGroup lastAction = stack.getLast();

      int timestamp = lastAction.getCommandTimestamp();
      if (myUndo ? timestamp > mostRecentDocTimestamp : timestamp < mostRecentDocTimestamp) {
        mostRecentAction = lastAction;
        mostRecentDocTimestamp = timestamp;
      }
    }

    // result must not be null
    return mostRecentAction;
  }

  @Nonnull
  public Set<DocumentReference> collectClashingActions(@Nonnull UndoableGroup group) {
    Set<DocumentReference> result = new HashSet<DocumentReference>();

    for (DocumentReference each : group.getAffectedDocuments()) {
      UndoableGroup last = getStack(each).getLast();
      if (last != group) {
        result.addAll(last.getAffectedDocuments());
      }
    }

    if (group.isGlobal()) {
      UndoableGroup last = myGlobalStack.getLast();
      if (last != group) {
        result.addAll(last.getAffectedDocuments());
      }
    }

    return result;
  }

  public void addToStacks(@Nonnull UndoableGroup group) {
    for (LinkedList<UndoableGroup> each : getAffectedStacks(group)) {
      doAddToStack(each, group, each == myGlobalStack ? UndoManagerImpl.getGlobalUndoLimit() : UndoManagerImpl.getDocumentUndoLimit());
    }
  }

  private void doAddToStack(@Nonnull LinkedList<UndoableGroup> stack, @Nonnull UndoableGroup group, int limit) {
    if (!group.isUndoable() && stack.isEmpty()) return;

    stack.addLast(group);
    while (stack.size() > limit) {
      clearStacksFrom(stack.getFirst());
    }
  }

  public void removeFromStacks(@Nonnull UndoableGroup group) {
    for (LinkedList<UndoableGroup> each : getAffectedStacks(group)) {
      assert each.getLast() == group;
      each.removeLast();
    }
  }

  public void clearStacks(boolean clearGlobal, @Nonnull Set<DocumentReference> refs) {
    for (LinkedList<UndoableGroup> each : getAffectedStacks(clearGlobal, refs)) {
      while(!each.isEmpty()) {
        clearStacksFrom(each.getLast());
      }
    }

    Set<DocumentReference> stacksToDrop = new HashSet<DocumentReference>();
    for (Map.Entry<DocumentReference, LinkedList<UndoableGroup>> each : myDocumentStacks.entrySet()) {
      if (each.getValue().isEmpty()) stacksToDrop.add(each.getKey());
    }
    for (DocumentReference each : stacksToDrop) {
      myDocumentStacks.remove(each);
    }


    Set<Document> docsToDrop = new HashSet<Document>();
    for (Document each : myDocumentsWithStacks) {
      LinkedList<UndoableGroup> stack = each.getUserData(STACK_IN_DOCUMENT_KEY);
      if (stack != null && stack.isEmpty()) {
        each.putUserData(STACK_IN_DOCUMENT_KEY, null);
        docsToDrop.add(each);
      }
    }
    myDocumentsWithStacks.removeAll(docsToDrop);
  }

  private void clearStacksFrom(@Nonnull UndoableGroup from) {
    for (LinkedList<UndoableGroup> each : getAffectedStacks(from)) {
      int pos = each.indexOf(from);
      if (pos == -1) continue;

      if (pos > 0) {
        int top = each.size() - pos;
        clearStacksFrom(each.get(pos - 1));
        assert each.size() == top && each.indexOf(from) == 0;
      }
      each.removeFirst();
    }
  }

  @Nonnull
  private List<LinkedList<UndoableGroup>> getAffectedStacks(@Nonnull UndoableGroup group) {
    return getAffectedStacks(group.isGlobal(), group.getAffectedDocuments());
  }

  @Nonnull
  private List<LinkedList<UndoableGroup>> getAffectedStacks(boolean global, @Nonnull Collection<DocumentReference> refs) {
    List<LinkedList<UndoableGroup>> result = new ArrayList<LinkedList<UndoableGroup>>(refs.size() + 1);
    if (global) result.add(myGlobalStack);
    for (DocumentReference each : refs) {
      result.add(getStack(each));
    }
    return result;
  }

  public void clearAllStacksInTests() {
    clearStacks(true, getAffectedDocuments());
  }

  public void collectAllAffectedDocuments(@Nonnull Collection<DocumentReference> result) {
    for (UndoableGroup each : myGlobalStack) {
      result.addAll(each.getAffectedDocuments());
    }
    collectLocalAffectedDocuments(result);
  }

  private void collectLocalAffectedDocuments(@Nonnull Collection<DocumentReference> result) {
    result.addAll(myDocumentStacks.keySet());
    for (Document each : myDocumentsWithStacks) {
      result.add(DocumentReferenceManager.getInstance().create(each));
    }
  }

  @Nonnull
  private Set<DocumentReference> getAffectedDocuments() {
    Set<DocumentReference> result = new HashSet<DocumentReference>();
    collectAllAffectedDocuments(result);
    return result;
  }

  public int getLastCommandTimestamp(@Nonnull DocumentReference r) {
    LinkedList<UndoableGroup> stack = getStack(r);
    if (stack.isEmpty()) return 0;
    return Math.max(stack.getFirst().getCommandTimestamp(), stack.getLast().getCommandTimestamp());
  }

  public void invalidateActionsFor(@Nonnull DocumentReference ref) {
    for (LinkedList<UndoableGroup> eachStack : getAffectedStacks(true, Collections.singleton(ref))) {
      for (UndoableGroup eachGroup : eachStack) {
        eachGroup.invalidateActionsFor(ref);
      }
    }
  }
}
