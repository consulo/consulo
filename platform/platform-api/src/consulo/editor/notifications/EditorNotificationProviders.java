package consulo.editor.notifications;

import com.intellij.openapi.project.Project;
import com.intellij.util.NotNullFunction;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import consulo.annotations.Exported;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 12.11.2015
 */
public class EditorNotificationProviders {
  private static final List<NotNullFunction<Project, EditorNotificationProvider<?>>> ourAdditionalEditorProviderFactories =
          new ArrayList<NotNullFunction<Project, EditorNotificationProvider<?>>>();

  @NotNull
  public static List<EditorNotificationProvider<?>> createProviders(@NotNull Project project) {
    EditorNotificationProvider<?>[] extensions = EditorNotificationProvider.EP_NAME.getExtensions(project);
    List<EditorNotificationProvider<?>> providers =
            new ArrayList<EditorNotificationProvider<?>>(extensions.length + ourAdditionalEditorProviderFactories.size());
    ContainerUtil.addAll(providers, extensions);
    for (NotNullFunction<Project, EditorNotificationProvider<?>> function : ourAdditionalEditorProviderFactories) {
      providers.add(function.fun(project));
    }
    return providers;
  }

  @Exported
  public static void registerProvider(@NotNull NotNullFunction<Project, EditorNotificationProvider<?>> function) {
    ourAdditionalEditorProviderFactories.add(function);
  }
}
