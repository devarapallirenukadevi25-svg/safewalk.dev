<h2 style="margin-bottom: 0;" align="center">Women Safety Box</h2>

<p align="center">
<img src="https://drive.google.com/file/d/1l7AVSq0-2Kaa43P-DPAvW8il25ji_V76/view?usp=sharing" height="100" width="100">
<h3 style="margin-top: 0;" align="center">Women safety app using Background Voice Recognition</h3>
<h5 style="margin-top: 0;" align="center">[ University 2nd Year SDP Project-2018 ]</h5>
</p>

# Safe Walk
This is an Android app ,an inclusive personal safety application designed for everyone — including men, women, students, elderly individuals, and specially-abled users such as the deaf, mute, and visually impaired. which Will Call to an Emergency number, based on a specific voice command like "Help" . It will also send SMS with current GPS coordinates to those numbers.

* Make Sure that "Google" App is Installed on your phone and you give this App proper Access Permission like Call, SMS, Microphone.

## Key Features

- 🔐 Works for everyone (inclusive safety solution)
- 🎙️ Keyword-based voice trigger (even in background)
- 📳 Shake + Tap activation for quick alerts
- 🔊 Volume button trigger for discreet usage
- 📍 Real-time location sharing via SMS
- 🚨 Instant emergency calling system

## 🌍 Special Feature: Journey Guardian Mode


## App Screenshots
<table>
  <tr>
     <td align="center">Registration</td>
     <td align="center">Background Voice Recognation, Help Mode, Auto Call & SMS</td>
  </tr>
  <tr>
    <td valign="top"><img src="https://user-images.githubusercontent.com/15268903/44601093-3d313680-a7fd-11e8-9e94-ba3b77d0dfc3.gif"></td>
    <td valign="top" align="center"><img src="https://user-images.githubusercontent.com/15268903/44601241-ab75f900-a7fd-11e8-92a9-28bbce9630ca.gif"></td>
  </tr>
 </table>
 <br>
 <table>
  <tr>
     <td align="center">Nearby Police and Hospital Location</td>
     <td align="center">Fast Aid and Self Defence Info</td>
  </tr>
  <tr>
    <td valign="top" align="center"><img src="https://user-images.githubusercontent.com/15268903/44601093-3d313680-a7fd-11e8-9e94-ba3b77d0dfc3.gif"></td>
    <td valign="top" align="center"><img src="https://user-images.githubusercontent.com/15268903/44601775-65219980-a7ff-11e8-93c2-547ecff322ee.gif"></td>
  </tr>
 </table>
 <br>
 
 ### Built With
 
 * Android Studio 4.0.1 The latest version can be downloaded from [here](https://developer.android.com/studio/)
 * Build gradle 3.1.4
 * Compile Sdk Version 26

 
 ### Directory Structure
 
 The following is a high level overview of relevant files and folders.
 
 ```
Women Safety Box
├───app
│   └───src
│       └───main
│           └───java
│               └───com
│                   └───womensafety
│                       └───shajt3ch
│                               AccelerometerListener.java
│                               AccelerometerManager.java
│                               BackgrndServices.java
│                               Constants.java
│                               DataHolder.java
│                               DataParser.java
│                               Display.java
│                               DownloadUrl.java
│                               GetNearbyPlacesData.java
│                               GPSTracker.java
│                               Holder.java
│                               MainActivity.java
│                               MapsActivity.java
│                               Model.java
│                               MyPagerAdapter.java
│                               MyService.java
│                               PhoneNumber.java
│                               RecyclerAdapter.java
│                               Register.java
│                               RGeocoder.java
│                               Safety.java
│                               Verify.java
│
└───speech
    └───src
        └───main
            └───java
                └───com
                    └───sac
                        └───speech
                            │   DefaultLoggerDelegate.java
                            │   DelayedOperation.java
                            │   GoogleVoiceTypingDisabledException.java
                            │   Logger.java
                            │   Speech.java
                            │   SpeechDelegate.java
                            │   SpeechRecognitionException.java
                            │   SpeechRecognitionNotAvailable.java
                            │   SpeechUtil.java
                            │   TextToSpeechCallback.java
                            │   TtsProgressListener.java
                            │
                            └───ui
                                │   SpeechBar.java
                                │   SpeechProgressView.java
                                │
                                └───animators
                                        BarParamsAnimator.java
                                        BarRmsAnimator.java
                                        IdleAnimator.java
                                        RmsAnimator.java
                                        RotatingAnimator.java
                                        TransformAnimator.java
```

## License
```
MIT License

Copyright (c) 2020 Women Safety Box

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```



