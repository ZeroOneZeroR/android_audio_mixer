#include "oboe/src/flowgraph/resampler/MultiChannelResampler.h"
#include <jni.h>
#include <algorithm>
#include <iostream>
#include <cmath>
#include <android/log.h>
#define  LOG_TAG    "resampler"
#define  ALOG(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)


long long createResampler(int channelCount,int inputSampleRate,int outputSampleRate){
    oboe::resampler::MultiChannelResampler *resampler = oboe::resampler::MultiChannelResampler::make(
            channelCount, // channel count
            inputSampleRate, // input sampleRate
            outputSampleRate, // output sampleRate
            oboe::resampler::MultiChannelResampler::Quality::Best);
    return (long long) resampler;
}

void deleteResampler(long long resamplerPointer){
    oboe::resampler::MultiChannelResampler *resampler= (oboe::resampler::MultiChannelResampler*) resamplerPointer;
    delete resampler;
}

int resample(long long resamplerPointer,int channelCount, float* inputBuffer,float* outputBuffer,int numInputFrames){
    //ALOG("Resample c++ Called!!!!!\n");
    oboe::resampler::MultiChannelResampler *resampler = (oboe::resampler::MultiChannelResampler*) resamplerPointer;

    //ALOG("Created Resampler with following Settings: Input SampleRate:%d, Output SampleRate: %d, Channels: %d, Num of Input Samples: %d",inputSampleRate,outputSampleRate,channelCount,numInputFrames);
    int numOutputFrames = 0;
    int inputFramesLeft = numInputFrames/channelCount;
    while (inputFramesLeft > -1) {
        //ALOG("%d Frames Left",inputFramesLeft);d
        if(resampler->isWriteNeeded()) {
            if(inputFramesLeft>0) {
                resampler->writeNextFrame(inputBuffer);
                inputBuffer += channelCount;
            }
            inputFramesLeft--;
        } else {
            resampler->readNextFrame(outputBuffer);
            outputBuffer += channelCount;
            numOutputFrames++;
        }
    }
    //ALOG("num Output frames:%d",numOutputFrames);
    return numOutputFrames * channelCount;
}

extern "C"
JNIEXPORT jint JNICALL
Java_zeroonezero_android_audio_1mixer_resample_DifferentSampleRateResampler_resampleCBuffer(JNIEnv *env,
                                                                                            jobject thiz,
                                                                                            jlong resamplerPointer,
                                                                                            jint channel_count,
                                                                                            jobject input_buffer,
                                                                                            jobject output_buffer,
                                                                                            jint num_input_frames) {
    //ALOG("Starting Conversion");

    //ALOG("Input Capicity send%d, real %lld",num_input_frames,env->GetDirectBufferCapacity(input_buffer));
    //jfloat *inputBuffer= (jfloat *)(env->GetDirectBufferAddress(input_buffer));
    jshort *inputShort=(jshort *)(env->GetDirectBufferAddress(input_buffer));
    float inputBuffer[num_input_frames];
    std::transform(inputShort, inputShort + num_input_frames, inputBuffer, [](int f){
        return f;
    });
    //jfloat* outputBuffer= (jfloat *)(env->GetDirectBufferAddress(output_buffer));
    jshort *outputShort= (jshort *)(env->GetDirectBufferAddress(output_buffer));
    float outputBuffer[env->GetDirectBufferCapacity(output_buffer)];
    //ALOG("Finished Conversion, starting Resample");
    int outputLength=resample(resamplerPointer,channel_count, inputBuffer, outputBuffer, num_input_frames);
    //ALOG("Finished Resample, starting Conversion");
    std::transform(outputBuffer, outputBuffer + outputLength, outputShort, [](float f){
        short x= std::round(f);
        //ALOG("Float %f to Short %d",f,x);
        return x;
    });
    return outputLength;
}
/*extern "C"
JNIEXPORT void JNICALL
Java_zeroonezero_android_audio_1mixer_resample_DifferentSampleRateResampler_test(JNIEnv *env,
                                                                                 jclass thiz) {
    //ALOG("Successfully initialised audio_mixer.cpp");
}
*/
extern "C"
JNIEXPORT jlong JNICALL
Java_zeroonezero_android_audio_1mixer_resample_DifferentSampleRateResampler_createResampler(
        JNIEnv *env, jobject thiz, jint channel_count, jint input_sample_rate,
        jint output_sample_rate) {
    return createResampler(channel_count,input_sample_rate,output_sample_rate);
}


extern "C"
JNIEXPORT void JNICALL
Java_zeroonezero_android_audio_1mixer_resample_DifferentSampleRateResampler_deleteResampler(
        JNIEnv *env, jobject thiz, jlong resampler_pointer) {
    deleteResampler(resampler_pointer);
}