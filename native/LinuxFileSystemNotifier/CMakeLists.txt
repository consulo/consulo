cmake_minimum_required(VERSION 3.5)
project(LinuxFsNotifier C)

if(TARGET_X64)
    set(FILE_NAME "fsnotifier64")
elseif(TARGET_ARM64)
    set(FILE_NAME "fsnotifier-aarch64")
elseif(TARGET_RISCV64)
    set(FILE_NAME "fsnotifier-riscv64")
elseif(TARGET_LOONG64)
    set(FILE_NAME "fsnotifier-loong64")
else()
    message(FATAL_ERROR "Unsupported platform")
endif()

add_compile_options("-O2" "-Wall" "-Wextra" "-Wpedantic" "-std=c11" "-D_DEFAULT_SOURCE")

add_executable(${FILE_NAME} fsnotifier.h inotify.c main.c util.c)