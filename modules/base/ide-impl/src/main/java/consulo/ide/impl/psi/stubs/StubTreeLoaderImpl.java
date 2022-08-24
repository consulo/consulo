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
package consulo.ide.impl.psi.stubs;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.util.RecursionManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.openapi.project.NoAccessDuringPsiEvents;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.indexing.FileBasedIndexImpl;
import consulo.language.psi.stub.SingleEntryFileBasedIndexExtension;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.impl.internal.psi.stub.FileContentImpl;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.psi.*;
import consulo.language.psi.internal.PsiFileWithStubSupport;
import consulo.language.psi.stub.*;
import consulo.logging.Logger;
import consulo.logging.attachment.Attachment;
import consulo.logging.attachment.AttachmentFactory;
import consulo.logging.attachment.RuntimeExceptionWithAttachments;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class StubTreeLoaderImpl extends StubTreeLoader {
  public static class UpToDateStubIndexMismatch extends RuntimeExceptionWithAttachments {
    public UpToDateStubIndexMismatch(String message, Throwable cause, Attachment... attachments) {
      super(message, cause, attachments);
    }
  }

  public static class ManyProjectsStubIndexMismatch extends RuntimeExceptionWithAttachments {
    public ManyProjectsStubIndexMismatch(String message, Throwable cause, Attachment... attachments) {
      super(message, cause, attachments);
    }
  }

  private static final Logger LOG = Logger.getInstance(StubTreeLoaderImpl.class);
  private static volatile boolean ourStubReloadingProhibited;

  private final ProjectLocator myProjectLocator;

  @Inject
  public StubTreeLoaderImpl(ProjectLocator projectLocator) {
    myProjectLocator = projectLocator;
  }

  @Override
  @Nullable
  public ObjectStubTree readOrBuild(Project project, final VirtualFile vFile, @Nullable PsiFile psiFile) {
    final ObjectStubTree fromIndices = readFromVFile(project, vFile);
    if (fromIndices != null) {
      return fromIndices;
    }

    try {
      byte[] content = vFile.contentsToByteArray();
      vFile.setPreloadedContentHint(content);
      try {
        final FileContentImpl fc = new FileContentImpl(vFile, content);
        fc.setProject(project);
        if (psiFile != null && !vFile.getFileType().isBinary()) {
          fc.putUserData(IndexingDataKeys.FILE_TEXT_CONTENT_KEY, psiFile.getViewProvider().getContents());
          // but don't reuse psiFile itself to avoid loading its contents. If we load AST, the stub will be thrown out anyway.
        }

        Stub element = RecursionManager.doPreventingRecursion(vFile, false, () -> StubTreeBuilder.buildStubTree(fc));
        ObjectStubTree tree = element instanceof PsiFileStub ? new StubTree((PsiFileStub)element) : element instanceof ObjectStubBase ? new ObjectStubTree((ObjectStubBase)element, true) : null;
        if (tree != null) {
          tree.setDebugInfo("created from file content");
          return tree;
        }
      }
      finally {
        vFile.setPreloadedContentHint(null);
      }
    }
    catch (IOException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(e);
      }
      else {
        // content can be not cached yet, and the file can be deleted on disk already, without refresh
        LOG.info("Can't load file content for stub building: " + e.getMessage());
      }
    }

    return null;
  }

  @Override
  @Nullable
  public ObjectStubTree readFromVFile(Project project, final VirtualFile vFile) {
    if (DumbService.getInstance(project).isDumb() || NoAccessDuringPsiEvents.isInsideEventProcessing()) {
      return null;
    }

    final int id = SingleEntryFileBasedIndexExtension.getFileKey(vFile);
    if (id <= 0) {
      return null;
    }

    boolean wasIndexedAlready = ((FileBasedIndexImpl)FileBasedIndex.getInstance()).isFileUpToDate(vFile);

    Document document = FileDocumentManager.getInstance().getCachedDocument(vFile);
    boolean saved = document == null || !FileDocumentManager.getInstance().isDocumentUnsaved(document);

    final Map<Integer, SerializedStubTree> datas = FileBasedIndex.getInstance().getFileData(StubUpdatingIndex.INDEX_ID, vFile, project);
    final int size = datas.size();

    if (size == 1) {
      SerializedStubTree stubTree = datas.values().iterator().next();

      if (!checkLengthMatch(project, vFile, wasIndexedAlready, document, saved)) {
        return null;
      }

      Stub stub;
      try {
        stub = stubTree.getStub(false);
      }
      catch (SerializerNotFoundException e) {
        return processError(vFile, "No stub serializer: " + vFile.getPresentableUrl() + ": " + e.getMessage(), e);
      }
      ObjectStubTree<?> tree = stub instanceof PsiFileStub ? new StubTree((PsiFileStub)stub) : new ObjectStubTree((ObjectStubBase)stub, true);
      tree.setDebugInfo("created from index");
      checkDeserializationCreatesNoPsi(tree);
      return tree;
    }
    if (size != 0) {
      return processError(vFile, "Twin stubs: " + vFile.getPresentableUrl() + " has " + size + " stub versions. Should only have one. id=" + id, null);
    }

    return null;
  }

  private boolean checkLengthMatch(Project project, VirtualFile vFile, boolean wasIndexedAlready, Document document, boolean saved) {
    PsiFile cachedPsi = PsiManagerEx.getInstanceEx(project).getFileManager().getCachedPsiFile(vFile);
    IndexingStampInfo indexingStampInfo = getIndexingStampInfo(vFile);
    if (indexingStampInfo != null && !indexingStampInfo.contentLengthMatches(vFile.getLength(), getCurrentTextContentLength(project, vFile, document, cachedPsi))) {
      diagnoseLengthMismatch(vFile, wasIndexedAlready, document, saved, cachedPsi);
      return false;
    }
    return true;
  }

  private void diagnoseLengthMismatch(VirtualFile vFile, boolean wasIndexedAlready, @Nullable Document document, boolean saved, @Nullable PsiFile cachedPsi) {
    String message = "Outdated stub in index: " +
                     vFile +
                     " " +
                     getIndexingStampInfo(vFile) +
                     ", doc=" +
                     document +
                     ", docSaved=" +
                     saved +
                     ", wasIndexedAlready=" +
                     wasIndexedAlready +
                     ", queried at " +
                     vFile.getTimeStamp();
    message += "\ndoc length=" + (document == null ? -1 : document.getTextLength()) + "\nfile length=" + vFile.getLength();
    if (cachedPsi != null) {
      message += "\ncached PSI " + cachedPsi.getClass();
      if (cachedPsi instanceof PsiFileImpl && ((PsiFileImpl)cachedPsi).isContentsLoaded()) {
        message += "\nPSI length=" + cachedPsi.getTextLength();
      }
      List<Project> projects = ContainerUtil.findAll(ProjectManager.getInstance().getOpenProjects(), p -> PsiManagerEx.getInstanceEx(p).getFileManager().findCachedViewProvider(vFile) != null);
      message += "\nprojects with file: " + (LOG.isDebugEnabled() ? projects.toString() : projects.size());
    }

    processError(vFile, message, new Exception());
  }

  private static void checkDeserializationCreatesNoPsi(ObjectStubTree<?> tree) {
    if (ourStubReloadingProhibited || !(tree instanceof StubTree)) return;

    for (PsiFileStub root : ((PsiFileStubImpl<?>)tree.getRoot()).getStubRoots()) {
      if (root instanceof StubBase) {
        StubList stubList = ((StubBase)root).getStubList();
        for (int i = 0; i < stubList.size(); i++) {
          StubBase<?> each = stubList.getCachedStub(i);
          PsiElement cachedPsi = each == null ? null : ((StubBase)each).getCachedPsi();
          if (cachedPsi != null) {
            ourStubReloadingProhibited = true;
            throw new AssertionError("Stub deserialization shouldn't create PSI: " + cachedPsi + "; " + each);
          }
        }
      }
    }
  }

  private static int getCurrentTextContentLength(Project project, VirtualFile vFile, Document document, PsiFile psiFile) {
    if (vFile.getFileType().isBinary()) {
      return -1;
    }
    if (psiFile instanceof PsiFileImpl && ((PsiFileImpl)psiFile).isContentsLoaded()) {
      return psiFile.getTextLength();
    }

    if (document != null) {
      return PsiDocumentManager.getInstance(project).getLastCommittedText(document).length();
    }
    return -1;
  }

  private static ObjectStubTree processError(final VirtualFile vFile, String message, @Nullable Exception e) {
    LOG.error(message, e);

    ApplicationManager.getApplication().invokeLater(() -> {
      final Document doc = FileDocumentManager.getInstance().getCachedDocument(vFile);
      if (doc != null) {
        FileDocumentManager.getInstance().saveDocument(doc);
      }

      // avoid deadlock by requesting reindex later.
      // processError may be invoked under stub index's read action and requestReindex in EDT starts dumb mode in writeAction (IDEA-197296)
      FileBasedIndex.getInstance().requestReindex(vFile);
    }, IdeaModalityState.NON_MODAL);

    return null;
  }

  @Override
  public void rebuildStubTree(VirtualFile virtualFile) {
    FileBasedIndex.getInstance().requestReindex(virtualFile);
  }

  @Override
  public boolean canHaveStub(VirtualFile file) {
    return StubUpdatingIndex.canHaveStub(myProjectLocator, file);
  }

  @Override
  protected boolean hasPsiInManyProjects(@Nonnull final VirtualFile virtualFile) {
    int count = 0;
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      if (PsiManagerEx.getInstanceEx(project).getFileManager().findCachedViewProvider(virtualFile) != null) {
        count++;
      }
    }
    return count > 1;
  }

  @Override
  protected IndexingStampInfo getIndexingStampInfo(@Nonnull VirtualFile file) {
    return StubUpdatingIndex.getIndexingStampInfo(file);
  }

  //@Override
  //protected boolean isPrebuilt(@NotNull VirtualFile virtualFile) {
  //  boolean canBePrebuilt = false;
  //  try {
  //    final PrebuiltStubsProvider prebuiltStubsProvider = PrebuiltStubsProviders.INSTANCE.forFileType(virtualFile.getFileType());
  //    if (prebuiltStubsProvider != null) {
  //      canBePrebuilt = null != prebuiltStubsProvider.findStub(new FileContentImpl(virtualFile, virtualFile.contentsToByteArray()));
  //    }
  //  }
  //  catch (Exception e) {
  //    // pass
  //  }
  //  return canBePrebuilt;
  //}

  @RequiredReadAction
  @Nonnull
  public RuntimeException stubTreeAndIndexDoNotMatch(@Nullable ObjectStubTree stubTree, @Nonnull PsiFileWithStubSupport psiFile, @Nullable Throwable cause) {
    VirtualFile file = psiFile.getViewProvider().getVirtualFile();
    StubTree stubTreeFromIndex = (StubTree)readFromVFile(psiFile.getProject(), file);
    boolean compiled = psiFile instanceof PsiCompiledElement;
    Document document = compiled ? null : FileDocumentManager.getInstance().getDocument(file);
    IndexingStampInfo indexingStampInfo = getIndexingStampInfo(file);
    boolean upToDate = indexingStampInfo != null && indexingStampInfo.isUpToDate(document, file, psiFile);

    boolean canBePrebuilt = isPrebuilt(psiFile.getVirtualFile());

    String msg = "PSI and index do not match.\nPlease report the problem to JetBrains with the files attached\n";

    if (canBePrebuilt) {
      msg += "This stub can have pre-built origin\n";
    }

    if (upToDate) {
      msg += "INDEXED VERSION IS THE CURRENT ONE";
    }

    msg += " file=" + psiFile;
    msg += ", file.class=" + psiFile.getClass();
    msg += ", file.lang=" + psiFile.getLanguage();
    msg += ", modStamp=" + psiFile.getModificationStamp();

    if (!compiled) {
      String text = psiFile.getText();
      PsiFile fromText = PsiFileFactory.getInstance(psiFile.getProject()).createFileFromText(psiFile.getName(), psiFile.getFileType(), text);
      if (fromText.getLanguage().equals(psiFile.getLanguage())) {
        boolean consistent = DebugUtil.psiToString(psiFile, true).equals(DebugUtil.psiToString(fromText, true));
        if (consistent) {
          msg += "\n tree consistent";
        }
        else {
          msg += "\n AST INCONSISTENT, perhaps after incremental reparse; " + fromText;
        }
      }
    }

    if (stubTree != null) {
      msg += "\n stub debugInfo=" + stubTree.getDebugInfo();
    }

    msg += "\nlatestIndexedStub=" + stubTreeFromIndex;
    if (stubTreeFromIndex != null) {
      if (stubTree != null) {
        msg += "\n   same size=" + (stubTree.getPlainList().size() == stubTreeFromIndex.getPlainList().size());
      }
      msg += "\n   debugInfo=" + stubTreeFromIndex.getDebugInfo();
    }

    FileViewProvider viewProvider = psiFile.getViewProvider();
    msg += "\n viewProvider=" + viewProvider;
    msg += "\n viewProvider stamp: " + viewProvider.getModificationStamp();

    msg += "; file stamp: " + file.getModificationStamp();
    msg += "; file modCount: " + file.getModificationCount();
    msg += "; file length: " + file.getLength();

    if (document != null) {
      msg += "\n doc saved: " + !FileDocumentManager.getInstance().isDocumentUnsaved(document);
      msg += "; doc stamp: " + document.getModificationStamp();
      msg += "; doc size: " + document.getTextLength();
      msg += "; committed: " + PsiDocumentManager.getInstance(psiFile.getProject()).isCommitted(document);
    }

    msg += "\nindexing info: " + indexingStampInfo;

    Attachment[] attachments = createAttachments(stubTree, psiFile, file, stubTreeFromIndex);

    // separate methods and separate exception classes for EA to treat these situations differently
    return hasPsiInManyProjects(file)
           ? handleManyProjectsMismatch(msg, attachments, cause)
           : upToDate ? handleUpToDateMismatch(msg, attachments, cause) : new RuntimeExceptionWithAttachments(msg, cause, attachments);
  }

  protected boolean isPrebuilt(@Nonnull VirtualFile virtualFile) {
    return false;
  }

  private static RuntimeExceptionWithAttachments handleManyProjectsMismatch(@Nonnull String message, Attachment[] attachments, @Nullable Throwable cause) {
    return new ManyProjectsStubIndexMismatch(message, cause, attachments);
  }

  private static RuntimeExceptionWithAttachments handleUpToDateMismatch(@Nonnull String message, Attachment[] attachments, @Nullable Throwable cause) {
    return new UpToDateStubIndexMismatch(message, cause, attachments);
  }

  @Nonnull
  private static Attachment[] createAttachments(@Nullable ObjectStubTree stubTree, @Nonnull PsiFileWithStubSupport psiFile, VirtualFile file, @Nullable StubTree stubTreeFromIndex) {
    List<Attachment> attachments = new ArrayList<>();
    attachments.add(AttachmentFactory.get().create(file.getPath() + "_file.txt", psiFile instanceof PsiCompiledElement ? "compiled" : psiFile.getText()));
    if (stubTree != null) {
      attachments.add(AttachmentFactory.get().create("stubTree.txt", ((PsiFileStubImpl)stubTree.getRoot()).printTree()));
    }
    if (stubTreeFromIndex != null) {
      attachments.add(AttachmentFactory.get().create("stubTreeFromIndex.txt", ((PsiFileStubImpl)stubTreeFromIndex.getRoot()).printTree()));
    }
    return attachments.toArray(Attachment.EMPTY_ARRAY);
  }

}
