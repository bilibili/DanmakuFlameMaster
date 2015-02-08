/*
 * Copyright (C) 2015 zheng qian <xqq@0ginr.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <cassert>
#include <jni.h>
#include <android/log.h>
#include "skia_redirector_jni.hpp"
#include "jni_entry.hpp"

#ifndef NELEM
    #define NELEM(x) ((int)(sizeof(x) / sizeof((x)[0])))
#endif

static const int jniVersion = JNI_VERSION_1_4;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "libDFMACC.so", "JNI_OnLoad");

    JNIEnv* env = nullptr;
    if (jvm->GetEnv((void**)&env, jniVersion) != JNI_OK) {
        return -1;
    }
    assert(env != nullptr);

    initSkiaRedirectorJni(env);
    registerSkiaRedirectorMethods(env, "master/flame/danmaku/ui/SkiaRedirector/SkStupidRenderer");

    return jniVersion;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* jvm, void* reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "libDFMACC.so", "JNI_OnUnload");

    termSkiaRedirectorJni();
}
