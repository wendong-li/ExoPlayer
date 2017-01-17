/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2;

import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectorResult;

/**
 * Holds information to be shared among renderers. The {@link RenderersSharedInfo} instance
 * is supposed to be accessed from the internal playback thread only.
 */
public class RenderersSharedInfo {
  public boolean hasAudioRenderer;
  public boolean hasVideoRenderer;
  public boolean hasTextRenderer;
  public boolean usesVideoTunneling;
  public int audioSessionId;

  public static RenderersSharedInfo buildFrom(TrackSelectorResult trackSelectorResult,
      RendererCapabilities[] rendererCapabilities) {
    RenderersSharedInfo renderersSharedInfo = new RenderersSharedInfo();

    TrackSelection trackSelection;
    int formatSupportsForAudio = 0;
    int formatSupportsForVideo = 0;
    int formatSupports = 0;
    for (int i = 0; i < rendererCapabilities.length; i++) {
      trackSelection = trackSelectorResult.selections.get(i);
      if (trackSelection == null) {
        continue;
      }
      try {
        formatSupports = rendererCapabilities[i].supportsFormat(trackSelection.getSelectedFormat());
      } catch (ExoPlaybackException e) {
        // Should not happen
        formatSupports = 0;
      }

      switch (rendererCapabilities[i].getTrackType()) {
        case C.TRACK_TYPE_AUDIO:
          renderersSharedInfo.hasAudioRenderer = true;
          formatSupportsForAudio = formatSupports;
          break;
        case C.TRACK_TYPE_VIDEO:
          renderersSharedInfo.hasVideoRenderer = true;
          formatSupportsForVideo = formatSupports;
          break;
        case C.TRACK_TYPE_TEXT:
          renderersSharedInfo.hasTextRenderer = true;
          break;
        default:
          break;
      }
    }

    // Video tunneling requires audio renderer with hw av sync feature
    // TODO: Consider the app preference on whether or not use video tunneling
    if (renderersSharedInfo.hasVideoRenderer && renderersSharedInfo.hasAudioRenderer) {
      renderersSharedInfo.usesVideoTunneling =
          ((formatSupportsForVideo & RendererCapabilities.TUNNELING_SUPPORTED) != 0)
              && ((formatSupportsForAudio & RendererCapabilities.TUNNELING_SUPPORTED) != 0);
    }
    return renderersSharedInfo;
  }

  public RenderersSharedInfo() {
    reset();
  }

  public void reset() {
    hasAudioRenderer = false;
    hasVideoRenderer = false;
    hasTextRenderer = false;
    usesVideoTunneling = false;
    audioSessionId = C.AUDIO_SESSION_ID_UNSET;
  }
}
