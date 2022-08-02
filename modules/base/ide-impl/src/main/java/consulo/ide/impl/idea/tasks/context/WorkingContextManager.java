/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package consulo.ide.impl.idea.tasks.context;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.container.boot.ContainerPathManager;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.openapi.diagnostic.Logger;
import consulo.ide.impl.idea.openapi.util.JDOMUtil;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.task.Task;
import consulo.ide.impl.idea.util.NullableFunction;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.io.zip.JBZipEntry;
import consulo.ide.impl.idea.util.io.zip.JBZipFile;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.task.context.WorkingContextProvider;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class WorkingContextManager {

  private static final Logger LOG = Logger.getInstance(WorkingContextManager.class);
  @NonNls private static final String TASKS_FOLDER = "tasks";

  private final Project myProject;
  @NonNls private static final String TASKS_ZIP_POSTFIX = ".tasks.zip";
  @NonNls private static final String TASK_XML_POSTFIX = ".task.xml";
  private static final String CONTEXT_ZIP_POSTFIX = ".contexts.zip";
  private static final Comparator<JBZipEntry> ENTRY_COMPARATOR = new Comparator<JBZipEntry>() {
    public int compare(JBZipEntry o1, JBZipEntry o2) {
      return Long.signum(o2.getTime() - o1.getTime());
    }
  };

  public static WorkingContextManager getInstance(Project project) {
    return ServiceManager.getService(project, WorkingContextManager.class);
  }

  @Inject
  public WorkingContextManager(Project project) {
    myProject = project;
  }

  private void loadContext(Element fromElement) {
    for (WorkingContextProvider provider : WorkingContextProvider.EP_NAME.getExtensionList(myProject)) {
      try {
        Element child = fromElement.getChild(provider.getId());
        if (child != null) {
          provider.loadContext(child);
        }
      }
      catch (InvalidDataException e) {
        LOG.error(e);
      }
    }
  }

  public void saveContext(Element toElement) {
    for (WorkingContextProvider provider : WorkingContextProvider.EP_NAME.getExtensionList(myProject)) {
      try {
        Element child = new Element(provider.getId());
        provider.saveContext(child);
        toElement.addContent(child);
      }
      catch (WriteExternalException e) {
        LOG.error(e);
      }
    }
  }

  public void clearContext() {
    for (WorkingContextProvider provider : WorkingContextProvider.EP_NAME.getExtensionList(myProject)) {
      provider.clearContext();
    }
  }

  public void saveContext(Task task) {
    String entryName = task.getId() + TASK_XML_POSTFIX;
    saveContext(entryName, TASKS_ZIP_POSTFIX, task.getSummary());
  }

  public void saveContext(@Nullable String entryName, @Nullable String comment) {
    saveContext(entryName, CONTEXT_ZIP_POSTFIX, comment);
  }

  private synchronized void saveContext(@Nullable String entryName, String zipPostfix, @Nullable String comment) {
    JBZipFile archive = null;
    try {
      archive = getTasksArchive(zipPostfix);
      if (entryName == null) {
        int i = archive.getEntries().size();
        do {
          entryName = "context" + i++;
        } while (archive.getEntry("/" + entryName) != null);
      }
      JBZipEntry entry = archive.getOrCreateEntry("/" + entryName);
      if (comment != null) {
        entry.setComment(comment);
      }
      Element element = new Element("context");
      saveContext(element);
      String s = new XMLOutputter().outputString(element);
      entry.setData(s.getBytes("UTF-8"));
    }
    catch (IOException e) {
      LOG.error(e);
    }
    finally {
      closeArchive(archive);
    }
  }

  private JBZipFile getTasksArchive(String postfix) throws IOException {
    File file = getArchiveFile(postfix);
    try {
      return new JBZipFile(file);
    }
    catch (IOException e) {
      file.delete();
      JBZipFile zipFile = null;
      try {
        zipFile = new JBZipFile(file);
        Notifications.Bus.notify(new Notification("Tasks", "Context Data Corrupted",
                                                  "Context information history for " + myProject.getName() + " was corrupted.\n" +
                                                  "The history was replaced with empty one.", NotificationType.ERROR), myProject);
      }
      catch (IOException e1) {
        LOG.error("Can't repair form context data corruption", e1);
      }
      return zipFile;
    }
  }

  private File getArchiveFile(String postfix) {
    File tasksFolder = new File(ContainerPathManager.get().getConfigPath(), TASKS_FOLDER);
    if (!tasksFolder.exists()) {
      //noinspection ResultOfMethodCallIgnored
      tasksFolder.mkdir();
    }
    String projectName = FileUtil.sanitizeFileName(myProject.getName());
    return new File(tasksFolder, projectName + postfix);
  }

  public void restoreContext(@Nonnull Task task) {
    loadContext(TASKS_ZIP_POSTFIX, task.getId() + TASK_XML_POSTFIX);
  }

  private synchronized boolean loadContext(String zipPostfix, String entryName) {
    JBZipFile archive = null;
    try {
      archive = getTasksArchive(zipPostfix);
      JBZipEntry entry = archive.getEntry(StringUtil.startsWithChar(entryName, '/') ? entryName : "/" + entryName);
      if (entry != null) {
        byte[] bytes = entry.getData();
        Document document = JDOMUtil.loadDocument(new String(bytes));
        Element rootElement = document.getRootElement();
        loadContext(rootElement);
        return true;
      }
    }
    catch (Exception e) {
      LOG.error(e);
    }
    finally {
      closeArchive(archive);
    }
    return false;
  }

  private static void closeArchive(JBZipFile archive) {
    if (archive != null) {
      try {
        archive.close();
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
  }

  public List<ContextInfo> getContextHistory() {
    return getContextHistory(CONTEXT_ZIP_POSTFIX);
  }

  private synchronized List<ContextInfo> getContextHistory(String zipPostfix) {
    JBZipFile archive = null;
    try {
      archive = getTasksArchive(zipPostfix);
      List<JBZipEntry> entries = archive.getEntries();
      return ContainerUtil.mapNotNull(entries, new NullableFunction<JBZipEntry, ContextInfo>() {
        public ContextInfo fun(JBZipEntry entry) {
          return entry.getName().startsWith("/context") ? new ContextInfo(entry.getName(), entry.getTime(), entry.getComment()) : null;
        }
      });
    }
    catch (IOException e) {
      LOG.error(e);
      return Collections.emptyList();
    }
    finally {
      closeArchive(archive);
    }
  }

  public boolean loadContext(String name) {
    return loadContext(CONTEXT_ZIP_POSTFIX, name);
  }

  public void removeContext(String name) {
    removeContext(name, CONTEXT_ZIP_POSTFIX);
  }

  public void removeContext(Task task) {
    removeContext(task.getId(), TASKS_ZIP_POSTFIX);
  }

  private void removeContext(String name, String postfix) {
    JBZipFile archive = null;
    try {
      archive = getTasksArchive(postfix);
      JBZipEntry entry = archive.getEntry(name);
      if (entry != null) {
        archive.eraseEntry(entry);
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
    finally {
      closeArchive(archive);
    }
  }

  public void pack(int max, int delta) {
    pack(max, delta, CONTEXT_ZIP_POSTFIX);
    pack(max, delta, TASKS_ZIP_POSTFIX);
  }

  private synchronized void pack(int max, int delta, String zipPostfix) {
    JBZipFile archive = null;
    try {
      archive = getTasksArchive(zipPostfix);
      List<JBZipEntry> entries = archive.getEntries();
      if (entries.size() > max + delta) {
        JBZipEntry[] array = entries.toArray(new JBZipEntry[entries.size()]);
        Arrays.sort(array, ENTRY_COMPARATOR);
        for (int i = array.length - 1; i >= max; i--) {
          archive.eraseEntry(array[i]);
        }
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
    finally {
      closeArchive(archive);
    }
  }

  @TestOnly
  public File getContextFile() throws IOException {
    return getArchiveFile(CONTEXT_ZIP_POSTFIX);
  }
}
