package consulo.application.util.matcher;

import consulo.application.progress.ProgressManager;

import java.util.*;

/**
 * @author peter
 */
public abstract class PrefixMatcher {
  public static final PrefixMatcher ALWAYS_TRUE = new PlainPrefixMatcher("");
  protected final String myPrefix;

  protected PrefixMatcher(String prefix) {
    myPrefix = prefix;
  }

  public boolean prefixMatches(CompositeStringHolder element) {
    for (String s : element.getAllStrings()) {
      if (prefixMatches(s)) {
        return true;
      }
    }
    return false;
  }

  public boolean isStartMatch(CompositeStringHolder element) {
    for (String s : element.getAllStrings()) {
      if (isStartMatch(s)) {
        return true;
      }
    }
    return false;
  }

  public boolean isStartMatch(String name) {
    return prefixMatches(name);
  }

  public abstract boolean prefixMatches(String name);

  
  public final String getPrefix() {
    return myPrefix;
  }

  
  public abstract PrefixMatcher cloneWithPrefix(String prefix);

  public int matchingDegree(String string) {
    return 0;
  }

  /**
   * Filters _names for strings that match given matcher and sorts them.
   * "Start matching" items go first, then others.
   * Within both groups names are sorted lexicographically in a case-insensitive way.
   */
  public LinkedHashSet<String> sortMatching(Collection<String> _names) {
    ProgressManager.checkCanceled();
    if (getPrefix().isEmpty()) {
      return new LinkedHashSet<>(_names);
    }

    List<String> sorted = new ArrayList<>();
    for (String name : _names) {
      if (prefixMatches(name)) {
        sorted.add(name);
      }
    }

    ProgressManager.checkCanceled();
    Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
    ProgressManager.checkCanceled();

    LinkedHashSet<String> result = new LinkedHashSet<>();
    for (String name : sorted) {
      if (isStartMatch(name)) {
        result.add(name);
      }
    }

    ProgressManager.checkCanceled();

    result.addAll(sorted);
    return result;
  }
}
