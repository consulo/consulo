// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.internal;

import consulo.util.collection.ArrayUtil;
import consulo.util.collection.CharSequenceHashingStrategy;
import consulo.util.collection.Maps;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.internal.matcher.ExactFileNameMatcherImpl;
import consulo.virtualFileSystem.internal.matcher.ExtensionFileNameMatcherImpl;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * @author max
 */
public class FileTypeAssocTable<T> {
  private final Map<CharSequence, T> myExtensionMappings;
  private final Map<CharSequence, T> myExactFileNameMappings;
  private final Map<CharSequence, T> myExactFileNameAnyCaseMappings;
  private final List<Pair<FileNameMatcher, T>> myMatchingMappings;

  private FileTypeAssocTable(@Nonnull Map<? extends CharSequence, ? extends T> extensionMappings,
                             @Nonnull Map<? extends CharSequence, ? extends T> exactFileNameMappings,
                             @Nonnull Map<? extends CharSequence, T> exactFileNameAnyCaseMappings,
                             @Nonnull List<? extends Pair<FileNameMatcher, T>> matchingMappings) {
    myExtensionMappings = Maps.newHashMap(Math.max(10, extensionMappings.size()), 0.5f, CharSequenceHashingStrategy.CASE_INSENSITIVE);
    myExtensionMappings.putAll(extensionMappings);
    myExactFileNameMappings = Maps.newHashMap(Math.max(10, exactFileNameMappings.size()), 0.5f, CharSequenceHashingStrategy.CASE_SENSITIVE);
    myExactFileNameMappings.putAll(exactFileNameMappings);
    myExactFileNameAnyCaseMappings = Maps.newHashMap(Math.max(10, exactFileNameAnyCaseMappings.size()), 0.5f, CharSequenceHashingStrategy.CASE_INSENSITIVE);
    myExactFileNameAnyCaseMappings.putAll(exactFileNameAnyCaseMappings);
    myMatchingMappings = new ArrayList<>(matchingMappings);
  }

  public FileTypeAssocTable() {
    this(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());
  }

  public boolean isAssociatedWith(@Nonnull T type, @Nonnull FileNameMatcher matcher) {
    if (matcher instanceof ExtensionFileNameMatcherImpl || matcher instanceof ExactFileNameMatcherImpl) {
      return findAssociatedFileType(matcher) == type;
    }

    for (Pair<FileNameMatcher, T> mapping : myMatchingMappings) {
      if (matcher.equals(mapping.getFirst()) && type == mapping.getSecond()) return true;
    }

    return false;
  }

  public void addAssociation(@Nonnull FileNameMatcher matcher, @Nonnull T type) {
    if (matcher instanceof ExtensionFileNameMatcherImpl) {
      myExtensionMappings.put(((ExtensionFileNameMatcherImpl)matcher).getExtension(), type);
    }
    else if (matcher instanceof ExactFileNameMatcherImpl) {
      ExactFileNameMatcherImpl exactFileNameMatcher = (ExactFileNameMatcherImpl)matcher;

      Map<CharSequence, T> mapToUse = exactFileNameMatcher.isIgnoreCase() ? myExactFileNameAnyCaseMappings : myExactFileNameMappings;
      mapToUse.put(exactFileNameMatcher.getFileName(), type);
    }
    else {
      myMatchingMappings.add(Pair.create(matcher, type));
    }
  }

  public boolean removeAssociation(@Nonnull FileNameMatcher matcher, @Nonnull T type) {
    if (matcher instanceof ExtensionFileNameMatcherImpl) {
      String extension = ((ExtensionFileNameMatcherImpl)matcher).getExtension();
      if (myExtensionMappings.get(extension) == type) {
        myExtensionMappings.remove(extension);
        return true;
      }
      return false;
    }

    if (matcher instanceof ExactFileNameMatcherImpl) {
      ExactFileNameMatcherImpl exactFileNameMatcher = (ExactFileNameMatcherImpl)matcher;
      String fileName = exactFileNameMatcher.getFileName();

      Map<CharSequence, T> mapToUse = exactFileNameMatcher.isIgnoreCase() ? myExactFileNameAnyCaseMappings : myExactFileNameMappings;
      if (mapToUse.get(fileName) == type) {
        mapToUse.remove(fileName);
        return true;
      }
      return false;
    }

    return myMatchingMappings.removeIf(assoc -> matcher.equals(assoc.getFirst()));
  }

  public boolean removeAllAssociations(@Nonnull T type) {
    boolean changed = removeAssociationsFromMap(myExtensionMappings, type, false);

    changed = removeAssociationsFromMap(myExactFileNameAnyCaseMappings, type, changed);
    changed = removeAssociationsFromMap(myExactFileNameMappings, type, changed);

    return myMatchingMappings.removeIf(assoc -> assoc.getSecond() == type);
  }

  private boolean removeAssociationsFromMap(@Nonnull Map<CharSequence, T> extensionMappings, @Nonnull T type, boolean changed) {
    return extensionMappings.entrySet().removeIf(entry -> entry.getValue() == type) || changed;
  }

  @Nullable
  public T findAssociatedFileType(@Nonnull @NonNls CharSequence fileName) {
    if (!myExactFileNameMappings.isEmpty()) {
      T t = myExactFileNameMappings.get(fileName);
      if (t != null) return t;
    }

    if (!myExactFileNameAnyCaseMappings.isEmpty()) {   // even hash lookup with case insensitive hasher is costly for isIgnored checks during compile
      T t = myExactFileNameAnyCaseMappings.get(fileName);
      if (t != null) return t;
    }

    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myMatchingMappings.size(); i++) {
      Pair<FileNameMatcher, T> mapping = myMatchingMappings.get(i);
      if (mapping.getFirst().acceptsCharSequence(fileName)) return mapping.getSecond();
    }

    return findByExtension(FileUtil.getExtension(fileName));
  }

  @Nullable
  public T findAssociatedFileType(@Nonnull FileNameMatcher matcher) {
    if (matcher instanceof ExtensionFileNameMatcherImpl) {
      return findByExtension(((ExtensionFileNameMatcherImpl)matcher).getExtension());
    }

    if (matcher instanceof ExactFileNameMatcherImpl) {
      ExactFileNameMatcherImpl exactFileNameMatcher = (ExactFileNameMatcherImpl)matcher;

      Map<CharSequence, T> mapToUse = exactFileNameMatcher.isIgnoreCase() ? myExactFileNameAnyCaseMappings : myExactFileNameMappings;
      return mapToUse.get(exactFileNameMatcher.getFileName());
    }

    for (Pair<FileNameMatcher, T> mapping : myMatchingMappings) {
      if (matcher.equals(mapping.getFirst())) return mapping.getSecond();
    }

    return null;
  }

  public T findByExtension(@Nonnull CharSequence extension) {
    return myExtensionMappings.get(extension);
  }

  @Deprecated
  @Nonnull
  public String[] getAssociatedExtensions(@Nonnull T type) {
    List<String> exts = new ArrayList<>();
    for (Map.Entry<CharSequence, T> entry : myExtensionMappings.entrySet()) {
      if (entry.getValue() == type) {
        exts.add(entry.getKey().toString());
      }
    }
    return ArrayUtil.toStringArray(exts);
  }

  @Nonnull
  public FileTypeAssocTable<T> copy() {
    return new FileTypeAssocTable<>(myExtensionMappings, myExactFileNameMappings, myExactFileNameAnyCaseMappings, myMatchingMappings);
  }

  @Nonnull
  public List<FileNameMatcher> getAssociations(@Nonnull T type) {
    List<FileNameMatcher> result = new ArrayList<>();
    for (Pair<FileNameMatcher, T> mapping : myMatchingMappings) {
      if (mapping.getSecond() == type) {
        result.add(mapping.getFirst());
      }
    }

    for (Map.Entry<CharSequence, T> entry : myExactFileNameMappings.entrySet()) {
      if (entry.getValue() == type) {
        result.add(new ExactFileNameMatcherImpl(entry.getKey().toString(), false));
      }
    }
    for (Map.Entry<CharSequence, T> entry : myExactFileNameAnyCaseMappings.entrySet()) {
      if (entry.getValue() == type) {
        result.add(new ExactFileNameMatcherImpl(entry.getKey().toString(), true));
      }
    }
    for (Map.Entry<CharSequence, T> entry : myExtensionMappings.entrySet()) {
      if (entry.getValue() == type) {
        result.add(new ExtensionFileNameMatcherImpl(entry.getKey().toString()));
      }
    }

    return result;
  }

  public boolean hasAssociationsFor(@Nonnull T fileType) {
    if (myExtensionMappings.containsValue(fileType) || myExactFileNameMappings.containsValue(fileType) || myExactFileNameAnyCaseMappings.containsValue(fileType)) {
      return true;
    }
    for (Pair<FileNameMatcher, T> mapping : myMatchingMappings) {
      if (mapping.getSecond() == fileType) {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  public Map<FileNameMatcher, T> getRemovedMappings(@Nonnull FileTypeAssocTable<T> newTable, @Nonnull Collection<? extends T> keys) {
    Map<FileNameMatcher, T> map = new HashMap<>();
    for (T key : keys) {
      List<FileNameMatcher> associations = getAssociations(key);
      associations.removeAll(newTable.getAssociations(key));
      for (FileNameMatcher matcher : associations) {
        map.put(matcher, key);
      }
    }
    return map;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FileTypeAssocTable<?> that = (FileTypeAssocTable)o;
    return myExtensionMappings.equals(that.myExtensionMappings) &&
           myMatchingMappings.equals(that.myMatchingMappings) &&
           myExactFileNameMappings.equals(that.myExactFileNameMappings) &&
           myExactFileNameAnyCaseMappings.equals(that.myExactFileNameAnyCaseMappings);
  }

  @Override
  public int hashCode() {
    int result = myExtensionMappings.hashCode();
    result = 31 * result + myMatchingMappings.hashCode();
    result = 31 * result + myExactFileNameMappings.hashCode();
    result = 31 * result + myExactFileNameAnyCaseMappings.hashCode();
    return result;
  }
}
