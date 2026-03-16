// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.fileTypes.impl;

import consulo.virtualFileSystem.internal.matcher.ExtensionFileNameMatcherImpl;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.language.file.FileTypeManager;
import org.jdom.Element;

import java.util.*;
import java.util.function.BiPredicate;

class RemovedMappingTracker {
  public static class RemovedMapping {
    private final FileNameMatcher myFileNameMatcher;
    private final String myFileTypeName;
    private boolean myApproved;

    private RemovedMapping(FileNameMatcher matcher, String name, boolean approved) {
      myFileNameMatcher = matcher;
      myFileTypeName = name;
      myApproved = approved;
    }

    public FileNameMatcher getFileNameMatcher() {
      return myFileNameMatcher;
    }

    public String getFileTypeName() {
      return myFileTypeName;
    }

    public boolean isApproved() {
      return myApproved;
    }

    public void setApproved(boolean approved) {
      myApproved = approved;
    }
  }

  private final Map<FileNameMatcher, RemovedMapping> myRemovedMappings = new HashMap<>();

  
  private static final String ELEMENT_REMOVED_MAPPING = "removed_mapping";
  /**
   * Applied for removed mappings approved by user
   */
  
  private static final String ATTRIBUTE_APPROVED = "approved";
  
  private static final String ATTRIBUTE_TYPE = "type";

  void clear() {
    myRemovedMappings.clear();
  }

  public void add(FileNameMatcher matcher, String fileTypeName, boolean approved) {
    myRemovedMappings.put(matcher, new RemovedMapping(matcher, fileTypeName, approved));
  }

  public void load(Element e) {
    for (RemovedMapping mapping : readRemovedMappings(e)) {
      myRemovedMappings.put(mapping.myFileNameMatcher, mapping);
    }
  }

  
  static List<RemovedMapping> readRemovedMappings(Element e) {
    List<Element> children = e.getChildren(ELEMENT_REMOVED_MAPPING);
    if (children.isEmpty()) {
      return Collections.emptyList();
    }

    List<RemovedMapping> result = new ArrayList<>();
    for (Element mapping : children) {
      String ext = mapping.getAttributeValue(AbstractFileType.ATTRIBUTE_EXT);
      FileNameMatcher matcher = ext == null ? FileTypeManager.parseFromString(mapping.getAttributeValue(AbstractFileType.ATTRIBUTE_PATTERN)) : new ExtensionFileNameMatcherImpl(ext);
      boolean approved = Boolean.parseBoolean(mapping.getAttributeValue(ATTRIBUTE_APPROVED));
      String fileTypeName = mapping.getAttributeValue(ATTRIBUTE_TYPE);
      if (fileTypeName == null) continue;

      RemovedMapping removedMapping = new RemovedMapping(matcher, fileTypeName, approved);
      result.add(removedMapping);
    }
    return result;
  }

  public void save(Element element) {
    for (RemovedMapping mapping : myRemovedMappings.values()) {
      Element content = writeRemovedMapping(mapping.myFileTypeName, mapping.myFileNameMatcher, true, mapping.myApproved);
      if (content != null) {
        element.addContent(content);
      }
    }
  }

  void saveRemovedMappingsForFileType(Element map, String fileTypeName, Set<? extends FileNameMatcher> associations, boolean specifyTypeName) {
    for (FileNameMatcher matcher : associations) {
      Element content = writeRemovedMapping(fileTypeName, matcher, specifyTypeName, isApproved(matcher));
      if (content != null) {
        map.addContent(content);
      }
    }
  }

  boolean hasRemovedMapping(FileNameMatcher matcher) {
    return myRemovedMappings.containsKey(matcher);
  }

  private boolean isApproved(FileNameMatcher matcher) {
    RemovedMapping mapping = myRemovedMappings.get(matcher);
    return mapping != null && mapping.isApproved();
  }

  void approveRemoval(String fileTypeName, FileNameMatcher matcher) {
    myRemovedMappings.put(matcher, new RemovedMapping(matcher, fileTypeName, true));
  }

  
  public List<RemovedMapping> getRemovedMappings() {
    return new ArrayList<>(myRemovedMappings.values());
  }

  List<FileNameMatcher> getMappingsForFileType(String name) {
    List<FileNameMatcher> result = new ArrayList<>();
    for (RemovedMapping mapping : myRemovedMappings.values()) {
      if (mapping.myFileTypeName.equals(name)) {
        result.add(mapping.myFileNameMatcher);
      }
    }
    return result;
  }

  void removeMatching(BiPredicate<? super FileNameMatcher, ? super String> predicate) {
    myRemovedMappings.entrySet().removeIf(next -> predicate.test(next.getValue().myFileNameMatcher, next.getValue().myFileTypeName));
  }

  
  List<RemovedMapping> retrieveUnapprovedMappings() {
    List<RemovedMapping> result = new ArrayList<>();
    for (Iterator<Map.Entry<FileNameMatcher, RemovedMapping>> it = myRemovedMappings.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<FileNameMatcher, RemovedMapping> next = it.next();
      if (!next.getValue().isApproved()) {
        result.add(next.getValue());
        it.remove();
      }
    }
    return result;
  }

  private static Element writeRemovedMapping(String fileTypeName, FileNameMatcher matcher, boolean specifyTypeName, boolean approved) {
    Element mapping = new Element(ELEMENT_REMOVED_MAPPING);
    if (matcher instanceof ExtensionFileNameMatcherImpl) {
      mapping.setAttribute(AbstractFileType.ATTRIBUTE_EXT, ((ExtensionFileNameMatcherImpl)matcher).getExtension());
    }
    else if (AbstractFileType.writePattern(matcher, mapping)) {
      return null;
    }
    if (approved) {
      mapping.setAttribute(ATTRIBUTE_APPROVED, "true");
    }
    if (specifyTypeName) {
      mapping.setAttribute(ATTRIBUTE_TYPE, fileTypeName);
    }

    return mapping;
  }
}
