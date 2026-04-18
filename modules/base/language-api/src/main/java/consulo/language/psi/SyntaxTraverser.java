package consulo.language.psi;

import consulo.annotation.ReviewAfterIssueFix;
import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.ast.*;
import consulo.language.parser.PsiBuilder;
import consulo.language.util.FlyweightCapableTreeStructure;
import consulo.util.collection.FilteredTraverserBase;
import consulo.util.collection.Iterators;
import consulo.util.collection.JBIterable;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.lang.ThreadLocalCachedValue;
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import static consulo.util.lang.function.Predicates.compose;

/**
 * @author gregsh
 */
public class SyntaxTraverser<T> extends FilteredTraverserBase<T, SyntaxTraverser<T>> implements UserDataHolder {
    public static ApiEx<PsiElement> psiApi() {
        return PsiApi.INSTANCE;
    }

    public static ApiEx<PsiElement> psiApiReversed() {
        return PsiApi.INSTANCE_REV;
    }

    public static ApiEx<ASTNode> astApi() {
        return ASTApi.INSTANCE;
    }

    public static Api<LighterASTNode> lightApi(PsiBuilder builder) {
        return new LighterASTApi(builder);
    }

    public static <T> SyntaxTraverser<T> syntaxTraverser(Api<T> api) {
        return new SyntaxTraverser<>(api, null);
    }

    public static SyntaxTraverser<PsiElement> psiTraverser() {
        return new SyntaxTraverser<>(psiApi(), null);
    }

    public static SyntaxTraverser<PsiElement> psiTraverser(@Nullable PsiElement root) {
        return psiTraverser().withRoot(root);
    }

    public static SyntaxTraverser<PsiElement> revPsiTraverser() {
        return new SyntaxTraverser<>(psiApiReversed(), null);
    }

    public static SyntaxTraverser<ASTNode> astTraverser() {
        return new SyntaxTraverser<>(astApi(), null);
    }

    public static SyntaxTraverser<ASTNode> astTraverser(@Nullable ASTNode root) {
        return astTraverser().withRoot(root);
    }

    public static SyntaxTraverser<LighterASTNode> lightTraverser(PsiBuilder builder) {
        LighterASTApi api = new LighterASTApi(builder);
        return new SyntaxTraverser<>(api, Meta.<LighterASTNode>empty().withRoots(JBIterable.of(api.getStructure().getRoot())));
    }

    public final Api<T> api;

    protected SyntaxTraverser(Api<T> api, @Nullable Meta<T> meta) {
        super(meta, api);
        this.api = api;
    }

    @Override
    protected SyntaxTraverser<T> newInstance(Meta<T> meta) {
        return new SyntaxTraverser<>(api, meta);
    }

    @Override
    protected boolean isAlwaysLeaf(T node) {
        return super.isAlwaysLeaf(node) && !(api.typeOf(node) instanceof IFileElementType);
    }

    @Override
    public <K> @Nullable K getUserData(Key<K> key) {
        return getUserDataHolder().getUserData(key);
    }

    @Override
    public <K> void putUserData(Key<K> key, @Nullable K value) {
        getUserDataHolder().putUserData(key, value);
    }

    private UserDataHolder getUserDataHolder() {
        return api instanceof LighterASTApi lighterASTApi
            ? lighterASTApi.builder
            : Objects.requireNonNull((UserDataHolder)api.parents(getRoot()).last());
    }

    public SyntaxTraverser<T> expandTypes(Predicate<? super IElementType> c) {
        return super.expand(compose(api.TO_TYPE, c));
    }

    public SyntaxTraverser<T> filterTypes(Predicate<? super IElementType> c) {
        return super.filter(compose(api.TO_TYPE, c));
    }

    public SyntaxTraverser<T> forceDisregardTypes(Predicate<? super IElementType> c) {
        return super.forceDisregard(compose(api.TO_TYPE, c));
    }

    public @Nullable T getRawDeepestLast() {
        for (T result = JBIterable.from(getRoots()).last(), last; result != null; result = last) {
            JBIterable<T> children = children(result);
            if (children.isEmpty()) {
                return result;
            }
            //noinspection AssignmentToForLoopParameter
            last = children.last();
        }
        return null;
    }

    public final SyntaxTraverser<T> onRange(TextRange range) {
        return onRange(e -> api.rangeOf(e).intersects(range));
    }

    public abstract static class Api<T> implements Function<T, Iterable<? extends T>> {
        public abstract IElementType typeOf(T node);

        public abstract TextRange rangeOf(T node);

        public abstract CharSequence textOf(T node);

        public abstract @Nullable T parent(T node);

        public abstract JBIterable<? extends T> children(T node);

        @Override
        public JBIterable<? extends T> apply(T t) {
            return children(t);
        }

        public JBIterable<T> parents(@Nullable T element) {
            return JBIterable.generate(element, this::parent);
        }

        public final Function<T, IElementType> TO_TYPE = new Function<>() {
            @Override
            public IElementType apply(T t) {
                return typeOf(t);
            }

            @Override
            public String toString() {
                return "TO_TYPE";
            }
        };

        public final Function<T, CharSequence> TO_TEXT = new Function<>() {
            @Override
            public CharSequence apply(T t) {
                return textOf(t);
            }

            @Override
            public String toString() {
                return "TO_TEXT";
            }
        };

        public final Function<T, TextRange> TO_RANGE = new Function<>() {
            @Override
            public TextRange apply(T t) {
                return rangeOf(t);
            }

            @Override
            public String toString() {
                return "TO_RANGE";
            }
        };
    }

    public abstract static class ApiEx<T> extends Api<T> {
        public abstract @Nullable T first(T node);

        public abstract @Nullable T last(T node);

        public abstract @Nullable T next(T node);

        public abstract @Nullable T previous(T node);

        @Override
        public JBIterable<? extends T> children(T node) {
            T first = first(node);
            if (first == null) {
                return JBIterable.empty();
            }
            return siblings(first);
        }

        public JBIterable<? extends T> siblings(T node) {
            return JBIterable.generate(node, TO_NEXT);
        }

        @ReviewAfterIssueFix(value = "github.com/uber/NullAway/issues/1504", todo = "Remove explicit generics in constructor")
        private final Function<T, @Nullable T> TO_NEXT = new Function<T, @Nullable T>() {
            @Override
            public @Nullable T apply(T t) {
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
            @Override
            @RequiredReadAction
            public @Nullable PsiElement previous(PsiElement node) {
                return super.next(node);
            }

            @Override
            @RequiredReadAction
            public @Nullable PsiElement next(PsiElement node) {
                return super.previous(node);
            }

            @Override
            @RequiredReadAction
            public @Nullable PsiElement last(PsiElement node) {
                return super.first(node);
            }

            @Override
            @RequiredReadAction
            public @Nullable PsiElement first(PsiElement node) {
                return super.last(node);
            }
        };

        @Override
        @RequiredReadAction
        public @Nullable PsiElement first(PsiElement node) {
            return node.getFirstChild();
        }

        @Override
        @RequiredReadAction
        public @Nullable PsiElement last(PsiElement node) {
            return node.getLastChild();
        }

        @Override
        @RequiredReadAction
        public @Nullable PsiElement next(PsiElement node) {
            return node.getNextSibling();
        }

        @Override
        @RequiredReadAction
        public @Nullable PsiElement previous(PsiElement node) {
            return node.getPrevSibling();
        }

        @Override
        public IElementType typeOf(PsiElement node) {
            IElementType type = PsiUtilCore.getElementType(node);
            return type != null ? type : IElementType.find((short)0);
        }

        @Override
        @RequiredReadAction
        public TextRange rangeOf(PsiElement node) {
            return node.getTextRange();
        }

        @Override
        @RequiredReadAction
        public CharSequence textOf(PsiElement node) {
            return node.getText();
        }

        @Override
        public @Nullable PsiElement parent(PsiElement node) {
            return node instanceof PsiFile ? null : node.getParent();
        }
    }

    private static class ASTApi extends ApiEx<ASTNode> {
        static final ASTApi INSTANCE = new ASTApi();

        @Override
        public @Nullable ASTNode first(ASTNode node) {
            return node.getFirstChildNode();
        }

        @Override
        public @Nullable ASTNode last(ASTNode node) {
            return node.getLastChildNode();
        }

        @Override
        public @Nullable ASTNode next(ASTNode node) {
            return node.getTreeNext();
        }

        @Override
        public @Nullable ASTNode previous(ASTNode node) {
            return node.getTreePrev();
        }

        @Override
        public IElementType typeOf(ASTNode node) {
            return node.getElementType();
        }

        @Override
        public TextRange rangeOf(ASTNode node) {
            return node.getTextRange();
        }

        @Override
        public CharSequence textOf(ASTNode node) {
            return node.getText();
        }

        @Override
        public @Nullable ASTNode parent(ASTNode node) {
            return node.getTreeParent();
        }
    }

    private abstract static class FlyweightApi<T> extends Api<T> {
        abstract FlyweightCapableTreeStructure<T> getStructure();

        @Override
        public @Nullable T parent(T node) {
            return getStructure().getParent(node);
        }

        @Override
        public JBIterable<? extends T> children(T node) {
            return new JBIterable<>() {
                @Override
                public Iterator<T> iterator() {
                    FlyweightCapableTreeStructure<T> structure = getStructure();
                    SimpleReference<T[]> ref = SimpleReference.create();
                    int count = structure.getChildren(node, ref);
                    if (count == 0) {
                        return Iterators.empty();
                    }
                    T[] array = ref.requiredGet();
                    LinkedList<T> list = new LinkedList<>();
                    int toDispose = 0;
                    for (int i = 0; i < count; i++) {
                        T child = array[i];
                        IElementType childType = typeOf(child);
                        // tokens and errors getParent() == null
                        if (childType == TokenType.WHITE_SPACE || childType == TokenType.BAD_CHARACTER) {
                            array[toDispose++] = array[i];
                            continue;
                        }
                        list.addLast(child);
                    }
                    structure.disposeChildren(array, toDispose);
                    return list.iterator();
                }
            };
        }
    }

    private static class LighterASTApi extends FlyweightApi<LighterASTNode> {
        private final PsiBuilder builder;
        private final ThreadLocalCachedValue<FlyweightCapableTreeStructure<LighterASTNode>> structure = new ThreadLocalCachedValue<>() {
            @Override
            protected FlyweightCapableTreeStructure<LighterASTNode> create() {
                return builder.getLightTree();
            }
        };

        public LighterASTApi(PsiBuilder builder) {
            this.builder = builder;
        }

        @Override
        FlyweightCapableTreeStructure<LighterASTNode> getStructure() {
            return structure.getValue();
        }

        @Override
        public IElementType typeOf(LighterASTNode node) {
            return Objects.requireNonNull(node.getTokenType());
        }

        @Override
        public TextRange rangeOf(LighterASTNode node) {
            return TextRange.create(node.getStartOffset(), node.getEndOffset());
        }

        @Override
        public CharSequence textOf(LighterASTNode node) {
            return rangeOf(node).subSequence(builder.getOriginalText());
        }

        @Override
        public @Nullable LighterASTNode parent(LighterASTNode node) {
            return node instanceof LighterASTTokenNode ? null : super.parent(node);
        }
    }
}
