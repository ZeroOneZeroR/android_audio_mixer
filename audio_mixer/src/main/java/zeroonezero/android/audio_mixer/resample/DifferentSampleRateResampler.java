package zeroonezero.android.audio_mixer.resample;

import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * An {@link AudioResampler} that upsamples/downsamples from a lower/higher sample rate to a higher/lower sample rate.
 */
public class DifferentSampleRateResampler implements AudioResampler {

    static {
        try {
            System.loadLibrary("libaudio_mixer");
            Log.i("resampler","Successfully loaded native libaudio_mixer.so");
            test();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void resample(@NonNull ShortBuffer inputBuffer, int inputSampleRate, @NonNull ShortBuffer outputBuffer, int outputSampleRate, int channels) {
        try {
            int remaining=inputBuffer.remaining();
            int oldPosition=outputBuffer.position();//8 hours of Debugging to add this line, before that nothing worked
            int newPosition=resampleCBuffer(channels, inputSampleRate, outputSampleRate, inputBuffer, outputBuffer, remaining) +oldPosition;
            outputBuffer.position(newPosition);//8 hours of Debugging to add this line, before that nothing worked
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    native int resampleCBuffer(int channelCount, int inputSampleRate, int outputSampleRate, ShortBuffer inputBuffer, ShortBuffer outputBuffer, int numInputBufferSamples);

    native static void test();
}
