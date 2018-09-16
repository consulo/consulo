/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.psi.impl.source.text;

import com.intellij.lang.ASTNode;
import com.intellij.lang.FileASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.DocumentBulkUpdateListener;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.PsiTreeChangeEventImpl;
import com.intellij.psi.impl.source.DummyHolder;
import com.intellij.psi.impl.source.DummyHolderFactory;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.tree.*;
import com.intellij.psi.templateLanguages.ITemplateDataElementType;
import com.intellij.psi.text.BlockSupport;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IReparseableElementType;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.CharTable;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.diff.DiffTree;
import com.intellij.util.diff.DiffTreeChangeBuilder;
import com.intellij.util.diff.FlyweightCapableTreeStructure;
import com.intellij.util.diff.ShallowNodeComparator;
import consulo.lang.LanguageVersion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BlockSupportImpl extends BlockSupport {
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.source.text.BlockSupportImpl");

  @Inject
  public BlockSupportImpl(Project project) {
    project.getMessageBus().connect().subscribe(DocumentBulkUpdateListener.TOPIC, new DocumentBulkUpdateListener.Adapter() {
      @Override
      public void updateStarted(@Nonnull final Document doc) {
        doc.putUserData(DO_NOT_REPARSE_INCREMENTALLY, Boolean.TRUE);
      }
    });
  }

  @Override
  public void reparseRange(@Nonnull PsiFile file, int startOffset, int endOffset, @Nonnull CharSequence newText) throws IncorrectOperationException {
    LOG.assertTrue(file.isValid());
    final PsiFileImpl psiFile = (PsiFileImpl)file;
    final Document document = psiFile.getViewProvider().getDocument();
    assert document != null;
    document.replaceString(startOffset, endOffset, newText);
    PsiDocumentManager.getInstance(psiFile.getProject()).commitDocument(document);
  }

  @Override
  @Nonnull
  public DiffLog reparseRange(@Nonnull final PsiFile file,
                              @Nonnull FileASTNode oldFileNode,
                              @Nonnull TextRange changedPsiRange,
                              @Nonnull final CharSequence newFileText,
                              @Nonnull final ProgressIndicator indicator,
                              @Nonnull CharSequence lastCommittedText) {
    final PsiFileImpl fileImpl = (PsiFileImpl)file;

    final Couple<ASTNode> reparseableRoots = findReparseableRoots(fileImpl, oldFileNode, changedPsiRange, newFileText);
    return reparseableRoots != null
           ? mergeTrees(fileImpl, reparseableRoots.first, reparseableRoots.second, indicator, lastCommittedText)
           : makeFullParse(fileImpl, oldFileNode, newFileText, indicator, lastCommittedText);
  }

  /**
   * This method searches ast node that could be reparsed incrementally and returns pair of target reparseable node and new replacement node.
   * Returns null if there is no any chance to make incremental parsing.
   */
  @Nullable
  public Couple<ASTNode> findReparseableRoots(@Nonnull PsiFileImpl file, @Nonnull FileASTNode oldFileNode, @Nonnull TextRange changedPsiRange, @Nonnull CharSequence newFileText) {
    Project project = file.getProject();
    final FileElement fileElement = (FileElement)oldFileNode;
    final CharTable charTable = fileElement.getCharTable();
    int lengthShift = newFileText.length() - fileElement.getTextLength();

    if (fileElement.getElementType() instanceof ITemplateDataElementType || isTooDeep(file)) {
      // unable to perform incremental reparse for template data in JSP, or in exceptionally deep trees
      return null;
    }

    final ASTNode leafAtStart = fileElement.findLeafElementAt(Math.max(0, changedPsiRange.getStartOffset() - 1));
    final ASTNode leafAtEnd = fileElement.findLeafElementAt(Math.min(changedPsiRange.getEndOffset(), fileElement.getTextLength() - 1));
    ASTNode node = leafAtStart != null && leafAtEnd != null ? TreeUtil.findCommonParent(leafAtStart, leafAtEnd) : fileElement;
    Language baseLanguage = file.getViewProvider().getBaseLanguage();

    while (node != null && !(node instanceof FileElement)) {
      IElementType elementType = node.getElementType();
      if (elementType instanceof IReparseableElementType) {
        final TextRange textRange = node.getTextRange();
        final IReparseableElementType reparseable = (IReparseableElementType)elementType;

        if (baseLanguage.isKindOf(reparseable.getLanguage()) && textRange.getLength() + lengthShift > 0) {
          final int start = textRange.getStartOffset();
          final int end = start + textRange.getLength() + lengthShift;
          if (end > newFileText.length()) {
            reportInconsistentLength(file, newFileText, node, start, end);
            break;
          }

          CharSequence newTextStr = newFileText.subSequence(start, end);

          if (reparseable.isParsable(file, newTextStr, baseLanguage, project)) {
            ASTNode chameleon = reparseable.createNode(newTextStr);
            if (chameleon != null) {
              DummyHolder holder = DummyHolderFactory.createHolder(file.getManager(), null, node.getPsi(), charTable);
              holder.getTreeElement().rawAddChildren((TreeElement)chameleon);

              if (holder.getTextLength() != newTextStr.length()) {
                String details = ApplicationManager.getApplication().isInternal() ? "text=" + newTextStr + "; treeText=" + holder.getText() + ";" : "";
                LOG.error("Inconsistent reparse: " + details + " type=" + elementType);
              }

              return Couple.of(node, chameleon);
            }
          }
        }
      }
      node = node.getTreeParent();
    }
    return null;
  }

  private static void reportInconsistentLength(PsiFile file, CharSequence newFileText, ASTNode node, int start, int end) {
    String message =
            "Index out of bounds: type=" + node.getElementType() + "; file=" + file + "; file.class=" + file.getClass() + "; start=" + start + "; end=" + end + "; length=" + node.getTextLength();
    String newTextBefore = newFileText.subSequence(0, start).toString();
    String oldTextBefore = file.getText().subSequence(0, start).toString();
    if (oldTextBefore.equals(newTextBefore)) {
      message += "; oldTextBefore==newTextBefore";
    }
    LOG.error(message, new Attachment(file.getName() + "_oldNodeText.txt", node.getText()), new Attachment(file.getName() + "_oldFileText.txt", file.getText()),
              new Attachment(file.getName() + "_newFileText.txt", newFileText.toString()));
  }

  @Nonnull
  private static DiffLog makeFullParse(@Nonnull PsiFileImpl fileImpl,
                                       @Nonnull FileASTNode oldFileNode,
                                       @Nonnull CharSequence newFileText,
                                       @Nonnull ProgressIndicator indicator,
                                       @Nonnull CharSequence lastCommittedText) {
    if (fileImpl instanceof PsiCodeFragment) {
      FileElement parent = fileImpl.getTreeElement();
      final FileElement holderElement = new DummyHolder(fileImpl.getManager(), fileImpl.getContext()).getTreeElement();
      holderElement.rawAddChildren(fileImpl.createContentLeafElement(holderElement.getCharTable().intern(newFileText, 0, newFileText.length())));
      DiffLog diffLog = new DiffLog();
      diffLog.appendReplaceFileElement(parent, (FileElement)holderElement.getFirstChildNode());

      return diffLog;
    }
    else {
      FileViewProvider viewProvider = fileImpl.getViewProvider();
      viewProvider.getLanguages();
      FileType fileType = viewProvider.getVirtualFile().getFileType();
      String fileName = fileImpl.getName();
      final LightVirtualFile lightFile = new LightVirtualFile(fileName, fileType, newFileText, viewProvider.getVirtualFile().getCharset(), fileImpl.getViewProvider().getModificationStamp());
      lightFile.setOriginalFile(viewProvider.getVirtualFile());

      FileViewProvider copy = viewProvider.createCopy(lightFile);
      if (copy.isEventSystemEnabled()) {
        throw new AssertionError("Copied view provider must be non-physical for reparse to deliver correct events: " + viewProvider);
      }
      copy.getLanguages();
      SingleRootFileViewProvider.doNotCheckFileSizeLimit(lightFile); // optimization: do not convert file contents to bytes to determine if we should codeinsight it
      PsiFileImpl newFile = getFileCopy(fileImpl, copy);

      newFile.setOriginalFile(fileImpl);

      final FileElement newFileElement = (FileElement)newFile.getNode();
      final FileElement oldFileElement = (FileElement)oldFileNode;
      if (!lastCommittedText.toString().equals(oldFileElement.getText())) {
        throw new IncorrectOperationException(viewProvider.toString());
      }
      DiffLog diffLog = mergeTrees(fileImpl, oldFileElement, newFileElement, indicator, lastCommittedText);

      ((PsiManagerEx)fileImpl.getManager()).getFileManager().setViewProvider(lightFile, null);
      return diffLog;
    }
  }

  @Nonnull
  public static PsiFileImpl getFileCopy(@Nonnull PsiFileImpl originalFile, @Nonnull FileViewProvider providerCopy) {
    FileViewProvider viewProvider = originalFile.getViewProvider();
    Language language = originalFile.getLanguage();

    PsiFile file = providerCopy.getPsi(language);
    if (file != null && !(file instanceof PsiFileImpl)) {
      throw new RuntimeException("View provider " + viewProvider + " refused to provide PsiFileImpl for " + language + details(providerCopy, viewProvider));
    }

    PsiFileImpl newFile = (PsiFileImpl)file;

    if (newFile == null && language == PlainTextLanguage.INSTANCE && originalFile == viewProvider.getPsi(viewProvider.getBaseLanguage())) {
      newFile = (PsiFileImpl)providerCopy.getPsi(providerCopy.getBaseLanguage());
    }

    if (newFile == null) {
      throw new RuntimeException("View provider " + viewProvider + " refused to parse text with " + language + details(providerCopy, viewProvider));
    }

    return newFile;
  }

  private static String details(FileViewProvider providerCopy, FileViewProvider viewProvider) {
    return "; languages: " +
           viewProvider.getLanguages() +
           "; base: " +
           viewProvider.getBaseLanguage() +
           "; copy: " +
           providerCopy +
           "; copy.base: " +
           providerCopy.getBaseLanguage() +
           "; vFile: " +
           viewProvider.getVirtualFile() +
           "; copy.vFile: " +
           providerCopy.getVirtualFile() +
           "; fileType: " +
           viewProvider.getVirtualFile().getFileType() +
           "; copy.original(): " +
           (providerCopy.getVirtualFile() instanceof LightVirtualFile ? ((LightVirtualFile)providerCopy.getVirtualFile()).getOriginalFile() : null);
  }

  @Nonnull
  private static DiffLog replaceElementWithEvents(@Nonnull CompositeElement oldRoot, @Nonnull CompositeElement newRoot) {
    DiffLog diffLog = new DiffLog();
    diffLog.appendReplaceElementWithEvents(oldRoot, newRoot);
    return diffLog;
  }

  @Nonnull
  public static DiffLog mergeTrees(@Nonnull final PsiFileImpl fileImpl,
                                   @Nonnull final ASTNode oldRoot,
                                   @Nonnull final ASTNode newRoot,
                                   @Nonnull ProgressIndicator indicator,
                                   @Nonnull CharSequence lastCommittedText) {
    if (newRoot instanceof FileElement) {
      ((FileElement)newRoot).setCharTable(fileImpl.getTreeElement().getCharTable());
    }

    try {
      newRoot.putUserData(TREE_TO_BE_REPARSED, Pair.create(oldRoot, lastCommittedText));
      newRoot.putUserData(LanguageVersion.KEY, fileImpl.getLanguageVersion());

      if (isReplaceWholeNode(fileImpl, newRoot)) {
        DiffLog treeChangeEvent = replaceElementWithEvents((CompositeElement)oldRoot, (CompositeElement)newRoot);
        fileImpl.putUserData(TREE_DEPTH_LIMIT_EXCEEDED, Boolean.TRUE);

        return treeChangeEvent;
      }
      newRoot.getFirstChildNode();  // maybe reparsed in PsiBuilderImpl and have thrown exception here
    }
    catch (ReparsedSuccessfullyException e) {
      // reparsed in PsiBuilderImpl
      return e.getDiffLog();
    }
    finally {
      newRoot.putUserData(TREE_TO_BE_REPARSED, null);
    }

    final ASTShallowComparator comparator = new ASTShallowComparator(indicator);
    final ASTStructure treeStructure = createInterruptibleASTStructure(newRoot, indicator);

    DiffLog diffLog = new DiffLog();
    diffTrees(oldRoot, diffLog, comparator, treeStructure, indicator, lastCommittedText);
    return diffLog;
  }

  public static <T> void diffTrees(@Nonnull final ASTNode oldRoot,
                                   @Nonnull final DiffTreeChangeBuilder<ASTNode, T> builder,
                                   @Nonnull final ShallowNodeComparator<ASTNode, T> comparator,
                                   @Nonnull final FlyweightCapableTreeStructure<T> newTreeStructure,
                                   @Nonnull ProgressIndicator indicator,
                                   @Nonnull CharSequence lastCommittedText) {
    TreeUtil.ensureParsedRecursivelyCheckingProgress(oldRoot, indicator);
    DiffTree.diff(createInterruptibleASTStructure(oldRoot, indicator), newTreeStructure, comparator, builder, lastCommittedText);
  }

  private static ASTStructure createInterruptibleASTStructure(@Nonnull final ASTNode oldRoot, @Nonnull final ProgressIndicator indicator) {
    return new ASTStructure(oldRoot) {
      @Override
      public int getChildren(@Nonnull ASTNode astNode, @Nonnull Ref<ASTNode[]> into) {
        indicator.checkCanceled();
        return super.getChildren(astNode, into);
      }
    };
  }

  private static boolean isReplaceWholeNode(@Nonnull PsiFileImpl fileImpl, @Nonnull ASTNode newRoot) throws ReparsedSuccessfullyException {
    final Boolean data = fileImpl.getUserData(DO_NOT_REPARSE_INCREMENTALLY);
    if (data != null) fileImpl.putUserData(DO_NOT_REPARSE_INCREMENTALLY, null);

    boolean explicitlyMarkedDeep = Boolean.TRUE.equals(data);

    if (explicitlyMarkedDeep || isTooDeep(fileImpl)) {
      return true;
    }

    final ASTNode childNode = newRoot.getFirstChildNode();  // maybe reparsed in PsiBuilderImpl and have thrown exception here
    boolean childTooDeep = isTooDeep(childNode);
    if (childTooDeep) {
      childNode.putUserData(TREE_DEPTH_LIMIT_EXCEEDED, null);
      fileImpl.putUserData(TREE_DEPTH_LIMIT_EXCEEDED, Boolean.TRUE);
    }
    return childTooDeep;
  }

  public static void sendBeforeChildrenChangeEvent(@Nonnull PsiManagerImpl manager, @Nonnull PsiElement scope, boolean isGenericChange) {
    if (!scope.isPhysical()) {
      manager.beforeChange(false);
      return;
    }
    PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(manager);
    event.setParent(scope);
    event.setFile(scope.getContainingFile());
    TextRange range = scope.getTextRange();
    event.setOffset(range == null ? 0 : range.getStartOffset());
    event.setOldLength(scope.getTextLength());
    // the "generic" event is being sent on every PSI change. It does not carry any specific info except the fact that "something has changed"
    event.setGenericChange(isGenericChange);
    manager.beforeChildrenChange(event);
  }

  public static void sendAfterChildrenChangedEvent(@Nonnull PsiManagerImpl manager, @Nonnull PsiFile scope, int oldLength, boolean isGenericChange) {
    if (!scope.isPhysical()) {
      manager.afterChange(false);
      return;
    }
    PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(manager);
    event.setParent(scope);
    event.setFile(scope);
    event.setOffset(0);
    event.setOldLength(oldLength);
    event.setGenericChange(isGenericChange);
    manager.childrenChanged(event);
  }
}
