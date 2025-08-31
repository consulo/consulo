package consulo.language.duplicateAnalysis.treeHash;

import consulo.component.macro.PathMacroManager;
import consulo.document.Document;
import consulo.language.duplicateAnalysis.*;
import consulo.language.duplicateAnalysis.util.DuplocatorUtil;
import consulo.language.duplicateAnalysis.util.PsiFragment;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.usage.UsageInfo;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectProcedure;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DuplocatorHashCallback implements FragmentsCollector {
  private static final Logger LOG = Logger.getInstance(DuplocatorHashCallback.class);

  private TIntObjectHashMap<List<List<PsiFragment>>> myDuplicates;
  private final int myBound;
  private boolean myReadOnly = false;
  private final int myDiscardCost;

  public DuplocatorHashCallback(int bound, int discardCost) {
    myDuplicates = new TIntObjectHashMap<>();
    myBound = bound;
    myDiscardCost = discardCost;
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public DuplocatorHashCallback(int bound, int discardCost, boolean readOnly) {
    this(bound, discardCost);
    myReadOnly = readOnly;
  }

  public DuplocatorHashCallback(int lowerBound) {
    this(lowerBound, 0);
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public void setReadOnly(boolean readOnly) {
    myReadOnly = readOnly;
  }

  // used in TeamCity
  @SuppressWarnings("UnusedParameters")
  public void add(int hash, int cost, PsiFragment frag, NodeSpecificHasher visitor) {
    forceAdd(hash, cost, frag);
  }

  private void forceAdd(int hash, int cost, PsiFragment frag) {
    if (frag == null) { //fake fragment
      myDuplicates.put(hash, new ArrayList<>());
      return;
    }

    frag.setCost(cost);

    List<List<PsiFragment>> fragments = myDuplicates.get(hash);

    if (fragments == null) {
      if (!myReadOnly) { //do not add new hashcodes
        List<List<PsiFragment>> list = new ArrayList<>();
        List<PsiFragment> listf = new ArrayList<>();

        listf.add(frag);
        list.add(listf);

        myDuplicates.put(hash, list);
      }

      return;
    }

    boolean found = false;

    PsiElement[] elements = frag.getElements();

    int discardCost = 0;

    if (myDiscardCost >= 0) {
      discardCost = myDiscardCost;
    }
    else {
      DuplocatorState state = DuplocatorUtil.getDuplocatorState(frag);
      if (state != null) {
        discardCost = state.getDiscardCost();
      }
    }

    for (Iterator<List<PsiFragment>> i = fragments.iterator(); i.hasNext() && !found; ) {
      List<PsiFragment> fi = i.next();
      PsiFragment aFrag = fi.get(0);

      if (aFrag.isEqual(elements, discardCost)) {
        boolean skipNew = false;

        for (Iterator<PsiFragment> frags = fi.iterator(); frags.hasNext() && !skipNew; ) {
          PsiFragment old = frags.next();
          if (frag.intersectsWith(old)) {
            if (old.getCost() < frag.getCost() || frag.contains(old)) {
              frags.remove();
            }
            else {
              skipNew = true;
            }
          }
        }

        if (!skipNew) fi.add(frag);

        found = true;
      }
    }

    if (!found) {
      List<PsiFragment> newFrags = new ArrayList<>();
      newFrags.add(frag);

      fragments.add(newFrags);
    }
  }

  @Override
  public void add(int hash, int cost, PsiFragment frag) {
    int bound;

    if (myBound >= 0) {
      bound = myBound;
    }
    else {
      DuplocatorState duplocatorState = DuplocatorUtil.getDuplocatorState(frag);
      if (duplocatorState == null) {
        return;
      }
      bound = duplocatorState.getLowerBound();
    }

    if (cost >= bound) {
      forceAdd(hash, cost, frag);
    }
  }

  public DupInfo getInfo() {
    final TObjectIntHashMap<PsiFragment[]> duplicateList = new TObjectIntHashMap<>();

    myDuplicates.forEachEntry(new TIntObjectProcedure<List<List<PsiFragment>>>() {
      @Override
      public boolean execute(int hash, List<List<PsiFragment>> listList) {
        for (List<PsiFragment> list : listList) {
          int len = list.size();
          if (len > 1) {
            PsiFragment[] filtered = new PsiFragment[len];
            int idx = 0;
            for (PsiFragment fragment : list) {
              fragment.markDuplicate();
              filtered[idx++] = fragment;
            }
            duplicateList.put(filtered, hash);
          }
        }

        return true;
      }
    });

    myDuplicates = null;

    for (TObjectIntIterator<PsiFragment[]> dups = duplicateList.iterator(); dups.hasNext(); ) {
      dups.advance();
      PsiFragment[] fragments = dups.key();
      LOG.assertTrue(fragments.length > 1);
      boolean nested = false;
      for (PsiFragment fragment : fragments) {
        if (fragment.isNested()) {
          nested = true;
          break;
        }
      }

      if (nested) {
        dups.remove();
      }
    }

    final Object[] duplicates = duplicateList.keys();

    Arrays.sort(duplicates, (x, y) -> ((PsiFragment[])y)[0].getCost() - ((PsiFragment[])x)[0].getCost());

    return new DupInfo() {
      private final TIntObjectHashMap<GroupNodeDescription> myPattern2Description = new TIntObjectHashMap<>();

      @Override
      public int getPatterns() {
        return duplicates.length;
      }

      @Override
      public int getPatternCost(int number) {
        return ((PsiFragment[])duplicates[number])[0].getCost();
      }

      @Override
      public int getPatternDensity(int number) {
        return ((PsiFragment[])duplicates[number]).length;
      }

      @Override
      public PsiFragment[] getFragmentOccurences(int pattern) {
        return (PsiFragment[])duplicates[pattern];
      }

      @Override
      public UsageInfo[] getUsageOccurences(int pattern) {
        PsiFragment[] occs = getFragmentOccurences(pattern);
        UsageInfo[] infos = new UsageInfo[occs.length];

        for (int i = 0; i < infos.length; i++) {
          infos[i] = occs[i].getUsageInfo();
        }

        return infos;
      }

      @Override
      public int getFileCount(int pattern) {
        if (myPattern2Description.containsKey(pattern)) {
          return myPattern2Description.get(pattern).getFilesCount();
        }
        return cacheGroupNodeDescription(pattern).getFilesCount();
      }

      private GroupNodeDescription cacheGroupNodeDescription(int pattern) {
        Set<PsiFile> files = new HashSet<>();
        PsiFragment[] occurencies = getFragmentOccurences(pattern);
        for (PsiFragment occurency : occurencies) {
          PsiFile file = occurency.getFile();
          if (file != null) {
            files.add(file);
          }
        }
        int fileCount = files.size();
        PsiFile psiFile = occurencies[0].getFile();
        DuplicatesProfile profile = DuplicatesProfileCache.getProfile(this, pattern);
        String comment = profile != null ? profile.getComment(this, pattern) : "";
        GroupNodeDescription description = new GroupNodeDescription(fileCount, psiFile != null ? psiFile.getName() : "unknown", comment);
        myPattern2Description.put(pattern, description);
        return description;
      }

      @Override
      @Nullable
      public String getTitle(int pattern) {
        if (getFileCount(pattern) == 1) {
          if (myPattern2Description.containsKey(pattern)) {
            return myPattern2Description.get(pattern).getTitle();
          }
          return cacheGroupNodeDescription(pattern).getTitle();
        }
        return null;
      }


      @Override
      @Nullable
      public String getComment(int pattern) {
        if (getFileCount(pattern) == 1) {
          if (myPattern2Description.containsKey(pattern)) {
            return myPattern2Description.get(pattern).getComment();
          }
          return cacheGroupNodeDescription(pattern).getComment();
        }
        return null;
      }

      @Override
      public int getHash(int i) {
        return duplicateList.get((PsiFragment[])duplicates[i]);
      }
    };
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public void report(String path, Project project) throws IOException {
    int[] hashCodes = myDuplicates.keys();
    Element rootElement = new Element("root");
    for (int hash : hashCodes) {
      List<List<PsiFragment>> dupList = myDuplicates.get(hash);
      Element hashElement = new Element("hash");
      hashElement.setAttribute("val", String.valueOf(hash));
      for (List<PsiFragment> psiFragments : dupList) {
        writeFragments(psiFragments, hashElement, project, false);
      }
      rootElement.addContent(hashElement);
    }


    try(FileWriter fileWriter = new FileWriter(path + File.separator + "fragments.xml")) {
      XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

      xmlOutputter.output(new org.jdom.Document(rootElement), fileWriter);
    }

    writeDuplicates(path, project, getInfo());
  }

  //duplicates
  public static void writeDuplicates(String path, Project project, DupInfo info) throws IOException {
    Element rootElement = new Element("root");
    int patterns = info.getPatterns();
    for (int i = 0; i < patterns; i++) {
      Element duplicate = new Element("duplicate");

      duplicate.setAttribute("cost", String.valueOf(info.getPatternCost(i)));
      duplicate.setAttribute("hash", String.valueOf(info.getHash(i)));
      writeFragments(Arrays.asList(info.getFragmentOccurences(i)), duplicate, project, true);

      rootElement.addContent(duplicate);
    }

    try (FileWriter fileWriter = new FileWriter(path + File.separator + "duplicates.xml")) {
      XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

      xmlOutputter.output(new org.jdom.Document(rootElement), fileWriter);
    }
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private static void writeFragments(List<? extends PsiFragment> psiFragments, Element duplicateElement, Project project, boolean shouldWriteOffsets) {
    PathMacroManager macroManager = ProjectPathMacroManager.getInstance(project);
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);

    for (PsiFragment fragment : psiFragments) {
      PsiFile psiFile = fragment.getFile();
      VirtualFile virtualFile = psiFile != null ? psiFile.getVirtualFile() : null;
      if (virtualFile != null) {
        Element fragmentElement = new Element("fragment");
        fragmentElement.setAttribute("file", macroManager.collapsePath(virtualFile.getUrl()));
        if (shouldWriteOffsets) {
          Document document = documentManager.getDocument(psiFile);
          LOG.assertTrue(document != null);
          int startOffset = fragment.getStartOffset();
          int line = document.getLineNumber(startOffset);
          fragmentElement.setAttribute("line", String.valueOf(line));
          int lineStartOffset = document.getLineStartOffset(line);
          if (StringUtil.isEmptyOrSpaces(document.getText().substring(lineStartOffset, startOffset))) {
            startOffset = lineStartOffset;
          }
          fragmentElement.setAttribute("start", String.valueOf(startOffset));
          fragmentElement.setAttribute("end", String.valueOf(fragment.getEndOffset()));
          if (fragment.containsMultipleFragments()) {
            int[][] offsets = fragment.getOffsets();
            for (int[] offset : offsets) {
              Element offsetElement = new Element("offset");
              offsetElement.setAttribute("start", String.valueOf(offset[0]));
              offsetElement.setAttribute("end", String.valueOf(offset[1]));
              fragmentElement.addContent(offsetElement);
            }
          }
        }
        duplicateElement.addContent(fragmentElement);
      }
    }
  }
}
