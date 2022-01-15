package com.intellij.diagnostic;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.logging.attachment.Attachment;
import javax.annotation.Nonnull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

/**
 * @author yole
 */
public class AttachmentFactory {
  private static final String ERROR_MESSAGE_PATTERN = "[[[Can't get file contents: {0}]]]";

  public static Attachment createContext(@Nonnull Object start, Object... more) {
    StringBuilder builder = new StringBuilder(String.valueOf(start));
    for (Object o : more) builder.append(",").append(o);
    return consulo.logging.attachment.AttachmentFactory.get().create("current-context.txt", builder.length() > 0 ? builder.toString() : "(unknown)");
  }

  public static Attachment createAttachment(Document document) {
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    return consulo.logging.attachment.AttachmentFactory.get().create(file != null ? file.getPath() : "unknown.txt", document.getText());
  }

  public static Attachment createAttachment(@Nonnull VirtualFile file) {
    return consulo.logging.attachment.AttachmentFactory.get().create(file.getPresentableUrl(), getBytes(file),
                          file.getFileType().isBinary() ? "File is binary" : LoadTextUtil.loadText(file).toString());
  }

  private static byte[] getBytes(VirtualFile file) {
    try {
      return file.contentsToByteArray();
    }
    catch (IOException e) {
      return MessageFormat.format(ERROR_MESSAGE_PATTERN, e.getMessage()).getBytes(StandardCharsets.UTF_8);
    }
  }
}
