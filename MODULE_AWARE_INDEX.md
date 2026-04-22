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

### Phase 3 — First plugin consumer: consulo-cpp

- `CppOptions` record with macros + include paths + toolchain fingerprint.
- `DataExternalizer<CppOptions>`.
- `ModuleAwareIndexOptionProvider` for `.cpp` / `.h` / `.hpp` returning `sharablePerOption(...)`.
- Store per-module options in a `CppBuildModuleExtension` (or similar) — plugin's own config surface via Project Structure. Prototyping can start with a hardcoded default.
- Rework `PreprocessorExpander` to accept `(includePaths, predefinedMacros)` from `CppOptions` — the parser / stub builder actually consumes options during indexing.
- Validate end-to-end: edit module options → rootsChanged → stub invalidated → next resolve reparses with new macros.

### Phase 4 — Additional language adopters

- C# (preprocessor symbols + TFM).
- Rust (cfg + features + target triple).
- Haxe (defines + target).
- TypeScript (tsconfig variants).
- Kotlin MPP (source-set / target).

Each is ~100 lines of glue once platform is stable.

### Phase 5 — Out of scope for this plan

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
- Cross-project content-addressed stub sharing. Deferred; correctness-first.
- Making individual stub element types context-aware. Options-keying applies at the storage layer, not inside `IStubElementType`.
- Replacing `FileBasedIndex`. This is an **extension**, not a new framework.
- Cross-process clangd / rust-analyzer integration. Orthogonal, future work.
