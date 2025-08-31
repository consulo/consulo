// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.editor.richcopy;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Caret;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.markup.MarkupModel;
import consulo.colorScheme.EditorColorsScheme;
import consulo.document.Document;
import consulo.ide.impl.idea.codeInsight.editorActions.CopyPastePostProcessor;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.openapi.editor.richcopy.SyntaxInfoBuilder.Context;
import consulo.ide.impl.idea.openapi.editor.richcopy.SyntaxInfoBuilder.MyMarkupIterator;
import consulo.ide.impl.idea.openapi.editor.richcopy.model.SyntaxInfo;
import consulo.ide.impl.idea.openapi.editor.richcopy.settings.RichCopySettings;
import consulo.ide.impl.idea.openapi.editor.richcopy.view.HtmlTransferableData;
import consulo.ide.impl.idea.openapi.editor.richcopy.view.RawTextWithMarkup;
import consulo.ide.impl.idea.openapi.editor.richcopy.view.RtfTransferableData;
import consulo.language.editor.highlight.HighlighterFactory;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.logging.attachment.AttachmentFactory;
import consulo.logging.attachment.RuntimeExceptionWithAttachments;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static consulo.ide.impl.idea.openapi.editor.richcopy.SyntaxInfoBuilder.createMarkupIterator;

/**
 * Generates text with markup (in RTF and HTML formats) for interaction via clipboard with third-party applications.
 * <p>
 * Interoperability with the following applications was tested:
 * MS Office 2010 (Word, PowerPoint, Outlook), OpenOffice (Writer, Impress), Gmail, Mac TextEdit, Mac Mail, Mac Keynote.
 */
@ExtensionImpl
public class TextWithMarkupProcessor extends CopyPastePostProcessor<RawTextWithMarkup> {
  private static final Logger LOG = Logger.getInstance(TextWithMarkupProcessor.class);

  private List<RawTextWithMarkup> myResult;

  @Nonnull
  @Override
  public List<RawTextWithMarkup> collectTransferableData(PsiFile file, Editor editor, int[] startOffsets, int[] endOffsets) {
    if (!RichCopySettings.getInstance().isEnabled()) {
      return Collections.emptyList();
    }

    EditorHighlighter highlighter = null;

    try {
      RichCopySettings settings = RichCopySettings.getInstance();
      List<Caret> carets = editor.getCaretModel().getAllCarets();
      Caret firstCaret = carets.get(0);
      int indentSymbolsToStrip;
      int firstLineStartOffset;
      if (Registry.is("editor.richcopy.strip.indents") && carets.size() == 1) {
        Pair<Integer, Integer> p = calcIndentSymbolsToStrip(editor.getDocument(), firstCaret.getSelectionStart(), firstCaret.getSelectionEnd());
        firstLineStartOffset = p.first;
        indentSymbolsToStrip = p.second;
      }
      else {
        firstLineStartOffset = firstCaret.getSelectionStart();
        indentSymbolsToStrip = 0;
      }
      logInitial(editor, startOffsets, endOffsets, indentSymbolsToStrip, firstLineStartOffset);
      CharSequence text = editor.getDocument().getCharsSequence();
      EditorColorsScheme schemeToUse = settings.getColorsScheme(editor.getColorsScheme());
      highlighter = HighlighterFactory.createHighlighter(file.getViewProvider().getVirtualFile(), schemeToUse, file.getProject());
      highlighter.setText(text);
      MarkupModel markupModel = DocumentMarkupModel.forDocument(editor.getDocument(), file.getProject(), false);
      Context context = new Context(text, schemeToUse, indentSymbolsToStrip);
      int endOffset = 0;
      Caret prevCaret = null;

      for (Caret caret : carets) {
        int caretSelectionStart = caret.getSelectionStart();
        int caretSelectionEnd = caret.getSelectionEnd();
        int startOffsetToUse;
        int additionalShift = 0;
        if (caret == firstCaret) {
          startOffsetToUse = firstLineStartOffset;
        }
        else {
          startOffsetToUse = caretSelectionStart;
          assert prevCaret != null;
          String prevCaretSelectedText = prevCaret.getSelectedText();
          // Block selection fills short lines by white spaces
          int fillStringLength = prevCaretSelectedText == null ? 0 : prevCaretSelectedText.length() - (prevCaret.getSelectionEnd() - prevCaret.getSelectionStart());
          context.addCharacter(endOffset + fillStringLength);
          additionalShift = fillStringLength + 1;
        }
        context.reset(endOffset - caretSelectionStart + additionalShift);
        endOffset = caretSelectionEnd;
        prevCaret = caret;
        if (endOffset <= startOffsetToUse) {
          continue;
        }
        MyMarkupIterator markupIterator = createMarkupIterator(highlighter, text, schemeToUse, markupModel, startOffsetToUse, endOffset);
        try {
          context.iterate(markupIterator, endOffset);
        }
        finally {
          markupIterator.dispose();
        }
      }
      SyntaxInfo syntaxInfo = context.finish();
      logSyntaxInfo(syntaxInfo);

      createResult(syntaxInfo, editor);
      return ObjectUtil.notNull(myResult, Collections.emptyList());
    }
    catch (Throwable t) {
      // catching the exception so that the rest of copy/paste functionality can still work fine
      LOG.error(new RuntimeExceptionWithAttachments("Error generating text with markup", t,
                                                    AttachmentFactory.get().create("highlighter.txt", String.valueOf(highlighter))));
    }
    return Collections.emptyList();
  }

  void createResult(SyntaxInfo syntaxInfo, Editor editor) {
    myResult = new ArrayList<>(2);
    myResult.add(new HtmlTransferableData(syntaxInfo, EditorUtil.getTabSize(editor)));
    myResult.add(new RtfTransferableData(syntaxInfo));
  }

  protected void setRawText(String rawText) {
    if (myResult == null) {
      return;
    }
    for (RawTextWithMarkup data : myResult) {
      data.setRawText(rawText);
    }
    myResult = null;
  }

  private static void logInitial(@Nonnull Editor editor, @Nonnull int[] startOffsets, @Nonnull int[] endOffsets, int indentSymbolsToStrip, int firstLineStartOffset) {
    if (!LOG.isDebugEnabled()) {
      return;
    }

    StringBuilder buffer = new StringBuilder();
    Document document = editor.getDocument();
    CharSequence text = document.getCharsSequence();
    for (int i = 0; i < startOffsets.length; i++) {
      int start = startOffsets[i];
      int lineStart = document.getLineStartOffset(document.getLineNumber(start));
      int end = endOffsets[i];
      int lineEnd = document.getLineEndOffset(document.getLineNumber(end));
      buffer.append("    region #").append(i).append(": ").append(start).append('-').append(end).append(", text at range ").append(lineStart).append('-').append(lineEnd).append(": \n'")
              .append(text.subSequence(lineStart, lineEnd)).append("'\n");
    }
    if (buffer.length() > 0) {
      buffer.setLength(buffer.length() - 1);
    }
    LOG.debug(
            String.format("Preparing syntax-aware text. Given: %s selection, indent symbols to strip=%d, first line start offset=%d, selected text:%n%s", startOffsets.length > 1 ? "block" : "regular",
                          indentSymbolsToStrip, firstLineStartOffset, buffer));
  }

  private static void logSyntaxInfo(@Nonnull SyntaxInfo info) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Constructed syntax info: " + info);
    }
  }

  private static Pair<Integer/* start offset to use */, Integer /* indent symbols to strip */> calcIndentSymbolsToStrip(@Nonnull Document document, int startOffset, int endOffset) {
    int startLine = document.getLineNumber(startOffset);
    int endLine = document.getLineNumber(endOffset);
    CharSequence text = document.getCharsSequence();
    int maximumCommonIndent = Integer.MAX_VALUE;
    int firstLineStart = startOffset;
    int firstLineEnd = startOffset;
    for (int line = startLine; line <= endLine; line++) {
      int lineStartOffset = document.getLineStartOffset(line);
      int lineEndOffset = document.getLineEndOffset(line);
      if (line == startLine) {
        firstLineStart = lineStartOffset;
        firstLineEnd = lineEndOffset;
      }
      int nonWsOffset = lineEndOffset;
      for (int i = lineStartOffset; i < lineEndOffset && (i - lineStartOffset) < maximumCommonIndent && i < endOffset; i++) {
        char c = text.charAt(i);
        if (c != ' ' && c != '\t') {
          nonWsOffset = i;
          break;
        }
      }
      if (nonWsOffset >= lineEndOffset) {
        continue; // Blank line
      }
      int indent = nonWsOffset - lineStartOffset;
      maximumCommonIndent = Math.min(maximumCommonIndent, indent);
      if (maximumCommonIndent == 0) {
        break;
      }
    }
    int startOffsetToUse = Math.min(firstLineEnd, Math.max(startOffset, firstLineStart + maximumCommonIndent));
    return Pair.create(startOffsetToUse, maximumCommonIndent);
  }

}
