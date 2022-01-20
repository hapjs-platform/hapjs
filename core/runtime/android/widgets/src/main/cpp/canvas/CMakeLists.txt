cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_BUILD_TYPE "Release")
set(CMAKE_C_VISIBILITY_PRESET hidden)
set(CMAKE_CXX_VISIBILITY_PRESET hidden)

add_library(canvas SHARED
        utils.cpp
        canvas_jni.cpp
        exceptions.cpp
        Log.cpp
        )

include_directories("./")

target_link_libraries(canvas android log jnigraphics)