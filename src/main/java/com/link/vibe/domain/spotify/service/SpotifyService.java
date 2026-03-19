package com.link.vibe.domain.spotify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.link.vibe.domain.spotify.config.SpotifyProperties;
import com.link.vibe.domain.spotify.dto.SpotifyTrackResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@EnableConfigurationProperties(SpotifyProperties.class)
public class SpotifyService {

    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SEARCH_URL = "https://api.spotify.com/v1/search";
    private static final String MUSICBRAINZ_URL = "https://musicbrainz.org/ws/2/recording";

    private final SpotifyProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String accessToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public SpotifyService(SpotifyProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    /**
     * ISRC로 Spotify 트랙 검색
     */
    public Optional<SpotifyTrackResponse> searchByIsrc(String isrc) {
        if (isrc == null || isrc.isBlank()) return Optional.empty();

        try {
            String token = getAccessToken();
            String query = "isrc:" + isrc;
            JsonNode tracks = executeSearch(token, query, "track", 1);
            return parseFirstTrack(tracks);
        } catch (Exception e) {
            log.warn("Spotify ISRC 검색 실패 [isrc={}]: {}", isrc, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * MusicBrainz ID (MBID)로 Spotify 트랙 검색
     * MusicBrainz API에서 ISRC를 조회한 후 Spotify 검색
     */
    public Optional<SpotifyTrackResponse> searchByMbid(String mbid) {
        if (mbid == null || mbid.isBlank()) return Optional.empty();

        try {
            String isrc = lookupIsrcFromMusicBrainz(mbid);
            if (isrc != null) {
                return searchByIsrc(isrc);
            }

            // ISRC가 없으면 MusicBrainz에서 트랙명과 아티스트를 가져와 검색
            return searchByMbidFallback(mbid);
        } catch (Exception e) {
            log.warn("Spotify MBID 검색 실패 [mbid={}]: {}", mbid, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 트랙 이름과 아티스트로 Spotify 검색
     */
    public Optional<SpotifyTrackResponse> searchByQuery(String trackName, String artist) {
        if (trackName == null || trackName.isBlank()) return Optional.empty();

        try {
            String token = getAccessToken();
            StringBuilder query = new StringBuilder("track:").append(trackName);
            if (artist != null && !artist.isBlank()) {
                query.append(" artist:").append(artist);
            }
            JsonNode tracks = executeSearch(token, query.toString(), "track", 1);
            return parseFirstTrack(tracks);
        } catch (Exception e) {
            log.warn("Spotify 쿼리 검색 실패 [track={}, artist={}]: {}", trackName, artist, e.getMessage());
            return Optional.empty();
        }
    }

    // ── Client Credentials 토큰 관리 ──

    private synchronized String getAccessToken() {
        if (accessToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return accessToken;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String credentials = Base64.getEncoder()
                .encodeToString((properties.clientId() + ":" + properties.clientSecret()).getBytes());
        headers.set("Authorization", "Basic " + credentials);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                TOKEN_URL,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                JsonNode.class
        );

        JsonNode tokenBody = response.getBody();
        if (tokenBody == null) {
            throw new RuntimeException("Spotify 토큰 응답이 비어있습니다");
        }

        this.accessToken = tokenBody.get("access_token").asText();
        int expiresIn = tokenBody.get("expires_in").asInt();
        this.tokenExpiresAt = Instant.now().plusSeconds(expiresIn - 60); // 만료 1분 전 갱신

        log.info("Spotify 액세스 토큰 갱신 완료 (만료: {}초)", expiresIn);
        return this.accessToken;
    }

    // ── Spotify Search API ──

    private JsonNode executeSearch(String token, String query, String type, int limit) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        String url = UriComponentsBuilder.fromUriString(SEARCH_URL)
                .queryParam("q", query)
                .queryParam("type", type)
                .queryParam("limit", limit)
                .build()
                .toUriString();

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
        );

        return response.getBody();
    }

    // ── MusicBrainz ISRC 조회 ──

    private String lookupIsrcFromMusicBrainz(String mbid) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "VibeConnector/1.0 (vibeconnector@link.com)");
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            String url = UriComponentsBuilder.fromUriString(MUSICBRAINZ_URL)
                    .pathSegment(mbid)
                    .queryParam("inc", "isrcs")
                    .queryParam("fmt", "json")
                    .build()
                    .toUriString();

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    JsonNode.class
            );

            JsonNode body = response.getBody();
            if (body != null && body.has("isrcs") && body.get("isrcs").isArray() && !body.get("isrcs").isEmpty()) {
                return body.get("isrcs").get(0).asText();
            }
        } catch (Exception e) {
            log.warn("MusicBrainz ISRC 조회 실패 [mbid={}]: {}", mbid, e.getMessage());
        }
        return null;
    }

    private Optional<SpotifyTrackResponse> searchByMbidFallback(String mbid) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "VibeConnector/1.0 (vibeconnector@link.com)");
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            String url = UriComponentsBuilder.fromUriString(MUSICBRAINZ_URL)
                    .pathSegment(mbid)
                    .queryParam("inc", "artists")
                    .queryParam("fmt", "json")
                    .build()
                    .toUriString();

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    JsonNode.class
            );

            JsonNode body = response.getBody();
            if (body != null) {
                String title = body.has("title") ? body.get("title").asText() : null;
                String artist = null;
                if (body.has("artist-credit") && body.get("artist-credit").isArray()
                        && !body.get("artist-credit").isEmpty()) {
                    artist = body.get("artist-credit").get(0).path("name").asText(null);
                }
                if (title != null) {
                    return searchByQuery(title, artist);
                }
            }
        } catch (Exception e) {
            log.warn("MusicBrainz 폴백 검색 실패 [mbid={}]: {}", mbid, e.getMessage());
        }
        return Optional.empty();
    }

    // ── JSON 파싱 ──

    private Optional<SpotifyTrackResponse> parseFirstTrack(JsonNode searchResult) {
        if (searchResult == null) return Optional.empty();

        JsonNode tracks = searchResult.path("tracks").path("items");
        if (!tracks.isArray() || tracks.isEmpty()) return Optional.empty();

        JsonNode track = tracks.get(0);
        return Optional.of(mapToResponse(track));
    }

    private SpotifyTrackResponse mapToResponse(JsonNode track) {
        String spotifyId = track.path("id").asText(null);
        String trackName = track.path("name").asText(null);

        List<String> artists = new ArrayList<>();
        JsonNode artistsNode = track.path("artists");
        if (artistsNode.isArray()) {
            for (JsonNode artist : artistsNode) {
                artists.add(artist.path("name").asText());
            }
        }

        String albumName = track.path("album").path("name").asText(null);

        // 앨범 커버 이미지 (크기별)
        String albumCoverUrl = null;
        String albumCoverMediumUrl = null;
        String albumCoverSmallUrl = null;
        JsonNode images = track.path("album").path("images");
        if (images.isArray()) {
            for (JsonNode img : images) {
                int height = img.path("height").asInt(0);
                String url = img.path("url").asText(null);
                if (height >= 500) albumCoverUrl = url;
                else if (height >= 200) albumCoverMediumUrl = url;
                else if (height > 0) albumCoverSmallUrl = url;
            }
            // 가장 큰 이미지가 없으면 첫 번째 이미지 사용
            if (albumCoverUrl == null && !images.isEmpty()) {
                albumCoverUrl = images.get(0).path("url").asText(null);
            }
        }

        String previewUrl = track.path("preview_url").asText(null);
        String spotifyUri = track.path("uri").asText(null);
        String spotifyUrl = track.path("external_urls").path("spotify").asText(null);
        int durationMs = track.path("duration_ms").asInt(0);

        // ISRC 추출
        String isrc = track.path("external_ids").path("isrc").asText(null);

        return new SpotifyTrackResponse(
                spotifyId, trackName, artists, albumName,
                albumCoverUrl, albumCoverMediumUrl, albumCoverSmallUrl,
                previewUrl, spotifyUri, spotifyUrl,
                durationMs > 0 ? durationMs : null,
                isrc
        );
    }
}
