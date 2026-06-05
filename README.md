# Plugin Platform

AI platform for Lutece 8. Conversational bots, orchestration pipelines, RAG and observability.

## Features

- **Bots** — conversational agents with SSE streaming, RAG, memory and tools ( pipelines / agentic rag / mcp ).
- **Pipelines** — node graph orchestration, async execution, triggers, REST execution API (inputs/outputs, status, SSE events).
- **RAG** — semantic search, embeddings, source citations.
- **Datasets** — folders, document upload and ingestion for semantic search.
- **Models** — configurable LLM providers (Mistral, Azure OpenAI, Anthropic); chat completions, embeddings and streaming exposed over REST.
- **Vision / OCR** — extractors and fields for text extraction from documents and images.
- **Conversations** — history, listing, rating and message feedback.
- **MCP** — tool exposure via Model Context Protocol.
- **Decision trees** — configurable decision trees (start + node navigation API).
- **Observability** — execution traces, per-node details, costs, real-time SSE streams; client and global views.
- **Clients & API keys** — multi-client isolation, one-shot API key display.
- **Subscriptions** — user subscriptions to resources, quotas (rate limiting).
- **RBAC** — per-resource permissions on the 8 resource types, enforced in front office and REST.

## Concurrency

Three isolated pools, exposed as CDI qualifiers. Values live in the site's `server.xml`.

| Qualifier | Usage | Backing |
|-----------|-------|---------|
| `@BlockingIO` | blocking I/O (LLM, embeddings) | bounded platform pool (EE 10 / OL 25) — virtual threads on EE 11 |
| `@Orchestration` | node dispatch, CPU-bound | platform pool |
| `@Scheduler` | periodic tasks | platform pool |

## Build

```bash
# Compile only
mvn clean install -Dmaven.test.skip=true

# Full build with tests
mvn clean lutece:exploded antrun:run -Dlutece-test-hsql test -q
```