package com.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LighterASTNode;
import com.intellij.lang.LighterASTTokenNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.util.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FilteredTraverserBase;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.diff.FlyweightCapableTreeStructure;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.LinkedList;

import static com.intellij.openapi.util.Conditions.compose;

/**
 * @author gregsh
 */
public class SyntaxTraverser<T> extends FilteredTraverserBase<T, SyntaxTraverser<T>> implements UserDataHolder {

  @Nonnull
  public static ApiEx<PsiElement> psiApi() {
    return PsiApi.INSTANCE;
  }

  @Nonnull
  public static ApiEx<PsiElement> psiApiReversed() {
    return PsiApi.INSTANCE_REV;
  }

  @Nonnull
  public static ApiEx<ASTNode> astApi() {
    return ASTApi.INSTANCE;
  }

  @Nonnull
  public static Api<LighterASTNode> lightApi(@Nonnull PsiBuilder builder) {
    return new LighterASTApi(builder);
  }

  @Nonnull
  public static <T> SyntaxTraverser<T> syntaxTraverser(@Nonnull Api<T> api) {
    return new SyntaxTraverser<>(api, null);
  }

  @Nonnull
  public static SyntaxTraverser<PsiElement> psiTraverser() {
    return new SyntaxTraverser<>(psiApi(), null);
  }

  @Nonnull
  public static SyntaxTraverser<PsiElement> psiTraverser(@Nullable PsiElement root) {
    return psiTraverser().withRoot(root);
  }

  @Nonnull
  public static SyntaxTraverser<PsiElement> revPsiTraverser() {
    return new SyntaxTraverser<>(psiApiReversed(), null);
  }

  @Nonnull
  public static SyntaxTraverser<ASTNode> astTraverser() {
    return new SyntaxTraverser<>(astApi(), null);
  }

  @Nonnull
  public static SyntaxTraverser<ASTNode> astTraverser(@Nullable ASTNode root) {
    return astTraverser().withRoot(root);
  }

  @Nonnull
  public static SyntaxTraverser<LighterASTNode> lightTraverser(@Nonnull PsiBuilder builder) {
    LighterASTApi api = new LighterASTApi(builder);
    return new SyntaxTraverser<>(api, Meta.<LighterASTNode>empty().withRoots(JBIterable.of(api.getStructure().getRoot())));
  }

  public final Api<T> api;

  protected SyntaxTraverser(@Nonnull Api<T> api, @Nullable Meta<T> meta) {
    super(meta, api);
    this.api = api;
  }

  @Nonnull
  @Override
  protected SyntaxTraverser<T> newInstance(Meta<T> meta) {
    return new SyntaxTraverser<>(api, meta);
  }

  @Override
  protected boolean isAlwaysLeaf(@Nonnull T node) {
    return super.isAlwaysLeaf(node) && !(api.typeOf(node) instanceof IFileElementType);
  }

  @Nullable
  @Override
  public <K> K getUserData(@Nonnull Key<K> key) {
    return getUserDataHolder().getUserData(key);
  }

  @Override
  public <K> void putUserData(@Nonnull Key<K> key, @Nullable K value) {
    getUserDataHolder().putUserData(key, value);
  }

  private UserDataHolder getUserDataHolder() {
    return api instanceof LighterASTApi ? ((LighterASTApi)api).builder : (UserDataHolder)api.parents(getRoot()).last();
  }

  @Nonnull
  public SyntaxTraverser<T> expandTypes(@Nonnull Condition<? super IElementType> c) {
    return super.expand(compose(api.TO_TYPE, c));
  }

  @Nonnull
  public SyntaxTraverser<T> filterTypes(@Nonnull Condition<? super IElementType> c) {
    return super.filter(compose(api.TO_TYPE, c));
  }

  @Nonnull
  public SyntaxTraverser<T> forceDisregardTypes(@Nonnull Condition<? super IElementType> c) {
    return super.forceDisregard(compose(api.TO_TYPE, c));
  }

  @Nullable
  public T getRawDeepestLast() {
    for (T result = JBIterable.from(getRoots()).last(), last; result != null; result = last) {
      JBIterable<T> children = children(result);
      if (children.isEmpty()) return result;
      //noinspection AssignmentToForLoopParameter
      last = children.last();
    }
    return null;
  }

  @Nonnull
  public final SyntaxTraverser<T> onRange(@Nonnull final TextRange range) {
    return onRange(e -> api.rangeOf(e).intersects(range));
  }

  public abstract static class Api<T> implements Function<T, Iterable<? extends T>> {
    @Nonnull
    public abstract IElementType typeOf(@Nonnull T node);

    @Nonnull
    public abstract TextRange rangeOf(@Nonnull T node);

    @Nonnull
    public abstract CharSequence textOf(@Nonnull T node);

    @Nullable
    public abstract T parent(@Nonnull T node);

    @Nonnull
    public abstract JBIterable<? extends T> children(@Nonnull T node);

    @Override
    public JBIterable<? extends T> fun(T t) {
      return children(t);
    }

    @Nonnull
    public JBIterable<T> parents(@Nullable final T element) {
      return JBIterable.generate(element, t -> parent(t));
    }

    public final Function<T, IElementType> TO_TYPE = new Function<T, IElementType>() {
      @Override
      public IElementType fun(T t) {
        return typeOf(t);
      }

      @Override
      public String toString() {
        return "TO_TYPE";
      }
    };

    public final Function<T, CharSequence> TO_TEXT = new Function<T, CharSequence>() {
      @Override
      public CharSequence fun(T t) {
        return textOf(t);
      }

      @Override
      public String toString() {
        return "TO_TEXT";
      }
    };

    public final Function<T, TextRange> TO_RANGE = new Function<T, TextRange>() {
      @Override
      public TextRange fun(T t) {
        return rangeOf(t);
      }

      @Override
      public String toString() {
        return "TO_RANGE";
      }
    };
  }

  public abstract static class ApiEx<T> extends Api<T> {

    @Nullable
    public abstract T first(@Nonnull T node);

    @Nullable
    public abstract T last(@Nonnull T node);

    @Nullable
    public abstract T next(@Nonnull T node);

    @Nullable
    public abstract T previous(@Nonnull T node);

    @Nonnull
    @Override
    public JBIterable<? extends T> children(@Nonnull T node) {
      T first = first(node);
      if (first == null) return JBIterable.empty();
      return siblings(first);
    }

    @Nonnull
    public JBIterable<? extends T> siblings(@Nonnull T node) {
      return JBIterable.generate(node, TO_NEXT);
    }

    private final Function<T, T> TO_NEXT = new Function<T, T>() {
      @Override
      public T fun(T t) {
        return next(t);
      }

      @Override
      public String toString() {
        return "TO_NEXT";
      }
    };
  }

  private static class PsiApi extends ApiEx<PsiElement> {

    static final ApiEx<PsiElement> INSTANCE = new PsiApi();
    static final ApiEx<PsiElement> INSTANCE_REV = new PsiApi() {
      @Nullable
      @Override
      public PsiElement previous(@Nonnull PsiElement node) {
        return super.next(node);
      }

      @Nullable
      @Override
      public PsiElement next(@Nonnull PsiElement node) {
        return super.previous(node);
      }

      @Nullable
      @Override
      public PsiElement last(@Nonnull PsiElement node) {
        return super.first(node);
      }

      @Nullable
      @Override
      public PsiElement first(@Nonnull PsiElement node) {
        return super.last(node);
      }
    };

    @Nullable
    @Override
    public PsiElement first(@Nonnull PsiElement node) {
      return node.getFirstChild();
    }

    @Nullable
    @Override
    public PsiElement last(@Nonnull PsiElement node) {
      return node.getLastChild();
    }

    @Nullable
    @Override
    public PsiElement next(@Nonnull PsiElement node) {
      return node.getNextSibling();
    }

    @Nullable
    @Override
    public PsiElement previous(@Nonnull PsiElement node) {
      return node.getPrevSibling();
    }

    @Nonnull
    @Override
    public IElementType typeOf(@Nonnull PsiElement node) {
      IElementType type = PsiUtilCore.getElementType(node);
      return type != null ? type : IElementType.find((short)0);
    }

    @Nonnull
    @Override
    public TextRange rangeOf(@Nonnull PsiElement node) {
      return node.getTextRange();
    }

    @Nonnull
    @Override
    public CharSequence textOf(@Nonnull PsiElement node) {
      return node.getText();
    }

    @Nullable
    @Override
    public PsiElement parent(@Nonnull PsiElement node) {
      return node instanceof PsiFile ? null : node.getParent();
    }
  }

  private static class ASTApi extends ApiEx<ASTNode> {

    static final ASTApi INSTANCE = new ASTApi();

    @Nullable
    @Override
    public ASTNode first(@Nonnull ASTNode node) {
      return node.getFirstChildNode();
    }

    @Nullable
    @Override
    public ASTNode last(@Nonnull ASTNode node) {
      return node.getLastChildNode();
    }

    @Nullable
    @Override
    public ASTNode next(@Nonnull ASTNode node) {
      return node.getTreeNext();
    }

    @Nullable
    @Override
    public ASTNode previous(@Nonnull ASTNode node) {
      return node.getTreePrev();
    }

    @Nonnull
    @Override
    public IElementType typeOf(@Nonnull ASTNode node) {
      return node.getElementType();
    }

    @Nonnull
    @Override
    public TextRange rangeOf(@Nonnull ASTNode node) {
      return node.getTextRange();
    }

    @Nonnull
    @Override
    public CharSequence textOf(@Nonnull ASTNode node) {
      return node.getText();
    }

    @Nullable
    @Override
    public ASTNode parent(@Nonnull ASTNode node) {
      return node.getTreeParent();
    }
  }

  private abstract static class FlyweightApi<T> extends Api<T> {

    @Nonnull
    abstract FlyweightCapableTreeStructure<T> getStructure();

    @Nullable
    @Override
    public T parent(@Nonnull T node) {
      return getStructure().getParent(node);
    }

    @Nonnull
    @Override
    public JBIterable<? extends T> children(@Nonnull final T node) {
      return new JBIterable<T>() {
        @Override
        public Iterator<T> iterator() {
          FlyweightCapableTreeStructure<T> structure = getStructure();
          Ref<T[]> ref = Ref.create();
          int count = structure.getChildren(node, ref);
          if (count == 0) return ContainerUtil.emptyIterator();
          T[] array = ref.get();
          LinkedList<T> list = ContainerUtil.newLinkedList();
          for (int i = 0; i < count; i++) {
            T child = array[i];
            IElementType childType = typeOf(child);
            // tokens and errors getParent() == null
            if (childType == TokenType.WHITE_SPACE || childType == TokenType.BAD_CHARACTER) {
              continue;
            }
            array[i] = null; // do not dispose meaningful TokenNodes
            list.addLast(child);
          }
          structure.disposeChildren(array, count);
          return list.iterator();
        }
      };
    }
  }

  private static class LighterASTApi extends FlyweightApi<LighterASTNode> {
    private final PsiBuilder builder;
    private final ThreadLocalCachedValue<FlyweightCapableTreeStructure<LighterASTNode>> structure =
            new ThreadLocalCachedValue<FlyweightCapableTreeStructure<LighterASTNode>>() {
              @Override
              protected FlyweightCapableTreeStructure<LighterASTNode> create() {
                return builder.getLightTree();
              }
            };

    public LighterASTApi(final PsiBuilder builder) {
      this.builder = builder;
    }

    @Nonnull
    @Override
    FlyweightCapableTreeStructure<LighterASTNode> getStructure() {
      return structure.getValue();
    }

    @Nonnull
    @Override
    public IElementType typeOf(@Nonnull LighterASTNode node) {
      return node.getTokenType();
    }

    @Nonnull
    @Override
    public TextRange rangeOf(@Nonnull LighterASTNode node) {
      return TextRange.create(node.getStartOffset(), node.getEndOffset());
    }

    @Nonnull
    @Override
    public CharSequence textOf(@Nonnull LighterASTNode node) {
      return rangeOf(node).subSequence(builder.getOriginalText());
    }

    @Nullable
    @Override
    public LighterASTNode parent(@Nonnull LighterASTNode node) {
      return node instanceof LighterASTTokenNode ? null : super.parent(node);
    }
  }
}
