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

#define TAG_SK_STUPID_RENDERER_18 18

#include <malloc.h>
#include <dlfcn.h>
#include <pthread.h>
#include <GLES2/gl2.h>
#include <android/log.h>
#include "version_utils.hpp"
#include "sk_stupid_def_18.hpp"
#include "sk_stupid_renderer_18.hpp"

static GrGLInterface_Symbol_t  GrGLInterface_Symbol;
static GrContext_Symbol_t      GrContext_Symbol;
static GrRenderTarget_Symbol_t GrRenderTarget_Symbol;
static SkGpuDevice_Symbol_t    SkGpuDevice_Symbol;
static SkCanvas_Symbol_t       SkCanvas_Symbol;

bool SkStupidRenderer_18::supportApi(int api) {
    return (api >= minSdkVersion) && (api <= maxSdkVersion);
}

SkStupidRenderer_18::SkStupidRenderer_18(void* nativeHandle) {
    mApiLevel = getDeviceApiLevel();
    mAndroidVersion = getDeviceAndroidVersion();
    loadSymbols();
    pthread_mutex_init(&this->mCanvasMutex, nullptr);
}

SkStupidRenderer_18::~SkStupidRenderer_18() {
    if (mBackendType != kNone_BackEndType) {
        teardownBackend();
    }

    pthread_mutex_destroy(&this->mCanvasMutex);

    if (mLibraryHandle) {
        dlclose(mLibraryHandle);
        mLibraryHandle = nullptr;
    }
}

void SkStupidRenderer_18::setExtraData(void* data) {
    mExtraData = data;
}

void* SkStupidRenderer_18::getExtraData() {
    return mExtraData;
}

void SkStupidRenderer_18::loadSymbols() {
    if (this->supportApi(mApiLevel) == false) {
        mSymbolsLoaded = false; mSymbolsComplete = false;
        return;
    }

    mLibraryHandle = dlopen("libskia.so", RTLD_NOW | RTLD_LOCAL);
    if (mLibraryHandle == nullptr) {
        mSymbolsLoaded = false; mSymbolsComplete = false;
        return;
    }

    GrGLInterface_Symbol.GrGLCreateNativeInterface = (Func_GrGLCreateNativeInterface)dlsym(mLibraryHandle, symbol_GrGLCreateNativeInterface);
    GrContext_Symbol.Create = (Func_Create)dlsym(mLibraryHandle, symbol_Create);
    GrContext_Symbol.contextDestroyed = (Func_ContextDestroyed)dlsym(mLibraryHandle, symbol_ContextDestroyed);
    GrContext_Symbol.wrapBackendRenderTarget = (Func_WrapBackendRenderTarget)dlsym(mLibraryHandle, symbol_WrapBackendRenderTarget);
    GrContext_Symbol.flush = (Func_Flush)dlsym(mLibraryHandle, symbol_Flush);
    GrContext_Symbol.Dtor = (Func_GrContext_Dtor)dlsym(mLibraryHandle, symbol_GrContext_Dtor);

    if (mApiLevel >= 21) {
        GrRenderTarget_Symbol.Dtor = (Func_GrRenderTarget_Dtor)dlsym(mLibraryHandle, symbol_GrRenderTarget_Dtor_21_later);
        SkGpuDevice_Symbol.Ctor = (Func_SkGpuDeviceCtor)dlsym(mLibraryHandle, symbol_SkGpuDevice_Ctor_21_later);
    } else {
        GrRenderTarget_Symbol.Dtor = (Func_GrRenderTarget_Dtor)dlsym(mLibraryHandle, symbol_GrRenderTarget_Dtor);
        SkGpuDevice_Symbol.Ctor = (Func_SkGpuDeviceCtor)dlsym(mLibraryHandle, symbol_SkGpuDevice_Ctor);
    }

    SkGpuDevice_Symbol.Dtor = (Func_SkGpuDeviceDtor)dlsym(mLibraryHandle, symbol_SkGpuDevice_Dtor);

    if ((mAndroidVersion.major == 4 && mAndroidVersion.minor == 4 && mAndroidVersion.revision >= 3)
        || (mAndroidVersion.major >= 5)) {
        SkCanvas_Symbol.Ctor = (Func_SkCanvasCtor)dlsym(mLibraryHandle, symbol_SkCanvas_Ctor_19_2_later);
    } else {
        SkCanvas_Symbol.Ctor = (Func_SkCanvasCtor)dlsym(mLibraryHandle, symbol_SkCanvas_Ctor);
    }

    SkCanvas_Symbol.Dtor = (Func_SkCanvasDtor)dlsym(mLibraryHandle, symbol_SkCanvas_Dtor);

    mSymbolsLoaded = true;
    mSymbolsComplete = checkSymbols();
}

bool SkStupidRenderer_18::checkSymbols() {
    if (mSymbolsLoaded) {
        if (GrGLInterface_Symbol.GrGLCreateNativeInterface &&
            GrContext_Symbol.Create &&
            GrContext_Symbol.contextDestroyed &&
            GrContext_Symbol.wrapBackendRenderTarget &&
            GrContext_Symbol.flush &&
            GrContext_Symbol.Dtor &&
            GrRenderTarget_Symbol.Dtor &&
            SkGpuDevice_Symbol.Ctor &&
            SkGpuDevice_Symbol.Dtor &&
            SkCanvas_Symbol.Ctor &&
            SkCanvas_Symbol.Dtor) {
            return true;
        } else {
            __android_log_print(ANDROID_LOG_WARN, "SkStupidRenderer_18", "Symbols: %p,%p,%p,%p,%p,%p,%p,%p,%p,%p,%p",
                    GrGLInterface_Symbol.GrGLCreateNativeInterface, GrContext_Symbol.Create, GrContext_Symbol.contextDestroyed,
                    GrContext_Symbol.wrapBackendRenderTarget, GrContext_Symbol.flush, GrContext_Symbol.Dtor,
                    GrRenderTarget_Symbol.Dtor, SkGpuDevice_Symbol.Ctor, SkGpuDevice_Symbol.Dtor, SkCanvas_Symbol.Ctor, SkCanvas_Symbol.Dtor);
            dlclose(mLibraryHandle);
            mLibraryHandle = nullptr;
        }
    }
    return false;
}

bool SkStupidRenderer_18::isDeviceSupported() {
    return mSymbolsLoaded && mSymbolsComplete;
}

bool SkStupidRenderer_18::setupBackend(int width, int height, int msaaSampleCount) {
    if (mSymbolsLoaded == false || mSymbolsComplete == false) {
        return false;
    }

    DbgAssert(mBackendType == kNone_BackEndType);
    DbgAssert(mCurrentContext == nullptr);
    DbgAssert(mCurrentInterface == nullptr);
    DbgAssert(mCurrentRenderTarget == nullptr);

    mMSAASampleCount = msaaSampleCount;
    mBackendType = kNativeGL_BackEndType;

    GrGLInterface_t* glInterface = GrGLInterface_Symbol.GrGLCreateNativeInterface();
    mCurrentInterface = glInterface;

    mCurrentContext = GrContext_Symbol.Create(GrBackend_t::kOpenGL_GrBackend, (GrBackendContext_t)mCurrentInterface);

    if (mCurrentContext == nullptr || mCurrentInterface == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "SkStupidRenderer_18",
                "Fail to setup GrContext/GrGLInterface: %p, %p", mCurrentContext, mCurrentInterface);
        Sk_SafeUnref(mCurrentContext, (void*)GrContext_Symbol.Dtor);
        Sk_SafeUnref(mCurrentInterface);
        mCurrentContext = nullptr;
        mCurrentInterface = nullptr;
        mBackendType = kNone_BackEndType;

        return false;
    }

    this->mWidth = width;
    this->mHeight = height;
    this->windowSizeChanged();

    return true;
}

bool SkStupidRenderer_18::teardownBackend() {
    if (mCanvas) {
        mCurrentContext->ref();  // temp workaround for "call to OpenGL ES API with no current context" error
        mCanvas->unref((void*)SkCanvas_Symbol.Dtor);
        mCanvas = nullptr;
    }

    if (mCurrentContext) {
        //GrContext_Symbol.contextDestroyed(mCurrentContext);  // WTF this function will block thread forever and 100% cpu?
        mCurrentContext->unref((void*)GrContext_Symbol.Dtor);  // WTF the GrContext's dtor decrease 2 refcnt while destruct?
        mCurrentContext = nullptr;
    }

    Sk_SafeUnref(mCurrentInterface);
    mCurrentInterface = nullptr;
    Sk_SafeUnref(mCurrentRenderTarget, (void*)GrRenderTarget_Symbol.Dtor);
    mCurrentRenderTarget = nullptr;

    mBackendType = kNone_BackEndType;
    return true;
}

void SkStupidRenderer_18::windowSizeChanged() {
    if (mCurrentContext) {
        if (mApiLevel == 18) {
            GrBackendRenderTargetDesc_18_t desc18;
            desc18.width = this->mWidth;
            desc18.height = this->mHeight;
            desc18.config = kSkia8888_GrPixelConfig;
            desc18.sampleCount = mMSAASampleCount;
            desc18.stencilBits = 8;

            GLint buffer = 0;
            glGetIntegerv(SK_GR_GL_FRAMEBUFFER_BINDING, &buffer);
            desc18.renderTargetHandle = buffer;

            Sk_SafeUnref(mCurrentRenderTarget, (void*)GrRenderTarget_Symbol.Dtor);
            mCurrentRenderTarget = GrContext_Symbol.wrapBackendRenderTarget(mCurrentContext, &desc18);
        } else if (mApiLevel >= 19) {
            GrBackendRenderTargetDesc_19later_t desc19;
            desc19.width = this->mWidth;
            desc19.height = this->mHeight;
            desc19.config = kSkia8888_GrPixelConfig;
            desc19.origin = GrSurfaceOrigin_t::kBottomLeft_GrSurfaceOrigin;
            desc19.sampleCount = mMSAASampleCount;
            desc19.stencilBits = 8;

            GLint buffer = 0;
            glGetIntegerv(SK_GR_GL_FRAMEBUFFER_BINDING, &buffer);
            desc19.renderTargetHandle = buffer;

            Sk_SafeUnref(mCurrentRenderTarget, (void*)GrRenderTarget_Symbol.Dtor);
            mCurrentRenderTarget = GrContext_Symbol.wrapBackendRenderTarget(mCurrentContext, (GrBackendRenderTargetDesc_18_t*)&desc19);
        }

        if (mCanvas) {
            mCanvas->unref((void*)SkCanvas_Symbol.Dtor);
            mCanvas = nullptr;

            this->createSkCanvas();
        }
    }
}

void SkStupidRenderer_18::createSkCanvas() {
    SkGpuDevice_t* gpuDevice = (SkGpuDevice_t*)malloc(sizeof_SkGpuDevice);
    SkGpuDevice_Symbol.Ctor(gpuDevice, mCurrentContext, mCurrentRenderTarget);

    SkCanvas_t* skcanvas = (SkCanvas_t*)malloc(sizeof_SkCanvas);
    SkCanvas_Symbol.Ctor(skcanvas, gpuDevice);
    mCanvas = skcanvas;

    gpuDevice->unref((void*)SkGpuDevice_Symbol.Dtor);
}

void SkStupidRenderer_18::updateSize(int width, int height) {
    this->mWidth = width;
    this->mHeight = height;
    this->windowSizeChanged();
}

SkCanvas_t* SkStupidRenderer_18::lockCanvas() {
    pthread_mutex_lock(&this->mCanvasMutex);
    if (mCanvas == nullptr) {
        if (mCurrentContext) {
            this->createSkCanvas();
        }
    }
    return mCanvas;
}

void SkStupidRenderer_18::unlockCanvasAndPost(SkCanvas_t* canvas) {
    if (mCurrentContext) {
        GrContext_Symbol.flush(mCurrentContext, 0);
    }
    pthread_mutex_unlock(&this->mCanvasMutex);
}
