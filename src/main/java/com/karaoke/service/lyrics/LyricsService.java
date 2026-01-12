package com.karaoke.service.lyrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class LyricsService {

    @Value("${app.lyrics.genius-api-key:}")
    private String geniusApiKey;

    @Value("${app.lyrics.musixmatch-api-key:}")
    private String musixmatchApiKey;

    private final WebClient.Builder webClientBuilder;
    private final SpeechToTextService speechToTextService;

    public LyricsProvider.LyricsResult getLyrics(String artist, String title, String audioPath) {
        log.info("Fetching lyrics for: {} - {}", artist, title);
        
        // Try online sources first
        LyricsProvider.LyricsResult result = tryOnlineSources(artist, title);
        
        if (result != null && !result.getLines().isEmpty()) {
            log.info("Lyrics found from online source");
            return result;
        }
        
        // Fallback to speech-to-text
        log.info("Online lyrics not found, falling back to speech-to-text");
        return speechToTextService.transcribeAudio(audioPath);
    }

    private LyricsProvider.LyricsResult tryOnlineSources(String artist, String title) {
        // Try Lyrics.ovh (free, no API key required)
        LyricsProvider.LyricsResult result = tryLyricsOvh(artist, title);
        if (result != null && !result.getLines().isEmpty()) {
            return result;
        }
        
        // Try Genius if API key is available
        if (!geniusApiKey.isEmpty()) {
            result = tryGenius(artist, title);
            if (result != null && !result.getLines().isEmpty()) {
                return result;
            }
        }
        
        // Try Musixmatch if API key is available
        if (!musixmatchApiKey.isEmpty()) {
            result = tryMusixmatch(artist, title);
            if (result != null && !result.getLines().isEmpty()) {
                return result;
            }
        }
        
        return null;
    }

    private LyricsProvider.LyricsResult tryLyricsOvh(String artist, String title) {
        try {
            WebClient webClient = webClientBuilder.build();
            String url = String.format("https://api.lyrics.ovh/v1/%s/%s", 
                    artist.replace(" ", "%20"), 
                    title.replace(" ", "%20"));
            
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response != null && response.contains("\"lyrics\"")) {
                // Parse JSON response (simple extraction)
                String lyrics = extractLyricsFromJson(response);
                if (lyrics != null && !lyrics.isEmpty()) {
                    List<LyricsProvider.LyricLine> lines = parseLyricsToLines(lyrics);
                    return new LyricsProvider.LyricsResult(lines, false);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch lyrics from Lyrics.ovh: {}", e.getMessage());
        }
        return null;
    }

    private LyricsProvider.LyricsResult tryGenius(String artist, String title) {
        // Genius API implementation would go here
        // For now, return null as it requires more complex implementation
        log.debug("Genius API not fully implemented yet");
        return null;
    }

    private LyricsProvider.LyricsResult tryMusixmatch(String artist, String title) {
        // Musixmatch API implementation would go here
        // For now, return null as it requires more complex implementation
        log.debug("Musixmatch API not fully implemented yet");
        return null;
    }

    private String extractLyricsFromJson(String json) {
        // Simple JSON parsing for lyrics.ovh format
        Pattern pattern = Pattern.compile("\"lyrics\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).replace("\\n", "\n").replace("\\r", "");
        }
        return null;
    }

    private List<LyricsProvider.LyricLine> parseLyricsToLines(String lyrics) {
        List<LyricsProvider.LyricLine> lines = new ArrayList<>();
        String[] lyricLines = lyrics.split("\n");
        for (String line : lyricLines) {
            line = line.trim();
            if (!line.isEmpty()) {
                lines.add(new LyricsProvider.LyricLine(line, null, null));
            }
        }
        return lines;
    }
}
