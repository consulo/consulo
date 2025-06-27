/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.internal;

import consulo.application.dumb.DumbAware;
import consulo.language.editor.impl.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.Notifications;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.io.FileUtil;
import consulo.util.io.ResourceUtil;
import jakarta.annotation.Nonnull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author stathik
 * @since 2003-11-06
 */
public class DumpInspectionDescriptionsAction extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(DumpInspectionDescriptionsAction.class);

  public DumpInspectionDescriptionsAction() {
    super("Dump inspection descriptions");
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull final AnActionEvent event) {
    final InspectionProfile profile = (InspectionProfile)InspectionProfileManager.getInstance().getRootProfile();
    final InspectionToolWrapper[] tools = profile.getInspectionTools(null);

    final Collection<String> classes = new TreeSet<>();
    final Map<String, Collection<String>> groups = new TreeMap<>();

    final String tempDirectory = FileUtil.getTempDirectory();
    final File descDirectory = new File(tempDirectory, "inspections");
    if (!descDirectory.mkdirs() && !descDirectory.isDirectory()) {
      LOG.error("Unable to create directory: " + descDirectory.getAbsolutePath());
      return;
    }

    for (InspectionToolWrapper toolWrapper : tools) {
      classes.add(getInspectionClass(toolWrapper).getName());

      final String group = getGroupName(toolWrapper);
      Collection<String> names = groups.get(group);
      if (names == null) groups.put(group, (names = new TreeSet<>()));
      names.add(toolWrapper.getShortName());

      final URL url = getDescriptionUrl(toolWrapper);
      if (url != null) {
        doDump(new File(descDirectory, toolWrapper.getShortName() + ".html"), writer-> writer.write(ResourceUtil.loadText(url)));
      }
    }
    doNotify("Inspection descriptions dumped to\n" + descDirectory.getAbsolutePath());

    final File fqnListFile = new File(tempDirectory, "inspection_fqn_list.txt");
    final boolean fqnOk = doDump(fqnListFile, writer-> {
      for (String name : classes) {
        writer.write(name);
        writer.newLine();
      }
    });
    if (fqnOk) {
      doNotify("Inspection class names dumped to\n" + fqnListFile.getAbsolutePath());
    }

    final File groupsFile = new File(tempDirectory, "inspection_groups.txt");
    final boolean groupsOk = doDump(groupsFile, writer-> {
      for (Map.Entry<String, Collection<String>> entry : groups.entrySet()) {
        writer.write(entry.getKey());
        writer.write(':');
        writer.newLine();
        for (String name : entry.getValue()) {
          writer.write("  ");
          writer.write(name);
          writer.newLine();
        }
      }
    });
    if (groupsOk) {
      doNotify("Inspection groups dumped to\n" + fqnListFile.getAbsolutePath());
    }
  }

  private static Class getInspectionClass(final InspectionToolWrapper toolWrapper) {
    return toolWrapper instanceof LocalInspectionToolWrapper localInspectionToolWrapper
      ? localInspectionToolWrapper.getTool().getClass() : toolWrapper.getClass();
  }

  private static String getGroupName(final InspectionToolWrapper toolWrapper) {
    return toolWrapper.getJoinedGroupPath();
  }

  private static URL getDescriptionUrl(final InspectionToolWrapper toolWrapper) {
    final Class aClass = getInspectionClass(toolWrapper);
    return ResourceUtil.getResource(aClass, "/inspectionDescriptions", toolWrapper.getShortName() + ".html");
  }

  private interface Processor {
    void process(BufferedWriter writer) throws Exception;
  }

  private static boolean doDump(final File file, final Processor processor) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
      processor.process(writer);
      return true;
    }
    catch (Exception e) {
      LOG.error(e);
      return false;
    }
  }

  private static void doNotify(String message) {
    NotificationService.getInstance()
        .newInfo(Notifications.SYSTEM_MESSAGES_GROUP)
        .title(LocalizeValue.localizeTODO("Inspection descriptions dumped"))
        .content(LocalizeValue.ofNullable(message))
        .notify(null);
  }
}