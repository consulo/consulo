// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package consulo.language.psi.stub;

import consulo.application.Application;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public abstract class IStubElementType<StubT extends StubElement, PsiT extends PsiElement> extends IElementType implements StubSerializer<StubT> {
  private static volatile boolean ourInitializedStubs;
  private static volatile Set<String> ourLazyExternalIds = Collections.emptySet();
  private static final Logger LOG = Logger.getInstance(IStubElementType.class);

  public IStubElementType(@Nonnull final String debugName, @Nullable final Language language) {
    super(debugName, language);
    if (!isLazilyRegistered()) {
      checkNotInstantiatedTooLate(this);
    }
  }

  public static void checkNotInstantiatedTooLate(IElementType thisElementType) {
    if (ourInitializedStubs) {
      LOG.error("All stub element types should be created before index initialization is complete.\n" +
                "This element type: " + thisElementType + ".\n" +
                "Please add the class containing stub element type constants to '" +
                StubElementTypeHolder.class +
                "' extension.\n" +
                "Registered extensions: " +
                Application.get().getExtensionList(StubElementTypeHolder.class));
    }
  }

  private boolean isLazilyRegistered() {
    try {
      return ourLazyExternalIds.contains(getExternalId());
    }
    catch (Throwable e) {
      // "getExternalId" might throw when called from constructor, if it accesses subclass fields
      // Lazily-registered types have a contract that their "getExternalId" doesn't throw like this,
      // so getting an exception here is a sign that someone indeed creates their stub type after StubElementTypeHolder initialization.
      return false;
    }
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public static List<ObjectStubSerializerProvider> loadRegisteredStubElementTypes() {
    List<ObjectStubSerializerProvider> result = new ArrayList<>();
    Set<String> lazyIds = new HashSet<>();

    Application.get().getExtensionPoint(StubElementTypeHolder.class).forEachExtensionSafe(holder -> {
      String externalPrefixId = holder.getExternalIdPrefix();
      if (externalPrefixId != null) {
        lazyIds.add(externalPrefixId);
      }

      // just invoke for registering not lazy element types
      List<ObjectStubSerializerProvider> serializers = holder.loadSerializers();
      // add only lazy stubs, others will be registered by class initialize, and registering to IElementType registry
      if (externalPrefixId != null) {
        result.addAll(serializers);
      }
    });

    ourLazyExternalIds = lazyIds;
    ourInitializedStubs = true;
    return result;
  }

  public abstract PsiT createPsi(@Nonnull StubT stub);

  @Nonnull
  public abstract StubT createStub(@Nonnull PsiT psi, final StubElement parentStub);

  public boolean shouldCreateStub(ASTNode node) {
    return true;
  }

}