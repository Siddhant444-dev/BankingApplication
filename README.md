# SecureSearch Cloud

**SecureSearch Cloud** is an educational, full-stack prototype for searching encrypted PDF documents. It combines a Spring Boot REST API, a browser-based cryptography client, persistent storage, and a repeatable synthetic benchmark. The browser encrypts document bytes and filenames before upload, derives deterministic HMAC-SHA-256 keyword tokens locally, and sends only those opaque tokens for search.

> **Research prototype—not production-ready.** The API is a single-user, unauthenticated demo; do not expose it publicly or upload sensitive real documents. Searchable encryption intentionally leaks equality/repeated-query and access/result patterns. See [Security model and limitations](#security-model-and-limitations).

The repository's original banking API is retained. SecureSearch is the default landing experience; banking endpoints continue to use the existing JWT security rules.

## Features

- Browser-side **AES-256-GCM** encryption of PDF contents and filenames, each with a fresh 96-bit nonce.
- Client-side key derivation using **PBKDF2-HMAC-SHA-256** (310,000 iterations; 128-bit workspace salt), with separate 256-bit AES and HMAC keys.
- A blind keyword index containing fixed-length HMAC tokens rather than plaintext words; search uses exact token lookup and **AND semantics** for all query words.
- Encrypted downloads are decrypted only in the browser.
- An interactive benchmark comparing plaintext and HMAC-indexed in-memory lookup across 1,000, 10,000, and 50,000 synthetic documents.
- Local H2 development storage and a PostgreSQL Docker Compose configuration.
- Spring Boot REST endpoints with input validation and tests.

## Technology

- Java 21, Spring Boot 3.5, Spring Web, Spring Data JPA and Spring Security
- H2 for local development; PostgreSQL 16 for the Compose environment
- Browser Web Crypto API (vanilla JavaScript frontend; no frontend build step)
- Maven Wrapper

The original project also retains its MySQL driver, banking services, JWT authentication, mail support, and existing endpoints.

## Run locally

### Prerequisites

- JDK 21 (not only a Java runtime)
- Internet access on the first Maven build so the wrapper can download Maven and dependencies

```bash
git clone https://github.com/Siddhant444-dev/BankingApplication.git
cd BankingApplication
chmod +x mvnw
./mvnw test
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). Local development uses a persistent H2 database at `./data/securesearch` by default. The first visit prompts you to set a passphrase of at least 12 characters. The workspace salt and a passphrase verifier are stored in this browser's local storage; the passphrase itself is not stored or sent to the server.

**Keep the same browser profile and do not clear site storage:** the salt is not stored on the server, and there is no key recovery or salt migration yet. Losing it means losing access to files encrypted under that workspace.

### PostgreSQL with Docker Compose

Set a unique database password and JWT secret in your deployment environment (or a local `.env` file ignored by Git), then run:

```bash
export POSTGRES_PASSWORD='replace-with-a-long-random-password'
export JWT_SECRET="$(openssl rand -base64 32)"
docker compose up --build
```

The application is available at `http://localhost:8080`. Compose stores database state in the `securesearch-db` volume. `JWT_SECRET` protects the retained banking API; it is separate from the client-side SecureSearch keys. The default Compose values are development placeholders and must be overridden before any shared deployment. Configure HTTPS/TLS at a trusted reverse proxy for anything beyond local testing.

### Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DATABASE_URL` | `jdbc:h2:file:./data/securesearch;DB_CLOSE_ON_EXIT=FALSE` | JDBC connection URL |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `sa` / empty | Database login |
| `DATABASE_DRIVER` | `org.h2.Driver` | JDBC driver class; use `org.postgresql.Driver` with PostgreSQL |
| `JWT_SECRET` | Development-only sample | Base64 HMAC signing key for the original banking API |
| `JWT_EXPIRATION` | `86400000` | Banking JWT lifetime in milliseconds |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Gmail SMTP defaults / empty credentials | Existing banking email feature |

## Demo walkthrough

1. Open the app in one browser profile and enter a strong passphrase (12+ characters; a randomly generated phrase is recommended).
2. Choose a PDF no larger than 10 MiB. Add comma-separated tags such as `policy, renewal, 2025` and upload.
3. Observe the encrypted filename and content being stored, and the derived token(s) being indexed. The server does not parse the PDF or receive plaintext tags.
4. Search for one or more tags. Search is case-normalized and uses AND semantics; every term must match for a document to appear.
5. Download a result. The browser fetches ciphertext and decrypts it locally.
6. Run **Search performance** to compare synthetic lookup latency and estimated key/value storage at three scales.
7. Open **Privacy model** to review the intended leakage and limitations.

The tags are user-provided. PDF text extraction is deliberately not performed: doing so on the server would expose plaintext to the storage/search service. A production client could extract locally with PDF.js before the terms are submitted to the browser-side tokenization function.

## API

All SecureSearch routes are under `/api/secure-search` and are permitted by the existing Spring security configuration for this local demo. The static client uses them directly.

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/api/secure-search/documents` | Save AES-GCM ciphertext, encrypted name and opaque keyword tokens |
| `GET` | `/api/secure-search/documents` | List encrypted summaries |
| `POST` | `/api/secure-search/search` | Search by one or more 64-character HMAC hex tokens (AND match) |
| `GET` | `/api/secure-search/documents/{id}` | Retrieve ciphertext and encrypted metadata |
| `DELETE` | `/api/secure-search/documents/{id}` | Delete a document and its index entries |
| `POST` | `/api/benchmarks/run` | Run synthetic in-memory lookup and estimated-storage comparison |

Example encrypted upload shape (values are illustrative and must be valid base64 / tokens):

```json
{
  "encryptedName": "BASE64_AES_GCM_CIPHERTEXT",
  "nameIv": "BASE64_12_BYTE_NONCE",
  "contentIv": "BASE64_12_BYTE_NONCE",
  "encryptedContent": "BASE64_AES_GCM_CIPHERTEXT",
  "mediaType": "application/pdf",
  "sizeBytes": 12345,
  "keywordTokens": ["64_HEX_CHAR_HMAC_SHA256_TOKEN"]
}
```

Search accepts only the tokens; it has no plaintext query field:

```json
{ "tokens": ["64_HEX_CHAR_HMAC_SHA256_TOKEN"] }
```

## Security model and limitations

### What is protected

- PDF bytes and filenames are AES-GCM encrypted in the browser. The API stores base64 ciphertext and nonces, not plaintext document data.
- Search terms are lowercased/normalized in the browser and HMACed with a key derived from the passphrase. The backend validates fixed-length hex tokens and performs exact index lookups; it does not receive the original terms or the HMAC key.
- Random nonces are generated independently for every encrypted name and PDF.

### What leaks / what is not implemented

- **Search-pattern leakage:** deterministic HMAC tokens let the server recognize repeated searches for the same term. The mapping from opaque tokens to actual words is not directly revealed, but frequency analysis may help an attacker guess common terms.
- **Access-pattern leakage:** the server sees which encrypted document IDs match a token set, result counts, timing, and request cadence.
- **Metadata leakage:** upload sizes, approximate document lengths, and timestamps are visible. The prototype does not pad ciphertext, batch results, or hide network metadata.
- **No tenant/account authorization on SecureSearch routes:** anyone who can reach this demo API can list encrypted records, submit uploads, or delete records. Client-side encryption protects contents only while the key remains secret; it does not prevent denial of service or metadata observation. Do not expose this service publicly.
- **Single-browser key management:** local storage holds a random salt and a verifier; there is no salt backup, multi-device sync, password reset, or key rotation. A wrong passphrase cannot decrypt existing content.
- **User-supplied keywords:** indexing is limited to manually entered tags. Browser-side PDF text extraction, robust tokenization/stop-word policy, and multi-language normalization are future work.
- **Prototype crypto composition:** the implementation demonstrates a simple client-side SSE pattern, not a reviewed or formally proven SSE construction. No forward privacy, oblivious RAM, public-key SSE, or formal leakage simulation is claimed.
- **Benchmark scope:** the benchmark is synthetic, in-memory, single-process and precomputed. It times equivalent HashMap lookups after query tokens have already been prepared. Storage values are byte estimates. It excludes database/network costs, real-world concurrency, token-generation setup, PDF parsing, and end-to-end browser latency. It must not be presented as a production performance claim.

For research comparisons, document the leakage profile and benchmark configuration alongside every result. A production system would need a reviewed SSE design, authenticated tenant-scoped APIs, TLS, stronger key lifecycle and recovery, abuse controls, database/network measurements, and independent security review.

## Tests

```bash
./mvnw test
```

The test suite runs against an in-memory H2 database and exercises the public SecureSearch API, including upload, exact blind-token matching, encrypted retrieval, and deletion.

## Retained banking API

Existing banking endpoints and JWT filtering remain in the project under `/api/user` and `/bankStatement`. They are separate from the SecureSearch prototype and still require appropriate database, mail, and account setup for meaningful operation. Their API documentation remains at `/swagger-ui/index.html`.

## Project structure

```text
src/main/java/com/siddhant/BankingApplication/securesearch/  SecureSearch API, entities and benchmark
src/main/resources/static/                                  Browser UI and Web Crypto client
src/main/resources/application.properties                   Environment-driven local configuration
src/test/java/...                                           Spring integration tests
compose.yaml                                                 PostgreSQL + application
```
