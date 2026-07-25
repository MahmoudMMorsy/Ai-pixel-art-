#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "stable-diffusion.h"

#define TAG "StableDiffusionJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" JNIEXPORT jlong JNICALL
Java_com_retro_pixelanimator_engine_LocalHDImageEngine_initModel(
    JNIEnv* env, jobject thiz, jstring model_path) {

    if (model_path == nullptr) {
        LOGE("Model path is null!");
        return 0;
    }

    const char* path = env->GetStringUTFChars(model_path, nullptr);
    LOGI("Initializing GGUF Model from path: %s", path);

    // Initialize standard context parameters
    sd_ctx_params_t params;
    sd_ctx_params_init(&params);

    // Configure GGUF model path
    params.model_path = path;
    params.rng_type = STD_DEFAULT_RNG;
    params.n_threads = 4;
    params.wtype = SD_TYPE_Q4_0;

    // Create new Stable Diffusion context
    sd_ctx_t* sd_ctx = new_sd_ctx(&params);

    env->ReleaseStringUTFChars(model_path, path);

    if (sd_ctx == nullptr) {
        LOGE("Failed to create Stable Diffusion context from GGUF file!");
        return 0;
    }

    LOGI("Successfully created Stable Diffusion context: %p", sd_ctx);
    return reinterpret_cast<jlong>(sd_ctx);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_retro_pixelanimator_engine_LocalHDImageEngine_generateImageFromC(
    JNIEnv* env, jobject thiz, jlong ctx_ptr, jstring prompt, jint steps, jint width, jint height) {

    if (ctx_ptr == 0) {
        LOGE("Invalid model context pointer!");
        return nullptr;
    }

    sd_ctx_t* sd_ctx = reinterpret_cast<sd_ctx_t*>(ctx_ptr);
    const char* c_prompt = env->GetStringUTFChars(prompt, nullptr);
    LOGI("Generating image with prompt: '%s' | Steps: %d | Size: %dx%d", c_prompt, steps, width, height);

    // Initialize image generation parameters using the new stable-diffusion.cpp API
    sd_img_gen_params_t params;
    sd_img_gen_params_init(&params);

    params.prompt = c_prompt;
    params.width = width;
    params.height = height;
    params.seed = 42;
    params.batch_count = 1;

    params.sample_params.sample_steps = steps;
    params.sample_params.sample_method = EULER_A_SAMPLE_METHOD;
    params.sample_params.scheduler = sd_get_default_scheduler(sd_ctx, EULER_A_SAMPLE_METHOD);

    sd_image_t* results = nullptr;
    int num_images = 0;

    // Call the native C++ Stable Diffusion generator
    bool success = generate_image(sd_ctx, &params, &results, &num_images);

    env->ReleaseStringUTFChars(prompt, c_prompt);

    if (!success || results == nullptr || num_images == 0 || results[0].data == nullptr) {
        LOGE("Generation failed or returned null image data!");
        if (results != nullptr) {
            free_sd_images(results, num_images);
        }
        return nullptr;
    }

    // Convert raw RGB byte buffer to Kotlin-compatible JNI byte array
    int pixel_count = width * height;
    int size = pixel_count * 4; // ARGB_8888 byte size
    jbyteArray arr = env->NewByteArray(size);

    // Convert 24-bit RGB to 32-bit ARGB_8888 for direct Bitmap loading in Android
    std::vector<uint32_t> argb_buffer(pixel_count);
    uint8_t* rgb_data = results[0].data;
    for (int i = 0; i < pixel_count; i++) {
        uint8_t r = rgb_data[i * 3 + 0];
        uint8_t g = rgb_data[i * 3 + 1];
        uint8_t b = rgb_data[i * 3 + 2];
        argb_buffer[i] = (0xFF000000) | (r << 16) | (g << 8) | b;
    }

    env->SetByteArrayRegion(arr, 0, size, reinterpret_cast<const jbyte*>(argb_buffer.data()));

    // Free native allocations using the official API
    free_sd_images(results, num_images);

    LOGI("Successfully converted and returned generated native bitmap buffer!");
    return arr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_retro_pixelanimator_engine_LocalHDImageEngine_freeModelContext(
    JNIEnv* env, jobject thiz, jlong ctx_ptr) {
    if (ctx_ptr != 0) {
        sd_ctx_t* sd_ctx = reinterpret_cast<sd_ctx_t*>(ctx_ptr);
        LOGI("Freeing model context: %p", sd_ctx);
        free_sd_ctx(sd_ctx);
    }
}
