/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#ifndef JAVA_V8_INSPECTOR_H
#define JAVA_V8_INSPECTOR_H

#include <jni.h>

#include <memory>

#include "JSEnv.h"

namespace inspector {

    class HybridInspector;

    class InspectorClientImpl final : public hybrid::JSInspectorClient {
    public:
        explicit InspectorClientImpl(HybridInspector *inspector, hybrid::JSEnv *env,
                                     int is_jscontext_recreated)
                : inspector_(inspector) {
            session_.reset(env->CreateInspectorSession(this, 1, "hybrid-inspector",
                                                       is_jscontext_recreated));
        }

        virtual ~InspectorClientImpl() = default;

        // channel
        void SendResponse(int call_id, const hybrid::jschar_t *message,
                          size_t size) override;

        void SendNotification(const hybrid::jschar_t *message, size_t size) override {
            SendResponse(0, message, size);
        }

        // Client
        void RunMessageLoopOnPause(int context_group_id) override;

        void QuitMessageLoopOnPause() override;

        void RunIfWaitingForDebugger(int contextGroupId) override {}

        void MuteMetrics(int contextGroupId) override {}

        void UnmuteMetrics(int contextGroupId) override {}

        void BeginUserGesture() override {}

        void EndUserGesture() override {}

        void BeginEnsureAllContextsInGroup(int contextGroupId) override {}

        void EndEnsureAllContextsInGroup(int contextGroupId) override {}

        hybrid::JSInspectorSession *GetSession() { return session_.get(); }

    private:
        HybridInspector *inspector_;
        std::unique_ptr <hybrid::JSInspectorSession> session_;
    };

    class HybridInspector {
    public:
        HybridInspector(int sessionId = 0);

        ~HybridInspector();

        static int Initialize(JNIEnv *env);

        void sendMessageToFrontend(int callId, const hybrid::jschar_t *message,
                                   size_t size);

        void runMessageLoopOnPause(int contextGroupId);

        void quitMessageLoopOnPause();

    private:
        static jclass _class;
        static jmethodID _sendResponse;
        static jmethodID _sendNotification;
        static jfieldID _v8NativePtr;
        static jfieldID _v8ValueHandlePtr;
        static jmethodID _runMessageLoopOnPause;
        static jmethodID _quitMessageLoopOnPause;
        static JNINativeMethod _methods[];

        static void nativeHandleMessage(JNIEnv *env, jobject, jlong ptr,
                                        jint sessionId, jstring message);

        static jlong initNative(JNIEnv *env, jobject, jboolean, jint);

        static void nativeSetV8Context(JNIEnv *env, jobject, jlong ptr, jobject v8,
                                       jint is_jscontext_recreated);

        static void nativeDisposeV8Context(JNIEnv *env, jobject, jlong ptr);

        static void nativeDestroy(JNIEnv *env, jobject, jlong ptr);

        static void nativeBeginLoadJsCode(JNIEnv *env, jobject, jstring uri,
                                          jstring content);

        static void nativeEndLoadJsCode(JNIEnv *env, jobject, jstring uri);

        static jstring nativeExecuteJsCode(JNIEnv *env, jobject, jlong ptr,
                                           jstring jsCode);

        static jstring nativeHandleConsoleMessage(JNIEnv *env, jobject, jlong ptr,
                                                  jobject v8Array);

        static void nativeFrontendReload(JNIEnv *env, jobject, jlong ptr);

        static HybridInspector *From(long ptr) {
            return reinterpret_cast<HybridInspector *>(ptr);
        }

        void onFrontendReload();

        void handleMessage(int sessionId, const hybrid::jschar_t *message,
                           size_t size);

        jstring executeJsCode(JNIEnv *env, jstring jscode);

        void initialzeV8Context(int is_jscontext_recreated);

        void clearV8Context();

        void showException();

        jobject m_jthis;
        hybrid::J2V8Handle m_v8Runtime;
        int m_sessionId;
        std::unique_ptr<hybrid::JSEnv, void (*)(hybrid::JSEnv *)> js_env_;
        std::unique_ptr <InspectorClientImpl> inspector_client_;
    };

}  // namespace inspector

#endif
