#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>
#include <SkGraphics.h>
#include <SkCanvas.h>
#include "sk_stupid_renderer.hpp"
#include "skia_redirector_jni.hpp"

#ifndef NELEM
	#define NELEM(x) ((int)(sizeof(x) / sizeof((x)[0])))
#endif

typedef void* (*rt_create_canvas_t)(SkCanvas* skcanvas);

static jclass    gSysCanvasClass = nullptr;
static jmethodID gSysCanvasCtorID = nullptr;

rt_create_canvas_t create_canvas = nullptr;

int initSkiaRedirectorJni(JNIEnv* env) {
	jclass clazz = env->FindClass("android/graphics/Canvas");
	gSysCanvasClass = (jclass)env->NewGlobalRef(clazz);
	env->DeleteLocalRef((jobject)clazz);
	gSysCanvasCtorID = env->GetMethodID(gSysCanvasClass, "<init>", "(J)V");

	void* pAndroidRuntimeLib = dlopen("libandroid_runtime.so", RTLD_NOW | RTLD_LOCAL);
	create_canvas = (rt_create_canvas_t)dlsym(pAndroidRuntimeLib, "_ZN7android6Canvas13create_canvasEP8SkCanvas");

	if (gSysCanvasCtorID != 0) {
		SkGraphics::Init();
		return 0;
	} else {
		return -1;
	}
}

int termSkiaRedirectorJni() {
	SkGraphics::Term();
	return 0;
}

static jboolean nativeIsSupported(JNIEnv* env, jclass clazz) {
	return true; // TODO
}

static jlong nativeInit(JNIEnv* env, jobject thiz, jint width, jint height, jint msaaSampleCount) {
	__android_log_print(ANDROID_LOG_DEBUG, "SkiaRedirector", "nativeInit");
	SkStupidRenderer* renderer = new SkStupidRenderer(nullptr);
	renderer->setupBackend(kNativeGL_BackEndType, width, height, msaaSampleCount);
	return reinterpret_cast<jlong>(renderer);
	__android_log_print(ANDROID_LOG_DEBUG, "SkiaRedirector", "nativeInit succeed!");
}

static void nativeTerm(JNIEnv* env, jobject thiz, jlong nativeHandle) {
	__android_log_print(ANDROID_LOG_DEBUG, "SkiaRedirector", "nativeTerm");
	if (nativeHandle != 0) {
		SkStupidRenderer* renderer = reinterpret_cast<SkStupidRenderer*>(nativeHandle);
		renderer->teardownBackend();
		delete renderer;
		__android_log_print(ANDROID_LOG_DEBUG, "SkiaRedirector", "nativeTerm succeed!");
	}
}

static jboolean nativeIsHardwareAccelerated(JNIEnv* env, jobject thiz, jlong nativeHandle) {
	if (nativeHandle != 0) {
		return reinterpret_cast<SkStupidRenderer*>(nativeHandle)->isHardwareAccelerated();
	} else {
		return static_cast<jboolean>(false);
	}
}

static void nativeUpdateSize(JNIEnv* env, jobject thiz, jlong nativeHandle, jint width, jint height) {
	if (nativeHandle != 0) {
		SkStupidRenderer* renderer = reinterpret_cast<SkStupidRenderer*>(nativeHandle);
		renderer->updateSize(width, height);
		if (renderer->javaCanvas) {
			env->DeleteGlobalRef(renderer->javaCanvas);
			renderer->javaCanvas = nullptr;
		}
	}
}

static jobject nativeLockCanvas(JNIEnv* env, jobject thiz, jlong nativeHandle) {
	if (nativeHandle != 0) {
		SkStupidRenderer* renderer = reinterpret_cast<SkStupidRenderer*>(nativeHandle);
		SkCanvas* skcanvas = renderer->lockCanvas();
		if (renderer->javaCanvas == nullptr) {
			skcanvas->ref();
			void* pCanvasWrapper = create_canvas(skcanvas);
			renderer->javaCanvas = env->NewObject(gSysCanvasClass, gSysCanvasCtorID, reinterpret_cast<jlong>(pCanvasWrapper));
			renderer->javaCanvas = env->NewGlobalRef(renderer->javaCanvas);
		}
		return renderer->javaCanvas;
	}
	return nullptr;
}

static void nativeUnlockCanvasAndPost(JNIEnv* env, jobject thiz, jlong nativeHandle, jobject canvas) {
	if (nativeHandle != 0) {
		reinterpret_cast<SkStupidRenderer*>(nativeHandle)->unlockCanvasAndPost(nullptr);
	}
}

static JNINativeMethod gMethods[] = {
	{"nativeIsSupported", "()Z", (void*)nativeIsSupported},
    {"nativeInit", "(III)J", (void*)nativeInit},
    {"nativeTerm", "(J)V", (void*)nativeTerm},
    {"nativeIsHardwareAccelerated", "(J)Z", (void*)nativeIsHardwareAccelerated},
    {"nativeUpdateSize", "(JII)V", (void*)nativeUpdateSize},
    {"nativeLockCanvas", "(J)Landroid/graphics/Canvas;", (void*)nativeLockCanvas},
    {"nativeUnlockCanvasAndPost", "(JLandroid/graphics/Canvas;)V", (void*)nativeUnlockCanvasAndPost}
};

int registerSkiaRedirectorMethods(JNIEnv* env, const char* className) {
	jclass clazz = env->FindClass(className);
	return env->RegisterNatives(clazz, gMethods, NELEM(gMethods));
}
