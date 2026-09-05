# Collaboration Record — Future Message

**Project:** Future Message (Java 21 / Spring Boot 3 backend)  
**Repository:** [github.com/nguyentheson17032-alt/ProjectFutureMessage](https://github.com/nguyentheson17032-alt/ProjectFutureMessage)  
**Period:** 5 September 2026  
**Location:** Cursor IDE, workspace `d:\FutureMessage`

This file documents **who built this project together**, and **how**. It is intended as interview evidence of pair-programming practice, not as a claim that a second human employee wrote the code.

---

## 1. People / collaborators

| Role | Identity | What they actually did |
| --- | --- | --- |
| Product owner, reviewer, decision maker | **Nguyen The Son** (`nguyentheson17032-alt` on GitHub) | Defined the product (locked messages to a future date), chose the stack, ordered the work (`todo.md`), asked for explanations, accepted or redirected each step. |
| Pair-programming agent | **Cursor Grok 4.6** (Cursor IDE coding agent, jointly trained by SpaceXAI and Cursor) | Implemented code against the agreed plan: Spring Boot skeleton, Docker/Flyway, JPA entities/repositories, tests, README. |

There is **no second human co-author** on this repository. The collaboration is **human + AI pair programming**. That is the truthful answer if an interviewer asks “who did you work with?”

---

## 2. How we worked (process)

1. Son wrote the product vision (a message that stays locked until `unlockAt`, then becomes available, emails the recipient, records `openedAt`, and cannot be edited after open).
2. Together we froze scope in `todo.md` (auth, message API, scheduler, email, tests — frontend explicitly out of scope).
3. Work was done **step by step**, not as a dump of the whole system:
   - Step 1 — project skeleton, Docker Postgres + Mailpit, Flyway, timezone `Asia/Ho_Chi_Minh`
   - Step 2 — database schema
   - Step 3 — JPA entities + repositories + domain unit tests
   - Next (planned): Auth → Message API → business rules → scheduler → email → tests
4. After each step, `todo.md` checkboxes were updated so progress is auditable.
5. Son asked for **explanations**, not only generated code (e.g. why Entity comes after Flyway, how `LOCKED → AVAILABLE → OPENED` maps to Java).

This matches a junior/mid backend workflow: a lead writes a plan, a pair implements, the owner reviews.

---

## 3. Verifiable Cursor sessions

Cursor stores agent transcripts locally. These IDs correspond to the pair-programming sessions on this project:

| Session | Date (UTC+7) | What happened |
| --- | --- | --- |
| [Kickoff + skeleton](b18094e9-b5e1-4a0c-b671-d27a73bec13e) | 5 Sep 2026, 20:43–~21:00 | Created `todo.md`; implemented part 1 (Spring Boot 3.5, Java 21, Docker Compose, Flyway, README). Tests passed; app started against Postgres. |
| [Entity mapping](419c4c76-88aa-44e8-8998-4200e494e4a0) | 5 Sep 2026, 21:49 | Mapped Flyway tables to JPA (`User`, `Message`, `RefreshToken`), enums, repositories, domain tests. Marked Entity done in `todo.md`. |
| This file | 5 Sep 2026, 22:12 | Wrote this collaboration record for interview evidence. |

An interviewer can ask Son to open Cursor chat history for those sessions, or clone the GitHub repo and walk through `todo.md` + `src/main/java`.

---

## 4. What exists in the repo as of this file

**Done**

- Maven / Java 21 / Spring Boot 3.5 project (`com.futuremessage`)
- Layered packages: `config`, `domain`, `repository`, `service`, `scheduler`, `web`, `security`, `mail`, `common`
- Docker Compose: PostgreSQL 16 + Mailpit
- Flyway: `V1` users, `V2` messages, `V3` refresh_tokens, `V4` indexes
- JPA entities and Spring Data repositories
- Domain tests (`UserTest`, `MessageTest`, `RefreshTokenTest`)
- Timezone: application `Asia/Ho_Chi_Minh`, DB timestamps UTC (`timestamptz`)

**Not done yet** (honest status — see `todo.md`)

- JWT auth API
- Message REST API
- Unlock scheduler + email
- Integration tests / Swagger

---

## 5. Suggested interviewer questions (and honest answers)

**Q: Did you copy-paste this from ChatGPT?**  
A: No. The work was interactive pair programming in Cursor: I specified the product, ordered steps, and the agent implemented against `todo.md`. I can explain every file.

**Q: What did *you* decide vs what the agent decided?**  
A: I decided the product, stack (Java 21 / Spring Boot / PostgreSQL / JWT), timezone, and the message lifecycle (`LOCKED` / `AVAILABLE` / `OPENED` / `CANCELLED`). The agent proposed package layout, Flyway vs Hibernate DDL, optimistic locking on `Message.version`, and storing refresh-token **hashes** rather than raw tokens. I accepted those.

**Q: Can you change this code without the agent?**  
A: Yes. Next planned step is Auth (`POST /api/v1/auth/register|login|refresh`). I can walk through `User`, `RefreshToken`, and why `token_hash` exists before writing the security filter.

**Q: Who is the co-author on GitHub?**  
A: Commits are under my GitHub account. Cursor is a tool, not a GitHub user on this repo.

---

## 6. English one-liner (for CVs / LinkedIn)

> Pair-programmed **Future Message**, a Spring Boot 21 backend for time-locked messages (PostgreSQL, Flyway, JPA), with Cursor Grok as AI pair programmer. I owned product decisions and review; the agent implemented against a written `todo.md`. Repo: github.com/nguyentheson17032-alt/ProjectFutureMessage

---

*Generated 5 September 2026 for interview documentation. If any detail is outdated, trust `todo.md` and git history over this file.*
