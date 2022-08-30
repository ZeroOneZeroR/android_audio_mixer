package zeroonezero.android.audio_mixer.resample;

import androidx.annotation.NonNull;

import java.nio.ShortBuffer;

/**
 * Resamples audio data. See {@link DifferentSampleRateResampler} or
 * {@link PassThroughAudioResampler} for concrete implementations.
 */
public interface AudioResampler {

    /**
     * Resamples input audio from input buffer into the output buffer.
     *
     * @param inputBuffer the input buffer
     * @param inputSampleRate the input sample rate
     * @param outputBuffer the output buffer
     * @param outputSampleRate the output sample rate
     * @param channels the number of channels
     */
    void resample(@NonNull final ShortBuffer inputBuffer, int inputSampleRate, @NonNull final ShortBuffer outputBuffer, int outputSampleRate, int channels);

    AudioResampler PASSTHROUGH = new PassThroughAudioResampler();
}
