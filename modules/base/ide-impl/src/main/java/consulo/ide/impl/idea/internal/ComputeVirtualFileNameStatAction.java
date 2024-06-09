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

/*
 * User: anna
 * Date: 28-Jun-2007
 */
package consulo.ide.impl.idea.internal;

import consulo.application.dumb.DumbAware;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.newvfs.impl.VirtualFileSystemEntry;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.VirtualFile;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntProcedure;

import java.util.*;

public class ComputeVirtualFileNameStatAction extends AnAction implements DumbAware {
  public ComputeVirtualFileNameStatAction() {
    super("Compute VF name statistics");
  }

  public static void main(String[] args) {
    new ComputeVirtualFileNameStatAction().actionPerformed(null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    long start = System.currentTimeMillis();

    suffixes.clear();
    nameCount.clear();
    VirtualFile[] roots = ManagingFS.getInstance().getRoots(LocalFileSystem.getInstance());
    for (VirtualFile root : roots) {
      compute(root);
    }

    final List<Pair<String, Integer>> names = new ArrayList<Pair<String, Integer>>(nameCount.size());
    nameCount.forEachEntry(new TObjectIntProcedure<String>() {
      @Override
      public boolean execute(String name, int count) {
        names.add(Pair.create(name, count));
        return true;
      }
    });
    Collections.sort(names, new Comparator<Pair<String, Integer>>() {
      @Override
      public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
        return o2.second - o1.second;
      }
    });

    System.out.println("Most frequent names (" + names.size() + " total):");
    int saveByIntern = 0;
    for (Pair<String, Integer> pair : names) {
      int count = pair.second;
      String name = pair.first;
      System.out.println(name + " -> " + count);
      saveByIntern += count * name.length();
      if (count == 1) break;
    }
    System.out.println("Total save if names were interned: " + saveByIntern + "; ------------");

    //System.out.println("Prefixes: ("+prefixes.size()+" total)");
    //show(prefixes);
    System.out.println("Suffix counts:(" + suffixes.size() + " total)");
    show(suffixes);


    final TObjectIntHashMap<String> save = new TObjectIntHashMap<String>();
    // compute economy
    suffixes.forEachEntry(new TObjectIntProcedure<String>() {
      @Override
      public boolean execute(String s, int count) {
        save.put(s, count * s.length());
        return true;
      }
    });

    System.out.println("Supposed save by stripping suffixes: (" + save.size() + " total)");
    final List<Pair<String, Integer>> saveSorted = show(save);


    final List<String> picked = new ArrayList<String>();
    //List<String> candidates = new ArrayList<String>();
    //int i =0;
    //for (Pair<String, Integer> pair : sorted) {
    //  if (i++>1000) break;
    //  candidates.add(pair.first);
    //}

    //final TObjectIntHashMap<String> counts = new TObjectIntHashMap<String>();
    //suffixes.forEachEntry(new TObjectIntProcedure<String>() {
    //  @Override
    //  public boolean execute(String a, int b) {
    //    counts.put(a, b);
    //    return true;
    //  }
    //});

    while (picked.size() != 15) {
      Pair<String, Integer> cp = saveSorted.get(0);
      final String candidate = cp.first;
      picked.add(candidate);
      System.out.println("Candidate: '" + candidate + "', save = " + cp.second);
      Collections.sort(picked, new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          return o2.length() - o1.length(); // longer first
        }
      });
      saveSorted.clear();

      // adjust
      suffixes.forEachEntry(new TObjectIntProcedure<String>() {
        @Override
        public boolean execute(String s, int count) {
          for (int i = picked.size() - 1; i >= 0; i--) {
            String pick = picked.get(i);
            if (pick.endsWith(s)) {
              count -= suffixes.get(pick);
              break;
            }
          }
          saveSorted.add(Pair.create(s, s.length() * count));
          return true;
        }
      });
      Collections.sort(saveSorted, new Comparator<Pair<String, Integer>>() {
        @Override
        public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
          return o2.second.compareTo(o1.second);
        }
      });
    }

    System.out.println("Picked: " + StringUtil.join(picked, s -> "\"" + s + "\"", ","));
    Collections.sort(picked, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return o2.length() - o1.length(); // longer first
      }
    });

    int saved = 0;
    for (int i = 0; i < picked.size(); i++) {
      String s = picked.get(i);
      int count = suffixes.get(s);
      for (int k = 0; k < i; k++) {
        String prev = picked.get(k);
        if (prev.endsWith(s)) {
          count -= suffixes.get(prev);
          break;
        }
      }
      saved += count * s.length();
    }
    System.out.println("total saved = " + saved);
    System.out.println("Time spent: " + (System.currentTimeMillis() - start));
  }

  private static List<Pair<String, Integer>> show(final TObjectIntHashMap<String> prefixes) {
    final List<Pair<String, Integer>> prefs = new ArrayList<Pair<String, Integer>>(prefixes.size());
    prefixes.forEachEntry(new TObjectIntProcedure<String>() {
      @Override
      public boolean execute(String s, int count) {
        prefs.add(Pair.create(s, count));
        return true;
      }
    });
    Collections.sort(prefs, new Comparator<Pair<String, Integer>>() {
      @Override
      public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
        return o2.second.compareTo(o1.second);
      }
    });
    int i = 0;
    for (Pair<String, Integer> pref : prefs) {
      Integer count = pref.second;
      System.out.printf("%60.60s : %d\n", pref.first, count);
      if (/*count<500 || */i++ > 100) {
        System.out.println("\n.......<" + count + "...\n");
        break;
      }
    }
    return prefs;
  }

  //TObjectIntHashMap<String> prefixes = new TObjectIntHashMap<String>();
  TObjectIntHashMap<String> suffixes = new TObjectIntHashMap<String>();
  TObjectIntHashMap<String> nameCount = new TObjectIntHashMap<String>();

  private void compute(VirtualFile root) {
    String name = root.getName();
    if (!nameCount.increment(name)) nameCount.put(name, 1);
    for (int i = 1; i <= name.length(); i++) {
      //String prefix = name.substring(0, i);
      //if (!prefixes.increment(prefix)) prefixes.put(prefix, 1);

      String suffix = name.substring(name.length() - i);
      if (!suffixes.increment(suffix)) suffixes.put(suffix, 1);
    }
    Collection<VirtualFile> cachedChildren = ((VirtualFileSystemEntry)root).getCachedChildren();
    //VirtualFile[] cachedChildren = ((VirtualFileSystemEntry)root).getChildren();
    for (VirtualFile cachedChild : cachedChildren) {
      compute(cachedChild);
    }
  }
}
