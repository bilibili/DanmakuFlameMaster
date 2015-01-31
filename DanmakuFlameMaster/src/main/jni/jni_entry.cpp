#include <assert.h>
#include <cstdlib>
#include <jni.h>
#include <android/log.h>
#include <SkGraphics.h>
#include <SkCanvas.h>
#include <SkPaint.h>
#include <SkString.h>
#include <SkTypeface.h>
#include "sk_stupid_renderer.hpp"
#include "skia_redirector_jni.hpp"
#include "jni_entry.hpp"

#ifndef NELEM
	#define NELEM(x) ((int)(sizeof(x) / sizeof((x)[0])))
#endif

static const int jniVersion = JNI_VERSION_1_4;

static jfieldID gCanvas_nativeInstanceID;

static SkCanvas* getSkCanvas(JNIEnv* env, jobject javaCanvas) {
	gCanvas_nativeInstanceID = env->GetFieldID(env->FindClass("android/graphics/Canvas"), "mNativeCanvas", "I");
	SkCanvas* c = (SkCanvas*)env->GetIntField(javaCanvas, gCanvas_nativeInstanceID);
	return c;
}

/*static JNINativeMethod gMethods[] = {
    {"nativePlus", "(II)I", (void*)nativePlus},
    {"nativeTestDraw", "(Landroid/graphics/Canvas;)V", (void*)nativeTestDraw}
};*/

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved) {
	__android_log_print(ANDROID_LOG_DEBUG, "JNITest_Native", "JNI_OnLoad");

	JNIEnv* env = nullptr;
	if (jvm->GetEnv((void**)&env, jniVersion) != JNI_OK) {
		return -1;
	}
	assert(env != nullptr);

	//jclass clazz = env->FindClass("com/example/jnitest/JNITest");
	//env->RegisterNatives(clazz, gMethods, NELEM(gMethods));

	initSkiaRedirectorJni(env);
	registerSkiaRedirectorMethods(env, "master/flame/danmaku/ui/SkiaRedirector/SkStupidRenderer");

	return jniVersion;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* jvm, void* reserved) {
	termSkiaRedirectorJni();
}
