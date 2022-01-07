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

package com.maddyhome.idea.copyright.psi;

import com.intellij.lang.Commenter;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.IncorrectOperationException;
import com.maddyhome.idea.copyright.CopyrightManager;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.pattern.EntityUtil;
import com.maddyhome.idea.copyright.pattern.VelocityHelper;
import com.maddyhome.idea.copyright.util.FileTypeUtil;
import consulo.copyright.config.CopyrightFileConfig;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class UpdatePsiFileCopyright<T extends CopyrightFileConfig> {
  public static final Logger LOGGER = Logger.getInstance(UpdatePsiFileCopyright.class);

  private final T myFileConfig;
  @Nonnull
  private final PsiFile myPsiFile;
  @Nonnull
  private final CopyrightProfile myCopyrightProfile;
  private final Set<CommentAction> myActions = new TreeSet<CommentAction>();

  private String myCommentText;

  private FileType myFileType;

  @SuppressWarnings("unchecked")
  protected UpdatePsiFileCopyright(@Nonnull PsiFile psiFile, @Nonnull CopyrightProfile copyrightProfile) {
    myPsiFile = psiFile;
    myCopyrightProfile = copyrightProfile;

    VirtualFile virtualFile = psiFile.getVirtualFile();
    assert virtualFile != null;
    myFileType = virtualFile.getFileType();
    myFileConfig = (T)CopyrightManager.getInstance(psiFile.getProject()).getCopyrightFileConfigManager().getMergedOptions(myFileType);
  }

  private static CommentRange getLineCopyrightComments(List<PsiComment> comments, Document doc, int i, PsiComment comment) {
    PsiElement firstComment = comment;
    PsiElement lastComment = comment;
    final Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(PsiUtilCore.findLanguageFromElement(comment));
    if (isLineComment(commenter, comment, doc)) {
      int sline = doc.getLineNumber(comment.getTextRange().getStartOffset());
      int eline = doc.getLineNumber(comment.getTextRange().getEndOffset());
      for (int j = i - 1; j >= 0; j--) {
        PsiComment cmt = comments.get(j);

        if (isLineComment(commenter, cmt, doc) && doc.getLineNumber(cmt.getTextRange().getEndOffset()) == sline - 1) {
          firstComment = cmt;
          sline = doc.getLineNumber(cmt.getTextRange().getStartOffset());
        }
        else {
          break;
        }
      }
      for (int j = i + 1; j < comments.size(); j++) {
        PsiComment cmt = comments.get(j);
        if (isLineComment(commenter, cmt, doc) && doc.getLineNumber(cmt.getTextRange().getStartOffset()) == eline + 1) {
          lastComment = cmt;
          eline = doc.getLineNumber(cmt.getTextRange().getEndOffset());
        }
        else {
          break;
        }
      }
    }
    return new CommentRange(firstComment, lastComment);
  }

  private static boolean isLineComment(Commenter commenter, PsiComment comment, Document doc) {
    final String lineCommentPrefix = commenter.getLineCommentPrefix();
    if (lineCommentPrefix != null) {
      return comment.getText().startsWith(lineCommentPrefix);
    }
    final TextRange textRange = comment.getTextRange();
    return doc.getLineNumber(textRange.getStartOffset()) == doc.getLineNumber(textRange.getEndOffset());
  }

  public void process() throws Exception {
    if (accept()) {
      scanFile();

      processActions();
    }
  }

  protected boolean accept() {
    return !(myPsiFile instanceof PsiPlainTextFile);
  }

  protected abstract void scanFile();

  protected void checkComments(PsiElement first, PsiElement last, boolean commentHere) {
    List<PsiComment> comments = new ArrayList<PsiComment>();
    collectComments(first, last, comments);
    checkComments(last, commentHere, comments);
  }

  protected void collectComments(PsiElement first, PsiElement last, List<PsiComment> comments) {
    if (first == last && first instanceof PsiComment) {
      comments.add((PsiComment)first);
      return;
    }
    PsiElement elem = first;
    while (elem != last && elem != null) {
      if (elem instanceof PsiComment) {
        comments.add((PsiComment)elem);
        LOGGER.debug("found comment");
      }

      elem = getNextSibling(elem);
    }
  }

  protected void checkComments(PsiElement last, boolean commentHere, List<PsiComment> comments) {
    try {
      final String keyword = myCopyrightProfile.getKeyword();
      final LinkedHashSet<CommentRange> found = new LinkedHashSet<CommentRange>();
      Document doc = null;
      if (!StringUtil.isEmpty(keyword)) {
        Pattern pattern = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
        doc = FileDocumentManager.getInstance().getDocument(getFile().getVirtualFile());
        for (int i = 0; i < comments.size(); i++) {
          PsiComment comment = comments.get(i);
          String text = comment.getText();
          Matcher match = pattern.matcher(text);
          if (match.find()) {
            found.add(getLineCopyrightComments(comments, doc, i, comment));
          }
        }
      }

      // Default insertion point to just before user chosen marker (package, import, class)
      PsiElement point = last;
      if (commentHere && !comments.isEmpty() && myFileConfig.isRelativeBefore()) {
        // Insert before first comment within this section of code.
        point = comments.get(0);
      }

      if (commentHere && found.size() == 1) {
        CommentRange range = found.iterator().next();
        // Is the comment in the right place?
        if (myFileConfig.isRelativeBefore() && range.getFirst() == comments.get(0) ||
            !myFileConfig.isRelativeBefore() && range.getLast() == comments.get(comments.size() - 1)) {
          // Check to see if current copyright comment matches new one.
          String newComment = getCommentText("", "");
          myCommentText = null;
          String oldComment =
                  doc.getCharsSequence().subSequence(range.getFirst().getTextRange().getStartOffset(), range.getLast().getTextRange().getEndOffset()).toString()
                          .trim();
          if (!StringUtil.isEmptyOrSpaces(myCopyrightProfile.getAllowReplaceKeyword()) && !oldComment.contains(myCopyrightProfile.getAllowReplaceKeyword())) {
            return;
          }
          if (newComment.trim().equals(oldComment)) {
            if (!getLanguageOptions().isAddBlankAfter()) {
              // TODO - do we need option to remove blank line after?
              return; // Nothing to do since the comment is the same
            }
            PsiElement next = getNextSibling(range.getLast());
            if (next instanceof PsiWhiteSpace && StringUtil.countNewLines(next.getText()) > 1) {
              return;
            }
            point = range.getFirst();
          }
          else if (!newComment.isEmpty()) {
            int start = range.getFirst().getTextRange().getStartOffset();
            int end = range.getLast().getTextRange().getEndOffset();
            addAction(new CommentAction(CommentAction.ACTION_REPLACE, start, end));

            return;
          }
        }
      }

      for (CommentRange range : found) {
        // Remove the old copyright
        int start = range.getFirst().getTextRange().getStartOffset();
        int end = range.getLast().getTextRange().getEndOffset();
        // If this is the only comment then remove the whitespace after unless there is none before
        if (range.getFirst() == comments.get(0) && range.getLast() == comments.get(comments.size() - 1)) {
          int startLen = 0;
          if (getPreviousSibling(range.getFirst()) instanceof PsiWhiteSpace) {
            startLen = StringUtil.countNewLines(getPreviousSibling(range.getFirst()).getText());
          }
          int endLen = 0;
          if (getNextSibling(range.getLast()) instanceof PsiWhiteSpace) {
            endLen = StringUtil.countNewLines(getNextSibling(range.getLast()).getText());
          }
          if (startLen == 1 && getPreviousSibling(range.getFirst()).getTextRange().getStartOffset() > 0) {
            start = getPreviousSibling(range.getFirst()).getTextRange().getStartOffset();
          }
          else if (endLen > 0) {
            end = getNextSibling(range.getLast()).getTextRange().getEndOffset();
          }
        }
        // If this is the last comment then remove the whitespace before the comment
        else if (range.getLast() == comments.get(comments.size() - 1)) {
          if (getPreviousSibling(range.getFirst()) instanceof PsiWhiteSpace && StringUtil.countNewLines(getPreviousSibling(range.getFirst()).getText()) > 1) {
            start = getPreviousSibling(range.getFirst()).getTextRange().getStartOffset();
          }
        }
        // If this is the first or middle comment then remove the whitespace after the comment
        else if (getNextSibling(range.getLast()) instanceof PsiWhiteSpace) {
          end = getNextSibling(range.getLast()).getTextRange().getEndOffset();
        }

        addAction(new CommentAction(CommentAction.ACTION_DELETE, start, end));
      }

      // Finally add the comment if user chose this section.
      if (commentHere) {
        String suffix = "\n";
        if (point != last && getPreviousSibling(point) != null && getPreviousSibling(point) instanceof PsiWhiteSpace) {
          suffix = getPreviousSibling(point).getText();
          if (StringUtil.countNewLines(suffix) == 1) {
            suffix = '\n' + suffix;
          }
        }
        if (point != last && getPreviousSibling(point) == null) {
          suffix = "\n\n";
        }
        if (getLanguageOptions().isAddBlankAfter() && StringUtil.countNewLines(suffix) == 1) {
          suffix += "\n";
        }
        String prefix = "";
        if (getPreviousSibling(point) != null) {
          if (getPreviousSibling(point) instanceof PsiComment) {
            prefix = "\n\n";
          }
          if (getPreviousSibling(point) instanceof PsiWhiteSpace &&
              getPreviousSibling(getPreviousSibling(point)) != null &&
              getPreviousSibling(getPreviousSibling(point)) instanceof PsiComment) {
            String ws = getPreviousSibling(point).getText();
            int cnt = StringUtil.countNewLines(ws);
            if (cnt == 1) {
              prefix = "\n";
            }
          }
        }

        int pos = 0;
        if (point != null) {
          final TextRange textRange = point.getTextRange();
          if (textRange != null) {
            pos = textRange.getStartOffset();
          }
        }
        addAction(new CommentAction(pos, prefix, suffix));
      }
    }
    catch (Exception e) {
      LOGGER.error(e);
    }
  }

  @Nonnull
  public PsiFile getFile() {
    return myPsiFile;
  }

  @Nonnull
  public CopyrightFileConfig getFileConfig() {
    return myFileConfig;
  }

  @Nonnull
  public FileType getFileType() {
    return myFileType;
  }

  @Nullable
  public Module getModule() {
    return ModuleUtilCore.findModuleForPsiElement(myPsiFile);
  }

  @Nonnull
  public Project getProject() {
    return myPsiFile.getProject();
  }

  protected CopyrightFileConfig getLanguageOptions() {
    return myFileConfig;
  }

  protected void addAction(CommentAction action) {
    myActions.add(action);
  }

  protected PsiElement getPreviousSibling(PsiElement element) {
    return element == null ? null : element.getPrevSibling();
  }

  protected PsiElement getNextSibling(PsiElement element) {
    return element == null ? null : element.getNextSibling();
  }

  protected void processActions() throws IncorrectOperationException {
    Application app = ApplicationManager.getApplication();
    app.runWriteAction(new Runnable() {
      @Override
      public void run() {
        Document doc = FileDocumentManager.getInstance().getDocument(myPsiFile.getVirtualFile());
        PsiDocumentManager.getInstance(myPsiFile.getProject()).doPostponedOperationsAndUnblockDocument(doc);
        for (CommentAction action : myActions) {
          int start = action.getStart();
          int end = action.getEnd();

          switch (action.getType()) {
            case CommentAction.ACTION_INSERT:
              String comment = getCommentText(action.getPrefix(), action.getSuffix());
              if (!comment.isEmpty()) {
                doc.insertString(start, comment);
              }
              break;
            case CommentAction.ACTION_REPLACE:
              doc.replaceString(start, end, getCommentText("", ""));
              break;
            case CommentAction.ACTION_DELETE:
              doc.deleteString(start, end);
              break;
          }
        }
      }
    });
  }

  protected String getCommentText(String prefix, String suffix) {
    if (myCommentText == null) {
      String base = EntityUtil.decode(myCopyrightProfile.getNotice());
      if (base.isEmpty()) {
        myCommentText = "";
      }
      else {
        String expanded = null;
        try {
          expanded = VelocityHelper.evaluate(myPsiFile, getProject(), getModule(), base);
        }
        catch (Exception e) {
          expanded = "";
        }
        String cmt = FileTypeUtil.buildComment(myFileType, expanded, myFileConfig);
        myCommentText = StringUtil.convertLineSeparators(prefix + cmt + suffix);
      }
    }

    return myCommentText;
  }

  private static class CommentRange {
    private final PsiElement first;
    private final PsiElement last;

    public CommentRange(PsiElement first, PsiElement last) {
      this.first = first;
      this.last = last;
    }

    public PsiElement getFirst() {
      return first;
    }

    public PsiElement getLast() {
      return last;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      CommentRange that = (CommentRange)o;

      if (first != null ? !first.equals(that.first) : that.first != null) return false;
      if (last != null ? !last.equals(that.last) : that.last != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = first != null ? first.hashCode() : 0;
      result = 31 * result + (last != null ? last.hashCode() : 0);
      return result;
    }
  }

  protected static class CommentAction implements Comparable<CommentAction> {
    public static final int ACTION_INSERT = 1;
    public static final int ACTION_REPLACE = 2;
    public static final int ACTION_DELETE = 3;
    private final int type;
    private final int start;
    private final int end;
    private String prefix = null;
    private String suffix = null;

    public CommentAction(int pos, String prefix, String suffix) {
      type = ACTION_INSERT;
      start = pos;
      end = pos;
      this.prefix = prefix;
      this.suffix = suffix;
    }

    public CommentAction(int type, int start, int end) {
      this.type = type;
      this.start = start;
      this.end = end;
    }

    public int getType() {
      return type;
    }

    public int getStart() {
      return start;
    }

    public int getEnd() {
      return end;
    }

    public String getPrefix() {
      return prefix;
    }

    public String getSuffix() {
      return suffix;
    }

    @Override
    public int compareTo(CommentAction object) {
      int s = object.getStart();
      int diff = s - start;
      if (diff == 0) {
        diff = type == ACTION_INSERT ? 1 : -1;
      }

      return diff;
    }
  }

}
