package consulo.task;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.internal.MigratedExtensionsTo;
import consulo.application.CommonBundle;
import consulo.task.localize.TaskLocalize;
import consulo.util.lang.ref.SoftReference;
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

  public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return CommonBundle.message(getBundle(), key, params);
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
