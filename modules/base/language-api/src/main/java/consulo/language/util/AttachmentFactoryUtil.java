package consulo.language.util;

import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.logging.attachment.Attachment;
import consulo.logging.attachment.AttachmentFactory;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

/**
 * @author yole
 */
public class AttachmentFactoryUtil {
  private static final String ERROR_MESSAGE_PATTERN = "[[[Can't get file contents: {0}]]]";

  public static Attachment createContext(@Nonnull Object start, Object... more) {
    StringBuilder builder = new StringBuilder(String.valueOf(start));
    for (Object o : more) builder.append(",").append(o);
    return AttachmentFactory.get().create("current-context.txt", builder.length() > 0 ? builder.toString() : "(unknown)");
  }

  public static Attachment createAttachment(Document document) {
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    return AttachmentFactory.get().create(file != null ? file.getPath() : "unknown.txt", document.getText());
  }

  public static Attachment createAttachment(@Nonnull VirtualFile file) {
    return AttachmentFactory.get().create(file.getPresentableUrl(), getBytes(file), file.getFileType().isBinary() ? "File is binary" : file.loadText().toString());
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
