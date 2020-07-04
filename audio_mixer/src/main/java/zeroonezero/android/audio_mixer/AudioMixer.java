package zeroonezero.android.audio_mixer;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import zeroonezero.android.audio_mixer.input.AudioInput;

public class AudioMixer {
    private static final String TAG = AudioMixer.class.getSimpleName();

    public enum MixingType{PARALLEL, SEQUENTIAL}

    private final int DEFAULT_SAMPLE_RATE = 44100;
    private final int DEFAULT_BIT_RATE = 128000;
    private final int DEFAULT_CHANNEL_COUNT = 2;

    private static final int TIMEOUT_USEC = 0000;
    private static final int BYTES_PER_SHORT = 2;

    private MediaCodec encoder;
    private MediaMuxer muxer;
    private final boolean isMuxerExternal;
    private int muxerTrackIndex = -1;

    private List<AudioInput> audioInputList = new ArrayList<>();

    /*
    * We will determine these in 'start()' method
    * according to the inputs if these are not explicitly set with valid values.
    */
    private int sampleRate = -1;
    private int bitRate = -1;
    private int channelCount = -1;

    private MixingType mixingType = MixingType.PARALLEL;

    /*
    * Looping means if an audio input reaches its end-time,
    * it will again go to its start-time.
    * Looping is avoided in sequential mixing.
    */
    private boolean loopingEnabled;

    /*
     * This holds the final output duration of our mixed audio.
     * Final output duration is calculated in 'start()' method.
     */
    private long outputDurationUs;

    /*
     * These variables are required for processing.
     * Their roles are explained in 'start()' method
     * */
    private AudioInput baseInputForParallelType;
    private int currentInputIndexForSequentialType;

    /*
    * These indicates different states.
    */
    private boolean started;
    private boolean processing;
    private boolean mixingDone;

    /*
    * Indicates progress of mixing. Values ranges from 0.0 to 1.0
    * */
    private double progress;

    private ProcessingListener processingListener;

    /*
    * We will do the asynchronous mixing in this worker thread
    */
    private Thread processingThread;

    public AudioMixer(String outputFilePath) throws IOException{
        this(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }

    /*
    * private for now
    */
    private AudioMixer(String outputFilePath, int mediaMuxerOutputFormat) throws IOException{
        muxer = new MediaMuxer(outputFilePath, mediaMuxerOutputFormat);
        isMuxerExternal = false;
    }

    @TargetApi(26)
    public AudioMixer(FileDescriptor fd) throws IOException{
        this(fd, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }

    /*
     * private for now
     */
    @TargetApi(26)
    private AudioMixer(FileDescriptor fd, int mediaMuxerOutputFormat) throws IOException{
        muxer = new MediaMuxer(fd, mediaMuxerOutputFormat);
        isMuxerExternal = false;
    }


    /*
    * Also external muxer can be used for muxing.
    * As for example, if we want to add audio with video,
    * we have to pass the muxer which is being used for video muxing.
    * Muxer starting, stopping, releasing must be handled externally.
    * In this case we must start processing after the muxer has started.
    * */
    public AudioMixer(MediaMuxer muxer){
        this.muxer = muxer;
        isMuxerExternal = true;
    }

    private MediaFormat createOutputFormat(int sampleRate, int bitRate, int channelCount){
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,1024 * 256); // Needs to be large enough to avoid BufferOverflowException
        return format;
    }


    public void start() throws IOException {
        if(started || processing || mixingDone) throw new IllegalStateException("Wrong state. AudioMixer can't start.");
        if(audioInputList.size() < 1) throw new UnsupportedOperationException("There should be at least one audio input.");

        if(mixingType == MixingType.PARALLEL){

            // Here we find the AudioInput which holds the maximum duration.
            // Maximum duration holder input will be considered as the base for other operations
            // and it's duration is our output duration
            outputDurationUs = Long.MIN_VALUE;
            for(AudioInput input: audioInputList){
                if(input.getDurationUs() > outputDurationUs) {
                    outputDurationUs = input.getDurationUs();
                    baseInputForParallelType = input;
                }
            }

            // If looping is enabled, we have to enable looping of each input except the base input.
            // Because we only loop the inputs which have durations less than outputDuration.
            if(loopingEnabled){
                for(AudioInput input: audioInputList){
                    input.setLoopingEnabled(true);
                }
            }
            baseInputForParallelType.setLoopingEnabled(false);

        }else if (mixingType == MixingType.SEQUENTIAL){

            // We start from the first input, encode it, move to next by incrementing 'currentInputIndexForSequentialType'
            // and continue this process till the last input.
            currentInputIndexForSequentialType = 0;

            /*
            * Summation of all input's duration is the final output duration
            */
            outputDurationUs = 0;
            for(AudioInput input: audioInputList){
                outputDurationUs += input.getDurationUs();
            }

            /* We avoid looping in sequential mixing */
        }

        /*
        * If sample-rate, bitrate and channel count are not explicitly set,
        * we will choose the largest ones
        */
        if(sampleRate < 1){
            for(AudioInput input: audioInputList){
                if(input.getSampleRate() > sampleRate) sampleRate = input.getSampleRate();
            }
        }

        if(bitRate < 1){
            for(AudioInput input: audioInputList){
                if(input.getBitrate() > bitRate) bitRate = input.getBitrate();
            }
        }

        if(channelCount < 1){
            for(AudioInput input: audioInputList){
                if(input.getChannelCount() > channelCount) channelCount = input.getChannelCount();
            }
        }

        if(sampleRate < 1) sampleRate = DEFAULT_SAMPLE_RATE;
        if(bitRate < 1) bitRate = DEFAULT_BIT_RATE;
        if(channelCount < 1) channelCount = DEFAULT_CHANNEL_COUNT;

        for(AudioInput input: audioInputList){
            input.start(sampleRate, channelCount);
        }

        MediaFormat outputFormat = createOutputFormat(sampleRate, bitRate, channelCount);
        encoder = MediaCodec.createEncoderByType(outputFormat.getString(MediaFormat.KEY_MIME));
        encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();

        // Here we have to add the audio track to the muxer.
        // Because we must add all tracks to the muxer before starting it
        // and as the starting of muxer will be handled externally we have to make sure that
        // the audio track is added before starting of the muxer.
        //
        // To add the track we start encoding here. Actually it will not encode anything here.
        // It will just add the audio track and find the track index;
        synchronized (muxer){
            encode(true);
            // if muxer is not external, we can start it
            if(!isMuxerExternal) muxer.start();
        }

        started = true;
    }

    public void processAsync(){
        checkProcessState();
        processing = true;
        processingThread = new Thread(){
            public void run() {
                encode(false);
                processing = false;
                if(processingListener != null){
                    processingListener.onEnd();
                }
            }
        };
        processingThread.start();
    }

    public void processSync(){
        checkProcessState();
        processing = true;
        encode(false);
        processing = false;
        if(processingListener != null){
            processingListener.onEnd();
        }
    }

    private void checkProcessState(){
        if(!started) throw new IllegalStateException("AudioMixer has not stared.");
        if(processing || mixingDone) throw new IllegalStateException("Wrong state.");
    }


    private long encoderInputPresentationTimeUs = 0;
    private boolean encoderInputDone = false;

    private void encode(boolean addTrackOnly){
        while (!mixingDone) {

            // muxerTrackIndex > -1 means track has been added.
            // So if it has been called only to add track we must stop here.
            if(addTrackOnly && muxerTrackIndex > -1) break;

            if(!encoderInputDone){
                int encoderBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                if(encoderBufferIndex >= 0){
                    if(isInputAvailable()){

                        ShortBuffer encoderBuffer;
                        if (Build.VERSION.SDK_INT >= 21) {
                            encoderBuffer = encoder.getInputBuffer(encoderBufferIndex).asShortBuffer();
                        }else{
                            encoderBuffer = encoder.getInputBuffers()[encoderBufferIndex].asShortBuffer();
                        }
                        // mix the audios and add to encoder input buffer
                        mix(encoderBuffer);

                        encoder.queueInputBuffer(encoderBufferIndex,
                                0,
                                encoderBuffer.position() * BYTES_PER_SHORT,
                                encoderInputPresentationTimeUs,
                                MediaCodec.BUFFER_FLAG_KEY_FRAME);
                        encoderInputPresentationTimeUs += AudioConversions.shortsToUs(encoderBuffer.position(), sampleRate, channelCount);

                    }else{

                        encoder.queueInputBuffer(encoderBufferIndex,0,0,0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        encoderInputDone = true;

                    }
                }
            }

            muxEncoderOutput();
        }

        if(!addTrackOnly){ // done encoding
            stopAndReleaseResources();

            progress = 1.0;
            if(processingListener != null){
                processingListener.onProgress(progress);
            }
        }
    }

    private boolean isInputAvailable(){
        if(mixingType == MixingType.PARALLEL){
            return baseInputForParallelType.hasRemaining();
        }else {
            return currentInputIndexForSequentialType < audioInputList.size();
        }
    }

    private void mix(ShortBuffer inputBuffer){
        final int size = inputBuffer.remaining();

        if(mixingType == MixingType.PARALLEL){

            for(int i = 0; i < size && !mixingDone; i++){
                // If all inputs are done we break the loop
                if(!isInputAvailable()) break;

                // Here we add all input's value after dividing by the number of inputs
                // and put the result into inputBuffer as single value.
                // It is actual parallel mixing
                boolean put = false;
                short result = 0;
                for(int j = 0; j < audioInputList.size(); j++){
                    // If all inputs are done we break the loop
                    if(!isInputAvailable()) break;

                    AudioInput input = audioInputList.get(j);
                    if(input.hasRemaining()) {
                        short value = input.getNext();
                        //controlling volume
                        value = (short)(value * input.getVolume());
                        result += value / audioInputList.size();
                        put = true;
                    }
                }
                if(put) inputBuffer.put(result);
            }

        }else{ // Sequential

            for(int i = 0; i < size && !mixingDone; i++){
                // If all inputs are done we break the loop
                if(!isInputAvailable()) break;

                // There must be at least one remaining value in any input, so we get that
                AudioInput input = audioInputList.get(currentInputIndexForSequentialType);
                short value = input.getNext();
                //controlling volume
                value = (short)(value * input.getVolume());
                inputBuffer.put(value);

                // If current input is done encoding we move to next
                if(!input.hasRemaining()){
                    currentInputIndexForSequentialType++;
                }
            }

        }
    }

    /*
     * These variables are needed at writing data to muxer and progress calculation
     */
    private long lastMuxingPresentationTimeUs;
    private long lastMuxingAudioTimeUs;

    private void muxEncoderOutput(){
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outBufferId = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);

        if (outBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
            // no output available yet
        } else if (outBufferId == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

        } else if (outBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

            muxerTrackIndex = muxer.addTrack(encoder.getOutputFormat());

        } else if (outBufferId < 0) {

            throw new RuntimeException("Unexpected result from decoder.dequeueOutputBuffer: " + outBufferId);

        } else if (outBufferId >= 0) {
            // Are we finished here?
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                mixingDone = true;
            }

            /*if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                bufferInfo.size = 0;
            }*/

            if(bufferInfo.size > 0){
                ByteBuffer encodedBuffer;
                if (Build.VERSION.SDK_INT >= 21) {
                    encodedBuffer = encoder.getOutputBuffer(outBufferId);
                }else{
                    encodedBuffer = encoder.getOutputBuffers()[outBufferId];
                }

                /*
                 * Current muxing-presentation-time can't be less than previous one for muxer.
                 * But we may get bufferInfo.presentation = 0 for the last buffer.
                 * That is why we use our last calculated audio-time as current presentation time in this case.
                 */
                if(bufferInfo.presentationTimeUs < lastMuxingPresentationTimeUs){
                    bufferInfo.presentationTimeUs = lastMuxingAudioTimeUs;
                }

                synchronized (muxer){
                    muxer.writeSampleData(muxerTrackIndex, encodedBuffer, bufferInfo);
                    lastMuxingPresentationTimeUs = bufferInfo.presentationTimeUs;

                    //AudioConversions.bytesToUs(bufferInfo.size, sampleRate, channelCount);
                    long approxPresentationTimeDiff = (1024 * 1000000) / sampleRate; // I don't know why this is;
                    lastMuxingAudioTimeUs = lastMuxingPresentationTimeUs + approxPresentationTimeDiff;
                }
            }
            encoder.releaseOutputBuffer(outBufferId, false);

            progress = lastMuxingAudioTimeUs / (double)outputDurationUs;
            if(progress > 1.0) progress = 1.0;

            if(processingListener != null){
                processingListener.onProgress(progress);
            }
        }
    }

    private synchronized void stopAndReleaseResources(){
        for(AudioInput input: audioInputList) input.release();
        audioInputList.clear();

        if(encoder != null){
            encoder.stop();
            encoder.release();
            encoder = null;
        }

        if(muxer != null){
            if(!isMuxerExternal){
                // Muxer may not has been ready to stop properly due to early stopping.
                // So we should handle exception here
                try{muxer.stop();} catch (Exception e){}
                muxer.release();
            }
            muxer = null;
        }
    }

    public void stop(){
        mixingDone = true;
        if(processingThread != null){
            try { processingThread.join(); }catch (InterruptedException e){ }
            processingThread = null;
        }
    }

    public void release(){
        stop();
        stopAndReleaseResources();
    }

    /************************ Getters and Setters ***********************/

    public void addDataSource(AudioInput audioInput) throws IOException {
        audioInputList.add(audioInput);
    }

    public MixingType getMixingType() {
        return mixingType;
    }

    public boolean isLoopingEnabled() {
        return loopingEnabled;
    }

    public boolean isProcessing() {
        return processing;
    }

    public double getProgress() {
        return progress;
    }

    public int getOutputSampleRate() {
        return sampleRate;
    }

    public int getOutputBitRate() {
        return bitRate;
    }

    public int getOutputChannelCount() {
        return channelCount;
    }

    public long getOutputDurationUs() {
        return outputDurationUs;
    }

    public void setMixingType(MixingType mixingType) {
        this.mixingType = mixingType;
    }

    public void setLoopingEnabled(boolean loopingEnabled) {
        this.loopingEnabled = loopingEnabled;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    public void setProcessingListener(ProcessingListener processingListener) {
        this.processingListener = processingListener;
    }



    /************************ Listeners ***********************/

    public interface ProcessingListener{
        void onProgress(double progress);
        void onEnd();
    }
}
