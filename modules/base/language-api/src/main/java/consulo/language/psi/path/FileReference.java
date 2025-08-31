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

package consulo.language.psi.path;

import consulo.document.util.TextRange;
import consulo.language.LangBundle;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.language.psi.resolve.PsiFileSystemItemProcessor;
import consulo.language.psi.resolve.ResolveCache;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.URI;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author cdr
 */
public class FileReference implements PsiFileReference, FileReferenceOwner, PsiPolyVariantReference, EmptyResolveMessageProvider, BindablePsiReference {

  private static final Logger LOG = Logger.getInstance(FileReference.class);

  public static final FileReference[] EMPTY = new FileReference[0];

  private final int myIndex;
  private TextRange myRange;
  private final String myText;
  @Nonnull
  private final FileReferenceSet myFileReferenceSet;

  public FileReference(@Nonnull FileReferenceSet fileReferenceSet, TextRange range, int index, String text) {
    myFileReferenceSet = fileReferenceSet;
    myIndex = index;
    myRange = range;
    myText = text;
  }

  public FileReference(FileReference original) {
    this(original.myFileReferenceSet, original.myRange, original.myIndex, original.myText);
  }

  @Nullable
  public static FileReference findFileReference(@Nonnull PsiReference original) {
    if (original instanceof PsiMultiReference) {
      PsiMultiReference multiReference = (PsiMultiReference)original;
      for (PsiReference reference : multiReference.getReferences()) {
        if (reference instanceof FileReference) {
          return (FileReference)reference;
        }
      }
    }
    else if (original instanceof FileReferenceOwner) {
      PsiFileReference fileReference = ((FileReferenceOwner)original).getLastFileReference();
      if (fileReference instanceof FileReference) {
        return (FileReference)fileReference;
      }
    }

    return null;
  }

  @Nonnull
  public Collection<PsiFileSystemItem> getContexts() {
    FileReference contextRef = getContextReference();
    ArrayList<PsiFileSystemItem> result = new ArrayList<>();

    if (contextRef == null) {
      Collection<PsiFileSystemItem> defaultContexts = myFileReferenceSet.getDefaultContexts();
      for (PsiFileSystemItem context : defaultContexts) {
        if (context == null) {
          LOG.error(myFileReferenceSet.getClass() + " provided a null context");
        }
      }
      result.addAll(defaultContexts);
    }
    else {
      ResolveResult[] resolveResults = contextRef.multiResolve(false);
      for (ResolveResult resolveResult : resolveResults) {
        if (resolveResult.getElement() != null) {
          result.add((PsiFileSystemItem)resolveResult.getElement());
        }
      }
    }

    result.addAll(myFileReferenceSet.getExtraContexts());

    return result;
  }

  @Override
  @Nonnull
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    PsiFile file = getElement().getContainingFile();
    return ResolveCache.getInstance(file.getProject()).resolveWithCaching(this, MyResolver.INSTANCE, false, false, file);
  }

  @Nonnull
  protected ResolveResult[] innerResolve(boolean caseSensitive, @Nonnull PsiFile containingFile) {
    String referenceText = getText();
    if (referenceText.isEmpty() && myIndex == 0) {
      return new ResolveResult[]{new PsiElementResolveResult(containingFile)};
    }
    Collection<PsiFileSystemItem> contexts = getContexts();
    Collection<ResolveResult> result = new HashSet<>();
    for (PsiFileSystemItem context : contexts) {
      innerResolveInContext(referenceText, context, result, caseSensitive);
    }
    if (contexts.isEmpty() && isAllowedEmptyPath(referenceText)) {
      result.add(new PsiElementResolveResult(containingFile));
    }
    int resultCount = result.size();
    return resultCount > 0 ? result.toArray(new ResolveResult[resultCount]) : ResolveResult.EMPTY_ARRAY;
  }

  protected void innerResolveInContext(@Nonnull String text, @Nonnull PsiFileSystemItem context, final Collection<ResolveResult> result, final boolean caseSensitive) {
    if (isAllowedEmptyPath(text) || "".equals(text) || "/".equals(text)) {
      result.add(new PsiElementResolveResult(context));
    }
    else if ("".equals(text)) {
      PsiFileSystemItem resolved = context.getParent();
      if (resolved != null) {
        result.add(new PsiElementResolveResult(resolved));
      }
    }
    else {
      int separatorIndex = text.indexOf('/');
      if (separatorIndex >= 0) {
        List<ResolveResult> resolvedContexts = new ArrayList<>();
        if (separatorIndex == 0 /*starts with slash*/ && "/".equals(context.getName())) {
          resolvedContexts.add(new PsiElementResolveResult(context));
        }
        else {
          innerResolveInContext(text.substring(0, separatorIndex), context, resolvedContexts, caseSensitive);
        }
        String restOfText = text.substring(separatorIndex + 1);
        for (ResolveResult contextVariant : resolvedContexts) {
          PsiFileSystemItem item = (PsiFileSystemItem)contextVariant.getElement();
          if (item != null) {
            innerResolveInContext(restOfText, item, result, caseSensitive);
          }
        }
      }
      else {
        final String decoded = decode(text);

        if (context instanceof PackagePrefixFileSystemItem) {
          context = ((PackagePrefixFileSystemItem)context).getDirectory();
        }
        else if (context instanceof FileReferenceResolver) {
          PsiFileSystemItem child = ((FileReferenceResolver)context).resolveFileReference(this, decoded);
          if (child != null) {
            result.add(new PsiElementResolveResult(getOriginalFile(child)));
            return;
          }
        }

        if (context.getParent() == null && FileUtil.namesEqual(decoded, context.getName())) {
          // match filesystem roots
          result.add(new PsiElementResolveResult(getOriginalFile(context)));
        }
        else if (context instanceof PsiDirectory && caseSensitivityApplies((PsiDirectory)context, caseSensitive)) {
          // optimization: do not load all children into VFS
          PsiDirectory directory = (PsiDirectory)context;
          PsiFileSystemItem child = directory.findFile(decoded);
          if (child == null) child = directory.findSubdirectory(decoded);
          if (child != null) {
            result.add(new PsiElementResolveResult(getOriginalFile(child)));
          }
        }
        else {
          processVariants(context, new PsiFileSystemItemProcessor() {
            @Override
            public boolean acceptItem(String name, boolean isDirectory) {
              return caseSensitive ? decoded.equals(name) : decoded.compareToIgnoreCase(name) == 0;
            }

            @Override
            public boolean execute(@Nonnull PsiFileSystemItem element) {
              result.add(new PsiElementResolveResult(getOriginalFile(element)));
              return true;
            }
          });
        }
      }
    }
  }

  @Nonnull
  public String getFileNameToCreate() {
    return decode(getCanonicalText());
  }

  @Nullable
  public String getNewFileTemplateName() {
    FileType fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(myText);
    if (fileType != UnknownFileType.INSTANCE) {
      return fileType.getId() + " File." + fileType.getDefaultExtension();
    }
    return null;
  }

  private static boolean caseSensitivityApplies(PsiDirectory context, boolean caseSensitive) {
    VirtualFileSystem fs = context.getVirtualFile().getFileSystem();
    return fs.isCaseSensitive() == caseSensitive;
  }

  private boolean isAllowedEmptyPath(String text) {
    return text.isEmpty() &&
           isLast() &&
           (StringUtil.isEmpty(myFileReferenceSet.getPathString()) && myFileReferenceSet.isEmptyPathAllowed() || !myFileReferenceSet.isEndingSlashNotAllowed() && myIndex > 0);
  }

  @Nonnull
  public String decode(@Nonnull String text) {
    if (Platform.current().os().isMac()) {
      text = Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    // strip http get parameters
    String _text = text;
    int paramIndex = text.lastIndexOf('?');
    if (paramIndex >= 0) {
      _text = text.substring(0, paramIndex);
    }

    if (myFileReferenceSet.isUrlEncoded()) {
      try {
        return StringUtil.notNullize(new URI(_text).getPath(), text);
      }
      catch (Exception ignored) {
        return text;
      }
    }

    return _text;
  }

  @Override
  @Nonnull
  public Object[] getVariants() {
    FileReferenceCompletion completion = FileReferenceCompletion.getInstance();
    if (completion != null) {
      return completion.getFileReferenceCompletionVariants(this);
    }
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  /**
   * Generates a lookup item for the specified completion variant candidate.
   *
   * @param candidate the element to show in the completion list.
   * @return the lookup item representation (PsiElement, LookupElement or String). If returns null,
   * {@code FileInfoManager.getFileLookupItem(candidate)} will be used to create the lookup item.
   */
  public Object createLookupItem(PsiElement candidate) {
    return null;
  }

  /**
   * Converts a wrapper like WebDirectoryElement into plain PsiFile
   */
  public static PsiFileSystemItem getOriginalFile(PsiFileSystemItem fileSystemItem) {
    VirtualFile file = fileSystemItem.getVirtualFile();
    if (file != null && !file.isDirectory()) {
      PsiManager psiManager = fileSystemItem.getManager();
      if (psiManager != null) {
        PsiFile psiFile = psiManager.findFile(file);
        if (psiFile != null) {
          fileSystemItem = psiFile;
        }
      }
    }
    return fileSystemItem;
  }

  @Nullable
  public String encode(String name, PsiElement psiElement) {
    try {
      return new URI(null, null, name, null).toString();
    }
    catch (Exception ignored) {
      return name;
    }
  }

  protected static void processVariants(PsiFileSystemItem context, PsiFileSystemItemProcessor processor) {
    context.processChildren(processor);
  }

  @Nullable
  private FileReference getContextReference() {
    return myIndex > 0 ? myFileReferenceSet.getReference(myIndex - 1) : null;
  }

  @Override
  public PsiElement getElement() {
    return myFileReferenceSet.getElement();
  }

  @Override
  public PsiFileSystemItem resolve() {
    ResolveResult[] resolveResults = multiResolve(false);
    return resolveResults.length == 1 ? (PsiFileSystemItem)resolveResults[0].getElement() : null;
  }

  @Nullable
  public PsiFileSystemItem innerSingleResolve(boolean caseSensitive, @Nonnull PsiFile containingFile) {
    ResolveResult[] resolveResults = innerResolve(caseSensitive, containingFile);
    return resolveResults.length == 1 ? (PsiFileSystemItem)resolveResults[0].getElement() : null;
  }

  @Override
  public boolean isReferenceTo(PsiElement element) {
    if (!(element instanceof PsiFileSystemItem)) return false;

    PsiFileSystemItem item = resolve();
    return item != null && FileReferenceHelperRegistrar.areElementsEquivalent(item, (PsiFileSystemItem)element);
  }

  @Override
  public TextRange getRangeInElement() {
    return myRange;
  }

  @Override
  @Nonnull
  public String getCanonicalText() {
    return myText;
  }

  public String getText() {
    return myText;
  }

  @Override
  public boolean isSoft() {
    return myFileReferenceSet.isSoft();
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    ElementManipulator<PsiElement> manipulator = CachingReference.getManipulator(getElement());
    myFileReferenceSet.setElement(manipulator.handleContentChange(getElement(), getRangeInElement(), newElementName));
    //Correct ranges
    int delta = newElementName.length() - myRange.getLength();
    myRange = new TextRange(getRangeInElement().getStartOffset(), getRangeInElement().getStartOffset() + newElementName.length());
    FileReference[] references = myFileReferenceSet.getAllReferences();
    for (int idx = myIndex + 1; idx < references.length; idx++) {
      references[idx].myRange = references[idx].myRange.shiftRight(delta);
    }
    return myFileReferenceSet.getElement();
  }

  public PsiElement bindToElement(@Nonnull PsiElement element, boolean absolute) throws IncorrectOperationException {
    if (!(element instanceof PsiFileSystemItem)) {
      throw new IncorrectOperationException("Cannot bind to element, should be instanceof PsiFileSystemItem: " + element);
    }

    // handle empty reference that resolves to current file
    if (getCanonicalText().isEmpty() && element == getElement().getContainingFile()) return getElement();

    PsiFileSystemItem fileSystemItem = (PsiFileSystemItem)element;
    VirtualFile dstVFile = fileSystemItem.getVirtualFile();
    if (dstVFile == null) throw new IncorrectOperationException("Cannot bind to non-physical element:" + element);

    PsiFile file = getElement().getContainingFile();
    PsiElement contextPsiFile = InjectedLanguageManager.getInstance(file.getProject()).getInjectionHost(file);
    if (contextPsiFile != null) file = contextPsiFile.getContainingFile(); // use host file!
    VirtualFile curVFile = file.getVirtualFile();
    if (curVFile == null) throw new IncorrectOperationException("Cannot bind from non-physical element:" + file);

    Project project = element.getProject();

    String newName;

    if (absolute) {
      PsiFileSystemItem root = null;
      PsiFileSystemItem dstItem = null;
      for (FileReferenceHelper helper : FileReferenceHelperRegistrar.getHelpers()) {
        if (!helper.isMine(project, dstVFile)) continue;
        PsiFileSystemItem _dstItem = helper.getPsiFileSystemItem(project, dstVFile);
        if (_dstItem != null) {
          PsiFileSystemItem _root = helper.findRoot(project, dstVFile);
          if (_root != null) {
            root = _root;
            dstItem = _dstItem;
            break;
          }
        }
      }
      if (root == null) {
        PsiFileSystemItem _dstItem = NullFileReferenceHelper.INSTANCE.getPsiFileSystemItem(project, dstVFile);
        if (_dstItem != null) {
          PsiFileSystemItem _root = NullFileReferenceHelper.INSTANCE.findRoot(project, dstVFile);
          if (_root != null) {
            root = _root;
            dstItem = _dstItem;
          }
        }

        if (root == null) {
          return getElement();
        }
      }

      String relativePath = PsiFileSystemItemUtil.getRelativePath(root, dstItem);
      if (relativePath == null) {
        return getElement();
      }
      newName = myFileReferenceSet.getNewAbsolutePath(root, relativePath);
    }
    else { // relative path

      FileReferenceHelper helper = FileReferenceHelperRegistrar.getNotNullHelper(file);

      Collection<PsiFileSystemItem> contexts = getContextsForBindToElement(curVFile, project, helper);

      for (PsiFileSystemItem context : contexts) {
        VirtualFile contextFile = context.getVirtualFile();
        assert contextFile != null;
        if (VirtualFileUtil.isAncestor(contextFile, dstVFile, true)) {
          String path = VirtualFileUtil.getRelativePath(dstVFile, contextFile, '/');
          if (path != null) {
            return rename(path);
          }
        }
      }

      PsiFileSystemItem dstItem = helper.getPsiFileSystemItem(project, dstVFile);
      PsiFileSystemItem curItem = helper.getPsiFileSystemItem(project, curVFile);

      if (curItem == null) {
        throw new IncorrectOperationException(
                "Cannot find path between files; " + "src = " + curVFile.getPresentableUrl() + "; " + "dst = " + dstVFile.getPresentableUrl() + "; " + "Contexts: " + contexts);
      }
      if (curItem.equals(dstItem)) {
        if (getCanonicalText().equals(dstItem.getName())) {
          return getElement();
        }
        return fixRefText(file.getName());
      }
      newName = PsiFileSystemItemUtil.getRelativePath(curItem, dstItem);
      if (newName == null) {
        return getElement();
      }
    }

    if (myFileReferenceSet.isUrlEncoded()) {
      newName = encode(newName, element);
    }

    return rename(newName);
  }

  /**
   * TODO: This should be fixed: bindToElement takes contexts from FileReferenceHelper.getContexts() while for resolve they are taken from
   * FileReference.getContexts(). Note that in this case it should rename only the text range of the reference
   */
  protected Collection<PsiFileSystemItem> getContextsForBindToElement(VirtualFile curVFile, Project project, FileReferenceHelper helper) {
    return helper.getContexts(project, curVFile);
  }

  protected PsiElement fixRefText(String name) {
    return ElementManipulators.getManipulator(getElement()).handleContentChange(getElement(), getRangeInElement(), name);
  }

  /* Happens when it's been moved to another folder */
  @Override
  public PsiElement bindToElement(@Nonnull PsiElement element) throws IncorrectOperationException {
    return bindToElement(element, myFileReferenceSet.isAbsolutePathReference());
  }

  protected PsiElement rename(String newName) throws IncorrectOperationException {
    TextRange range = new TextRange(myFileReferenceSet.getStartInElement(), getRangeInElement().getEndOffset());
    PsiElement element = getElement();
    try {
      return CachingReference.getManipulator(element).handleContentChange(element, range, newName);
    }
    catch (IncorrectOperationException e) {
      LOG.error("Cannot rename " + getClass() + " from " + myFileReferenceSet.getClass() + " to " + newName, e);
      throw e;
    }
  }

  @Nonnull
  protected static List<FileReferenceHelper> getHelpers() {
    return FileReferenceHelperRegistrar.getHelpers();
  }

  public int getIndex() {
    return myIndex;
  }

  @Nonnull
  @Override
  public LocalizeValue buildUnresolvedMessage(@Nonnull String referenceText) {
    return LocalizeValue.localizeTODO(new StringBuilder().append(LangBundle.message("error.cannot.resolve"))
                                                         .append(" ")
                                                         .append(LangBundle.message(isLast() ? "terms.file" : "terms.directory"))
                                                         .append(" '")
                                                         .append(StringUtil.escapePattern(decode(getCanonicalText())))
                                                         .append("'")
                                                         .toString());
  }

  public final boolean isLast() {
    return myIndex == myFileReferenceSet.getAllReferences().length - 1;
  }

  @Nonnull
  public FileReferenceSet getFileReferenceSet() {
    return myFileReferenceSet;
  }

  @Override
  public FileReference getLastFileReference() {
    return myFileReferenceSet.getLastReference();
  }

  static class MyResolver implements ResolveCache.PolyVariantContextResolver<FileReference> {
    static final MyResolver INSTANCE = new MyResolver();

    @Nonnull
    @Override
    public ResolveResult[] resolve(@Nonnull FileReference ref, @Nonnull PsiFile containingFile, boolean incompleteCode) {
      return ref.innerResolve(ref.getFileReferenceSet().isCaseSensitive(), containingFile);
    }
  }
}
