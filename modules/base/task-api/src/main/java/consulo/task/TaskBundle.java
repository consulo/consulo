package consulo.task;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.internal.MigratedExtensionsTo;
import consulo.application.CommonBundle;
import consulo.task.localize.TaskLocalize;
import consulo.util.lang.ref.SoftReference;
import jakarta.annotation.Nonnull;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.util.ResourceBundle;

/**
 * Contains common and repository specific messages for "Tasks and Contexts" subsystem.
 * Initialization logic follows the same pattern as most of the other bundles in project.
 *
 * @author Mikhail Golubev
 */
@Deprecated
@DeprecationInfo("Use TaskLocalize")
@MigratedExtensionsTo(TaskLocalize.class)
public class TaskBundle {

  private static Reference<ResourceBundle> ourBundle;

  private static final String BUNDLE = "consulo.task.TaskBundle";

  private TaskBundle() {
    // empty
  }

  public static String message(@Nonnull @PropertyKey(resourceBundle = BUNDLE) String key, @Nonnull Object... params) {
    return CommonBundle.message(getBundle(), key, params);
  }

  @Nonnull
  public static String messageForStatusCode(int statusCode) {
    if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
      return message("failure.login");
    }
    else if (statusCode == HttpStatus.SC_FORBIDDEN) {
      return message("failure.permissions");
    }
    return message("failure.http.error", statusCode, "");
  }

  private static ResourceBundle getBundle() {
    ResourceBundle bundle = SoftReference.dereference(ourBundle);
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE);
      ourBundle = new SoftReference<>(bundle);
    }
    return bundle;
  }
}
