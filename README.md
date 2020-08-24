# android_audio_mixer
A simple android library for processing audio and mixing multiple audios parallelly or sequentially, made with android native media APIs (MediaCodec, MediaFormat, MediaMuxer) and JAVA.
## Installation

Add it in your root build.gradle at the end of repositories:

```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

Add the dependency
```
dependencies {
	        implementation 'com.github.rajib010:android_audio_mixer:v1.1'
	}
```
