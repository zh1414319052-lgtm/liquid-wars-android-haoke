// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include <mutex>
// One lock covers rules, render, immutable JNI copies and native lifetime.
// Dynamic armies must never reallocate while the GL thread reads their vector.
extern std::mutex nativeStateMutex;
