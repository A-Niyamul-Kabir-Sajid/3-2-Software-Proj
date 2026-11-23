# E5 — AI (RAG) Assistant

| Field | Value |
|---|---|
| Status | **Not done** |
| Owner | Dev3 |
| Reviewer | Dev1 |
| Branch | `feature/E5-rag-assistant` |
| Depends on | E1, E2, E3 (it reads routine, notices and assignments, and acts on the routine) |
| Blocks | none |
| Rubric targets | patterns Adapter, Facade, Command, State; tables `assistant_actions`; 5 unit + 2 IT |

## Goal
A user writes **one prompt**. The assistant either:
1. **answers a question** using class data (retrieval-augmented), citing its sources, or
2. **proposes a change** (e.g. "move Sunday CSE 3201 to 10:00"). It shows a preview and applies the change **only after the CR/Teacher confirms**. If the prompt is unclear, it asks a clarifying question instead of guessing.

Students may only ask questions; action prompts from a student get the answer "Only CR or teachers can make changes."

**Keep it small and deterministic.** Tests use a fake LLM, so CI never calls a real API, and the app must also work when no API key is set (fallback mode).

## Stories

### E5-S1 LLM Adapter
- `rag.llm.LlmClient` interface: `String complete(String systemPrompt, String userPrompt)`.
- **Adapter pattern**: `GeminiLlmAdapter` (or `OpenAiLlmAdapter`) wraps the provider's HTTP API using Spring `RestClient`. It adapts the provider's request/response JSON to `LlmClient`. The key comes from env `LLM_API_KEY`, the model from `LLM_MODEL`, and the base URL from `LLM_BASE_URL`.
- `FallbackLlmClient`: used when `LLM_API_KEY` is empty. It returns a template answer built from the top retrieved documents ("Here is what I found: …").
- Selected via `@ConditionalOnProperty` / a config bean.
- Timeouts: connect 5 s, read 20 s. Provider errors throw a new `common.exception.ServiceUnavailableException("Assistant temporarily unavailable")`, which `GlobalExceptionHandler` maps to **503**.

### E5-S2 Retrieval
- `rag.retrieval.Document{sourceType: NOTICE|ASSIGNMENT|ROUTINE, sourceId, title, text}`.
- `DocumentCollector` builds documents from the DB on demand: each notice, each PUBLISHED assignment (with subject and due date), and one document per routine day.
- `Retriever` interface with `List<ScoredDocument> top(String query, int k)`. Implement `KeywordRetriever` (lower-case tokenising, stop-word removal, TF-IDF-like scoring). This is simple, deterministic and needs no vector DB. The interface leaves room for an embedding retriever later.

### E5-S3 Ask (Facade)
**Facade**: `RagFacade.ask(User user, String prompt)` hides intent detection, retrieval, prompt building, the LLM call and response mapping.

`AssistantController` at `/api/v1/assistant`:
| Method | Path | Role | Response |
|---|---|---|---|
| POST | `/assistant/ask` | any authenticated | `{prompt}` → 200 `AssistantReply` |
| POST | `/assistant/actions/{id}/confirm` | CR, TEACHER (the creator of the action) | 200 `{status:"APPLIED", result}` |
| POST | `/assistant/actions/{id}/cancel` | creator | 200 `{status:"CANCELLED"}` |
| POST | `/assistant/actions/{id}/undo` | CR, TEACHER | 200 `{status:"UNDONE"}`; only for APPLIED actions |
| GET | `/assistant/actions?status=PENDING` | CR, TEACHER | the user's actions |

`AssistantReply`:
```json
{ "type": "ANSWER | CLARIFY | CONFIRM",
  "message": "…",
  "sources": [{"type":"NOTICE","id":3,"title":"Quiz postponed"}],
  "action": { "id": 7, "summary": "Move CSE 3201 on SUNDAY 08:00 → 10:00", "before": {...}, "after": {...} } }
```

### E5-S4 Actions: Command + State
**Intent parsing** (`rag.intent.IntentParser`): a rule-based parser first (regex for "move/shift/change … <subject code> … <day> … to <time>" and "post notice …"). If an LLM is configured, it may ask the LLM to return JSON `{intent, subjectCode, day, fromTime, toTime, title, body}`, then validate that JSON strictly.
- Missing or ambiguous fields (e.g. the subject has 2 slots that day and no time was given) produce `type: CLARIFY`, with a question listing the options.

**Command pattern** in `rag.command`:
```
AssistantCommand (interface): String summary(); Object preview(); Object execute(); void undo();
  UpdateRoutineSlotCommand  (uses E2 RoutineService; undo restores old day/time)
  CreateNoticeCommand       (uses E2 NoticeService; undo deletes the created notice)
CommandFactory.from(ParsedIntent, User) -> AssistantCommand
```

**State pattern** for an action's lifecycle. `AssistantAction` entity: `createdBy`, `prompt`, `commandType`, `payloadJson`, `status`, `resultJson`. Table `assistant_actions`.
```
ActionStatus: PENDING -> APPLIED | CANCELLED ; APPLIED -> UNDONE
ActionState implementations: PendingState(confirm, cancel OK), AppliedState(undo OK), CancelledState / UndoneState (everything -> 409)
```
The flow is: ask → parse → command → save PENDING → reply CONFIRM. On confirm, rebuild the command from the payload → `execute()` in a transaction → APPLIED.

### E5-S5 Frontend
- The assistant page uses `Api.post('/assistant/ask')`.
- `ANSWER` renders text plus source chips that link to the notice or assignment.
- `CLARIFY` renders the question; the user replies in the same input.
- `CONFIRM` renders a card with the summary, a before→after diff and **Confirm / Cancel** buttons. After confirming it shows "Applied", with an Undo button.
- Remove the mock AI from `app.js` and `data.js`.

### E5-S6 Tests (quota: 5 unit + 2 integration)
Use `FakeLlmClient` (a test bean that returns canned strings) everywhere.

Unit tests:
1. `KeywordRetrieverTest.top_returnsMostRelevantNoticeFirst`
2. `IntentParserTest.movePrompt_parsesSubjectDayAndTimes`
3. `IntentParserTest.ambiguousPrompt_returnsClarify`
4. `UpdateRoutineSlotCommandTest.execute_thenUndo_restoresOriginalTime`
5. `ActionStateTest.cancelled_confirm_throwsConflict`
6. `RagFacadeTest.studentActionPrompt_isRefused`

Integration tests:
1. `AssistantControllerIT.ask_question_returnsAnswerWithSources`
2. `AssistantControllerIT.movePrompt_confirm_updatesRoutine`
3. `AssistantControllerIT.confirm_asStudent_returns403`

## Definition of Done
- [ ] Q&A works with no API key (fallback) and with a key
- [ ] CR: "move Sunday CSE 3201 to 10:00" → preview → Confirm → the routine changes → Undo restores it
- [ ] An unclear prompt gets a clarifying question; a student cannot trigger actions
- [ ] No API keys in code; `LLM_API_KEY` is documented in `.env.example`
- [ ] ≥ 5 unit + 2 IT green; `docs/agent.md` updated
