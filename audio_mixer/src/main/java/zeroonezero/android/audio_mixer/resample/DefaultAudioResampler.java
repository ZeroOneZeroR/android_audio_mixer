package zeroonezero.android.audio_mixer.resample;

import androidx.annotation.NonNull;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * An {@link AudioResampler} that delegates to appropriate classes
 * based on input and output size.
 */
public class DefaultAudioResampler implements AudioResampler {


    private static class AudioResamplerBuffer{
        private final HashMap<String,AudioResampler> resamplers;
        private AudioResamplerBuffer(int maxItems){
            resamplers=new HashMap<>(maxItems);
        }
        private AudioResampler getResampler(int inputSampleRate, int outputSampleRate, int channelCount){
            String hashString=inputSampleRate+":"+outputSampleRate+":"+channelCount;
            AudioResampler r=resamplers.get(hashString);
            if(r == null){
                r=new DifferentSampleRateResampler(inputSampleRate,outputSampleRate,channelCount);
                resamplers.put(hashString,r);
            }
            return r;
        }
    }

    AudioResamplerBuffer buffer= new AudioResamplerBuffer(1);

    @Override
    public void resample(@NonNull ShortBuffer inputBuffer, int inputSampleRate, @NonNull ShortBuffer outputBuffer, int outputSampleRate, int channels) {
        if (inputSampleRate != outputSampleRate) {
            buffer.getResampler(inputSampleRate,outputSampleRate,channels).resample(inputBuffer, inputSampleRate, outputBuffer, outputSampleRate, channels);
        }else {
            PASSTHROUGH.resample(inputBuffer, inputSampleRate, outputBuffer, outputSampleRate, channels);
        }
    }
}
