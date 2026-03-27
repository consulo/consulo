/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.language.impl.internal.psi;

import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBus;
import consulo.document.Document;
import consulo.document.internal.DocumentEx;
import consulo.document.util.TextRange;
import consulo.document.DocumentWindow;
import consulo.language.impl.psi.ForeignLeafPsiElement;
import consulo.language.psi.PsiFileEx;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.event.PsiTreeChangeAdapter;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.language.internal.ExternalChangeActionUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.*;
import org.jetbrains.annotations.TestOnly;

import org.jspecify.annotations.Nullable;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class PsiToDocumentSynchronizer extends PsiTreeChangeAdapter {
  private static final Logger LOG = Logger.getInstance(PsiToDocumentSynchronizer.class);
  private static final Key<Boolean> PSI_DOCUMENT_ATOMIC_ACTION = Key.create("PSI_DOCUMENT_ATOMIC_ACTION");

  private final PsiDocumentManagerBase myPsiDocumentManager;
  private final MessageBus myBus;
  private final Map<Document, Pair<DocumentChangeTransaction, Integer>> myTransactionsMap = ContainerUtil.newConcurrentMap();

  private volatile Document mySyncDocument;

  PsiToDocumentSynchronizer(PsiDocumentManagerBase psiDocumentManager, MessageBus bus) {
    myPsiDocumentManager = psiDocumentManager;
    myBus = bus;
  }

  public @Nullable DocumentChangeTransaction getTransaction(Document document) {
    Pair<DocumentChangeTransaction, Integer> pair = myTransactionsMap.get(document);
    return Pair.getFirst(pair);
  }

  public boolean isInSynchronization(Document document) {
    return mySyncDocument == document;
  }

  @TestOnly
  void cleanupForNextTest() {
    myTransactionsMap.clear();
    mySyncDocument = null;
  }

  @FunctionalInterface
  private interface DocSyncAction {
    void syncDocument(Document document, PsiTreeChangeEventImpl event);
  }

  private void checkPsiModificationAllowed(PsiTreeChangeEvent event) {
    if (!toProcessPsiEvent()) return;
    PsiFile psiFile = event.getFile();
    if (!(psiFile instanceof PsiFileEx) || !((PsiFileEx)psiFile).isContentsLoaded()) return;

    Document document = myPsiDocumentManager.getCachedDocument(psiFile);
    if (document != null && myPsiDocumentManager.isUncommited(document)) {
      throw new IllegalStateException("Attempt to modify PSI for non-committed Document!");
    }
  }

  private DocumentEx getCachedDocument(PsiFile psiFile, boolean force) {
    DocumentEx document = (DocumentEx)myPsiDocumentManager.getCachedDocument(psiFile);
    if (document == null || document instanceof DocumentWindow || !force && getTransaction(document) == null) {
      return null;
    }
    return document;
  }

  private void doSync(PsiTreeChangeEvent event, boolean force, DocSyncAction syncAction) {
    if (!toProcessPsiEvent()) return;
    PsiFile psiFile = event.getFile();
    if (!(psiFile instanceof PsiFileEx) || !((PsiFileEx)psiFile).isContentsLoaded()) return;

    DocumentEx document = getCachedDocument(psiFile, force);
    if (document == null) return;

    performAtomically(psiFile, () -> syncAction.syncDocument(document, (PsiTreeChangeEventImpl)event));

    boolean insideTransaction = myTransactionsMap.containsKey(document);
    if (!insideTransaction) {
      document.setModificationStamp(psiFile.getViewProvider().getModificationStamp());
    }
  }

  static boolean isInsideAtomicChange(PsiFile file) {
    return Objects.equals(file.getUserData(PSI_DOCUMENT_ATOMIC_ACTION), Boolean.TRUE);
  }

  public static void performAtomically(PsiFile file, Runnable runnable) {
    PsiUtilCore.ensureValid(file);
    assert !isInsideAtomicChange(file);
    file.putUserData(PSI_DOCUMENT_ATOMIC_ACTION, Boolean.TRUE);

    try {
      runnable.run();
    }
    finally {
      file.putUserData(PSI_DOCUMENT_ATOMIC_ACTION, null);
    }
  }

  @Override
  public void beforeChildAddition(PsiTreeChangeEvent event) {
    checkPsiModificationAllowed(event);
  }

  @Override
  public void beforeChildRemoval(PsiTreeChangeEvent event) {
    checkPsiModificationAllowed(event);
  }

  @Override
  public void beforeChildReplacement(PsiTreeChangeEvent event) {
    checkPsiModificationAllowed(event);
  }

  @Override
  public void beforeChildrenChange(PsiTreeChangeEvent event) {
    checkPsiModificationAllowed(event);
  }

  @Override
  public void childAdded(PsiTreeChangeEvent event) {
    if (!(event.getChild() instanceof ForeignLeafPsiElement)) {
      doSync(event, false, (document, event1) -> insertString(document, event1.getOffset(), event1.getChild().getText()));
    }
  }

  @Override
  public void childRemoved(PsiTreeChangeEvent event) {
    if (!(event.getChild() instanceof ForeignLeafPsiElement)) {
      doSync(event, false, (document, event1) -> deleteString(document, event1.getOffset(), event1.getOffset() + event1.getOldLength()));
    }
  }

  @Override
  public void childReplaced(PsiTreeChangeEvent event) {
    doSync(event, false, (document, event1) -> {
      int oldLength = event1.getOldChild() instanceof ForeignLeafPsiElement ? 0 : event1.getOldLength();
      String newText = event1.getNewChild() instanceof ForeignLeafPsiElement ? "" : event1.getNewChild().getText();
      replaceString(document, event1.getOffset(), event1.getOffset() + oldLength, newText, event1.getNewChild());
    });
  }

  @Override
  public void childrenChanged(PsiTreeChangeEvent event) {
    doSync(event, false, (document, event1) -> replaceString(document, event1.getOffset(), event1.getOffset() + event1.getOldLength(), event1.getParent().getText(), event1.getParent()));
  }

  private boolean myIgnorePsiEvents;

  public void setIgnorePsiEvents(boolean ignorePsiEvents) {
    myIgnorePsiEvents = ignorePsiEvents;
  }

  public boolean isIgnorePsiEvents() {
    return myIgnorePsiEvents;
  }

  public boolean toProcessPsiEvent() {
    return !myIgnorePsiEvents && !myPsiDocumentManager.isCommitInProgress() && !ExternalChangeActionUtil.isExternalChangeInProgress();
  }

  @TestOnly
  public void replaceString(Document document, int startOffset, int endOffset, String s) {
    replaceString(document, startOffset, endOffset, s, null);
  }

  private void replaceString(Document document, int startOffset, int endOffset, String s, @Nullable PsiElement replacement) {
    DocumentChangeTransaction documentChangeTransaction = getTransaction(document);
    if (documentChangeTransaction != null) {
      documentChangeTransaction.replace(startOffset, endOffset - startOffset, s, replacement);
    }
  }

  public void insertString(Document document, int offset, String s) {
    DocumentChangeTransaction documentChangeTransaction = getTransaction(document);
    if (documentChangeTransaction != null) {
      documentChangeTransaction.replace(offset, 0, s, null);
    }
  }

  private void deleteString(Document document, int startOffset, int endOffset) {
    DocumentChangeTransaction documentChangeTransaction = getTransaction(document);
    if (documentChangeTransaction != null) {
      documentChangeTransaction.replace(startOffset, endOffset - startOffset, "", null);
    }
  }

  public void startTransaction(Project project, Document doc, PsiElement scope) {
    LOG.assertTrue(!project.isDisposed());
    Pair<DocumentChangeTransaction, Integer> pair = myTransactionsMap.get(doc);
    Pair<DocumentChangeTransaction, Integer> prev = pair;
    if (pair == null) {
      PsiFile psiFile = scope.getContainingFile();
      pair = new Pair<>(new DocumentChangeTransaction(doc, psiFile), 0);
      myBus.syncPublisher(PsiDocumentTransactionListener.class).transactionStarted(doc, psiFile);
    }
    else {
      pair = new Pair<>(pair.getFirst(), pair.getSecond().intValue() + 1);
    }
    LOG.assertTrue(myTransactionsMap.put(doc, pair) == prev);
  }

  public boolean commitTransaction(Document document) {
    ApplicationManager.getApplication().assertIsWriteThread();
    DocumentChangeTransaction documentChangeTransaction = removeTransaction(document);
    if (documentChangeTransaction == null) return false;
    PsiFile changeScope = documentChangeTransaction.myChangeScope;
    try {
      mySyncDocument = document;

      PsiTreeChangeEventImpl fakeEvent = new PsiTreeChangeEventImpl(changeScope.getManager());
      fakeEvent.setParent(changeScope);
      fakeEvent.setFile(changeScope);
      checkPsiModificationAllowed(fakeEvent);
      doSync(fakeEvent, true, (document1, event) -> doCommitTransaction(document1, documentChangeTransaction));
      myBus.syncPublisher(PsiDocumentTransactionListener.class).transactionCompleted(document, changeScope);
    }
    catch (Throwable e) {
      myPsiDocumentManager.forceReload(changeScope.getViewProvider().getVirtualFile(), changeScope.getViewProvider());
      ExceptionUtil.rethrowAllAsUnchecked(e);
    }
    finally {
      mySyncDocument = null;
    }
    return true;
  }

  private static void doCommitTransaction(Document document, DocumentChangeTransaction documentChangeTransaction) {
    DocumentEx ex = (DocumentEx)document;
    ex.suppressGuardedExceptions();
    try {
      boolean isReadOnly = !document.isWritable();
      ex.setReadOnly(false);

      for (Map.Entry<TextRange, CharSequence> entry : documentChangeTransaction.myAffectedFragments.descendingMap().entrySet()) {
        ex.replaceString(entry.getKey().getStartOffset(), entry.getKey().getEndOffset(), entry.getValue());
      }

      ex.setReadOnly(isReadOnly);
    }
    finally {
      ex.unSuppressGuardedExceptions();
    }
  }

  private @Nullable DocumentChangeTransaction removeTransaction(Document doc) {
    Pair<DocumentChangeTransaction, Integer> pair = myTransactionsMap.get(doc);
    if (pair == null) return null;
    int nestedCount = pair.getSecond().intValue();
    if (nestedCount > 0) {
      pair = Pair.create(pair.getFirst(), nestedCount - 1);
      myTransactionsMap.put(doc, pair);
      return null;
    }
    myTransactionsMap.remove(doc);
    return pair.getFirst();
  }

  public boolean isDocumentAffectedByTransactions(Document document) {
    return myTransactionsMap.containsKey(document);
  }

  public static class DocumentChangeTransaction {
    private final TreeMap<TextRange, CharSequence> myAffectedFragments = new TreeMap<>(Comparator.comparingInt(TextRange::getStartOffset));
    private final PsiFile myChangeScope;
    private ImmutableCharSequence myPsiText;

    DocumentChangeTransaction(Document doc, PsiFile scope) {
      myChangeScope = scope;
      myPsiText = CharArrayUtil.createImmutableCharSequence(doc.getImmutableCharSequence());
    }

    
    public Map<TextRange, CharSequence> getAffectedFragments() {
      return myAffectedFragments;
    }

    public void replace(int psiStart, int length, String replace, @Nullable PsiElement replacement) {
      // calculating fragment
      // minimize replace
      int start = 0;
      int end = start + length;

      CharSequence chars = myPsiText.subSequence(psiStart, psiStart + length);
      if (StringUtil.equals(chars, replace)) return;

      int newStartInReplace = 0;
      int replaceLength = replace.length();
      while (newStartInReplace < replaceLength && start < end && replace.charAt(newStartInReplace) == chars.charAt(start)) {
        start++;
        newStartInReplace++;
      }

      int newEndInReplace = replaceLength;
      while (start < end && newStartInReplace < newEndInReplace && replace.charAt(newEndInReplace - 1) == chars.charAt(end - 1)) {
        newEndInReplace--;
        end--;
      }

      // increase the changed range to start and end on PSI token boundaries
      // this will help to survive smart pointers with the same boundaries
      if (replacement != null && (newStartInReplace > 0 || newEndInReplace < replaceLength)) {
        PsiElement startLeaf = replacement.findElementAt(newStartInReplace);
        PsiElement endLeaf = replacement.findElementAt(newEndInReplace - 1);
        if (startLeaf != null && endLeaf != null) {
          int leafStart = startLeaf.getTextRange().getStartOffset() - replacement.getTextRange().getStartOffset();
          int leafEnd = endLeaf.getTextRange().getEndOffset() - replacement.getTextRange().getStartOffset();
          start += leafStart - newStartInReplace;
          end += leafEnd - newEndInReplace;
          newStartInReplace = leafStart;
          newEndInReplace = leafEnd;
        }
      }

      // optimization: when delete fragment from the middle of the text, prefer split at the line boundaries
      if (newStartInReplace == newEndInReplace && start > 0 && start < end && StringUtil.indexOf(chars, '\n', start, end) != -1) {
        // try to align to the line boundaries
        while (start > 0 && newStartInReplace > 0 && chars.charAt(start - 1) == chars.charAt(end - 1) && chars.charAt(end - 1) != '\n') {
          start--;
          end--;
          newStartInReplace--;
          newEndInReplace--;
        }
      }

      start += psiStart;
      end += psiStart;

      //[mike] dirty hack for xml:
      //make sure that deletion of <t> in: <tag><t/><tag> doesn't remove t/><
      //which is perfectly valid but invalidates range markers
      CharSequence charsSequence = myPsiText;
      while (start < charsSequence.length() && end < charsSequence.length() && start > 0 && charsSequence.subSequence(start, end).toString().endsWith("><") && charsSequence.charAt(start - 1) == '<') {
        start--;
        newStartInReplace--;
        end--;
        newEndInReplace--;
      }

      updateFragments(start, end, replace.substring(newStartInReplace, newEndInReplace));
    }

    private void updateFragments(int start, int end, String replace) {
      int docStart = psiToDocumentOffset(start);
      int docEnd = psiToDocumentOffset(end);

      TextRange startRange = findFragment(docStart);
      TextRange endRange = findFragment(docEnd);

      myPsiText = myPsiText.delete(start, end).insert(start, replace);

      TextRange newFragment = new TextRange(startRange != null ? startRange.getStartOffset() : docStart, endRange != null ? endRange.getEndOffset() : docEnd);
      CharSequence newReplacement =
              myPsiText.subSequence(documentToPsiOffset(newFragment.getStartOffset(), false), documentToPsiOffset(newFragment.getEndOffset(), true) + replace.length() - (end - start));

      myAffectedFragments.keySet().removeIf(range -> range.intersects(newFragment));
      myAffectedFragments.put(newFragment, newReplacement);
    }

    private TextRange findFragment(int docOffset) {
      return ContainerUtil.find(myAffectedFragments.keySet(), range -> range.containsOffset(docOffset));
    }

    private int psiToDocumentOffset(int offset) {
      for (Map.Entry<TextRange, CharSequence> entry : myAffectedFragments.entrySet()) {
        int lengthAfter = entry.getValue().length();
        TextRange range = entry.getKey();
        if (range.getStartOffset() + lengthAfter < offset) {
          offset += range.getLength() - lengthAfter;
          continue;
        }

        // for offsets inside replaced ranges, return the starts of the original affected fragments in document
        return Math.min(range.getStartOffset(), offset);
      }
      return offset;
    }

    private int documentToPsiOffset(int offset, boolean greedyRight) {
      int delta = 0;
      for (Map.Entry<TextRange, CharSequence> entry : myAffectedFragments.entrySet()) {
        int lengthAfter = entry.getValue().length();
        TextRange range = entry.getKey();
        // for offsets inside affected fragments, return either start or end of the updated range
        if (range.containsOffset(offset)) {
          return range.getStartOffset() + delta + (greedyRight ? lengthAfter : 0);
        }
        if (range.getStartOffset() > offset) {
          break;
        }
        delta += lengthAfter - range.getLength();
      }
      return offset + delta;
    }
  }
}
