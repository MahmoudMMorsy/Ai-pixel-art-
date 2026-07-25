cmake_minimum_required(VERSION 3.22.1)

project("stable_diffusion_jni")

# Absolute path to the stable-diffusion.cpp root folder in the sandbox
set(SD_CPP_DIR "/app/stable-diffusion.cpp")

# Define job pool to limit parallel compiles to 1 to prevent Out-Of-Memory (OOM) failures in restricted environments
set_property(GLOBAL PROPERTY JOB_POOLS compile_job_pool=1)
set(CMAKE_JOB_POOL_COMPILE compile_job_pool)

# Add GGML_MAX_NAME compile definition
add_compile_definitions(GGML_MAX_NAME=128)

# Standard includes for stable-diffusion.cpp and ggml
include_directories(
    "${SD_CPP_DIR}"
    "${SD_CPP_DIR}/src"
    "${SD_CPP_DIR}/include"
    "${SD_CPP_DIR}/ggml/include"
    "${SD_CPP_DIR}/ggml/src"
    "${SD_CPP_DIR}/thirdparty"
    "${SD_CPP_DIR}/thirdparty/libwebp/src"
)

# Add source files from stable-diffusion.cpp and ggml
set(SD_SOURCES
    stable_diffusion_jni.cpp
    "${SD_CPP_DIR}/src/stable-diffusion.cpp"
    "${SD_CPP_DIR}/src/upscaler.cpp"
    "${SD_CPP_DIR}/src/tokenizers/tokenizer.cpp"
    "${SD_CPP_DIR}/src/tokenizers/tokenize_util.cpp"
    "${SD_CPP_DIR}/src/tokenizers/vocab/vocab.cpp"
    "${SD_CPP_DIR}/src/tokenizers/mistral_tokenizer.cpp"
    "${SD_CPP_DIR}/src/tokenizers/qwen2_tokenizer.cpp"
    "${SD_CPP_DIR}/src/tokenizers/t5_unigram_tokenizer.cpp"
    "${SD_CPP_DIR}/src/version.cpp"

    # GGML source files for tensor arithmetic
    "${SD_CPP_DIR}/ggml/src/ggml.c"
    "${SD_CPP_DIR}/ggml/src/ggml-alloc.c"
    "${SD_CPP_DIR}/ggml/src/ggml-backend.cpp"
    "${SD_CPP_DIR}/ggml/src/ggml-quants.c"
)

# Compile as shared native JNI library
add_library(
    stable_diffusion_jni
    SHARED
    ${SD_SOURCES}
)

# Link standard Android logging libraries
find_library(log-lib log)

target_link_libraries(
    stable_diffusion_jni
    ${log-lib}
)
