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

#include <jni.h>
#include <dlfcn.h>
#include "version_utils.hpp"
#include "sk_stupid_common_def.hpp"
#include "sk_stupid_injector.hpp"

bool SkStupidInjector::supportApi(int api) {
    return (api >= minSdkVersion) && (api <= maxSdkVersion);
}

SkStupidInjector::SkStupidInjector(JNIEnv* env) {
    mApiLevel = getDeviceApiLevel();
    if (this->supportApi(mApiLevel) == false) {
        mSymbolsComplete = false;
        return;
    }

    loadSymbols(env);
}

SkStupidInjector::~SkStupidInjector() {

}

void SkStupidInjector::loadSymbols(JNIEnv* env) {
    if (mApiLevel >= 21) {  // Android 5.0+
        mRTLibrary = dlopen("libandroid_runtime.so", RTLD_NOW | RTLD_LOCAL);
        if (mRTLibrary == nullptr) {
            mSymbolsComplete = false;
            return;
        }
        create_canvas = (Func_create_canvas)dlsym(mRTLibrary, "_ZN7android6Canvas13create_canvasEP8SkCanvas");
    }

    jclass clazz = env->FindClass("android/graphics/Canvas");
    mJavaCanvasClass = (jclass)env->NewGlobalRef(clazz);
    env->DeleteLocalRef(clazz);

    if (mApiLevel >= 20) {  // Android 4.4w+
        mJavaCanvasCtorID = env->GetMethodID(mJavaCanvasClass, "<init>", "(J)V"); // 4.4w+ use int64 to store pointer
    } else {                // before
        mJavaCanvasCtorID = env->GetMethodID(mJavaCanvasClass, "<init>", "(I)V");
    }

    if (mApiLevel >= 19) {
        mJavaCanvasReleaseID = env->GetMethodID(mJavaCanvasClass, "release", "()V");
    } else {
        /*jclass clazz = env->FindClass("android/graphics/Canvas$CanvasFinalizer");
        mJavaCanvasFinalizerClass = (jclass)env->NewGlobalRef(clazz);
        env->DeleteLocalRef(clazz);

        mJavaCanvasFinalizer = env->GetFieldID(mJavaCanvasClass, "mFinalizer", "android/graphics/Canvas$CanvasFinalizer");
        mJavaCanvasFinalizerHandleID = env->GetFieldID(mJavaCanvasFinalizerClass, "mNativeCanvas", "I");
        mJavaCanvasHandleID = env->GetFieldID(mJavaCanvasClass, "mNativeCanvas", "I");*/
    }

    mSymbolsComplete = checkSymbols();
}

bool SkStupidInjector::checkSymbols() {
    if (mApiLevel >= 21 && (mRTLibrary == nullptr || create_canvas == nullptr)) {
        return false;
    }

    if (mJavaCanvasClass == nullptr || mJavaCanvasCtorID == nullptr) {
        return false;
    }

    if (mApiLevel >= 19 && mJavaCanvasReleaseID == nullptr) {
        return false;
    }

    /*if (mApiLevel < 19) {
        if (mJavaCanvasFinalizerClass == nullptr || mJavaCanvasFinalizer == nullptr
            || mJavaCanvasFinalizerHandleID == nullptr || mJavaCanvasHandleID == nullptr) {
            return false;
        }
    }*/

    return true;
}

bool SkStupidInjector::isDeviceSupported() {
    return mSymbolsComplete;
}

void SkStupidInjector::dispose(JNIEnv* env) {
    if (mJavaCanvas) {
        if (mApiLevel >= 19) {
            env->CallVoidMethod(mJavaCanvas, mJavaCanvasReleaseID);
        } else {

        }
        env->DeleteGlobalRef(mJavaCanvas);
        mJavaCanvas = nullptr;
    }
    if (mJavaCanvasClass) {
        env->DeleteGlobalRef(mJavaCanvasClass);
        mJavaCanvasClass = nullptr;
    }
    if (mApiLevel >= 21 && mRTLibrary) {
        dlclose(mRTLibrary);
        mRTLibrary = nullptr;
        create_canvas = nullptr;
    }
}

jobject SkStupidInjector::getJavaCanvas(JNIEnv* env, SkCanvas_t* skcanvas) {
    if (mSymbolsComplete == false) {
        return nullptr;
    }

    if (mSkCanvas != skcanvas) {
        mSkCanvas = skcanvas;

        if (mJavaCanvas) {
            if (mApiLevel >= 19) {
                env->CallVoidMethod(mJavaCanvas, mJavaCanvasReleaseID);
            } else {

            }
            env->DeleteGlobalRef(mJavaCanvas);
            mJavaCanvas = nullptr;
        }

        if (mApiLevel >= 20) {  // Android 4.4w+
            mSkCanvas->ref();
            void* canvasWrapper = nullptr;
            if (mApiLevel >= 21) {  // Android 5.0+
                canvasWrapper = create_canvas(mSkCanvas);
            } else {                // Android 4.4w
                canvasWrapper = mSkCanvas;
            }
            mJavaCanvas = env->NewObject(mJavaCanvasClass, mJavaCanvasCtorID, reinterpret_cast<jlong>(canvasWrapper));
        } else {                // before
            mSkCanvas->ref();
            mJavaCanvas = env->NewObject(mJavaCanvasClass, mJavaCanvasCtorID, reinterpret_cast<jint>(mSkCanvas));
        }

        mJavaCanvas = env->NewGlobalRef(mJavaCanvas);
    }
    return mJavaCanvas;
}
