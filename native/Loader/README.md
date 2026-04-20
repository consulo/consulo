# Consulo Native Loader

Cross-platform Rust launcher for Consulo. Replaces `WinLoader` + `MacLoader` + `WinLoaderLibrary` + `MacLoaderLibrary`; adds Linux support (x86_64, aarch64, loongarch64, riscv64gc).

**Scope and rationale:** see [PLAN.md](./PLAN.md).

## Workspace layout

```
core/     — shared library: build discovery, JRE lookup, vmoptions, JVM bootstrap
loader/   — binary artifact: the .exe / .app bin / ELF executable
library/  — cdylib artifact: the .dll / .dylib / .so (C ABI gate)
```

## Build

```bash
# Host, fast dev build (debug, no resource embedding, small LOC rebuilds ~3s):
scripts/dev-build.sh

# Host, release:
cargo build --release

# Cross-compile — install the cross linker first, then:
rustup target add loongarch64-unknown-linux-gnu
cargo build --release --target loongarch64-unknown-linux-gnu

# Cross linkers needed per target (apt package names):
#   aarch64-unknown-linux-gnu       → gcc-aarch64-linux-gnu
#   loongarch64-unknown-linux-gnu   → gcc-loongarch64-linux-gnu
#   riscv64gc-unknown-linux-gnu     → gcc-riscv64-linux-gnu
```

## UI variant (AWT / SWT / …)

AWT is default. To switch:

```bash
cargo build --release --no-default-features --features swt
```

Variant selection is compile-time; each variant produces its own binary filename suffix (e.g. `libconsulo-swt.so`). AWT has no suffix.

## Debug logging

Set `CONSULO_LAUNCHER_DEBUG=1` (or the legacy `IDEA_LAUNCHER_DEBUG=1`) in the environment.

## Typing loop

- `cargo check` — type-check only, ~500 ms.
- `scripts/dev-build.sh` — compile host target.
- `scripts/dev-run.sh` — build + run the loader.
- `cargo test -p consulo-loader-core` — run core crate unit tests.

## ABI surface gate

The `library` cdylib's exported C symbols are load-bearing — existing Consulo Loaders in the field call them by name. CI diffs actual exports against [abi-surface.expected.txt](./abi-surface.expected.txt). Any new or renamed export requires an explicit edit to that file.

## Status

Scaffold (PLAN.md Phase 0). `cargo check` passes; `cargo build` produces stub binaries. Full JNI bootstrap, single-instance IPC, and platform hacks are Phase 2 (tagged `TODO(phase-2)` in source).
