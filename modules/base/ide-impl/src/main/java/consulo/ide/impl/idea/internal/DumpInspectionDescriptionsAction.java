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
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.impl.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.logging.Logger;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.io.ResourceUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

/**
 * @author stathik
 * Date: Nov 6, 2003
 */
public class DumpInspectionDescriptionsAction extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(DumpInspectionDescriptionsAction.class);

  public DumpInspectionDescriptionsAction() {
    super("Dump inspection descriptions");
  }

  @Override
  public void actionPerformed(final AnActionEvent event) {
    final InspectionProfile profile = (InspectionProfile)InspectionProfileManager.getInstance().getRootProfile();
    final InspectionToolWrapper[] tools = (InspectionToolWrapper[])profile.getInspectionTools(null);

    final Collection<String> classes = ContainerUtil.newTreeSet();
    final Map<String, Collection<String>> groups = ContainerUtil.newTreeMap();

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
      if (names == null) groups.put(group, (names = ContainerUtil.newTreeSet()));
      names.add(toolWrapper.getShortName());

      final URL url = getDescriptionUrl(toolWrapper);
      if (url != null) {
        doDump(new File(descDirectory, toolWrapper.getShortName() + ".html"), new Processor() {
          @Override public void process(BufferedWriter writer) throws Exception {
            writer.write(ResourceUtil.loadText(url));
          }
        });
      }
    }
    doNotify("Inspection descriptions dumped to\n" + descDirectory.getAbsolutePath());

    final File fqnListFile = new File(tempDirectory, "inspection_fqn_list.txt");
    final boolean fqnOk = doDump(fqnListFile, new Processor() {
      @Override public void process(BufferedWriter writer) throws Exception {
        for (String name : classes) {
          writer.write(name);
          writer.newLine();
        }
      }
    });
    if (fqnOk) {
      doNotify("Inspection class names dumped to\n" + fqnListFile.getAbsolutePath());
    }

    final File groupsFile = new File(tempDirectory, "inspection_groups.txt");
    final boolean groupsOk = doDump(groupsFile, new Processor() {
      @Override public void process(BufferedWriter writer) throws Exception {
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
      }
    });
    if (groupsOk) {
      doNotify("Inspection groups dumped to\n" + fqnListFile.getAbsolutePath());
    }
  }

  private static Class getInspectionClass(final InspectionToolWrapper toolWrapper) {
    return toolWrapper instanceof LocalInspectionToolWrapper ? ((LocalInspectionToolWrapper)toolWrapper).getTool().getClass() : toolWrapper.getClass();
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
    try {
      final BufferedWriter writer = new BufferedWriter(new FileWriter(file));
      try {
        processor.process(writer);
        return true;
      }
      finally {
        writer.close();
      }
    }
    catch (Exception e) {
      LOG.error(e);
      return false;
    }
  }

  private static void doNotify(final String message) {
    Notifications.Bus.notify(new Notification(Notifications.SYSTEM_MESSAGES_GROUP, "Inspection descriptions dumped", message, NotificationType.INFORMATION));
  }
}