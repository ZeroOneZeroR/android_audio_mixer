# AudioMixer-android
A simple android library for processing audio and mixing multiple audios parallelly or sequentially,
made with android native media APIs (MediaCodec, MediaFormat, MediaMuxer) and JAVA.


## Features
- Mixing multple audios parallely or sequentially or processing single audio
- Trimming audio
- Changing sample-rate, bit-rate, channel(mono to stereo and vice versa) of audio
- Controlling volume of audio
- Making an audio file without any real audio data
- Making audio file by providing audio data
- Merging audios with video
- Fast transcoding to AAC
- Hardware accelerated


## Requirements
- Minimum API level 18


## Installation
Step 1. Add the JitPack repository to your build file
````
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
````
Step 2. Add the dependency
````
dependencies {
	        implementation 'com.github.rajib010:android_audio_mixer:v1.0'
	}
````



## Sample Usage
````
            AudioInput input1 = new GeneralAudioInput(input1Path);
            input1.setStartTimeUs(startTimeUs); //Optional
            input1.setEndTimeUs(endTimeUs); //Optional
            input1.setVolume(0.5f); //Optional

            // It will produce a blank portion of 3 seconds between input1 and input2
            AudioInput blankInput = new BlankAudioInput(3000000);

            AudioInput input2 = new GeneralAudioInput(context, input2Uri, null);



            String outputPath = Environment.getDownloadCacheDirectory().getAbsolutePath()
            +"/" +"audio_mixer_output.mp3"; // for example
            final AudioMixer audioMixer = new AudioMixer(outputPath);

            audioMixer.addDataSource(input1);
            audioMixer.addDataSource(blankInput);
            audioMixer.addDataSource(input2);

            audioMixer.setSampleRate(44100); // Optional
            audioMixer.setBitRate(128000); // Optional
            audioMixer.setChannelCount(2); // Optional //1(mono) or 2(stereo)
	    
	    // Smaller audio inputs will be encoded from start-time again if it reaches end-time
            // It is only valid for parallel mixing
            //audioMixer.setLoopingEnabled(true);

            audioMixer.setMixingType(AudioMixer.MixingType.SEQUENTIAL); // or AudioMixer.MixingType.PARALLEL
            audioMixer.setProcessingListener(new AudioMixer.ProcessingListener() {
                @Override
                public void onProgress(final double progress) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.setProgress((int) (progress * 100));
                        }
                    });
                }

                @Override
                public void onEnd() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Success!!!", Toast.LENGTH_SHORT).show();
                            audioMixer.release();
                        }
                    });
                }
            });
            
            
            //it is for setting up the all the things
            audioMixer.start();
            
            /* These getter methods must be called after calling 'start()'*/
            //audioMixer.getOutputSampleRate();
            //audioMixer.getOutputBitRate();
            //audioMixer.getOutputChannelCount();
            //audioMixer.getOutputDurationUs();
            
            //starting real processing
            audioMixer.processAsync();
            
            // We can stop the processing immediately by calling audioMixer.stop() when we want.
            
            // audioMixer.processSync() is generally not used.
            // We have to use this carefully.
            // Tt will do the processing in caller thread
            // And calling audioMixer.stop() from the same thread won't stop the processing
````



### Merging audio with video and using external MediaMuxer
Also external MediaMuxer can be used for muxing through ````new AudioMixer(externalMediaMuxer)````.
As for example, if we want to add audio with video,
we have to pass the media-muxer which is currently being used for video muxing.
MediaMuxer's starting, stopping, releasing must be handled externally.
In this case, we must call ````AudioMixer.start()```` before ````MediaMuxer.start()```` and
````AudioMixer.processAsync()```` after ````MediaMuxer.start()````.

## Custom AudioInput
You can implement AudioInput interface and make your own audio processing system.
