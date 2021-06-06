package com.intellij.dupLocator;

import com.intellij.dupLocator.treeHash.FragmentsCollector;
import com.intellij.lang.Language;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class DuplicatesProfileCache {
  private static final Map<DupInfo, IntObjectMap<DuplicatesProfile>> ourProfileCache = new HashMap<>();

  private DuplicatesProfileCache() {
  }

  public static void clear(@Nonnull DupInfo info) {
    ourProfileCache.remove(info);
  }

  @Nullable
  public static DuplicatesProfile getProfile(@Nonnull DupInfo dupInfo, int index) {
    IntObjectMap<DuplicatesProfile> patternCache = ourProfileCache.get(dupInfo);
    if (patternCache == null) {
      patternCache = IntMaps.newIntObjectHashMap();
      ourProfileCache.put(dupInfo, patternCache);
    }
    DuplicatesProfile result = patternCache.get(index);
    if (result == null) {
      DuplicatesProfile theProfile = null;
      for (DuplicatesProfile profile : DuplicatesProfile.EP_NAME.getExtensionList()) {
        if (profile.isMyDuplicate(dupInfo, index)) {
          theProfile = profile;
          break;
        }
      }
      result = theProfile == null ? NULL_PROFILE : theProfile;
      patternCache.put(index, result);
    }
    return result == NULL_PROFILE ? null : result;
  }

  private static final DuplicatesProfile NULL_PROFILE = new DuplicatesProfile() {
    @Nonnull
    @Override
    public DuplocateVisitor createVisitor(@Nonnull FragmentsCollector collector) {
      return null;
    }

    @Override
    public boolean isMyLanguage(@Nonnull Language language) {
      return false;
    }

    @Nonnull
    @Override
    public DuplocatorState getDuplocatorState(@Nonnull Language language) {
      return null;
    }

    @Override
    public boolean isMyDuplicate(@Nonnull DupInfo info, int index) {
      return false;
    }
  };
}
