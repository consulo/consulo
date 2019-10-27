// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.pom.PomManager;
import com.intellij.pom.PomModel;
import com.intellij.pom.event.PomModelEvent;
import com.intellij.pom.impl.PomTransactionBase;
import com.intellij.pom.tree.TreeAspect;
import com.intellij.pom.tree.TreeAspectEvent;
import com.intellij.pom.tree.events.impl.TreeChangeEventImpl;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.util.diff.DiffTreeChangeBuilder;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class DiffLog implements DiffTreeChangeBuilder<ASTNode, ASTNode> {
  public DiffLog() {
  }

  private abstract static class LogEntry {
    protected LogEntry() {
      ProgressIndicatorProvider.checkCanceled();
    }

    abstract void doActualPsiChange(@Nonnull PsiFile file, @Nonnull TreeChangeEventImpl event);
  }

  private final List<LogEntry> myEntries = new ArrayList<>();

  @Nonnull
  public TreeChangeEventImpl performActualPsiChange(@Nonnull PsiFile file) {
    TreeAspect modelAspect = PomManager.getModel(file.getProject()).getModelAspect(TreeAspect.class);
    TreeChangeEventImpl event = new TreeChangeEventImpl(modelAspect, ((PsiFileImpl)file).calcTreeElement());
    for (LogEntry entry : myEntries) {
      entry.doActualPsiChange(file, event);
    }
    file.subtreeChanged();
    return event;
  }

  @Override
  public void nodeReplaced(@Nonnull ASTNode oldNode, @Nonnull ASTNode newNode) {
    if (oldNode instanceof FileElement && newNode instanceof FileElement) {
      appendReplaceFileElement((FileElement)oldNode, (FileElement)newNode);
    }
    else {
      myEntries.add(new ReplaceEntry(oldNode, newNode));
    }
  }

  void appendReplaceElementWithEvents(@Nonnull CompositeElement oldRoot, @Nonnull CompositeElement newRoot) {
    myEntries.add(new ReplaceElementWithEvents(oldRoot, newRoot));
  }

  void appendReplaceFileElement(@Nonnull FileElement oldNode, @Nonnull FileElement newNode) {
    myEntries.add(new ReplaceFileElement(oldNode, newNode));
  }

  @Override
  public void nodeDeleted(@Nonnull ASTNode oldParent, @Nonnull ASTNode oldNode) {
    myEntries.add(new DeleteEntry(oldParent, oldNode));
  }

  @Override
  public void nodeInserted(@Nonnull ASTNode oldParent, @Nonnull ASTNode newNode, int pos) {
    myEntries.add(new InsertEntry(oldParent, newNode, pos));
  }

  private static class ReplaceEntry extends LogEntry {
    private final TreeElement myOldChild;
    private final TreeElement myNewChild;

    private ReplaceEntry(@Nonnull ASTNode oldNode, @Nonnull ASTNode newNode) {
      myOldChild = (TreeElement)oldNode;
      myNewChild = (TreeElement)newNode;
      ASTNode parent = oldNode.getTreeParent();
      assert parent != null : "old:" + oldNode + " new:" + newNode;
    }

    @Override
    void doActualPsiChange(@Nonnull PsiFile file, @Nonnull TreeChangeEventImpl changeEvent) {
      CompositeElement parent = myOldChild.getTreeParent();
      assert parent != null : "old:" + myOldChild + " new:" + myNewChild;

      final PsiElement psiParent = parent.getPsi();
      final PsiElement psiOldChild = file.isPhysical() ? myOldChild.getPsi() : null;
      if (psiParent != null && psiOldChild != null) {
        final PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(file.getManager());
        event.setParent(psiParent);
        event.setFile(file);
        event.setOldChild(psiOldChild);
        PsiElement psiNewChild = getPsi(myNewChild, file);
        event.setNewChild(psiNewChild);
        ((PsiManagerEx)file.getManager()).beforeChildReplacement(event);
      }

      if (!(myOldChild instanceof FileElement) || !(myNewChild instanceof FileElement)) {
        changeEvent.addElementaryChange(myOldChild.getTreeParent());
      }

      myNewChild.rawRemove();
      myOldChild.rawReplaceWithList(myNewChild);

      myNewChild.clearCaches();
      if (!(myNewChild instanceof FileElement)) {
        myNewChild.getTreeParent().subtreeChanged();
      }

      DebugUtil.checkTreeStructure(parent);
    }
  }

  private static class DeleteEntry extends LogEntry {
    @Nonnull
    private final CompositeElement myOldParent;
    @Nonnull
    private final TreeElement myOldNode;

    private DeleteEntry(@Nonnull ASTNode oldParent, @Nonnull ASTNode oldNode) {
      myOldParent = (CompositeElement)oldParent;
      myOldNode = (TreeElement)oldNode;
    }

    @Override
    void doActualPsiChange(@Nonnull PsiFile file, @Nonnull TreeChangeEventImpl changeEvent) {
      PsiElement psiParent = myOldParent.getPsi();
      PsiElement psiChild = file.isPhysical() ? myOldNode.getPsi() : null;

      if (psiParent != null && psiChild != null) {
        PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(file.getManager());
        event.setParent(psiParent);
        event.setChild(psiChild);
        event.setFile(file);
        ((PsiManagerEx)file.getManager()).beforeChildRemoval(event);
      }

      changeEvent.addElementaryChange(myOldParent);

      myOldNode.rawRemove();
      myOldParent.subtreeChanged();

      DebugUtil.checkTreeStructure(myOldParent);
    }
  }

  private static class InsertEntry extends LogEntry {
    @Nonnull
    private final CompositeElement myOldParent;
    @Nonnull
    private final TreeElement myNewNode;
    private final int myPos;

    private InsertEntry(@Nonnull ASTNode oldParent, @Nonnull ASTNode newNode, int pos) {
      assert pos >= 0 : pos;
      myOldParent = (CompositeElement)oldParent;
      myNewNode = (TreeElement)newNode;
      myPos = pos;
    }

    @Override
    void doActualPsiChange(@Nonnull PsiFile file, @Nonnull TreeChangeEventImpl changeEvent) {
      TreeElement anchor = null;
      TreeElement firstChildNode = myOldParent.getFirstChildNode();
      for (int i = 0; i < myPos; i++) {
        anchor = anchor == null ? firstChildNode : anchor.getTreeNext();
      }

      PsiElement psiParent = myOldParent.getPsi();
      PsiElement psiChild = getPsi(myNewNode, file);
      if (psiParent != null && psiChild != null) {
        PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(file.getManager());
        event.setParent(psiParent);
        event.setChild(psiChild);
        event.setFile(file);
        ((PsiManagerEx)file.getManager()).beforeChildAddition(event);
      }

      changeEvent.addElementaryChange(myOldParent);

      myNewNode.rawRemove();
      if (anchor != null) {
        anchor.rawInsertAfterMe(myNewNode);
      }
      else {
        if (firstChildNode != null) {
          firstChildNode.rawInsertBeforeMe(myNewNode);
        }
        else {
          myOldParent.rawAddChildren(myNewNode);
        }
      }

      myNewNode.clearCaches();
      myOldParent.subtreeChanged();

      DebugUtil.checkTreeStructure(myOldParent);
    }
  }

  private static PsiElement getPsi(ASTNode node, PsiFile file) {
    node.putUserData(TreeUtil.CONTAINING_FILE_KEY_AFTER_REPARSE, ((PsiFileImpl)file).getTreeElement());
    PsiElement psiChild = file.isPhysical() ? node.getPsi() : null;
    node.putUserData(TreeUtil.CONTAINING_FILE_KEY_AFTER_REPARSE, null);
    return psiChild;
  }

  private static class ReplaceFileElement extends LogEntry {
    @Nonnull
    private final FileElement myOldNode;
    @Nonnull
    private final FileElement myNewNode;

    private ReplaceFileElement(@Nonnull FileElement oldNode, @Nonnull FileElement newNode) {
      myOldNode = oldNode;
      myNewNode = newNode;
    }

    @Override
    void doActualPsiChange(@Nonnull PsiFile file, @Nonnull TreeChangeEventImpl event) {
      PsiFileImpl fileImpl = (PsiFileImpl)file;
      final int oldLength = myOldNode.getTextLength();
      PsiManagerImpl manager = (PsiManagerImpl)fileImpl.getManager();
      BlockSupportImpl.sendBeforeChildrenChangeEvent(manager, fileImpl, false);
      if (myOldNode.getFirstChildNode() != null) myOldNode.rawRemoveAllChildren();
      final TreeElement firstChildNode = myNewNode.getFirstChildNode();
      if (firstChildNode != null) myOldNode.rawAddChildren(firstChildNode);
      fileImpl.calcTreeElement().setCharTable(myNewNode.getCharTable());
      myOldNode.subtreeChanged();
      BlockSupportImpl.sendAfterChildrenChangedEvent(manager, fileImpl, oldLength, false);
    }
  }

  private static class ReplaceElementWithEvents extends LogEntry {
    @Nonnull
    private final CompositeElement myOldRoot;
    @Nonnull
    private final CompositeElement myNewRoot;

    private ReplaceElementWithEvents(@Nonnull CompositeElement oldRoot, @Nonnull CompositeElement newRoot) {
      myOldRoot = oldRoot;
      myNewRoot = newRoot;
      // parse in background to reduce time spent in EDT and to ensure the newRoot light containing file is still valid
      TreeUtil.ensureParsed(myOldRoot.getFirstChildNode());
      TreeUtil.ensureParsed(myNewRoot.getFirstChildNode());
    }

    @Override
    void doActualPsiChange(@Nonnull PsiFile file, @Nonnull TreeChangeEventImpl event) {
      myOldRoot.replaceAllChildrenToChildrenOf(myNewRoot);
    }
  }

  public void doActualPsiChange(@Nonnull PsiFile file) {
    CodeStyleManager.getInstance(file.getProject()).performActionWithFormatterDisabled((Runnable)() -> {
      FileViewProvider viewProvider = file.getViewProvider();
      synchronized (((AbstractFileViewProvider)viewProvider).getFilePsiLock()) {
        viewProvider.beforeContentsSynchronized();

        final Document document = viewProvider.getDocument();
        PsiDocumentManagerBase documentManager = (PsiDocumentManagerBase)PsiDocumentManager.getInstance(file.getProject());
        PsiToDocumentSynchronizer.DocumentChangeTransaction transaction = documentManager.getSynchronizer().getTransaction(document);

        if (transaction == null) {
          final PomModel model = PomManager.getModel(file.getProject());

          model.runTransaction(new PomTransactionBase(file, model.getModelAspect(TreeAspect.class)) {
            @Override
            public PomModelEvent runInner() {
              return new TreeAspectEvent(model, performActualPsiChange(file));
            }
          });
        }
        else {
          performActualPsiChange(file);
        }
      }
    });
  }
}
