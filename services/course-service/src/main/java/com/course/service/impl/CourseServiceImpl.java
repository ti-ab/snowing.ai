package com.course.service.impl;


import com.course.model.*;
import com.course.repository.BookRepository;
import com.course.service.CourseService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main service responsible for generating and **persisting** a full course book
 * with GPT‑4o.
 * <p>
 * OpenAI calls are parallelised via <strong>virtual threads</strong> (Java 21),
 * then the resulting object graph is persisted in a PostgreSQL database through
 * Spring Data JPA.
 */
@Service
@Transactional
public class CourseServiceImpl implements CourseService {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    private final BookRepository bookRepository;

    @Autowired
    public CourseServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // Inject via application.yml: openai.api.key
    @Value("${openai.api.key}")
    private String openAiApiKey;

    /* ─────────────────────────────────────────────────────────────
       Public API
       ───────────────────────────────────────────────────────────*/

    /**
     * Generates <em>and persiste</em> the course book corresponding to the given
     * description (e.g. « Apprendre l'anglais pour les nuls »).
     *
     * @return the detached, fully‑saved {@link Book} entity.
     */
    public Book generateCourseBook(String bookDescription) {
        /* 1️⃣ Plan global */
        String planJson = callOpenAi(Prompts.bookPlan(bookDescription));
        BookPlan plan = JsonUtils.read(planJson, BookPlan.class);

        /* 2️⃣ Création de l'entité racine */
        Book book = new Book(plan.mainTitle(), plan.authors());

        /* 3️⃣ Chapitres en parallèle (threads virtuels) */
        try (ExecutorService vThreads = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Chapter>> futures = new ArrayList<>();

            int i = 0;
            for (BookPlan.ChapterPlan cPlan : plan.chapters()) {
                int chapterIndex = i++;  // ou ++i si tu veux 1-based
                futures.add(CompletableFuture.supplyAsync(() -> {
                    Chapter c = buildChapter(bookDescription, cPlan);
                    c.setIdx(chapterIndex);
                    return c;
                }, vThreads));
            }

            futures.forEach(cf -> book.addChapter(cf.join()));
        }

        book.setCreatedAt(LocalDateTime.now());

        /* 4️⃣ Sauvegarde ; grâce à CascadeType.ALL tout l'arbre est stocké */
        return bookRepository.save(book);
    }

    /* ─────────────────────────────────────────────────────────────
       Construction d'un chapitre complet
       ───────────────────────────────────────────────────────────*/
    private Chapter buildChapter(String description, BookPlan.ChapterPlan plan) {
        String chapterJson = callOpenAi(Prompts.chapter(description, plan));

        Chapter chapter = JsonUtils.read(chapterJson, Chapter.class);

        chapter.setTitle(plan.title());
        // Lien parent
        chapter.subchapters().forEach(sc -> sc.setChapter(chapter));

        // Génération séquentielle des sections (évite le rate‑limit)
        int subIndex = 0;

        for (Subchapter sc : chapter.subchapters()) {
            sc.sections().forEach(s -> s.setSubchapter(sc));
            sc.setIdx(subIndex++);

            //chapter.addSubchapter(sc);      // ✅ maintient les deux côtés
            int secIndex = 0;
            for (Section sec : sc.sections()) {
                //sc.addSection(sec);         // ✅ idem pour les sections
                sec.setIdx(secIndex++);
                String prompt = Prompts.section(description, chapter.title(), sc.title(), sec);
                String content = callOpenAi(prompt);
                sec.setContent(content.trim());
            }
        }
        return chapter;
    }

    /* ─────────────────────────────────────────────────────────────
       HTTP helper vers OpenAI
       ───────────────────────────────────────────────────────────*/
    private String callOpenAi(String prompt) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            Map<String, Object> body = Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "temperature", 0.7
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();
            HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                throw new IllegalStateException("OpenAI error " + res.statusCode() + " → " + res.body());
            }

            JsonNode root = MAPPER.readTree(res.body());
            String rawContent = root.at("/choices/0/message/content").asText();

            // On enlève un éventuel bloc ```json ... ```, un bloc ``` ... ```,
            // ou un préfixe "json", puis on trim()
            String cleaned = rawContent
                    // 1) début de chaîne : ```json ou ``` ou json
                    .replaceAll("(?i)^(?:```json|```|json)\\s*", "")
                    // 2) fin de chaîne : ```
                    .replaceAll("```\\s*$", "")
                    .trim();

            return cleaned;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Unable to reach OpenAI", e);
        }
    }


    /* ─────────────────────────────────────────────────────────────
       Prompts factory
       ───────────────────────────────────────────────────────────*/
    private static final class Prompts {
        static String bookPlan(String description) {
            return """
                    Can you generate a 200 pages training course book described as following: "%s".
                    Give me the book plan in 8 chapters. Each chapter must contain 4 subchapters. Each subchapter must contain 4 sections.
                    mainTitle represents the main book title in at most 10 words. Authors must be completed only if it is mentioned in the book description.
                    Do not include the chapter number neither the word \"chapter\" in chapter title. Do not include subchapter number neither the word \"subchapter\" in subchapter subtitle.
                    You have to answer only in parsable JSON format. Ensure that your answer is parsable in JSON. Answer in the language of the book description. 
                    Here is a short example of how the JSON must look like:\s
                      {mainTitle: "main title of the book",
                      authors: "authors of the book",
                      chapters: [
                      {title: "Burpees from A to Z", subchapters: [{title: "Subchapter 1 example", sections: ["section 1 example", "section 2 example"]}, {title: "Subchapter 2 example", sections: ["section 1 example", "section 2 example"]}]},\s
                      {title: "ABC", subchapters: [{title: "Subchapter 1 example", sections: ["section 1 example", "section 2 example"]}, {title: "Subchapter 2 example", sections: ["section 1 example", "section 2 example"]}]},\s
                      ]}
                    """.formatted(description);
        }

        static String chapter(String description, BookPlan.ChapterPlan ch) {
            return """
                    Consider the training course book described as following: "%s". Each chapter has subchapters, and each subchapter has sections.
                    each section must have title, firstParagraph, middleParagraph, endParagraph and sectionSummary attributes.
                    Here is a chapter of the book containing subchapters and sections.
                    You have to answer only in parsable JSON format. Ensure that your answer is parsable in JSON.
                    
                    %s
                    
                    Here is a short example of how the JSON must look like:\s
                      {title: "Burpees from A to Z", subchapters: [{title: "Subchapter 1 example", sections: [{title: "section 1 title example", sectionSummary: "section 1 summary example", firstParagraph: "section 1 first paragraph example", middleParagraph: "section 1 middle paragraph example", endParagraph: "section 1 end paragraph example"}, {title: "section 2 title example", sectionSummary: "section 2 summary example", firstParagraph: "section 2 first paragraph example", middleParagraph: "section 2 middle paragraph example", endParagraph: "section 2 end paragraph example"}]}, {title: "Subchapter 2 example", sections: [{title: "section 1 title example", sectionSummary: "section 1 summary example", firstParagraph: "section 1 first paragraph example", middleParagraph: "section 1 middle paragraph example", endParagraph: "section 1 end paragraph example"}, {title: "section 2 title example", sectionSummary: "section 2 summary example", firstParagraph: "section 2 first paragraph example", middleParagraph: "section 2 middle paragraph example", endParagraph: "section 2 end paragraph example"}]}]}
                    """.formatted(description, JsonUtils.write(ch));
        }

        static String section(String description, String chapTitle, String subTitle, Section s) {
            return """
                    You are writing a training course book described as the following "%s".
                    Can you generate the content of the section \"%s\" from the subchapter \"%s\" of the chapter \"%s\" in at least 250 words and at most 400 words ?
                    This section is summarized as the following \"%s\" and starts like the following \"%s\" and continues like the following \"%s\" and ends like the following \"%s\".
                    Do not include the section title in the output.
                    Answer in the language of the book description.
                    Output must be plain text, not JSON.
                    """.formatted(description,
                    s.title(), subTitle, chapTitle,
                    s.sectionSummary(), s.firstParagraph(), s.middleParagraph(), s.endParagraph());
        }
    }

    /* ─────────────────────────────────────────────────────────────
       JSON helpers
       ───────────────────────────────────────────────────────────*/
    private static final class JsonUtils {
        static <T> T read(String j, Class<T> t) {
            try {
                return MAPPER.readValue(j, t);
            } catch (IOException e) {
                throw new IllegalArgumentException("Bad JSON", e);
            }
        }

        static String write(Object v) {
            try {
                return MAPPER.writeValueAsString(v);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}