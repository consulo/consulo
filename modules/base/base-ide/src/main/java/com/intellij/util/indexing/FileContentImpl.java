// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.lang.FileASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.LighterAST;
import com.intellij.lang.TreeBackedLighterAST;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.DefaultProjectFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.LanguageSubstitutors;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import consulo.annotation.access.RequiredReadAction;
import consulo.lang.LanguageVersion;
import consulo.lang.LanguageVersionResolvers;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author nik
 * <p>
 * Class is not final since it is overridden in Upsource
 */
public class FileContentImpl extends IndexedFileImpl implements PsiDependentFileContent {
  private Charset myCharset;
  private byte[] myContent;
  private CharSequence myContentAsText;
  private final long myStamp;
  private byte[] myHash;
  private boolean myLighterASTShouldBeThreadSafe;
  private final boolean myPhysicalContent;

  public FileContentImpl(@Nonnull final VirtualFile file, @Nonnull final CharSequence contentAsText, long documentStamp) {
    this(file, contentAsText, null, documentStamp, false);
  }

  public FileContentImpl(@Nonnull final VirtualFile file, @Nonnull final byte[] content) {
    this(file, null, content, -1, true);
  }

  FileContentImpl(@Nonnull final VirtualFile file) {
    this(file, null, null, -1, true);
  }

  private FileContentImpl(@Nonnull VirtualFile file, CharSequence contentAsText, byte[] content, long stamp, boolean physicalContent) {
    super(file, FileTypeRegistry.getInstance().getFileTypeByFile(file, content));
    myContentAsText = contentAsText;
    myContent = content;
    myStamp = stamp;
    myPhysicalContent = physicalContent;
  }

  private static final Key<PsiFile> CACHED_PSI = Key.create("cached psi from content");

  @Nonnull
  @Override
  public PsiFile getPsiFile() {
    return getPsiFileForPsiDependentIndex();
  }

  @Nonnull
  private PsiFile getFileFromText() {
    PsiFile psi = getUserData(IndexingDataKeys.PSI_FILE);

    if (psi == null) {
      psi = getUserData(CACHED_PSI);
    }

    if (psi == null) {
      psi = createFileFromText(getContentAsText());
      psi.putUserData(IndexingDataKeys.VIRTUAL_FILE, getFile());
      putUserData(CACHED_PSI, psi);
    }
    return psi;
  }

  @Override
  @Nonnull
  public LighterAST getLighterAST() {
    LighterAST lighterAST = getUserData(IndexingDataKeys.LIGHTER_AST_NODE_KEY);
    if (lighterAST == null) {
      FileASTNode node = getPsiFile().getNode();
      lighterAST = myLighterASTShouldBeThreadSafe ? new TreeBackedLighterAST(node) : node.getLighterAST();
      putUserData(IndexingDataKeys.LIGHTER_AST_NODE_KEY, lighterAST);
    }
    return lighterAST;
  }

  /**
   * Expand the AST to ensure {@link com.intellij.lang.FCTSBackedLighterAST} won't be used, because it's not thread-safe,
   * but unsaved documents may be indexed in many concurrent threads
   */
  void ensureThreadSafeLighterAST() {
    myLighterASTShouldBeThreadSafe = true;
  }

  @RequiredReadAction
  public PsiFile createFileFromText(@Nonnull CharSequence text) {
    Project project = getProject();
    if (project == null) {
      project = DefaultProjectFactory.getInstance().getDefaultProject();
    }
    FileType fileType = getFileTypeWithoutSubstitution();
    if (!(fileType instanceof LanguageFileType)) {
      throw new AssertionError("PSI can be created only for a file with LanguageFileType but actual is " +
                               fileType.getClass() +
                               "." +
                               "\nPlease use a proper FileBasedIndexExtension#getInputFilter() implementation for the caller index");
    }
    return createFileFromText(project, text, (LanguageFileType)fileType, myFile, myFileName);
  }

  @Nonnull
  @RequiredReadAction
  public static PsiFile createFileFromText(@Nonnull Project project, @Nonnull CharSequence text, @Nonnull LanguageFileType fileType, @Nonnull VirtualFile file, @Nonnull String fileName) {
    Language language = fileType.getLanguage();
    Language substitutedLanguage = LanguageSubstitutors.INSTANCE.substituteLanguage(language, file, project);
    LanguageVersion languageVersion = LanguageVersionResolvers.INSTANCE.forLanguage(substitutedLanguage).getLanguageVersion(substitutedLanguage, project, file);
    PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, languageVersion, text, false, false, false, file);
    if (psiFile == null) {
      throw new IllegalStateException("psiFile is null. language = " + language.getID() + ", substitutedLanguage = " + substitutedLanguage.getID());
    }
    return psiFile;
  }

  public static class IllegalDataException extends RuntimeException {
    IllegalDataException(final String message) {
      super(message);
    }
  }

  @Nonnull
  private FileType getSubstitutedFileType() {
    return SubstitutedFileType.substituteFileType(myFile, myFileType, getProject());
  }

  @TestOnly
  public static FileContent createByFile(@Nonnull VirtualFile file) {
    try {
      return new FileContentImpl(file, file.contentsToByteArray());
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Nonnull
  public static FileContent createByContent(@Nonnull VirtualFile file, @Nonnull byte[] content) {
    return new FileContentImpl(file, content);
  }

  @Nonnull
  public static FileContent createByFile(@Nonnull VirtualFile file, @Nullable Project project) throws IOException {
    FileContentImpl content = (FileContentImpl)createByContent(file, file.contentsToByteArray(false));
    if (project != null) {
      content.setProject(project);
    }
    return content;
  }

  private FileType getFileTypeWithoutSubstitution() {
    return myFileType;
  }

  @Nonnull
  @Override
  public FileType getFileType() {
    return getSubstitutedFileType();
  }

  @Nonnull
  @Override
  public VirtualFile getFile() {
    return myFile;
  }

  @Nonnull
  @Override
  public String getFileName() {
    return myFileName;
  }

  @Nonnull
  public Charset getCharset() {
    Charset charset = myCharset;
    if (charset == null) {
      myCharset = charset = myFile.getCharset();
    }
    return charset;
  }

  public long getStamp() {
    return myStamp;
  }

  @Nonnull
  @Override
  public byte[] getContent() {
    byte[] content = myContent;
    if (content == null) {
      myContent = content = myContentAsText.toString().getBytes(getCharset());
    }
    return content;
  }

  @Nonnull
  @Override
  public CharSequence getContentAsText() {
    if (myFileType.isBinary()) {
      throw new IllegalDataException("Cannot obtain text for binary file type : " + myFileType.getDescription());
    }
    final CharSequence content = getUserData(IndexingDataKeys.FILE_TEXT_CONTENT_KEY);
    if (content != null) {
      return content;
    }
    CharSequence contentAsText = myContentAsText;
    if (contentAsText == null) {
      myContentAsText = contentAsText = LoadTextUtil.getTextByBinaryPresentation(myContent, myFile);
      myContent = null; // help gc, indices are expected to use bytes or chars but not both
    }
    return contentAsText;
  }

  @Override
  public String toString() {
    return myFileName;
  }

  @Nullable
  public byte[] getHash() {
    return myHash;
  }

  public void setHash(byte[] hash) {
    myHash = hash;
  }

  /**
   * @deprecated use {@link FileContent#getPsiFile()}
   */
  @SuppressWarnings("DeprecatedIsStillUsed")
  @Deprecated
  @Nonnull
  public PsiFile getPsiFileForPsiDependentIndex() {
    PsiFile psi = null;
    if (!myPhysicalContent) {
      Document document = FileDocumentManager.getInstance().getCachedDocument(getFile());

      if (document != null) {
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(getProject());
        if (psiDocumentManager.isUncommited(document)) {
          PsiFile existingPsi = psiDocumentManager.getPsiFile(document);
          if (existingPsi != null) {
            psi = existingPsi;
          }
        }
      }
    }
    if (psi == null) {
      psi = getFileFromText();
    }
    return psi;
  }
}
