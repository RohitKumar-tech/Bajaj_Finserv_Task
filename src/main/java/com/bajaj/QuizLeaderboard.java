package com.bajaj;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class QuizLeaderboard {

    private static final String BASE_URL = "https://devapigw.vidalhealthtpa.com/srm-quiz-task";
    private static final String REG_NO = "RA2311003011411";
    private static final int TOTAL_POLLS = 10;
    private static final int DELAY_SECONDS = 5;

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    // Patterns for parsing JSON response
    private static final Pattern EVENTS_BLOCK = Pattern.compile("\"events\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern ROUND_ID     = Pattern.compile("\"roundId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PARTICIPANT  = Pattern.compile("\"participant\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SCORE        = Pattern.compile("\"score\"\\s*:\\s*(\\d+)");

    public static void main(String[] args) throws Exception {
        System.out.println("=== Quiz Leaderboard System ===");
        System.out.println("RegNo: " + REG_NO);

        Set<String> seen = new HashSet<>();
        Map<String, Integer> scores = new LinkedHashMap<>();

        for (int poll = 0; poll < TOTAL_POLLS; poll++) {
            System.out.println("\n[Poll " + poll + "] Fetching...");
            String body = fetchMessages(poll);

            if (body == null) {
                System.out.println("[Poll " + poll + "] ERROR: no response, skipping.");
            } else {
                int newEvents = 0, dupEvents = 0;
                List<Map<String, String>> events = parseEvents(body);

                for (Map<String, String> event : events) {
                    String roundId    = event.get("roundId");
                    String participant = event.get("participant");
                    int score         = Integer.parseInt(event.get("score"));
                    String key        = roundId + "|" + participant;

                    if (seen.add(key)) {
                        scores.merge(participant, score, Integer::sum);
                        newEvents++;
                    } else {
                        dupEvents++;
                    }
                }
                System.out.println("[Poll " + poll + "] New: " + newEvents + ", Duplicates ignored: " + dupEvents);
            }

            if (poll < TOTAL_POLLS - 1) {
                System.out.println("Waiting " + DELAY_SECONDS + "s...");
                Thread.sleep(DELAY_SECONDS * 1000L);
            }
        }

        // Sort by totalScore descending
        List<Map.Entry<String, Integer>> sorted = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        int totalScore = scores.values().stream().mapToInt(Integer::intValue).sum();

        System.out.println("\n=== Leaderboard ===");
        sorted.forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));
        System.out.println("Total Score: " + totalScore);

        System.out.println("\n=== Submitting ===");
        submitLeaderboard(sorted);
    }

    private static String fetchMessages(int poll) throws Exception {
        String url = BASE_URL + "/quiz/messages?regNo=" + REG_NO + "&poll=" + poll;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[Poll " + poll + "] HTTP " + response.statusCode());

        if (response.statusCode() == 200) {
            return response.body();
        }
        System.out.println("[Poll " + poll + "] Error body: " + response.body());
        return null;
    }

    private static List<Map<String, String>> parseEvents(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        Matcher blockMatcher = EVENTS_BLOCK.matcher(json);
        if (!blockMatcher.find()) return result;

        String eventsStr = blockMatcher.group(1);
        // Split on object boundaries: each event is { ... }
        Pattern objPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher objMatcher = objPattern.matcher(eventsStr);

        while (objMatcher.find()) {
            String obj = objMatcher.group(1);
            Matcher rid = ROUND_ID.matcher(obj);
            Matcher par = PARTICIPANT.matcher(obj);
            Matcher sc  = SCORE.matcher(obj);

            if (rid.find() && par.find() && sc.find()) {
                Map<String, String> event = new HashMap<>();
                event.put("roundId", rid.group(1));
                event.put("participant", par.group(1));
                event.put("score", sc.group(1));
                result.add(event);
            }
        }
        return result;
    }

    private static void submitLeaderboard(List<Map.Entry<String, Integer>> leaderboard) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"regNo\": \"").append(REG_NO).append("\",\n");
        sb.append("  \"leaderboard\": [\n");

        for (int i = 0; i < leaderboard.size(); i++) {
            Map.Entry<String, Integer> e = leaderboard.get(i);
            sb.append("    {\"participant\": \"").append(e.getKey())
              .append("\", \"totalScore\": ").append(e.getValue()).append("}");
            if (i < leaderboard.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}");

        String payload = sb.toString();
        System.out.println("Payload:\n" + payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/quiz/submit"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("\nSubmit HTTP " + response.statusCode());
        System.out.println("Response: " + response.body());
    }
}
