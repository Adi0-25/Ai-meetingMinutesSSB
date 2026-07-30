# Ai-meetingMinutesSSB
# Minutes.AI — AI Meeting Minutes Generator (Java + Spring Boot)

An AI-powered web app that turns a meeting recording into a polished, professional set of
meeting minutes: upload or record audio → automatic speech-to-text transcription → AI-generated
Executive Summary, Key Discussion Points, Decisions Made, and Action Items — exportable as a PDF.

This is a full Java/Spring Boot rewrite of an original Python/Flask + faster-whisper prototype,
redesigned as a layered REST backend with a persistent meeting history, backed by the same
glassmorphism-styled HTML/CSS/JS frontend.

## ✨ Features

- 🎙️ **Audio ingestion** — drag & drop upload or live in-browser microphone recording
- 📝 **Speech-to-text transcription** via the OpenAI Whisper API
- 🤖 **AI-generated meeting minutes** (Executive Summary, Key Discussion Points, Decisions,
  Action Items) via OpenAI GPT, formatted as Markdown
- 🛡️ **Offline fallback summarizer** — if no OpenAI API key is configured (or a request fails),
  the app automatically degrades to a self-contained, keyword/heuristic-based Java summarizer,
  so the project still runs and produces usable output with zero external dependencies or cost
- 🌍 **Multi-language output** — translate the generated minutes into Spanish, French, German,
  Chinese, Japanese, or Korean
- 💾 **Meeting history** — save generated minutes to a database and reload them later
  (Spring Data JPA + H2)
- 📄 **One-click PDF export** (client-side, via html2pdf.js)
- 📚 **Interactive API docs** via Swagger UI (springdoc-openapi)
- 🐳 **Dockerized** for easy deployment
- ✅ **Unit & controller tests** (JUnit 5 + MockMvc)

## 🏗️ Architecture

```
Browser (HTML/CSS/JS)
      │  fetch()
      ▼
Spring Boot REST API  (controller → service → repository)
      │                              │
      ├── TranscriptionService ──────┼──▶ OpenAI Whisper API
      ├── SummarizationService ──────┼──▶ OpenAI Chat Completions API
      │        └── falls back to a local extractive/keyword summarizer
      └── MeetingService ────────────┴──▶ Spring Data JPA ──▶ H2 (file-based)
```

**Backend stack:** Java 17, Spring Boot 3, Spring Web, Spring Data JPA, Bean Validation,
Spring Boot Actuator, H2 Database, springdoc-openapi (Swagger UI).

**Frontend stack:** vanilla HTML5, CSS3 (glassmorphism / neon design system, no framework),
vanilla JavaScript (`fetch`, `MediaRecorder`), [marked.js](https://github.com/markedjs/marked) for
Markdown rendering, [html2pdf.js](https://github.com/eKoopmans/html2pdf.js) for client-side PDF export.

## 📁 Project Structure

```
ai-meeting-minutes/
├── pom.xml
├── Dockerfile
├── src/
│   ├── main/
│   │   ├── java/com/aiminutes/
│   │   │   ├── AiMeetingMinutesApplication.java
│   │   │   ├── config/            # CORS configuration
│   │   │   ├── controller/        # REST controllers + global exception handler
│   │   │   ├── dto/                # Request/response payloads
│   │   │   ├── exception/         # Custom exceptions
│   │   │   ├── model/              # JPA entities
│   │   │   ├── repository/        # Spring Data JPA repositories
│   │   │   └── service/            # Business logic (+ impl/ for provider implementations)
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/             # index.html, style.css, app.js, favicon.svg
│   └── test/java/com/aiminutes/    # Unit & controller tests
```

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+ (or use the included wrapper, if you generate one with `mvn -N wrapper:wrapper`)
- An [OpenAI API key](https://platform.openai.com/api-keys) (optional — see below)

### Run locally

```bash
git clone https://github.com/<your-username>/ai-meeting-minutes.git
cd ai-meeting-minutes

# Optional but recommended: enables real transcription + GPT-generated minutes
export OPENAI_API_KEY=sk-...

mvn spring-boot:run
```

Then open **http://localhost:8080** in your browser.

> **Without an API key:** transcription requires `OPENAI_API_KEY` to be set (there's no bundled
> offline speech-to-text engine). Summarization, however, works out of the box even without a
> key — it automatically uses the built-in local summarizer.

### Run with Docker

```bash
docker build -t ai-meeting-minutes .
docker run -p 8080:8080 -e OPENAI_API_KEY=sk-... ai-meeting-minutes
```

### Run the tests

```bash
mvn test
```

## 🔌 API Reference

Interactive Swagger UI is available at **http://localhost:8080/swagger-ui.html** once the app is
running. Summary of endpoints:

| Method | Endpoint             | Description                                      |
|--------|----------------------|---------------------------------------------------|
| POST   | `/api/transcribe`    | Upload an audio file (`multipart/form-data`), returns transcript |
| POST   | `/api/summarize`     | Generate Markdown meeting minutes from transcript text |
| POST   | `/api/meetings`      | Save a meeting (transcript + minutes) to history  |
| GET    | `/api/meetings`      | List all saved meetings                            |
| GET    | `/api/meetings/{id}` | Get a single saved meeting                         |
| DELETE | `/api/meetings/{id}` | Delete a saved meeting                             |

The H2 database console is available at **http://localhost:8080/h2-console**
(JDBC URL: `jdbc:h2:file:./data/meetingsdb`).

## ⚙️ Configuration

All configuration lives in `src/main/resources/application.properties`:

| Property                | Default            | Description                                  |
|-------------------------|---------------------|-----------------------------------------------|
| `openai.api-key`         | `${OPENAI_API_KEY}` | Your OpenAI API key                           |
| `openai.model.whisper`   | `whisper-1`          | Whisper model used for transcription          |
| `openai.model.chat`      | `gpt-4o-mini`        | Chat model used for generating minutes        |
| `spring.datasource.url`  | `jdbc:h2:file:./data/meetingsdb` | H2 database file location    |
| `spring.servlet.multipart.max-file-size` | `100MB` | Max uploaded audio file size |

## 🗺️ Roadmap Ideas

- User accounts & authentication (Spring Security + JWT)
- Speaker diarization
- Real-time streaming transcription over WebSockets
- Export to Word/Notion in addition to PDF

## 📜 License

MIT — see [LICENSE](LICENSE).

---

*Originally prototyped in Python (Flask + faster-whisper + a local Qwen LLM); rebuilt from the
ground up in Java and Spring Boot with a persistent history feature, layered service architecture,
and a graceful offline fallback for summarization.*
