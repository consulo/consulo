/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.update;

import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.Pair;
import consulo.util.xml.serializer.DefaultJDOMExternalizer;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.status.FileStatus;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.*;

public class FileGroup implements JDOMExternalizable {

  public String myUpdateName;
  public String myStatusName;
  private final Map<String, String> myErrorsMap = new HashMap<>();

  private final Collection<UpdatedFile> myFiles = new ArrayList<>();
  public boolean mySupportsDeletion;
  public boolean myCanBeAbsent;
  public String myId;
  private static final String PATH = "PATH";
  private static final String VCS_ATTRIBUTE = "vcs";
  private static final String REVISION_ATTRIBUTE = "revision";

  private final List<FileGroup> myChildren = new ArrayList<>();
  private static final String FILE_GROUP_ELEMENT_NAME = "FILE-GROUP";

  public static final String MODIFIED_ID = "MODIFIED";
  public static final String MERGED_WITH_CONFLICT_ID = "MERGED_WITH_CONFLICTS";
  public static final String MERGED_WITH_TREE_CONFLICT = "MERGED_WITH_TREE_CONFLICT";
  public static final String MERGED_WITH_PROPERTY_CONFLICT_ID = "MERGED_WITH_PROPERTY_CONFLICT";
  public static final String MERGED_ID = "MERGED";
  public static final String UNKNOWN_ID = "UNKNOWN";
  public static final String LOCALLY_ADDED_ID = "LOCALLY_ADDED";
  public static final String LOCALLY_REMOVED_ID = "LOCALLY_REMOVED";
  public static final String UPDATED_ID = "UPDATED";
  public static final String REMOVED_FROM_REPOSITORY_ID = "REMOVED_FROM_REPOSITORY";
  public static final String CREATED_ID = "CREATED";
  public static final String RESTORED_ID = "RESTORED";
  public static final String CHANGED_ON_SERVER_ID = "CHANGED_ON_SERVER";
  public static final String SKIPPED_ID = "SKIPPED";
  public static final String SWITCHED_ID = "SWITCHED";

  /**
   * @param updateName       - Name for "update" action
   * @param statusName       - Name for "status action"
   * @param supportsDeletion - User can perform delete action for files from the group
   * @param id               - Using in order to find the group
   * @param canBeAbsent      - If canBeAbsent == true absent files from the group will not be marked as invalid
   */
  public FileGroup(String updateName, String statusName, boolean supportsDeletion, String id, boolean canBeAbsent) {
    mySupportsDeletion = supportsDeletion;
    myId = id;
    myCanBeAbsent = canBeAbsent;
    myUpdateName = updateName;
    myStatusName = statusName;
  }

  public FileGroup() {
  }

  public void addChild(FileGroup child) {
    myChildren.add(child);
  }

  public boolean getSupportsDeletion() {
    return mySupportsDeletion;
  }

  public void addError(@Nonnull String path, @Nonnull String error) {
    myErrorsMap.put(path, error);
  }

  @Nonnull
  public Map<String, String> getErrorsMap() {
    return myErrorsMap;
  }

  public void add(@Nonnull String path, @Nonnull String vcsName, @Nullable VcsRevisionNumber revision) {
    myFiles.add(new UpdatedFile(path, vcsName, revision == null ? "" : revision.asString()));
  }

  public void add(@Nonnull String path, @Nonnull VcsKey vcsKey, @Nullable VcsRevisionNumber revision) {
    myFiles.add(new UpdatedFile(path, vcsKey, revision == null ? "" : revision.asString()));
  }

  public void remove(String path) {
    for (UpdatedFile file : myFiles) {
      if (file.getPath().equals(path)) {
        myFiles.remove(file);
        break;
      }
    }
  }

  public int getImmediateFilesSize() {
    return myFiles.size();
  }

  public Collection<String> getFiles() {
    ArrayList<String> files = new ArrayList<>();
    for (UpdatedFile file : myFiles) {
      files.add(file.getPath());
    }
    return files;
  }

  public Collection<UpdatedFile> getUpdatedFiles() {
    return new ArrayList<>(myFiles);
  }

  public List<Pair<String, VcsRevisionNumber>> getFilesAndRevisions(ProjectLevelVcsManager vcsManager) {
    ArrayList<Pair<String, VcsRevisionNumber>> files = new ArrayList<>();
    for (UpdatedFile file : myFiles) {
      VcsRevisionNumber number = getRevision(vcsManager, file);
      files.add(Pair.create(file.getPath(), number));
    }
    return files;
  }

  public boolean isEmpty() {
    if (!myFiles.isEmpty()) return false;
    for (FileGroup child : myChildren) {
      if (!child.isEmpty()) return false;
    }
    return true;
  }

  public SimpleTextAttributes getInvalidAttributes() {
    if (myCanBeAbsent) {
      return new SimpleTextAttributes(Font.PLAIN, TargetAWT.to(FileStatus.DELETED.getColor()));
    }
    else {
      return SimpleTextAttributes.ERROR_ATTRIBUTES;
    }
  }

  public String getId() {
    return myId;
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    DefaultJDOMExternalizer.writeExternal(this, element);
    for (UpdatedFile file : myFiles) {
      Element path = new Element(PATH);
      path.setText(file.getPath());
      if (file.getVcsName() != null) {
        path.setAttribute(VCS_ATTRIBUTE, file.getVcsName());
      }
      if (file.getRevision() != null) {
        path.setAttribute(REVISION_ATTRIBUTE, file.getRevision());
      }
      element.addContent(path);
    }
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, element);
    List pathElements = element.getChildren(PATH);
    for (Object pathElement1 : pathElements) {
      Element pathElement = (Element)pathElement1;
      String path = pathElement.getText();
      String vcsName = pathElement.getAttributeValue(VCS_ATTRIBUTE);
      String revision = pathElement.getAttributeValue(REVISION_ATTRIBUTE);
      if (vcsName != null) {   // ignore UpdatedFiles from previous version
        myFiles.add(new UpdatedFile(path, vcsName, revision));
      }
    }
  }

  public List<FileGroup> getChildren() {
    return myChildren;
  }

  public static void writeGroupsToElement(List<FileGroup> groups, Element element) throws WriteExternalException {
    for (FileGroup fileGroup : groups) {
      Element groupElement = new Element(FILE_GROUP_ELEMENT_NAME);
      element.addContent(groupElement);
      fileGroup.writeExternal(groupElement);
      writeGroupsToElement(fileGroup.getChildren(), groupElement);
    }
  }

  public static void readGroupsFromElement(List<FileGroup> groups, Element element) throws InvalidDataException {
    List groupElements = element.getChildren();
    for (Object groupElement1 : groupElements) {
      Element groupElement = (Element)groupElement1;
      FileGroup fileGroup = new FileGroup();
      fileGroup.readExternal(groupElement);
      groups.add(fileGroup);
      readGroupsFromElement(fileGroup.myChildren, groupElement);
    }
  }

  public String getStatusName() {
    return myStatusName;
  }

  public String getUpdateName() {
    return myUpdateName;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public String toString() {
    return myId + " " + myFiles.size() + " items";
  }

  @Nullable
  public VcsRevisionNumber getRevision(ProjectLevelVcsManager vcsManager, String path) {
    for (UpdatedFile file : myFiles) {
      if (file.getPath().equals(path)) {
        return getRevision(vcsManager, file);
      }
    }
    return null;
  }

  @Nullable
  private static VcsRevisionNumber getRevision(ProjectLevelVcsManager vcsManager, UpdatedFile file) {
    String vcsName = file.getVcsName();
    String revision = file.getRevision();
    if (vcsName != null && revision != null) {
      AbstractVcs vcs = vcsManager.findVcsByName(vcsName);
      if (vcs != null) {
        try {
          return vcs.parseRevisionNumber(revision, VcsUtil.getFilePath(file.getPath()));
        }
        catch (VcsException e) {
          //
        }
      }
    }
    return null;
  }

  public void setRevisions(String path, AbstractVcs vcs, VcsRevisionNumber revision) {
    for (UpdatedFile file : myFiles) {
      if (file.getPath().startsWith(path)) {
        file.setVcsKey(vcs.getKeyInstanceMethod());
        file.setRevision(revision.asString());
      }
    }
    for (FileGroup group : myChildren) {
      group.setRevisions(path, vcs, revision);
    }
  }

  public static class UpdatedFile {
    private final String myPath;
    private String myVcsName;
    private String myRevision;

    public UpdatedFile(String path) {
      myPath = path;
    }

    public UpdatedFile(String path, @Nonnull VcsKey vcsKey, String revision) {
      myPath = path;
      myVcsName = vcsKey.getName();
      myRevision = revision;
    }

    private UpdatedFile(String path, @Nonnull String vcsName, String revision) {
      myPath = path;
      myVcsName = vcsName;
      myRevision = revision;
    }

    public String getPath() {
      return myPath;
    }

    public String getVcsName() {
      return myVcsName;
    }

    public void setVcsKey(VcsKey vcsKey) {
      myVcsName = vcsKey.getName();
    }

    public String getRevision() {
      return myRevision;
    }

    public void setRevision(String revision) {
      myRevision = revision;
    }
  }
}
