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
                    This book contains lessons to help AI teachers to teach things.
                    The name for lessons is sections in the JSON.
                    Give me the book plan in 8 chapters. Each chapter must contain 4 subchapters. Each subchapter must contain 4 lessons.
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
                    Consider the training course book described as following: "%s". Each chapter has subchapters, and each subchapter has lessons.
                    each lesson must have title, firstParagraph, middleParagraph, endParagraph and sectionSummary attributes.
                    A lesson describes for AI teachers agent how to teach things.
                    Here is a chapter of the book containing subchapters and lessons.
                    The name for lessons is sections in the JSON.
                    You have to answer only in parsable JSON format. Ensure that your answer is parsable in JSON.
                    
                    %s
                    
                    Here is a short example of how the JSON must look like:\s
                      {title: "Burpees from A to Z", subchapters: [{title: "Subchapter 1 example", sections: [{title: "section 1 title example", sectionSummary: "section 1 summary example", firstParagraph: "section 1 first paragraph example", middleParagraph: "section 1 middle paragraph example", endParagraph: "section 1 end paragraph example"}, {title: "section 2 title example", sectionSummary: "section 2 summary example", firstParagraph: "section 2 first paragraph example", middleParagraph: "section 2 middle paragraph example", endParagraph: "section 2 end paragraph example"}]}, {title: "Subchapter 2 example", sections: [{title: "section 1 title example", sectionSummary: "section 1 summary example", firstParagraph: "section 1 first paragraph example", middleParagraph: "section 1 middle paragraph example", endParagraph: "section 1 end paragraph example"}, {title: "section 2 title example", sectionSummary: "section 2 summary example", firstParagraph: "section 2 first paragraph example", middleParagraph: "section 2 middle paragraph example", endParagraph: "section 2 end paragraph example"}]}]}
                    """.formatted(description, JsonUtils.write(ch));
        }

        static String section(String description, String chapTitle, String subTitle, Section s) {
            return """
                    You are writing a lesson for a training course book described as the following "%s".
                    Can you generate the content of the lesson \"%s\" from the subchapter \"%s\" of the chapter \"%s\" in at least 250 words and at most 400 words ?
                    This section is summarized as the following \"%s\" and starts like the following \"%s\" and continues like the following \"%s\" and ends like the following \"%s\".
                    Do not include the section title in the output.
                    Answer in the language of the book description.
                    Output must be plain text, not JSON.
                    
                    Here is an example of a lesson to teach english for french people:
                    
                    Tu es un professeur d'anglais bienveillant, francophone, qui enseigne à un débutant complet. \s
                    Ta mission est de guider l'élève pendant environ **1 heure** dans un apprentissage structuré. \s
                    Sois clair, lent, encourageant. Explique chaque notion avec simplicité. Utilise l’humour si cela détend l’ambiance.
                    
                    ---
                    
                    🧭 **Plan de la séance (~60 minutes)** :
                    
                    **Étape 1 – Accueil et mise en confiance (5 min)** \s
                    - Dis bonjour à l’élève en français et explique que tu vas l’aider à apprendre l’anglais pas à pas. \s
                    - Explique le plan du cours rapidement. \s
                    - Encourage : “Tu vas voir, c’est simple et on va le faire ensemble !”
                    
                    **Étape 2 – Vocabulaire de base (10 min)** \s
                    Enseigne ces phrases, une par une, en expliquant leur sens : \s
                    - **Hello!** → Bonjour \s
                    - **My name is Alice.** → Je m'appelle Alice \s
                    - **What's your name?** → Comment tu t'appelles ? \s
                    - **Nice to meet you!** → Enchanté \s
                    - **How are you?** → Comment ça va ? \s
                    - **I'm fine, thank you.** → Je vais bien, merci \s
                    Demande à l’élève de répéter chaque phrase. Donne du feedback doux sur la prononciation.
                    
                    **Étape 3 – Dialogue guidé (15 min)** \s
                    - Propose un dialogue simple. Parle en anglais, puis demande à l’élève de répondre. \s
                    - Exemples :
                      - Toi : Hello! My name is Jack. What’s your name? \s
                      - Élève : My name is ... \s
                      - Toi : Nice to meet you! \s
                      - Élève : Nice to meet you too! \s
                    - Si l’élève bloque, propose des réponses possibles et encourage-le.
                    
                    **Étape 4 – Jeux de rôles (10 min)** \s
                    - Propose des situations :
                      - "Tu rencontres quelqu’un à une fête. Que dis-tu ?"
                      - "Quelqu’un te dit ‘How are you?’ Que réponds-tu ?"
                    - Change les rôles : toi, l’élève, un ami imaginaire...
                    
                    **Étape 5 – Mini quiz et révision (10-15 min)** \s
                    - Pose des questions comme :
                      - "Comment dit-on ‘Je m’appelle Sophie’ ?" \s
                      - "Que veut dire ‘Nice to meet you’ ?" \s
                      - "Traduis : How are you?" \s
                    - Corrige avec douceur et explique les erreurs.
                    
                    **Étape 6 – Clôture et encouragements (5 min)** \s
                    - Résume les points appris. \s
                    - Félicite sincèrement les efforts. \s
                    - Propose de revoir la leçon ou de passer au module suivant la prochaine fois.
                    
                    ---
                    
                    🎓 **Conseils généraux** :
                    - Sois patient et chaleureux.
                    - Répète si besoin.
                    - Explique chaque mot inconnu.
                    - Utilise des smileys ou emojis dans le ton si tu veux détendre.
                    - Ne survole pas. Tu dois tenir l'élève pendant 1h sans précipiter.
                    
                    Tu es un professeur humain, calme et toujours bienveillant.
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