
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

# Declares and names the project.

project("libaudio_mixer")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17")

set (OBOE_DIR ./oboe)
set (OBOE_SOURCES
        ${OBOE_DIR}/src/flowgraph/resampler/IntegerRatio.cpp
        ${OBOE_DIR}/src/flowgraph/resampler/LinearResampler.cpp
        ${OBOE_DIR}/src/flowgraph/resampler/MultiChannelResampler.cpp
        ${OBOE_DIR}/src/flowgraph/resampler/PolyphaseResampler.cpp
        ${OBOE_DIR}/src/flowgraph/resampler/PolyphaseResamplerMono.cpp
        ${OBOE_DIR}/src/flowgraph/resampler/PolyphaseResamplerStereo.cpp
        ${OBOE_DIR}/src/flowgraph/resampler/SincResampler.cpp
        ${OBOE_DIR}/src/flowgraph/resampler/SincResamplerStereo.cpp)



# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             libaudio_mixer

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             audio_mixer.cpp
             ${OBOE_SOURCES}
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )
# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

# Add the Oboe library as a subdirectory in your project.
# add_subdirectory tells CMake to look in this directory to
# compile oboe source files using oboe's CMake file.
# ./oboe specifies where the compiled binaries will be stored
#add_subdirectory (${OBOE_DIR} ./oboe)

# Specify the path to the Oboe header files.
# This allows targets compiled with this CMake (application code)
# to see public Oboe headers, in order to access its API.
#include_directories (${OBOE_DIR}/include)


target_link_libraries( # Specifies the target library.
                        libaudio_mixer

                        # Links the target library to the log library
                        # included in the NDK.
                        ${log-lib}
                        #oboe
        )
