# FocusRoom: Muse EEG → HoloLens pipeline

This repository contains:

* `android-app/`: an Android/Kotlin app that connects to a Muse headset, computes an attention level (3 or 5 buckets), and streams the result over UDP to a HoloLens on the same network.
* `unity/AttentionReceiver.cs`: a Unity script for HoloLens that listens for the UDP packets and updates a character or environment color based on the attention level.

## Android app quickstart

1. Open `android-app` in Android Studio (Arctic Fox or newer).
2. Ensure the Muse 2/2016 headset is paired and powered on. The sample uses the [LibMuse SDK](https://developer.choosemuse.com/tools/windows-linux-mac) (`com.choosemuse:libmuse:6.0.1`).
3. Update the HoloLens IP/port defaults in `MainActivity.DEFAULT_HOST/DEFAULT_PORT` if needed.
4. Run the app on a device (not the emulator). Tap **스트림 시작** to begin:
   * The app starts a foreground service (`MuseStreamService`) that opens the Muse raw EEG stream.
   * `AttentionLevelCalculator` estimates attention from the envelope/derivative of the EEG channels.
   * `HoloLensBroadcaster` sends a small JSON packet with `score`, `level`, `lowEnergy`, `fastEnergy` over UDP.

## Unity (HoloLens) quickstart

1. Copy `unity/AttentionReceiver.cs` into your Unity project and add the component to a GameObject.
2. Assign a target `Renderer` (character mesh/material) and `Gradient` for colors. Optionally assign a `Light`.
3. Match the `listenPort` to the Android app (default `9010`).
4. Deploy to HoloLens. When the Android app streams, the component maps attention level → color/lighting.
