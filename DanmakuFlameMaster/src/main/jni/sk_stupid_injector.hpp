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

#ifndef _SK_STUPID_INJECTOR_HPP
#define _SK_STUPID_INJECTOR_HPP

#include <jni.h>
#include "sk_stupid_common_def.hpp"

class SkStupidInjector {
public:
    static bool supportApi(int api);
public:
    static const int minSdkVersion = 16;
    static const int maxSdkVersion = 21;
public:
    SkStupidInjector(JNIEnv* env);

    ~SkStupidInjector();

    bool isDeviceSupported();

    jobject getJavaCanvas(JNIEnv* env, SkCanvas_t* skcanvas);

    void dispose(JNIEnv* env);
private:
    void loadSymbols(JNIEnv* env);

    bool checkSymbols();
private:
    typedef void* (*Func_create_canvas)(SkCanvas_t* skcanvas);
private:
    int mApiLevel = 0;
    bool mSymbolsComplete = false;
    SkCanvas_t* mSkCanvas = nullptr;
    jobject mJavaCanvas = nullptr;
    jclass mJavaCanvasClass = nullptr;
    jmethodID mJavaCanvasCtorID = nullptr;
    void* mRTLibrary = nullptr;
    Func_create_canvas create_canvas = nullptr;
};


#endif // _SK_STUPID_INJECTOR_HPP
