# Plan: Module-Aware Indexing for Consulo Platform

## Goal

Extend `FileBasedIndex` so that, for languages where a file's semantic meaning depends on its build context, index entries can be keyed by richer context than just `(fileId)`. Must be **opt-in, backward-compatible, and language-agnostic at the platform level**.

## Motivation

A large class of languages parse the same file differently under different build configurations:

| Language | Context dimension |
|---|---|
| C/C++ | Preprocessor macros, include paths, toolchain |
| C# | Preprocessor symbols, TargetFramework, LangVersion |
| Rust | `cfg`, features, target triple, edition |
| Haxe | `-D` defines, target backend |
| Swift / Obj-C | Build settings, platform conditionals |
| Kotlin MPP | Source-set, target, expect/actual |
| TypeScript | `tsconfig.json` lib/target/paths |
| Dart/Flutter | Platform-conditional imports |
| Shader languages | `#define` variants per pipeline |

Today each plugin either ignores the problem (wrong resolve) or reinvents a private resolve layer (duplication, inconsistent UX, invalidation bugs). Platform abstraction pays for itself immediately.

## Why not `FilePropertyPusher`?

`FilePropertyPusher` + `FileAttribute` was evaluated as the foundation and **rejected**:

- **App-level storage**: `FileAttribute.writeAttribute()` writes to `~/.consulo/system/caches/attrib.dat`, keyed by `(fileId, attributeId)` only. No project or module dimension.
- **Last-writer-wins**: two projects viewing the same file with different contexts overwrite each other. Structurally incapable of holding distinct values per (file × project × module).
- **Single typed value per pusher** — can't represent rich options payloads.

Pushed properties remain the right mechanism for **intrinsic file properties** (template language, language level) but cannot back module-aware indexing.

**What to borrow**: the invalidation choreography in `PushedFilePropertiesUpdaterImpl`:

- project load → initialize all contexts, bulk reindex dirty files
- VFS events → per-file lazy reindex in dumb mode
- `ModuleRootListener.rootsChanged` → bulk re-evaluate module files
- explicit "options changed" → scoped reindex

Copy the pattern, replace the storage layer.

**Removal path (decided during the preprocessor pivot).** The new architecture has zero pushed-properties dependency: seed/options drift invalidation uses `FileBasedIndex.requestReindex` + `FileContentUtilCore.reparseFiles` (public primitives, headless-proven via the relocated `PsiVFSListener` bridge), and seed persistence uses a raw `FileAttribute`. The machinery itself stays only for its three remaining consumers, each with a designated migration: `CSharpFilePropertyPusher` → the sand seed model (the testbed is its blueprint), `JavaLanguageLevelPusher` → module-aware options (language level is a per-module option), `TemplateDataLanguagePusher` → its own per-file `FileAttribute` + reparse-on-change (per-file user assignment does not fit module options). After those, delete `FilePropertyPusher`/`PushedFilePropertiesUpdater(+Internal/Impl)` and the `processAfterVfsChanges` hook in `GlobalPsiVFSBulkFileListener`.

## Core model: single stub per file, options-aware invalidation

Goal: **correct** storage of stubs/indexes per file, respecting the current module's options. Not multi-variant, not cross-project sharing.

- Exactly one stub per file at any time (matches existing `SingleEntryFileBasedIndexExtension` contract).
- That stub is built with the currently-applicable module options.
- When options drift (module config edit, build-system reload → `rootsChanged`), the stub is invalidated and rebuilt with the new options.

The `IndexOption` sealed hierarchy still exists and serves two purposes:
1. **`FullySharable`** — file has no module-aware context, uses existing global-stub behaviour unchanged.
2. **`UniqueToModule`** / **`SharablePerOption`** — file is options-sensitive, its stored stub + options-meta get invalidated when those options change.

The distinction between `UniqueToModule` and `SharablePerOption` is retained only for potential future use. Under the current "single stub" model both behave identically — the stub is stored, and is invalidated when options drift. No cross-project sharing, no concurrent variants.

## Platform API

### `IndexOption` — sealed, factory-only construction

Public interface, sealed to a single internal impl. Stateless variants are singletons.

```java
// Public API — consulo.language.index
public sealed interface IndexOption
        permits consulo.language.index.impl.internal.IndexOptionImpl {

    static IndexOption fullySharable() {
        return IndexOptionImpl.FullySharable.INSTANCE;
    }

    static IndexOption uniqueToModule(LocalizeValue displayName) {
        return new IndexOptionImpl.UniqueToModule(displayName);
    }

    static <T extends Record> IndexOption sharablePerOption(
            T value,
            DataExternalizer<T> externalizer,
            LocalizeValue displayName) {
        return new IndexOptionImpl.SharablePerOption<>(value, externalizer, displayName);
    }
}
```

```java
// Internal impl — consulo.language.index.impl.internal
// Sealed interface (not abstract class) because records can't extend classes.
public sealed interface IndexOptionImpl extends IndexOption
        permits IndexOptionImpl.FullySharable,
                IndexOptionImpl.UniqueToModule,
                IndexOptionImpl.SharablePerOption {

    // Singleton — no variant, never appears in UI picker.
    record FullySharable() implements IndexOptionImpl {
        public static final FullySharable INSTANCE = new FullySharable();
    }

    // Carries display label reserved for the future UI layer. Not used by index logic.
    record UniqueToModule(LocalizeValue displayName) implements IndexOptionImpl {}

    // <T extends Record> — structural equals/hashCode guaranteed by the language.
    record SharablePerOption<T extends Record>(
            T value,
            DataExternalizer<T> externalizer,
            LocalizeValue displayName)
            implements IndexOptionImpl {}
}
```

**Cache identity is `value()` only — not the whole record.**

`SharablePerOption` carries three components (`value`, `externalizer`, `displayName`), but the platform keys the cache on `value()` alone. `externalizer` and `displayName` are infrastructural and cosmetic respectively — neither participates in cache identity, invalidation, or lookup.

The record's auto-generated equals/hashCode is a by-product of using records; it is not used by the indexing layer. Two-tier equality model that actually matters:

| Check | What it compares | Purpose |
|---|---|---|
| `value.equals(other)` (auto from `T extends Record`) | Record payload only | Fast in-memory "did options change" check during invalidation |
| `hash(externalizer.save(value))` | Serialized bytes of payload only | Durable cache key across sessions and processes |

Both operate on `value` alone. `T extends Record` guarantees the first. Deterministic serialization guarantees agreement between the two.

**Display label (`LocalizeValue`)**

`UniqueToModule` and `SharablePerOption` carry a `LocalizeValue` reserved for the future UI layer (module picker, debug output). Not used by any index-integration logic. Example intended labels: `Debug`, `Release`, `net8`, `x86_64-linux-gnu`, `cfg(windows, gnu)`. Lazy translation preserved for whenever UI consumes it.

`FullySharable` has no label — it represents a single global variant, never shown as a choice.

**Plugin contract for `SharablePerOption<T extends Record>`**

Strong structural equality on `T` is enforced by the language (`T extends Record` → auto equals/hashCode). Plugin owns two guarantees for cross-project content-addressed caching:

1. **`DataExternalizer<T>.save` is deterministic.** Same record value → same bytes. Sort unordered collections before writing; map iteration order must be stable. Prefer `List` over array fields.
2. **No hidden inputs** in `T` or the externalizer — no env vars, clock, random. Output is a pure function of the record.

Meet both → `value.equals` agrees with serialized-bytes hash → cache correctness across projects and sessions.

Label and externalizer can change freely without affecting cache.

**Why this shape**

- Sealed + single `permits` into internal pkg → no plugin can implement or extend `IndexOption`.
- Private constructors → only factories produce instances.
- Singletons for stateless variants → zero allocation on common paths; `==` identity dispatch works.
- Pattern matching in the storage layer (inside impl module) stays type-safe and exhaustive.
- Adding a fourth tier later = new internal class + new factory method, zero breakage for clients.

Matches the `LocalizeValue` / `Image` / `UIAccess` Consulo idiom.

### `ModuleAwareIndexOptionProvider` — plugin contract

```java
@ExtensionAPI(ComponentScope.APPLICATION)
interface ModuleAwareIndexOptionProvider {
    String getId();
    int getVersion();                                // bump to invalidate all stored hashes for this provider
    Set<FileType> getInputFileTypes();               // static filter — platform precomputes FileType → providers map
    IndexOption getOptions(Module module, VirtualFile file);   // called only for matching files; non-null
}
```

A file may be claimed by **multiple providers** — one for macros, another for toolchain fingerprint, another for target triple, etc. Each contributes an independent dimension. Platform composes them into one effective `IndexOption` per (index, file).

**`getInputFileTypes` — bulk filtering**

Platform builds `Map<FileType, List<ModuleAwareIndexOptionProvider>>` once at plugin load. On any file walk (rootsChanged iteration, project load bulk pass, per-file invalidation), platform checks `file.getFileType()` against this map — files whose type no provider claims are skipped without allocation or provider call.

Matches the `FileBasedIndexExtension.getInputFilter()` idiom plugin authors already know. Static set, not dynamic filter — keeps dispatch O(1).

### `FileBasedIndexExtension` — declare options-dependence per index

```java
abstract class FileBasedIndexExtension<K, V> {
    // ... existing methods ...

    default List<String> getOptionProviderIds() {
        return List.of();   // empty = options-agnostic (current behaviour)
    }
}
```

**One file may participate in many indexes, each with its own options-dependence.** An options-sensitive stub index lists the providers it depends on (C++ macros, toolchain, target). A word/identifier index on the same file lists none. A todo index lists none. Invalidation stays narrow.

**Composition** — when an index lists multiple providers:

```
for each p in index.getOptionProviderIds():
    if p.getInputFileTypes() contains file.fileType:
        contributions.add(p.getOptions(module, file))

effective =
    if any contribution is UniqueToModule     -> UniqueToModule(<merged display labels>)
    else if any is SharablePerOption          -> SharablePerOption(
                                                     value   = tuple of all SharablePerOption payloads,
                                                     ex      = composite externalizer writing each in order,
                                                     display = <merged display labels>)
    else                                      -> FullySharable()
```

Tier precedence: most restrictive wins. Any `UniqueToModule` contribution forces the whole composition into per-module tier. Sharable contributions concatenate — hash is a deterministic function of all inputs, so cross-project cache sharing still works when every contributing provider returns identical output.

### Invalidation — platform listens to existing events

No explicit API for plugins to call. The platform hooks into `ModuleRootListener.rootsChanged` (and related VFS / module-extension commit events, which already fire roots-changed under the hood).

On rootsChanged:
- Iterate affected project files lazily.
- Re-query each options-sensitive index's provider for each file.
- Diff the new options against stored hash; mark-dirty on mismatch.
- Reindex happens on next query (lazy) or in the background indexing pass (eager, under dumb mode).

Plugins don't signal anything explicitly. Their module-extension commits already fire rootsChanged via Consulo's existing plumbing. External build-system reloads (CMake, Cargo, MSBuild) produce rootsChanged through their integration layers.

## Storage

All data lives under Consulo's existing system cache root — no new physical storage location. Reuses existing project-aware subdir plumbing.

### Stub storage: single-variant, options-invalidated

The existing `StubUpdatingIndex` stays as-is — `SingleEntryFileBasedIndexExtension<SerializedStubTree>`, one stub per `fileId`. No parallel index. Correctness for the module-aware case comes through invalidation: when options drift, the existing stub is discarded and a fresh one is built with current options.

`StubUpdatingIndex.getOptionProviderIds()` is overridden to return **every registered provider id** — stubs are declared implicitly dependent on any options the platform knows about. Per-file-type filtering at record / revalidate time keeps this narrow (files untouched by any provider pay zero).

`StubTreeLoaderImpl.readFromVFile` checks options-meta before returning the stored stub. If stale, requests reindex and returns null so the caller re-parses from AST.

```
~/.consulo/system/caches/
    index/
        shared/                            # cross-project cache
            <indexId>/
                fully-sharable.dat         # (fileId) → entry
                sharable-per-option.dat    # (fileId, optionsHash) → entry
        projects/<project-hash>/
            index/<indexId>/
                unique-to-module.dat       # (fileId, moduleId) → entry
                meta.dat                   # stored per-file meta (providerId, variantTag, hash, ...)
            ui-state/
                pins.dat                   # per-file pinned module
                last-used.dat              # per-file last-used module
```

**Nothing lives in the project's `.consulo/` directory.** All derived state — indexes, metadata, user-intent pins, last-used selections — sits under `~/.consulo/system/caches/`. The project dir stays lightweight (project config only, committable to VCS).

**Variants present in the cache**: exactly 1 per file, regardless of tier. When options drift, that single entry is replaced with a fresh one. No concurrent variants, no cross-project sharing in v1.

**Storage dispatch** (inside impl module):

```java
switch (option) {
    case IndexOptionImpl.FullySharable  f -> cacheSharedGlobal(indexId, fileId, ...);
    case IndexOptionImpl.UniqueToModule u -> cachePerModule(indexId, fileId, projectId, moduleId, ...);
    case IndexOptionImpl.SharablePerOption<?> s -> {
        byte[] bytes = serialize(s.externalizer, s.value);
        int hash = stableHash(bytes);
        cacheByHash(indexId, fileId, hash, ...);
    }
}
```

Files with no options-sensitive index registered: exactly today's layout, zero overhead.

## Invalidation

Triggers (borrowed choreography from `PushedFilePropertiesUpdaterImpl`):

- **Project load** → initialize stored-hash map, bulk evaluate options-sensitive indexes over project files, mark dirty where stored hash differs from current.
- **VFS events** → per-file lazy re-evaluation during dumb mode.
- **`ModuleRootListener.rootsChanged`** → iterate affected module files, diff hashes, mark dirty. Covers module-extension commits, external build-system reloads, SDK changes — all fire rootsChanged through existing Consulo plumbing.

**Stored per-file meta per options-sensitive index (multi-provider aware):**

```
(indexId, fileId) → {
    indexVersion : int,
    providers    : Map<providerId, PerProviderMeta>
}

PerProviderMeta = {
    providerVersion : int,
    variantTag      : FullySharable | UniqueToModule | SharablePerOption,
    optionsHash     : int       // 0 if tier doesn't use hash
}
```

**Revalidation algorithm per (file F, options-sensitive index I):**

```
if stored.indexVersion != index.version     -> reindex   // index itself changed

currentProviders = { p in index.getOptionProviderIds()
                     if p.getInputFileTypes() contains F.fileType }

if set_of_ids(currentProviders) != stored.providers.keys:
    reindex   // a provider was added, removed, or swapped

for each p in currentProviders:
    storedMeta = stored.providers[p.id]
    if storedMeta.providerVersion != p.version                    -> reindex
    opt = p.getOptions(module, F)
    tag = tagOf(opt)
    if storedMeta.variantTag != tag                               -> reindex
    if tag == SharablePerOption
       and hash(serialize(opt.value)) != storedMeta.optionsHash   -> reindex
    // FullySharable / UniqueToModule need no payload check
```

Unanimous agreement across all applicable providers required to skip reindex.

**What triggers reindex (summary)**

| Change | Detected by |
|---|---|
| Any provider added (new one now applies to file type) | new id in `currentProviders` not in `stored.providers.keys` |
| Any provider removed (plugin uninstalled / no longer applies) | stored id no longer in `currentProviders` |
| Provider set swap (different set of ids matches) | `keys != keys` |
| Any provider schema bumped (`getVersion()` incremented) | `storedMeta.providerVersion != current` |
| Any variant tier changed (per provider) | `storedMeta.variantTag != tag` |
| Any `SharablePerOption` payload changed | `hash != storedMeta.optionsHash` |
| Index itself rev'd | `stored.indexVersion != index.version` |

Deep changes are handled automatically — provider is the single source of truth. Anything it observes (macro set, TFM, cfg flag, target) changing the serialized options flows into the new hash. Provider lifecycle changes (installed, removed, versioned) are handled by the stored `providerId` + `providerVersion` tuple.

## UI — out of scope

This plan is **index-integration only**. Editor notifications, module pickers, status surfaces, navigation-origin tracking, pin persistence — all deferred to a separate implementation phase.

What the platform guarantees for the future UI layer:
- Every file has at most one module-perspective at a time under the single-variant model.
- Options for `(module, file)` are queryable via `ModuleAwareIndexOptionProvider.getOptions`.
- `rootsChanged` is the only invalidation trigger; UI doesn't need its own.

Nothing else is promised here.

## Setup switching — plugin's own concern

How setups are defined, stored, and switched is entirely up to each plugin. Typical Consulo approach: a `ModuleExtension` subclass stores the options, and the plugin contributes a Project Structure page for editing them. The extension commit already fires `rootsChanged` through existing Consulo plumbing — no explicit invalidation call is needed from the plugin.

Platform does not model setups. Not an enumeration, not an active-setup concept, no per-setup API. Just:
- Plugin's provider returns the current `IndexOption` for `(module, file)` based on whatever it stores.
- rootsChanged events (fired naturally by module-extension commits, SDK changes, build-system reloads) drive invalidation.

## The two dimensions: standalone options vs usage contexts

Not conflicting — complementary, and each is the *only* correct mechanism for its dimension:

- **Standalone files — module-aware options (drift-reindex).** A file's own context is statically known at index time: its module's options. Parse consumes them; drift is a configuration-time event, so an asynchronous reindex is safe and correct. This is what the `IndexOption`/provider/meta machinery guards.
- **Included files — condition-annotated stubs (query-time environments).** A usage context arrives per includer, at *resolve* time — and **reindexing during resolution is impossible** (resolve runs inside read actions and index queries). No single reindex can ever satisfy several includers either. Therefore the index must already contain **all possibilities**: every conditional branch is parsed into real declarations, each stub carries its guard, and the usage environment is applied as a filter at query/resolve time. The parse stays a pure function of file content; editing an includer shifts only environments, never the index.

One file can use both at once: module options as the standalone baseline, conditions for whatever inclusion adds.

**Environments are multi-context.** A file included from modules A and B carries the full-A context and the full-B context (each = includer's module options merged with its walk), plus its own standalone context. "Active" declaration-side queries match against *any* context; resolve-side queries supply the requester's environment explicitly. All contexts come from the same simulation pass.

**The platform owns the correct view**: `StubVariantFilter` (project-scoped, by-language EP) decides whether an indexed variant is active in some context of its declaring file; `StubIndex.getActiveElements` is the default query for navigation/search. Resolution is two-staged: candidates are the <b>declaration-side active</b> variants (was the declaration real in some context of its own file — this keeps negative guards like the `#ifndef` include-guard idiom resolvable), then the referencing file's environment picks the preferred candidate — so the same name resolves to different declarations from different files, with no reindexing anywhere.

Implemented in sand: nested `CONDITIONAL_BLOCK` parse (`SandParser` — `#if`/`#ifndef`/`#elif`/`#else`/`#end`, `#flag`/`#undef`/`#include`), guard stored in `SandClassStub` (single source — written by `createStub`, restored by the deserializer; `#elif` segments carry the negations of their predecessors), environments merged in `SandFlagEnv` (`allContexts` = standalone + every inclusion site + end-of-file self context under each; `resolutionEnv` = union), queries via `StubIndex.getActiveElements`/`SandClassSearch.matching`, resolution in `SandExtendsRef` (`class User : Item {}`), and `SandIncludeDirective` making the `#include` file name a reference to the included file (same sibling-resolution rule as the simulator, so navigation and contexts always agree).

**View context travels with navigation — platform mechanism.** `NavigationContextCollector` (PROJECT EP, `language-editor-api`) collects language contexts at the start of the goto gesture; `NavigationContexts` carries them: a thread-local scope (`withContexts`, `AccessToken`-restored) makes them readable arbitrarily deep during the navigation (`currentContext(Class)` — resolve/handlers need no parameter plumbing), `currentContexts()` captures the list for continuation on other threads or deferred callbacks (the ambiguity popup restores it at click time), and `GotoDeclarationAction` finally stamps the list onto the opened target `FileEditor`s under `NavigationContexts.NAVIGATION_CONTEXTS` and refreshes editor notifications. Sand's whole integration is one collector returning `SandViewContext(resolutionEnv(sourceFile), sourceFile)`. The banner (`SandContextEditorNotificationProvider`) shows only when it matters: arrival environment differs from the file's own default context, the file actually contains guarded variant declarations, and it isn't self-navigation — trivial gotos stay banner-free. The stamped editor user data is the contract the view layer reads — implemented: `SandViewEnv.viewEnv(project, file)` resolves the single environment an open file is *viewed* under (stamped `SandViewContext` on any of its editors wins, else the module flags = standalone view), and three consumers make inactive branches actually present as disabled, CLion-style, while text/PSI/index stay untouched: `SandInactiveBranchAnnotator` (per-language `AnnotatorFactory`) grays every conditional-block segment whose guard fails the view env; `SandLocalInspection` skips inactive declarations; `SandInactiveUsagesHandlerFactory` suppresses caret-usage highlighting inside inactive declarations (the identifier pass otherwise presents a dead branch as live). `GotoDeclarationAction` restarts the daemon for the target file after stamping, so arriving with a different context is a re-highlight, never a re-parse. The index-level rule is unchanged and deliberate: a variant is `active` if *any* known context selects it (standalone + every inclusion site) — same-name variants under disjoint guards (`A` vs `!A`) are one logical class with two conditional definitions, not a conflict; only the per-file *view* narrows to one world.

**Unparsable content degrades, content-driven, with run granularity.** Real languages have inactive regions that are not parsable at all (C++ under other defines). The parse must never be steered by context — instead a contiguous run of tokens that starts no declaration or directive is consumed into one opaque raw block: no declarations indexed for the run, no error markers, and parsing resumes at the next declaration or directive start. Declarations before and after a garbage run always survive — branch-level speculative rollback was the first cut and turned out too coarse in manual IDE testing: a single stray token after a valid `class` erased the whole branch ("second class works, first doesn't"). The decision still depends only on the bytes, so the parse stays a pure function of the file; only the truly unknowable run is absent from the index. (Sand: `RAW_BLOCK` runs in `SandParser.parseRawRun`; `class Ghost` after leading garbage is indexed under its branch condition, a trailing stray token costs nothing.)

**Reparse invariants.** Context changes cause zero cross-file reparses: editing a file reparses that file only; an includer's directive change drops the env memo and the included file's view flips on the next query — its content, tree, stubs and index entries are untouched. Contexts follow <b>saved</b> state: the walk reads VFS content, so unsaved typing shifts nothing and a save is the recompute boundary; the same save produces an out-of-code-block PSI change, so open editors re-highlight through the normal daemon restart and a future inactive-branch annotator needs no extra plumbing. Switching to unsaved-document sensitivity would be a one-place policy change (walk reading through the document manager) — deliberately not done.

**Memory shape — no global context table.** The include graph is derived data in an inverted `SandIncludeIndex` (`include spec → files`); contexts are computed per file on demand by recursing through the includer chain (`getContainingFiles` reverse lookup, single-file `SandIncludeSimulator.walk` — never a project iteration or whole-project simulation) and memoized in a soft-value map dropped on VFS/roots change. First query after a change pays only for the files it touches; the GC may reclaim the memo entirely. Nothing runs at project open — the first *query* is the first computation, on the querying thread inside its read action.

## Pre-indexing step — entry states for includes

Module options alone are not enough for C-family semantics: `#define A` in an includer changes how `some.h` parses. The header is not standalone-indexable — its correct parse needs the **macro state at the inclusion point**. The platform answer is a **pre-indexing pass** during the scan phase that computes, per included file, its *entry state*, stored and fed to the parser at index time.

**The legality rule.** An index entry may depend on non-content inputs only if every such input is (a) stored, and (b) schedules a reindex when it changes. That is exactly how JB's pushed properties work (Java language level: stub = f(content, storedLanguageLevel)). Entry state follows the same contract: stub = f(content, moduleOptions, storedEntryState), with drift detection via the existing `optionsHash` machinery — the provider folds the entry-state hash into its `SharablePerOption` payload, and the recorded meta diverges the moment the pre-pass produces a different state.

**The pre-pass** (dumb-mode-safe, no PSI, no index queries):

```
for each source file, deterministic order (content-root order, then stable file order):
    directive-only scan of raw text (# lines only)
    simulate: define / undef / if evaluation, include resolution via options' include paths
    on entering an included file:
        record entry state (first includer wins; pin overrides later)
        record reverse edge (included ← includer)
store per file: entryStateHash + serialized state
```

Determinism is mandatory — the state is hashed and compared across runs.

**Invalidation edges:**

| Change | Handled by |
|---|---|
| included file's own content | normal per-file reindex, entry state unchanged |
| includer's directive set (edit touching `#define` / `#include` lines) | language plugin recomputes affected subtree, `requestReindex` for drifted targets |
| module options / rootsChanged | pre-pass reruns in the scan phase; `optionsHash` drift reindexes (safety net) |

**Index-time include handling: macro state only, no text inlining.** The expander at index time consumes includes solely to seed the macro table — it must not inline included text into the includer's tree. Inlining duplicates every header's declarations into every TU's stubs (storage bloat, duplicated query results) and forces transitive reindex of all includers on any header edit. With seeded entry states, each file's stub holds only its own declarations, correctly conditional-filtered; resolve unions them. Full inline expansion remains available for the editor path.

**No platform hook needed.** A scan-phase visitor EP (JB's `IndexableFileScanner`) was ported and then removed: it is only structurally necessary when a pre-pass feeds *indexing itself* (results must exist before the indexer runs). Under the condition-annotated model entry environments are query/resolve-time inputs consumed in smart mode — so the language service computes them fully lazily (first query populates, any later query after a VFS change recomputes, guarded by the VFS modification count). No scan hook, no change listener, no data pinned beyond the small entry-state map. If a language ever has genuine parse-time pre-pass inputs, a scan hook gets re-added in the Consulo shape: project-scoped extension, stateless, session object owns all collected state, platform-guaranteed session finish.

**What stays out**: TU-rooted indexing (index sources expanding headers, attribute symbols back — the CLion custom-engine path) is rejected for the index layer; macro-expanded declarations from foreign macros (X-macros) are a resolve-layer concern via full expansion, accepted as the v1 index gap.

## PIVOT — the C#-preprocessor model ✅ DONE (user-directed, supersedes the condition-annotated view for sand)

Manual IDE testing rejected "parsed but presentation-disabled": disabled blocks **must not be parsed** — their tokens must never become real tokens ("can contain errors… remapped to comments or whitespaces"). The reference implementation is Consulo's own consulo-csharp, not CLion:

- **Mechanism (port of `CSharpBuilderWrapper` + `CSharpFileStubElementType`)**: a builder wrapper holds preprocessor state; directives are evaluated during the build against seeded variables; inside a false branch every token is remapped to `NON_ACTIVE_SYMBOL` and transparently skipped — the parser never sees disabled content. No PSI, no errors, text preserved as inert leaves, **one tree serves editor and stubs** (binding stays trivially consistent). Gray rendering = highlighter color for the inert token, nothing else.
- **Seeding** = union(module flags, all includer entry environments) — computed outside indexing (option-provider time; C# uses a `FilePropertyPusher`d attribute), deterministic. Seed change = option drift → **reparse + reindex** of the file ("we ok with that… maybe on save"). Navigation context re-seeds the target on arrival the same way.
- **Contract test first**: `SandDisabledBlockTest` (red against the old model) pins: exactly one variant in PSI *and* index; the absent name's offset is covered by no `SandClass`; garbage in a disabled branch produces no `PsiErrorElement`; module-flag flip and includer `#flag` edit each flip the surviving variant via reparse+reindex.
- **Retired by the pivot**: `CONDITIONAL_BLOCK`/branch parsing, `SandConditions`, the stub `condition` field, `SandStubVariantFilter` usage, and the whole view-gating layer (dimming annotator, inspection gate, caret-usage suppression, inlay gate) — obsolete because disabled code no longer exists as PSI. `SandFlagConditionalIndexTest`, `SandIncludeEntryStateTest`, `SandPreprocessorVariantsTest`, `SandContextResolveTest`, `SandEditorPsiTest` get rewritten to enabled-only semantics (one env per file at a time; conflicting includers alternate by reindex, never coexist). Stub version bump.
- The platform pieces stay full-surface per port policy (`StubVariantFilter`/`getActiveElements` remain for adopters that do want query-time filtering; `NavigationContexts` keeps carrying arrival envs — now consumed as a re-seed trigger).
- **Implemented and verified — 79/79 modules/it green.** `SandDisabledBlockTest` (both methods) pins the contract; `SandPreprocessorVariantsTest`/`SandContextResolveTest`/`SandEditorPsiTest` rewritten to enabled-only semantics; `SandFlagConditionalIndexTest`/`SandIncludeEntryStateTest` deleted as superseded. Two platform relocations fell out of making it pass headlessly: `PsiVFSListener` + `GlobalPsiVFSBulkFileListener` + `PsiVFSListenerStartUpActivity` moved ide-impl → `consulo.language.impl.internal.file` (headless had **no VFS→PSI invalidation at all** — `FileContentUtilCore.reparseFiles` published force-reload events into the void, so seed-drift reparses never invalidated cached PSI; `MUST_RECOMPUTE_FILE_TYPE` extracted to `consulo.document.internal.RecomputeFileTypeMarker` so both writer and reader modules share it), and drifted files are reparsed by `ModuleAwareIndexRootChangeListener` (options steer the parse now, so option drift = tree rebuild, not just index rows). Headless additions: real command builder in `HeadlessCommandProcessor` (inline execution + thread-local command depth — VFS reload-from-disk runs inside commands), `HeadlessLanguageEditorInternalHelper`. `SandClassSearch` scopes to `projectScope` (shared-JVM suites leak same-name classes across still-open projects under `allScope`). Stub version 8.

## Phased rollout

### Phase 1 — Platform plumbing ✅ DONE

- `IndexOption` public sealed interface + internal `IndexOptionImpl` with three record variants.
- `ModuleAwareIndexOptionProvider` extension point with `getInputFileTypes()` dispatch.
- `FileBasedIndexExtension.getOptionProviderIds()` hook.
- `OptionsMeta` + `OptionsMetaExternalizer` + `OptionsRevalidator` (revalidation algorithm).
- `ModuleAwareIndexMetaStorage` app-scoped service under `~/.consulo/system/caches/module-aware-index-meta/`.
- `ModuleAwareIndexMetaRecorder` write-path + read-path helpers.
- `FileBasedIndexImpl.doIndexFileContent` hook — records meta after successful `updateSingleIndex`.
- `ModuleAwareIndexRootChangeListener` — `rootsChanged` scans project files, diffs meta, triggers reindex on drift.

### Phase 2 — Stub integration ✅ DONE

- `StubUpdatingIndex.getOptionProviderIds()` returns every registered provider (per-file-type filtering narrows at record/revalidate time).
- `StubTreeLoaderImpl.readFromVFile` checks `isStale` up-front; on drift requests reindex and returns null so caller re-parses.

### Phase 3 — Platform correctness follow-ups ✅ DONE

1. **`stored == null` hole** — [ModuleAwareIndexMetaRecorder.isStale](modules/base/language-index-impl/src/main/java/consulo/language/index/impl/internal/moduleAware/ModuleAwareIndexMetaRecorder.java) and [ModuleAwareIndexRootChangeListener.revalidateOne](modules/base/language-index-impl/src/main/java/consulo/language/index/impl/internal/moduleAware/ModuleAwareIndexRootChangeListener.java) treat missing meta as "fine". A file indexed before its provider existed (plugin installed later) therefore never revalidates. Fix: for an options-sensitive index with applicable providers, missing meta on an already-indexed file (`isIndexedStateForFile`) means **reindex**.
2. **Read-path re-fire guard** — `StubTreeLoaderImpl.readFromVFile` calls `requestReindex` on every stale read; between request and completed reindex each stub load re-fires it. Add a per-fileId in-flight set in the recorder, cleared when fresh meta is recorded.
3. **Options composition cache** — `(module, file) → composed IndexOption` cached on a rootsChanged modification tracker; `isStale` currently serializes + hashes per stub load.

### Phase 4 — `IndexableFileScanner` port — REMOVED

Ported, reshaped (project-scoped, session-owned state, platform-managed finish), then deleted: the condition-annotated model moved all pre-pass outputs to query/resolve time, leaving the scan-phase hook without a consumer. The Consulo shape is documented in the pre-indexing section for the day a genuine parse-time pre-pass input appears.

### Phase 5 — Sand becomes the C-family testbed ("index works differently per flag") ✅ DONE

The test language already exists — `sandbox/sand-language-plugin`, consumed by `modules/it` (`SandStubIndexTest`), already carrying `SandModuleAwareIndexOptionProvider`/`SandOptions`. Extend it with minimal preprocessor semantics:

Final shape (condition-annotated; earlier seeded-parse variant replaced):

1. **Grammar** (`SandTokens` + lexer + `SandParser`): `#flag NAME`, `#if NAME` … `#else` … `#end`, `#include "file.sand"`. **Both** branches parse into real declarations inside a nested `CONDITIONAL_BLOCK` — no evaluation at parse time; the parse is a pure function of file content. Stub version bumped.
2. **Guards in stubs**: `SandClassStub` carries the conjunction of enclosing flag literals (`"A&!B"`), derived once in `createStub` from the block structure (`SandConditions.conditionOf`), restored by the deserializer — the single source (`SandClass.getCondition()` reads the green stub only).
3. **Module flags**: `SandMutableModuleExtension` gets `Set<String> flags` state; tests mutate it via `ModifiableRootModel` commit (fires rootsChanged naturally).
4. **Entry environments** (`SandIncludeSimulator` + `SandFlagEnv`): the simulation records **every distinct** inclusion environment per file (multi-context: full-A, full-B, standalone). Computed as a project-level `CachedValue` invalidated by the VFS modification tracker — fully lazy, no service, no scan hook, no listener.
5. **Queries** (`SandClassSearch`): `active(project, name)` = guard matches *any* of the declaring file's contexts; `matching(project, name, env)` = explicit requester environment (the resolve-through-includer path). `allVariants` = raw index.
6. **Provider**: `SandOptions(symbols, target)` — module flags only (the standalone mechanism); inclusion contexts never enter the payload. For sand the flags don't affect the pure parse — the provider exists to exercise the platform drift machinery (meta-hole test).

### Phase 6 — Integration tests (`modules/it`) ✅ DONE — 79/79 IT suite green

- **`SandFlagConditionalIndexTest`** — `a.sand`: `#if A class Foo {} #else class Bar {} #end`. Both variants always in the index; filtered `active` query answers `Bar` without the flag, flips to `Foo` on module-flag commit — read live at query time, no reindex dependence.
- **`SandIncludeEntryStateTest`** — the `#define A` + `#include "some.h"` scenario: `some.sand` has two contexts (standalone → `NoA`, inclusion from `main.sand` with `#flag A` → `WithA`) — both variants active, each in its context. Externally dropping the includer's `#flag` removes the only `A`-context → `WithA` goes inactive with **no reindex of `some.sand`**; a requester supplying `{A}` explicitly still selects it (the index carries all possibilities).
- **`SandPreprocessorVariantsTest`** — broader C-family semantics in one file: `#ifndef` include-guard idiom, self-defined flag activating the file's own guard, three-way `#if`/`#elif`/`#else` chain (module flag selects the segment), `#undef` keeping a variant dead while indexed.
- **`SandContextResolveTest`** — resolution through context: two same-name `Item` variants in an included file; `class UserA : Item {}` in a `#flag A` includer resolves to the `"A"`-guarded variant, `UserB` in a flag-less includer to `"!A"` — per referencing file, no reindex.
- **Meta-hole guard test** (Phase 3.1) — index a sand file, delete its meta entry through the storage API, fire rootsChanged, assert reindex re-records meta.
- **`SandEditorPsiTest`** — the headless editor-path simulation: loaded document over an indexed file (stub→AST reconciliation), a transferred-write keystroke committed by the **asynchronous commit pipeline alone** (compute on the commit pool, finish + write action on the UI thread — an explicit `commitDocument` from a second thread would race the async finish; production serializes both on the UI thread), unsaved-document indexing, variant binding probes before/after edit and after save. Green stub discipline it enforces: an AST-switched element has no green stub, so `SandClass.getCondition()` falls back to the same structural walk `createStub` uses (index path still never parses).
- Headless env grown for the editor path: production `DocumentCommitThread` relocated to `language-impl` (one commit processor everywhere), JB's `ThreadLocal` commit-progress counter ported into `PsiDocumentManagerBase` + `PomModelImpl` dropped the `isDispatchThread()` conjunct (JB parity — commit-transaction classification is per-thread, so headless commits off-EDT classify correctly), `ReadMostlyRWLock` transfer-polling makes UI-thread write acquisition deadlock-free (the write-on-UI-thread test ban was deleted), plus headless `AsyncExecutionService`/`DataManager`/`IdeFocusManager` (shared `PassThroughApplicationIdeFocusManager` base with web/Qt)/`AttachmentFactory`.

Verification per phase: full build (`mvn package -T 1C -Dmaven.test.skip=true > log; check $?`), then append `\n` to `modules/it/src/main/java/module-info.java` (the cache stores the skip-tests run; `touch` doesn't invalidate content hashes), then full-reactor `mvn test -T 1C -Dtest='Sand*Test,ExternalChangesReindexTest' -Dsurefire.failIfNoSpecifiedTests=false` — confirm `Tests run:` lines (build cache can silently skip). Never bare `mvn test` after source edits: without re-jarring, the it module path falls back to the stale `~/.m2` snapshot.

### Phase 7 — First plugin consumer: consulo-cpp

- `CppOptions` record with macros + include paths + toolchain fingerprint.
- `DataExternalizer<CppOptions>`.
- `ModuleAwareIndexOptionProvider` for `.cpp` / `.h` / `.hpp` returning `sharablePerOption(...)`.
- Store per-module options in a `CppBuildModuleExtension` (or similar) — plugin's own config surface via Project Structure. Prototyping can start with a hardcoded default.
- Rework `PreprocessorExpander` to accept `(includePaths, predefinedMacros)` from `CppOptions` — the parser / stub builder actually consumes options during indexing.
- Split the expander into three modes over one engine (the `myProcessedFiles`/include-path machinery already has the constructor seams):
  - **pre-pass**: directive-only walk for the `IndexableFileScanner` session, computes entry states;
  - **index parse**: seeded with stored entry state, consumes includes for macro state only — no text inlining (today `CFileElementType.doParseContents` inlines via `buildText`; index path must stop doing that);
  - **editor parse**: full expansion allowed.
- Entry-state storage: persistent (`PersistentHashMap` under `~/.consulo/system/caches/`), unlike sand's in-memory testbed variant; recompute listener per the sand pattern.
- Validate end-to-end: edit module options → rootsChanged → stub invalidated → next resolve reparses with new macros; edit an includer's `#define` → included header reindexed.

### Phase 8 — Additional language adopters

- C# (preprocessor symbols + TFM).
- Rust (cfg + features + target triple).
- Haxe (defines + target).
- TypeScript (tsconfig variants).
- Kotlin MPP (source-set / target).

Each is ~100 lines of glue once platform is stable.

### Phase 9 — Out of scope for this plan

UI layer (editor notifications, module-view pickers, pin/last-used persistence) lives in a separate effort — distinct implementation, distinct module. This plan stays index-only.

## Design decisions (locked)

| Decision | Rationale |
|---|---|
| Sealed public API, single internal impl | Prevents plugins from implementing `IndexOption`; storage layer can pattern-match safely; evolution-safe. |
| Factory methods, private constructors | Locks down instantiation. Plugin code reads as `IndexOption.sharablePerOption(...)` — no records/sealed noise. |
| Singleton only for `FullySharable` | `UniqueToModule` / `SharablePerOption` carry a `LocalizeValue` display label (reserved for future UI layer) — per-call allocation is unavoidable. Acceptable: these paths are not hot. |
| `DataExternalizer<T>` for payload | Matches existing `FileBasedIndex` idiom; hash derived from serialized bytes; versioning via `getVersion()`. |
| Options-dependence declared per-index, not per-file | Word index / todo index on a C++ file are not invalidated when macros change — only macro-sensitive indexes are. Right granularity. |
| Multiple providers per file supported, composed per-index | Macros / toolchain / target are independent dimensions. Plugins contribute each separately; platform composes with "most restrictive tier wins" rule. |
| Single active setup per module | Matches CLion; storage cost identical to today; switching = bounded reindex. |
| Single stub per file, invalidated on options drift | Cross-project sharing / multi-variant storage deferred. Correctness via invalidation is sufficient for v1. |
| All derived state under `~/.consulo/system/caches/` | Project's `.consulo/` dir must stay lightweight. Indexes and options-meta go to the global caches dir (project-scoped via `<project-hash>` subdir). |
| `FilePropertyPusher` is not the foundation | App-level storage keyed only by fileId — structurally wrong for project-specific context. |

## Open questions / deferred

- **Orphan shard GC** — `SharablePerOption` cache entries live until LRU eviction or manual invalidate. Acceptable for v1.
- **Global-scope stub queries** — `StubIndex.getElements(key, scope=all)` on an options-sensitive index: scope resolution picks the active setup per module. Platform plumbs module through scope resolution.
- **Cross-module find-usages** — shared header viewed from N modules: union over each module's active setup; dedupe at result level.
- **Options-hash collisions** — 32-bit hash has 1-in-4B false match; upgrade to 64-bit xxhash if real-world collisions appear.
- **Provider call rate** — provider hit on every index read path; cache `(module, file) → IndexOption` with invalidation tied to rootsChanged.
- **Determinism contract for `SharablePerOption`** — plugin must include every factor affecting output in the options payload; no hidden inputs (env, wallclock, random). Document explicitly for plugin authors.

## What this unlocks

- Correct resolve for ~10 language plugins that currently guess or ignore build context.
- Unblocks consulo-cpp preprocessing correctness work: predefined macros finally have a proper home.
- Natural foundation the UI layer (separate effort) can build on.
- Upstream differentiation: IntelliJ platform never generalized this; CLion solved it privately for C++.

## Non-goals (for this plan)

- Any UI — editor notifications, module-view pickers, pin/last-used persistence, status surfaces. Separate implementation.
- Multi-variant concurrent storage. Single stub per file at any time; options drift → stub replaced.
- Cross-project content-addressed stub sharing. **Rejected permanently**, not deferred: it is structurally the snapshot-input-mappings bet — content-hash-keyed shared state under diff-based updates — which JB ran 2014→2023 (`8b124a1d3fc7` … disabled `13c96044293b` IJPL-147, deleted `5bfedcc0b98e`, −1269 lines) and abandoned after concluding forward-index recomputation is cheaper than the corruption surface. Options-dependence makes the purity assumption even weaker than it was for plain content.
- TU-rooted indexing (index sources with full include expansion, attribute symbols to headers, reverse-graph invalidation). That is a separate engine outside `FileBasedIndex` — the CLion path — not an extension of it.
- Making individual stub element types context-aware. Options-keying applies at the storage layer, not inside `IStubElementType`.
- Replacing `FileBasedIndex`. This is an **extension**, not a new framework.
- Cross-process clangd / rust-analyzer integration. Orthogonal, future work.
