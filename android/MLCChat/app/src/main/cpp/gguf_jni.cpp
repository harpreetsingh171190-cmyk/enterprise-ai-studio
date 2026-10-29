#include <jni.h>
#include <atomic>
#include <mutex>
#include <string>
#include <algorithm>
#include <cstring>
#include <vector>
#include "llama.h"

static std::mutex engine_mutex;
static std::atomic<bool> cancelled{false};
static llama_model* model = nullptr;
static llama_context* ctx = nullptr;
static const llama_vocab* vocab = nullptr;

static void release_engine() {
    if (ctx) { llama_free(ctx); ctx = nullptr; }
    if (model) { llama_model_free(model); model = nullptr; }
    vocab = nullptr;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_ai_mlc_mlcchat_enterprise_GgufNative_load(JNIEnv* env, jobject, jstring path, jint contextSize, jint threads) {
    std::lock_guard<std::mutex> lock(engine_mutex);
    release_engine();
    const char* p = env->GetStringUTFChars(path, nullptr);
    llama_backend_init();
    auto mp = llama_model_default_params();
    mp.n_gpu_layers = 0; // conservative CPU baseline; no fake GPU support
    model = llama_model_load_from_file(p, mp);
    env->ReleaseStringUTFChars(path, p);
    if (!model) return JNI_FALSE;
    auto cp = llama_context_default_params();
    cp.n_ctx = (uint32_t)contextSize;
    cp.n_batch = std::min((uint32_t)contextSize, (uint32_t)256);
    cp.n_threads = threads;
    ctx = llama_init_from_model(model, cp);
    if (!ctx) { release_engine(); return JNI_FALSE; }
    vocab = llama_model_get_vocab(model);
    cancelled.store(false);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_ai_mlc_mlcchat_enterprise_GgufNative_cancel(JNIEnv*, jobject) { cancelled.store(true); }

extern "C" JNIEXPORT void JNICALL
Java_ai_mlc_mlcchat_enterprise_GgufNative_unload(JNIEnv*, jobject) {
    cancelled.store(true);
    std::lock_guard<std::mutex> lock(engine_mutex);
    release_engine();
}

extern "C" JNIEXPORT jint JNICALL
Java_ai_mlc_mlcchat_enterprise_GgufNative_generate(JNIEnv* env, jobject, jstring prompt,
    jint maxTokens, jfloat temperature, jfloat topP, jobject callback) {
    std::lock_guard<std::mutex> lock(engine_mutex);
    if (!ctx || !model || !vocab) return -1;
    cancelled.store(false);
    const char* raw = env->GetStringUTFChars(prompt, nullptr);
    std::string text(raw);
    env->ReleaseStringUTFChars(prompt, raw);
    int count = -llama_tokenize(vocab, text.c_str(), (int)text.size(), nullptr, 0, true, true);
    if (count <= 0 || count >= (int)llama_n_ctx(ctx)) return -2;
    std::vector<llama_token> tokens(count);
    if (llama_tokenize(vocab, text.c_str(), (int)text.size(), tokens.data(), count, true, true) < 0) return -3;
    llama_memory_clear(llama_get_memory(ctx), true);
    llama_batch batch = llama_batch_get_one(tokens.data(), (int)tokens.size());
    if (llama_decode(ctx, batch) != 0) return -4;
    auto params = llama_sampler_chain_default_params();
    llama_sampler* sampler = llama_sampler_chain_init(params);
    if (temperature <= 0.f) llama_sampler_chain_add(sampler, llama_sampler_init_greedy());
    else {
        llama_sampler_chain_add(sampler, llama_sampler_init_top_p(topP, 1));
        llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    }
    jclass cls = env->GetObjectClass(callback);
    jmethodID emit = env->GetMethodID(cls, "onBytes", "([B)V");
    if (!emit) { llama_sampler_free(sampler); return -5; }
    int emitted = 0;
    for (; emitted < maxTokens && !cancelled.load() && count + emitted + 1 < (int)llama_n_ctx(ctx); ++emitted) {
        llama_token next = llama_sampler_sample(sampler, ctx, -1);
        if (llama_vocab_is_eog(vocab, next)) break;
        // llama.cpp may return a negative required size for a token piece.
        int len = llama_token_to_piece(vocab, next, nullptr, 0, 0, true);
        if (len < 0) len = -len;
        if (len > 0 && len < 1048576) {
            std::vector<char> piece((size_t)len);
            int written = llama_token_to_piece(vocab, next, piece.data(), len, 0, true);
            if (written > 0) {
                jbyteArray bytes = env->NewByteArray(written);
                if (!bytes) break;
                env->SetByteArrayRegion(bytes, 0, written, reinterpret_cast<const jbyte*>(piece.data()));
                env->CallVoidMethod(callback, emit, bytes);
                env->DeleteLocalRef(bytes);
                if (env->ExceptionCheck()) break;
            }
        }
        llama_batch step = llama_batch_get_one(&next, 1);
        if (llama_decode(ctx, step) != 0) { emitted = -6; break; }
    }
    llama_sampler_free(sampler);
    return emitted;
}

extern "C" JNIEXPORT jstring JNICALL
Java_ai_mlc_mlcchat_enterprise_GgufNative_formatChat(JNIEnv* env, jobject,
    jstring system, jstring user) {
    std::lock_guard<std::mutex> lock(engine_mutex);
    if (!model) return nullptr;
    const char* sys = env->GetStringUTFChars(system, nullptr);
    const char* usr = env->GetStringUTFChars(user, nullptr);
    std::string s(sys), u(usr);
    env->ReleaseStringUTFChars(system, sys);
    env->ReleaseStringUTFChars(user, usr);
    std::vector<llama_chat_message> messages;
    if (!s.empty()) messages.push_back({"system", s.c_str()});
    messages.push_back({"user", u.c_str()});
    const char* tmpl = llama_model_chat_template(model, nullptr);
    int required = llama_chat_apply_template(tmpl, messages.data(),
                                              messages.size(), true, nullptr, 0);
    if (required <= 0 || required > 1024 * 1024) return nullptr;
    std::vector<char> buffer((size_t)required + 1, 0);
    int actual = llama_chat_apply_template(tmpl, messages.data(),
                                            messages.size(), true,
                                            buffer.data(), (int)buffer.size());
    if (actual < 0) return nullptr;
    return env->NewStringUTF(buffer.data());
}
