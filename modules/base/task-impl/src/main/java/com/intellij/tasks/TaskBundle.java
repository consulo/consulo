package com.intellij.tasks;

import com.intellij.CommonBundle;
import com.intellij.reference.SoftReference;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import javax.annotation.Nonnull;
import java.lang.ref.Reference;
import java.util.ResourceBundle;

/**
 * Contains common and repository specific messages for "Tasks and Contexts" subsystem.
 * Initialization logic follows the same pattern as most of the other bundles in project.
 *
 * @author Mikhail Golubev
 */
public class TaskBundle {

  private static Reference<ResourceBundle> ourBundle;
  @NonNls
  private static final String BUNDLE = "com.intellij.tasks.TaskBundle";

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
      ourBundle = new SoftReference<ResourceBundle>(bundle);
    }
    return bundle;
  }
}
