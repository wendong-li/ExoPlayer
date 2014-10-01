/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.android.exoplayer.hls;

import com.google.android.exoplayer.hls.HlsMediaPlaylist.Segment;
import com.google.android.exoplayer.util.ManifestParser;

import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * HLS Media playlists parsing logic.
 */
public final class HlsMediaPlaylistParser implements ManifestParser<HlsMediaPlaylist> {

  private static final String DISCONTINUITY_TAG = "#EXT-X-DISCONTINUITY";
  private static final String MEDIA_DURATION_TAG = "#EXTINF";
  private static final String MEDIA_SEQUENCE_TAG = "#EXT-X-MEDIA-SEQUENCE";
  private static final String TARGET_DURATION_TAG = "#EXT-X-TARGETDURATION";
  private static final String VERSION_TAG = "#EXT-X-VERSION";
  private static final String ENDLIST_TAG = "#EXT-X-ENDLIST";

  private static final Pattern MEDIA_DURATION_REGEX =
      Pattern.compile(MEDIA_DURATION_TAG + ":([\\d.]+),");
  private static final Pattern MEDIA_SEQUENCE_REGEX =
      Pattern.compile(MEDIA_SEQUENCE_TAG + ":(\\d+)\\b");
  private static final Pattern TARGET_DURATION_REGEX =
      Pattern.compile(TARGET_DURATION_TAG + ":(\\d+)\\b");
  private static final Pattern VERSION_REGEX =
      Pattern.compile(VERSION_TAG + ":(\\d+)\\b");

  @Override
  public HlsMediaPlaylist parse(InputStream inputStream, String inputEncoding,
      String contentId, Uri baseUri) throws IOException {
    return parseMediaPlaylist(inputStream, inputEncoding, baseUri);
  }

  private static HlsMediaPlaylist parseMediaPlaylist(InputStream inputStream, String inputEncoding,
      Uri baseUri) throws IOException {
    BufferedReader reader = new BufferedReader((inputEncoding == null)
        ? new InputStreamReader(inputStream) : new InputStreamReader(inputStream, inputEncoding));

    int mediaSequence = 0;
    int targetDurationSecs = 0;
    int version = 1; // Default version == 1.
    boolean live = true;
    List<Segment> segments = new ArrayList<Segment>();

    double segmentDurationSecs = 0.0;
    boolean segmentDiscontinuity = false;
    long segmentStartTimeUs = 0;

    String line;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      if (line.isEmpty()) {
        continue;
      }
      if (line.startsWith(TARGET_DURATION_TAG)) {
        targetDurationSecs = HlsParserUtil.parseIntAttr(line, TARGET_DURATION_REGEX,
            TARGET_DURATION_TAG);
      } else if (line.startsWith(MEDIA_SEQUENCE_TAG)) {
        mediaSequence = HlsParserUtil.parseIntAttr(line, MEDIA_SEQUENCE_REGEX, MEDIA_SEQUENCE_TAG);
      } else if (line.startsWith(VERSION_TAG)) {
        version = HlsParserUtil.parseIntAttr(line, VERSION_REGEX, VERSION_TAG);
      } else if (line.startsWith(MEDIA_DURATION_TAG)) {
        segmentDurationSecs = HlsParserUtil.parseDoubleAttr(line, MEDIA_DURATION_REGEX,
            MEDIA_DURATION_TAG);
      } else if (line.equals(DISCONTINUITY_TAG)) {
        segmentDiscontinuity = true;
      } else if (!line.startsWith("#")) {
        segments.add(new Segment(line, segmentDurationSecs, segmentDiscontinuity,
            segmentStartTimeUs));
        segmentStartTimeUs += (long) (segmentDurationSecs * 1000000);
        segmentDiscontinuity = false;
        segmentDurationSecs = 0.0;
      } else if (line.equals(ENDLIST_TAG)) {
        live = false;
        break;
      }
    }
    return new HlsMediaPlaylist(baseUri, mediaSequence, targetDurationSecs, version, live,
        Collections.unmodifiableList(segments));
  }

}
