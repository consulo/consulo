package consulo.language.duplicateAnalysis;

import consulo.language.Language;
import consulo.language.duplicateAnalysis.treeHash.FragmentsCollector;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DuplicatesProfileCache {
    private static final Map<DupInfo, IntObjectMap<DuplicatesProfile>> ourProfileCache = new HashMap<>();

    private DuplicatesProfileCache() {
    }

    public static void clear(DupInfo info) {
        ourProfileCache.remove(info);
    }

    @Nullable
    public static DuplicatesProfile getProfile(DupInfo dupInfo, int index) {
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
        
        @Override
        public DuplocateVisitor createVisitor(FragmentsCollector collector) {
            return null;
        }

        @Override
        public boolean isMyLanguage(Language language) {
            return false;
        }

        
        @Override
        public DuplocatorState getDuplocatorState(Language language) {
            return null;
        }

        @Override
        public boolean isMyDuplicate(DupInfo info, int index) {
            return false;
        }
    };
}
