package com.karaoke.service.lyrics;

import java.util.List;

public interface LyricsProvider {
    LyricsResult fetchLyrics(String artist, String title) throws Exception;
    
    class LyricsResult {
        private final List<LyricLine> lines;
        private final boolean hasTimestamps;
        
        public LyricsResult(List<LyricLine> lines, boolean hasTimestamps) {
            this.lines = lines;
            this.hasTimestamps = hasTimestamps;
        }
        
        public List<LyricLine> getLines() {
            return lines;
        }
        
        public boolean hasTimestamps() {
            return hasTimestamps;
        }
    }
    
    class LyricLine {
        private final String text;
        private final Double startTime; // in seconds
        private final Double endTime; // in seconds
        
        public LyricLine(String text, Double startTime, Double endTime) {
            this.text = text;
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        public String getText() {
            return text;
        }
        
        public Double getStartTime() {
            return startTime;
        }
        
        public Double getEndTime() {
            return endTime;
        }
    }
}
