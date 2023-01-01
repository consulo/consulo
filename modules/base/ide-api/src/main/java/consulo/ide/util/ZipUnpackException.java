package consulo.ide.util;

/**
 * @author Sergey Simonchik
 */
public class ZipUnpackException extends Exception {
  public ZipUnpackException(String message) {
    super(message);
  }

  public ZipUnpackException(String message, Throwable cause) {
    super(message, cause);
  }
}
