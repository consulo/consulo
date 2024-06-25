// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.change;

import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListData;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.util.VcsUtil;
import jakarta.annotation.Nonnull;
import org.jdom.Element;
import org.jdom.Verifier;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nullable;

import java.util.*;

public final class ChangeListManagerSerialization {
  private static final int DISABLED_CHANGES_THRESHOLD = 100;

  @NonNls
  private static final String ATT_ID = "id";
  @NonNls
  private static final String ATT_NAME = "name";
  @NonNls
  private static final String ATT_COMMENT = "comment";
  @NonNls
  private static final String ATT_DEFAULT = "default";
  @NonNls
  private static final String ATT_VALUE_TRUE = "true";
  @NonNls
  private static final String ATT_CHANGE_BEFORE_PATH = "beforePath";
  @NonNls
  private static final String ATT_CHANGE_AFTER_PATH = "afterPath";
  @NonNls
  private static final String ATT_CHANGE_BEFORE_PATH_ESCAPED = "beforePathEscaped";
  @NonNls
  private static final String ATT_CHANGE_AFTER_PATH_ESCAPED = "afterPathEscaped";
  @NonNls
  private static final String ATT_CHANGE_BEFORE_PATH_IS_DIR = "beforeDir";
  @NonNls
  private static final String ATT_CHANGE_AFTER_PATH_IS_DIR = "afterDir";
  @NonNls
  private static final String NODE_LIST = "list";
  @NonNls
  private static final String NODE_CHANGE = "change";

  public static void writeExternal(@Nonnull Element element,
                                   @Nullable List<? extends LocalChangeList> changeLists,
                                   boolean areChangeListsEnabled) {
    if (changeLists == null) return;
    for (LocalChangeList list : changeLists) {
      element.addContent(writeChangeList(list, areChangeListsEnabled));
    }
  }

  @Nonnull
  public  static List<LocalChangeListImpl> readExternal(@Nonnull Element element, @Nonnull Project project) {
    List<LocalChangeListImpl> lists = new ArrayList<>();
    for (Element listNode : element.getChildren(NODE_LIST)) {
      lists.add(readChangeList(listNode, project));
    }
    return new ArrayList<>(removeDuplicatedLists(lists));
  }

  @Nonnull
  private static Collection<LocalChangeListImpl> removeDuplicatedLists(@Nonnull List<LocalChangeListImpl> lists) {
    // workaround for loading incorrect settings (with duplicate changelist names)

    boolean hasDefault = false;
    Map<String, LocalChangeListImpl> map = new HashMap<>();

    for (LocalChangeListImpl list : lists) {
      if (list.isDefault() && hasDefault) {
        list = new LocalChangeListImpl.Builder(list).setDefault(false).build();
      }
      hasDefault |= list.isDefault();

      LocalChangeListImpl otherList = map.get(list.getName());
      if (otherList != null) {
        list = new LocalChangeListImpl.Builder(otherList)
          .setChanges(ContainerUtil.union(list.getChanges(), otherList.getChanges()))
          .setDefault(list.isDefault() || otherList.isDefault())
          .build();
      }

      map.put(list.getName(), list);
    }
    return map.values();
  }

  @Nonnull
  private static Element writeChangeList(@Nonnull LocalChangeList list, boolean areChangeListsEnabled) {
    Element listNode = new Element(NODE_LIST);

    if (list.isDefault()) listNode.setAttribute(ATT_DEFAULT, ATT_VALUE_TRUE);

    listNode.setAttribute(ATT_ID, list.getId());
    listNode.setAttribute(ATT_NAME, list.getName());
    String comment = list.getComment();
    if (comment != null) {
      listNode.setAttribute(ATT_COMMENT, comment);
    }

    Object listData = list.getData();
    if (listData instanceof ChangeListData) {
      listNode.addContent(ChangeListData.writeExternal((ChangeListData)listData));
    }

    Collection<Change> changes = list.getChanges();
    if (areChangeListsEnabled || changes.size() < DISABLED_CHANGES_THRESHOLD) {
      List<Change> sortedChanges = ContainerUtil.sorted(changes, new ChangeComparator());
      for (Change change : sortedChanges) {
        listNode.addContent(writeChange(change));
      }
    }

    return listNode;
  }

  private static class ChangeComparator implements Comparator<Change> {
    @Override
    public int compare(Change o1, Change o2) {
      ContentRevision bRev1 = o1.getBeforeRevision();
      ContentRevision bRev2 = o2.getBeforeRevision();
      int delta = compareRevisions(bRev1, bRev2);
      if (delta != 0) return delta;

      ContentRevision aRev1 = o1.getAfterRevision();
      ContentRevision aRev2 = o2.getAfterRevision();
      return compareRevisions(aRev1, aRev2);
    }

    private static int compareRevisions(@Nullable ContentRevision bRev1, @Nullable ContentRevision bRev2) {
      if (bRev1 == null && bRev2 == null) return 0;
      if (bRev1 == null) return -1;
      if (bRev2 == null) return 1;
      String path1 = bRev1.getFile().getPath();
      String path2 = bRev2.getFile().getPath();
      return path1.compareTo(path2);
    }
  }

  @Nonnull
  private static LocalChangeListImpl readChangeList(@Nonnull Element listNode, @Nonnull Project project) {
    String id = listNode.getAttributeValue(ATT_ID);
    String name = StringUtil.notNullize(listNode.getAttributeValue(ATT_NAME), LocalChangeList.getDefaultName());
    String comment = StringUtil.notNullize(listNode.getAttributeValue(ATT_COMMENT));
    ChangeListData data = ChangeListData.readExternal(listNode);
    boolean isDefault = ATT_VALUE_TRUE.equals(listNode.getAttributeValue(ATT_DEFAULT));

    List<Change> changes = new ArrayList<>();
    for (Element changeNode : listNode.getChildren(NODE_CHANGE)) {
      changes.add(readChange(changeNode, project));
    }

    return new LocalChangeListImpl.Builder(project, name)
      .setId(id)
      .setComment(comment)
      .setChanges(changes)
      .setData(data)
      .setDefault(isDefault)
      .build();
  }

  @Nonnull
  private static Element writeChange(@Nonnull Change change) {
    Element changeNode = new Element(NODE_CHANGE);
    writeContentRevision(changeNode, change.getBeforeRevision(), RevisionSide.BEFORE);
    writeContentRevision(changeNode, change.getAfterRevision(), RevisionSide.AFTER);
    return changeNode;
  }

  @Nonnull
  private static Change readChange(@Nonnull Element changeNode, @Nonnull Project project) {
    FakeRevision bRev = readContentRevision(changeNode, project, RevisionSide.BEFORE);
    FakeRevision aRev = readContentRevision(changeNode, project, RevisionSide.AFTER);
    return new Change(bRev, aRev);
  }

  private static void writeContentRevision(@Nonnull Element changeNode, @Nullable ContentRevision rev, @Nonnull RevisionSide side) {
    if (rev == null) return;
    FilePath filePath = rev.getFile();
    String path = filePath.getPath();
    if (hasIllegalXmlChars(path)) {
      changeNode.setAttribute(side.getPathKey(), JDOMUtil.removeControlChars(path));
      changeNode.setAttribute(side.getEscapedPathKey(), XmlStringUtil.escapeIllegalXmlChars(path));
    }
    else {
      changeNode.setAttribute(side.getPathKey(), path);
    }
    changeNode.setAttribute(side.getIsDirKey(), String.valueOf(filePath.isDirectory()));
  }

  @Nullable
  private static FakeRevision readContentRevision(@Nonnull Element changeNode, @Nonnull Project project, @Nonnull RevisionSide side) {
    String plainPath = changeNode.getAttributeValue(side.getPathKey());
    String escapedPath = changeNode.getAttributeValue(side.getEscapedPathKey());
    String path = escapedPath != null ? XmlStringUtil.unescapeIllegalXmlChars(escapedPath) : plainPath;
    String isDirValue = changeNode.getAttributeValue(side.getIsDirKey());
    if (StringUtil.isEmpty(path)) return null;

    boolean isCurrentRevision = side == RevisionSide.AFTER;
    boolean isDirectory = isDirValue != null && Boolean.parseBoolean(isDirValue);
    FilePath filePath = VcsUtil.getFilePath(path, isDirectory);
    return new FakeRevision(project, filePath, isCurrentRevision);
  }

  private enum RevisionSide {
    BEFORE(ATT_CHANGE_BEFORE_PATH, ATT_CHANGE_BEFORE_PATH_ESCAPED, ATT_CHANGE_BEFORE_PATH_IS_DIR),
    AFTER(ATT_CHANGE_AFTER_PATH, ATT_CHANGE_AFTER_PATH_ESCAPED, ATT_CHANGE_AFTER_PATH_IS_DIR);

    @Nonnull
    private final String myPathKey;
    @Nonnull
    private final String myEscapedPathKey;
    @Nonnull
    private final String myIsDirKey;

    RevisionSide(@Nonnull String pathKey, @Nonnull String escapedPathKey, @Nonnull String isDirKey) {
      myPathKey = pathKey;
      myEscapedPathKey = escapedPathKey;
      myIsDirKey = isDirKey;
    }

    @Nonnull
    public String getPathKey() {
      return myPathKey;
    }

    @Nonnull
    String getEscapedPathKey() {
      return myEscapedPathKey;
    }

    @Nonnull
    public String getIsDirKey() {
      return myIsDirKey;
    }
  }

  private static boolean hasIllegalXmlChars(@Nonnull String text) {
    return text.chars().anyMatch(c -> !Verifier.isXMLCharacter(c));
  }
}
