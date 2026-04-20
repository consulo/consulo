/*
 * Copyright 2013-2026 consulo.io
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
//! Minimal stderr logger gated by `CONSULO_LAUNCHER_DEBUG` (or legacy `IDEA_LAUNCHER_DEBUG`).

use log::{LevelFilter, Metadata, Record};
use std::env;
use std::sync::OnceLock;
use std::time::Instant;

pub const DEBUG_ENV_VAR: &str = "CONSULO_LAUNCHER_DEBUG";
pub const DEBUG_ENV_VAR_LEGACY: &str = "IDEA_LAUNCHER_DEBUG";

static START: OnceLock<Instant> = OnceLock::new();

pub fn init() {
    let _ = START.get_or_init(Instant::now);
    let filter = if is_debug() { LevelFilter::Debug } else { LevelFilter::Error };
    let _ = log::set_boxed_logger(Box::new(Logger));
    log::set_max_level(filter);
}

pub fn is_debug() -> bool {
    env::var(DEBUG_ENV_VAR).is_ok() || env::var(DEBUG_ENV_VAR_LEGACY).is_ok()
}

struct Logger;

impl log::Log for Logger {
    fn enabled(&self, _: &Metadata<'_>) -> bool {
        true
    }
    fn log(&self, record: &Record<'_>) {
        let start = START.get_or_init(Instant::now);
        let ms = start.elapsed().as_millis();
        eprintln!(
            "{:>5} [{:5}] {}: {}",
            ms,
            record.level(),
            record.target(),
            record.args()
        );
    }
    fn flush(&self) {}
}
