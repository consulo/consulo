// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle.internal;

import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.codeStyle.FormattingDocumentModel;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.logging.attachment.AttachmentFactory;

import jakarta.annotation.Nonnull;
import java.util.List;

public class RangesAssert {
  private static final Logger LOG = Logger.getInstance(RangesAssert.class);

  public void assertInvalidRanges(final int startOffset, final int newEndOffset, FormattingDocumentModel model, String message) {
    final StringBuilder buffer = new StringBuilder();

    int minOffset = Math.max(Math.min(startOffset, newEndOffset), 0);
    int maxOffset = Math.min(Math.max(startOffset, newEndOffset), model.getTextLength());

    final StringBuilder messageBuffer = new StringBuilder();
    messageBuffer.append(message);
    Language language = model.getFile().getLanguage();
    messageBuffer.append(" in #").append(language.getDisplayName());

    messageBuffer.append(" #formatter");
    messageBuffer.append("\nRange: [").append(startOffset).append(",").append(newEndOffset).append("], ").append("text fragment: [").append(minOffset).append(",").append(maxOffset).append("]\n");

    buffer.append("Fragment text: '").append(model.getText(new TextRange(minOffset, maxOffset))).append("'\n");
    buffer.append("File text:(").append(model.getTextLength()).append(")\n'");
    buffer.append(model.getText(new TextRange(0, model.getTextLength())).toString());
    buffer.append("'\n");
    buffer.append("model (").append(model.getClass()).append("): ").append(model);

    buffer.append("Psi Tree:\n");
    final PsiFile file = model.getFile();
    final List<PsiFile> roots = file.getViewProvider().getAllFiles();
    for (PsiFile root : roots) {
      buffer.append("Root ");
      CodeStyleInternalHelper.getInstance().debugTreeToBuffer(buffer, root.getNode(), 0, false, true, true, true);
    }
    buffer.append('\n');

    LOG.error(message.toString(), new Throwable(), AttachmentFactory.get().create("current-context.txt", buffer.toString()));
  }

  public boolean checkChildRange(@Nonnull TextRange parentRange, @Nonnull TextRange childRange, @Nonnull FormattingDocumentModel model) {
    if (childRange.getStartOffset() < parentRange.getStartOffset()) {
      assertInvalidRanges(childRange.getStartOffset(), parentRange.getStartOffset(), model, "child block start is less than parent block start");
      return false;
    }

    if (childRange.getEndOffset() > parentRange.getEndOffset()) {
      assertInvalidRanges(childRange.getStartOffset(), parentRange.getStartOffset(), model, "child block end is after parent block end");
      return false;
    }
    return true;
  }

}
