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
#include <android/log.h>
#include "version_utils.hpp"
#include "sk_stupid_common_def.hpp"
#include "sk_stupid_renderer_base.hpp"
#include "sk_stupid_renderer_16.hpp"
#include "sk_stupid_renderer_18.hpp"
#include "sk_stupid_injector.hpp"
#include "skia_redirector_jni.hpp"

#ifndef NELEM
    #define NELEM(x) ((int)(sizeof(x) / sizeof((x)[0])))
#endif

static const int minSdkVersion = 18;
static const int maxSdkVersion = 21;
static bool isDeviceSupported = false;

static bool testIsDeviceSupported();

int initSkiaRedirectorJni(JNIEnv* env) {
    isDeviceSupported = testIsDeviceSupported();
    return 0;
}

int termSkiaRedirectorJni() {
    return 0;
}

static bool testIsDeviceSupported() {
    int apiLevel = getDeviceApiLevel();
    if (apiLevel >= minSdkVersion && apiLevel <= maxSdkVersion) {
        SkStupidRendererBase* testRenderer = nullptr;

        if (false/*SkStupidRenderer_16::supportApi(apiLevel)*/) {
            //testRenderer = new SkStupidRenderer_16(nullptr);
        } else if (SkStupidRenderer_18::supportApi(apiLevel)) {
            testRenderer = new SkStupidRenderer_18(nullptr);
        } else {
            return false;
        }

        bool support = testRenderer->isDeviceSupported();
        delete testRenderer;
        return support;
    }
    return false;
}

static SkStupidRendererBase* createCompatibleRenderer() {
    if (isDeviceSupported == false) {
        return nullptr;
    }

    int apiLevel = getDeviceApiLevel();
    if (false/*SkStupidRenderer_16::supportApi(apiLevel)*/) {
        //return new SkStupidRenderer_16(nullptr);
    } else if (SkStupidRenderer_18::supportApi(apiLevel)) {
        return new SkStupidRenderer_18(nullptr);
    }

    return nullptr;
}

static jboolean nativeIsSupported(JNIEnv* env, jclass clazz) {
    return static_cast<jboolean>(isDeviceSupported);
}

static jlong nativeInit(JNIEnv* env, jobject thiz, jint width, jint height, jint msaaSampleCount) {
    __android_log_print(ANDROID_LOG_DEBUG, "SkiaRedirector", "nativeInit");

    if (isDeviceSupported == false) {
        return 0;
    }

    SkStupidRendererBase* renderer = createCompatibleRenderer();
    SkStupidInjector* injector = new SkStupidInjector(env);
    if (renderer->isDeviceSupported() == false || injector->isDeviceSupported() == false) {
        injector->dispose(env);
        delete injector;
        delete renderer;
        return 0;
    }

    renderer->setExtraData(injector);
    renderer->setupBackend(width, height, msaaSampleCount);
    return reinterpret_cast<jlong>(renderer);
}

static void nativeTerm(JNIEnv* env, jobject thiz, jlong nativeHandle) {
    __android_log_print(ANDROID_LOG_DEBUG, "SkiaRedirector", "nativeTerm");

    if (nativeHandle != 0) {
        SkStupidRendererBase* renderer = reinterpret_cast<SkStupidRendererBase*>(nativeHandle);
        SkStupidInjector* injector = reinterpret_cast<SkStupidInjector*>(renderer->getExtraData());
        renderer->teardownBackend();
        injector->dispose(env);
        delete injector;
        delete renderer;
        __android_log_print(ANDROID_LOG_DEBUG, "SkiaRedirector", "Cleanup succeed!");
    }
}

static void nativeUpdateSize(JNIEnv* env, jobject thiz, jlong nativeHandle, jint width, jint height) {
    __android_log_print(ANDROID_LOG_DEBUG, "SkiaRedirector", "nativeUpdateSize");

    if (nativeHandle != 0) {
        SkStupidRendererBase* renderer = reinterpret_cast<SkStupidRendererBase*>(nativeHandle);
        renderer->updateSize(width, height);
    }
}

static jobject nativeLockCanvas(JNIEnv* env, jobject thiz, jlong nativeHandle) {
    if (nativeHandle != 0) {
        SkStupidRendererBase* renderer = reinterpret_cast<SkStupidRendererBase*>(nativeHandle);
        SkStupidInjector* injector = reinterpret_cast<SkStupidInjector*>(renderer->getExtraData());
        if (injector) {
            jobject javaCanvas = injector->getJavaCanvas(env, renderer->lockCanvas());
            return javaCanvas;
        }
    }
    return nullptr;
}

static void nativeUnlockCanvasAndPost(JNIEnv* env, jobject thiz, jlong nativeHandle, jobject canvas) {
    if (nativeHandle != 0) {
        reinterpret_cast<SkStupidRendererBase*>(nativeHandle)->unlockCanvasAndPost(nullptr);
    }
}

static JNINativeMethod gMethods[] = {
    {"nativeIsSupported", "()Z", (void*)nativeIsSupported},
    {"nativeInit", "(III)J", (void*)nativeInit},
    {"nativeTerm", "(J)V", (void*)nativeTerm},
    {"nativeUpdateSize", "(JII)V", (void*)nativeUpdateSize},
    {"nativeLockCanvas", "(J)Landroid/graphics/Canvas;", (void*)nativeLockCanvas},
    {"nativeUnlockCanvasAndPost", "(JLandroid/graphics/Canvas;)V", (void*)nativeUnlockCanvasAndPost}
};

int registerSkiaRedirectorMethods(JNIEnv* env, const char* className) {
    jclass clazz = env->FindClass(className);
    return env->RegisterNatives(clazz, gMethods, NELEM(gMethods));
}
