// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.file;

import consulo.application.ApplicationManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.content.FileIndexFacade;
import consulo.language.file.FileViewProvider;
import consulo.language.file.inject.DocumentWindow;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.internal.file.FileManager;
import consulo.language.impl.internal.file.FileManagerImpl;
import consulo.language.impl.internal.psi.*;
import consulo.language.impl.plain.PsiPlainTextFileImpl;
import consulo.language.impl.psi.*;
import consulo.language.psi.PsiFileEx;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.logging.attachment.Attachment;
import consulo.logging.attachment.AttachmentFactory;
import consulo.project.Project;
import consulo.undoRedo.util.UndoConstants;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.LocalTimeCounter;
import consulo.virtualFileSystem.NonPhysicalFileSystem;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public abstract class AbstractFileViewProvider extends UserDataHolderBase implements FileViewProvider {
  private static final Logger LOG = Logger.getInstance(AbstractFileViewProvider.class);
  public static final Key<Object> FREE_THREADED = Key.create("FREE_THREADED");
  private static final Key<Set<AbstractFileViewProvider>> KNOWN_COPIES = Key.create("KNOWN_COPIES");
  @Nonnull
  private final PsiManagerEx myManager;
  @Nonnull
  private final VirtualFile myVirtualFile;
  private final boolean myEventSystemEnabled;
  private final boolean myPhysical;
  private volatile Content myContent;
  private volatile Reference<Document> myDocument;
  private final PsiLock myPsiLock = new PsiLock();

  protected AbstractFileViewProvider(@Nonnull PsiManager manager, @Nonnull VirtualFile virtualFile, boolean eventSystemEnabled) {
    myManager = (PsiManagerEx)manager;
    myVirtualFile = virtualFile;
    myEventSystemEnabled = eventSystemEnabled;
    setContent(new VirtualFileContent());
    myPhysical = isEventSystemEnabled() && !(virtualFile instanceof LightVirtualFile) && !(virtualFile.getFileSystem() instanceof NonPhysicalFileSystem);
    virtualFile.putUserData(FREE_THREADED, isFreeThreaded(this));
    if (virtualFile instanceof VirtualFileWindow && !(this instanceof FreeThreadedFileViewProvider) && !isFreeThreaded(this)) {
      throw new IllegalArgumentException("Must not create " + getClass() + " for injected file " + virtualFile + "; InjectedFileViewProvider must be used instead");
    }
  }

  protected boolean shouldCreatePsi() {
    if (isIgnored()) return false;

    VirtualFile vFile = getVirtualFile();
    if (isPhysical() && vFile.isInLocalFileSystem()) { // check directories consistency
      VirtualFile parent = vFile.getParent();
      if (parent == null) return false;

      PsiDirectory psiDir = getManager().findDirectory(parent);
      if (psiDir == null) {
        FileIndexFacade indexFacade = FileIndexFacade.getInstance(getManager().getProject());
        if (!indexFacade.isInLibrarySource(vFile) && !indexFacade.isInLibraryClasses(vFile)) {
          return false;
        }
      }
    }
    return true;
  }

  public static boolean isFreeThreaded(@Nonnull FileViewProvider provider) {
    return provider.getVirtualFile() instanceof LightVirtualFile && !provider.isEventSystemEnabled();
  }

  @Nonnull
  public PsiLock getFilePsiLock() {
    return myPsiLock;
  }

  protected final boolean isIgnored() {
    final VirtualFile file = getVirtualFile();
    return !(file instanceof LightVirtualFile) && FileTypeRegistry.getInstance().isFileIgnored(file);
  }

  @Nullable
  protected PsiFile createFile(@Nonnull Project project, @Nonnull VirtualFile file, @Nonnull FileType fileType) {
    return createFile(file, fileType, getBaseLanguage());
  }

  @Nonnull
  protected PsiFile createFile(@Nonnull VirtualFile file, @Nonnull FileType fileType, @Nonnull Language language) {
    if (fileType.isBinary() || file.is(VFileProperty.SPECIAL)) {
      return SingleRootFileViewProvider.isTooLargeForContentLoading(file) ? new PsiLargeBinaryFileImpl((PsiManagerImpl)getManager(), this) : new PsiBinaryFileImpl((PsiManagerImpl)getManager(), this);
    }
    if (!SingleRootFileViewProvider.isTooLargeForIntelligence(file)) {
      final PsiFile psiFile = createFile(language);
      if (psiFile != null) return psiFile;
    }

    if (SingleRootFileViewProvider.isTooLargeForContentLoading(file)) {
      return new PsiLargeTextFileImpl(this);
    }

    return new PsiPlainTextFileImpl(this);
  }

  @Nullable
  protected PsiFile createFile(@Nonnull Language lang) {
    if (lang != getBaseLanguage()) return null;
    final ParserDefinition parserDefinition = ParserDefinition.forLanguage(lang);
    if (parserDefinition != null) {
      return parserDefinition.createFile(this);
    }
    return null;
  }

  @Override
  @Nonnull
  public final PsiManager getManager() {
    return myManager;
  }

  @Override
  @Nonnull
  public CharSequence getContents() {
    return getContent().getText();
  }

  @Override
  @Nonnull
  public VirtualFile getVirtualFile() {
    return myVirtualFile;
  }

  @Nullable
  private Document getCachedDocument() {
    final Document document = consulo.util.lang.ref.SoftReference.dereference(myDocument);
    if (document != null) return document;
    return FileDocumentManager.getInstance().getCachedDocument(getVirtualFile());
  }

  @Override
  public Document getDocument() {
    Document document = consulo.util.lang.ref.SoftReference.dereference(myDocument);
    if (document == null) {
      document = FileDocumentManager.getInstance().getDocument(getVirtualFile());
      myDocument = document == null ? null : new SoftReference<>(document);
    }
    return document;
  }

  @Override
  @Nullable
  public final PsiFile getPsi(@Nonnull Language target) {
    if (!isPhysical()) {
      FileManager fileManager = ((PsiManagerEx)getManager()).getFileManager();
      VirtualFile virtualFile = getVirtualFile();
      if (fileManager.findCachedViewProvider(virtualFile) == null && getCachedPsiFiles().isEmpty()) {
        fileManager.setViewProvider(virtualFile, this);
      }
    }
    return getPsiInner(target);
  }

  @Nullable
  protected abstract PsiFile getPsiInner(Language target);

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public FileViewProvider clone() {
    VirtualFile origFile = getVirtualFile();
    LightVirtualFile copy = new LightVirtualFile(origFile.getName(), origFile.getFileType(), getContents(), origFile.getCharset(), getModificationStamp());
    origFile.copyCopyableDataTo(copy);
    copy.setOriginalFile(origFile);
    copy.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);
    copy.setCharset(origFile.getCharset());

    return createCopy(copy);
  }

  @Override
  public PsiElement findElementAt(final int offset, @Nonnull final Language language) {
    final PsiFile psiFile = getPsi(language);
    return psiFile != null ? findElementAt(psiFile, offset) : null;
  }

  @Override
  @Nullable
  public PsiReference findReferenceAt(final int offset, @Nonnull final Language language) {
    final PsiFile psiFile = getPsi(language);
    return psiFile != null ? findReferenceAt(psiFile, offset) : null;
  }

  @Nullable
  protected static PsiReference findReferenceAt(@Nullable final PsiFile psiFile, final int offset) {
    if (psiFile == null) return null;
    int offsetInElement = offset;
    PsiElement child = psiFile.getFirstChild();
    while (child != null) {
      final int length = child.getTextLength();
      if (length <= offsetInElement) {
        offsetInElement -= length;
        child = child.getNextSibling();
        continue;
      }
      return child.findReferenceAt(offsetInElement);
    }
    return null;
  }

  @Nullable
  public static PsiElement findElementAt(@Nullable PsiElement psiFile, final int offset) {
    ASTNode node = psiFile == null ? null : psiFile.getNode();
    return node == null ? null : SourceTreeToPsiMap.treeElementToPsi(node.findLeafElementAt(offset));
  }

  @Override
  public void beforeContentsSynchronized() {
  }

  @Override
  public void contentsSynchronized() {
    if (myContent instanceof PsiFileContent) {
      setContent(new VirtualFileContent());
    }
    checkLengthConsistency();
  }

  public final void onContentReload() {
    List<PsiFile> files = getCachedPsiFiles();
    List<PsiTreeChangeEventImpl> events = new ArrayList<>(files.size());
    List<PsiTreeChangeEventImpl> genericEvents = new ArrayList<>(files.size());
    for (PsiFile file : files) {
      genericEvents.add(createChildrenChangeEvent(file, true));
      events.add(createChildrenChangeEvent(file, false));
    }

    beforeContentsSynchronized();

    for (PsiTreeChangeEventImpl event : genericEvents) {
      ((PsiManagerImpl)getManager()).beforeChildrenChange(event);
    }
    for (PsiTreeChangeEventImpl event : events) {
      ((PsiManagerImpl)getManager()).beforeChildrenChange(event);
    }

    for (PsiFile psiFile : files) {
      if (psiFile instanceof PsiFileEx psiFileEx) {
        psiFileEx.onContentReload();
      }
    }

    contentsSynchronized();

    for (PsiTreeChangeEventImpl event : events) {
      ((PsiManagerImpl)getManager()).childrenChanged(event);
    }
    for (PsiTreeChangeEventImpl event : genericEvents) {
      ((PsiManagerImpl)getManager()).childrenChanged(event);
    }
  }

  private PsiTreeChangeEventImpl createChildrenChangeEvent(PsiFile file, boolean generic) {
    PsiTreeChangeEventImpl event = new PsiTreeChangeEventImpl(myManager);
    event.setParent(file);
    event.setFile(file);
    event.setGenericChange(generic);
    if (file instanceof PsiFileImpl psiFile && psiFile.isContentsLoaded()) {
      event.setOffset(0);
      event.setOldLength(file.getTextLength());
    }
    return event;
  }

  @Override
  public void rootChanged(@Nonnull PsiFile psiFile) {
    if (psiFile instanceof PsiFileImpl psiFileImpl && psiFileImpl.isContentsLoaded() && psiFileImpl.isValid()) {
      setContent(new PsiFileContent(psiFileImpl.calcTreeElement(), LocalTimeCounter.currentTime()));
    }
  }

  @Override
  public boolean isEventSystemEnabled() {
    return myEventSystemEnabled;
  }

  @Override
  public boolean isPhysical() {
    return myPhysical;
  }

  @Override
  public long getModificationStamp() {
    return getContent().getModificationStamp();
  }

  @Override
  public boolean supportsIncrementalReparse(@Nonnull final Language rootLanguage) {
    return true;
  }

  @Nonnull
  private Content getContent() {
    return myContent;
  }

  private void setContent(@Nonnull Content content) {
    myContent = content;
  }

  private void checkLengthConsistency() {
    Document document = getCachedDocument();
    if (document instanceof DocumentWindow) {
      return;
    }
    if (document != null && ((PsiDocumentManagerBase)PsiDocumentManager.getInstance(myManager.getProject())).getSynchronizer().isInSynchronization(document)) {
      return;
    }

    List<FileElement> knownTreeRoots = getKnownTreeRoots();
    if (knownTreeRoots.isEmpty()) return;

    int fileLength = myContent.getTextLength();
    for (FileElement fileElement : knownTreeRoots) {
      int nodeLength = fileElement.getTextLength();
      if (!isDocumentConsistentWithPsi(fileLength, fileElement, nodeLength)) {
        PsiUtilCore.ensureValid(fileElement.getPsi());
        List<Attachment> attachments = ContainerUtil.newArrayList(AttachmentFactory.get().create(myVirtualFile.getName(), myContent.getText().toString()),
                                                                  AttachmentFactory.get().create(myVirtualFile.getNameWithoutExtension() + ".tree.txt", fileElement.getText()));
        if (document != null) {
          attachments.add(AttachmentFactory.get().create(myVirtualFile.getNameWithoutExtension() + ".document.txt", document.getText()));
        }
        // exceptions here should be assigned to peter
        LOG.error("Inconsistent " + fileElement.getElementType() + " tree in " + this + "; nodeLength=" + nodeLength + "; fileLength=" + fileLength, attachments.toArray(Attachment.EMPTY_ARRAY));
      }
    }
  }

  private boolean isDocumentConsistentWithPsi(int fileLength, FileElement fileElement, int nodeLength) {
    if (nodeLength != fileLength) return false;

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return fileElement.textMatches(myContent.getText());
    }

    return true;
  }


  @NonNls
  @Override
  public String toString() {
    return getClass().getName() + "{myVirtualFile=" + myVirtualFile + ", content=" + getContent() + '}';
  }

  public abstract PsiFile getCachedPsi(@Nonnull Language target);

  @Nonnull
  public abstract List<PsiFile> getCachedPsiFiles();

  @Nonnull
  public abstract List<FileElement> getKnownTreeRoots();

  public final void markInvalidated() {
    invalidateCachedPsi();
    forKnownCopies(copy -> myManager.getFileManager().setViewProvider(copy.getVirtualFile(), null));
  }

  public final void markPossiblyInvalidated() {
    invalidateCachedPsi();
    forKnownCopies(FileManagerImpl::markPossiblyInvalidated);
  }

  private void invalidateCachedPsi() {
    for (PsiFile file : getCachedPsiFiles()) {
      if (file instanceof PsiFileEx psiFileEx) {
        psiFileEx.markInvalidated();
      }
    }
  }

  private void forKnownCopies(Consumer<? super AbstractFileViewProvider> action) {
    Set<AbstractFileViewProvider> knownCopies = getUserData(KNOWN_COPIES);
    if (knownCopies != null) {
      for (AbstractFileViewProvider copy : knownCopies) {
        if (copy.getCachedPsiFiles().stream().anyMatch(f -> f.getOriginalFile().getViewProvider() == this)) {
          action.accept(copy);
        }
      }
    }
  }

  public final void registerAsCopy(@Nonnull AbstractFileViewProvider copy) {
    if (copy instanceof FreeThreadedFileViewProvider) {
      LOG.assertTrue(this instanceof FreeThreadedFileViewProvider, "Injected file can't have non-injected original file");
    }
    Set<AbstractFileViewProvider> copies = getUserData(KNOWN_COPIES);
    if (copies == null) {
      copies = putUserDataIfAbsent(KNOWN_COPIES, Collections.newSetFromMap(ContainerUtil.createConcurrentWeakMap()));
    }
    if (copy.getUserData(KNOWN_COPIES) != null) {
      LOG.error("A view provider copy must be registered before it may have its own copies, to avoid cycles");
    }
    copies.add(copy);
  }

  private interface Content {
    @Nonnull
    CharSequence getText();

    int getTextLength();

    long getModificationStamp();
  }

  private class VirtualFileContent implements Content {
    @Nonnull
    @Override
    public CharSequence getText() {
      final VirtualFile virtualFile = getVirtualFile();
      if (virtualFile instanceof LightVirtualFile lightVirtualFile) {
        Document doc = getCachedDocument();
        if (doc != null) return getLastCommittedText(doc);
        return lightVirtualFile.getContent();
      }

      final Document document = getDocument();
      if (document == null) {
        return LoadTextUtil.loadText(virtualFile);
      }
      return getLastCommittedText(document);
    }

    @Override
    public int getTextLength() {
      return getText().length();
    }

    @Override
    public long getModificationStamp() {
      final Document document = getCachedDocument();
      if (document == null) {
        return getVirtualFile().getModificationStamp();
      }
      return getLastCommittedStamp(document);
    }

    @NonNls
    @Override
    public String toString() {
      return "VirtualFileContent{size=" + getVirtualFile().getLength() + "}";
    }
  }

  @Nonnull
  private CharSequence getLastCommittedText(@Nonnull Document document) {
    return PsiDocumentManager.getInstance(myManager.getProject()).getLastCommittedText(document);
  }

  private long getLastCommittedStamp(@Nonnull Document document) {
    return PsiDocumentManager.getInstance(myManager.getProject()).getLastCommittedStamp(document);
  }

  private static class PsiFileContent implements Content {
    private final long myModificationStamp;
    private final FileElement myFileElement;

    PsiFileContent(@Nonnull FileElement fileElement, long modificationStamp) {
      myModificationStamp = modificationStamp;
      myFileElement = fileElement;
    }

    @Nonnull
    @Override
    public CharSequence getText() {
      return myFileElement.getText();
    }

    @Override
    public int getTextLength() {
      return myFileElement.getTextLength();
    }

    @Override
    public long getModificationStamp() {
      return myModificationStamp;
    }
  }

  @Nonnull
  @Override
  public PsiFile getStubBindingRoot() {
    final PsiFile psi = getPsi(getBaseLanguage());
    assert psi != null;
    return psi;
  }

  @Nonnull
  @Override
  public final FileType getFileType() {
    return myVirtualFile.getFileType();
  }
}
