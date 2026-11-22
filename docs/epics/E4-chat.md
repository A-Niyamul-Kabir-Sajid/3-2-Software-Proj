# E4 — Group Chat & Notifications

| Field | Value |
|---|---|
| Status | **Not done** |
| Owner | Dev3 |
| Reviewer | Dev1 |
| Branch | `feature/E4-chat` |
| Depends on | E1 (JWT, User). The Observer story (S4) needs E2/E3 merged, or it can stub the event publishers |
| Blocks | none |
| Rubric targets | 5th+ controller, table `chat_messages`; patterns Mediator, Observer; 3 unit + 2 IT |

## Goal
A real-time class group chat over WebSocket (STOMP) with persisted history, plus live notifications whenever a new notice or assignment is published.

## Stories

### E4-S1 Entity
`chat.entity.ChatMessage`: `sender` (`@ManyToOne User`, not null), `content` (≤ 2000, not blank), `sentAt` (`Instant`), `deleted` (boolean soft delete). Table `chat_messages`. Index on `sent_at`.

### E4-S2 WebSocket + Mediator
- `chat.config.WebSocketConfig` (`@EnableWebSocketMessageBroker`): endpoint `/ws` (with `setAllowedOriginPatterns` from `app.cors.allowed-origin`, plus SockJS fallback), simple broker `/topic`, app prefix `/app`.
- `chat.config.StompAuthChannelInterceptor`: on `CONNECT`, it reads the `Authorization: Bearer <jwt>` native header, validates it with E1's `JwtService` and sets the user `Principal`. An invalid or missing token rejects the connection.
- **Mediator pattern**: `chat.mediator.ChatMediator` interface (`void send(User sender, String content)`, `void join(User)`, `void leave(User)`), implemented by `ClassChatRoom`. It:
  1. validates the content (not blank, ≤ 2000, trimmed),
  2. persists a `ChatMessage`,
  3. broadcasts a `ChatMessageResponse{id, sender{id,name,role}, content, sentAt}` to `/topic/class` via `SimpMessagingTemplate`,
  4. tracks online users (a `ConcurrentHashMap`) and broadcasts `/topic/presence` `{online:[{id,name}]}` on join and leave (`SessionConnectedEvent` / `SessionDisconnectEvent` listeners).
- `ChatWsController`: `@MessageMapping("/chat.send")` receives `{content}` and calls `mediator.send(currentUser, content)`. Participants never talk to each other directly; everything goes through the mediator.

### E4-S3 REST history
`ChatController` at `/api/v1/chat`:
| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/chat/messages?before=<ISO instant>&size=30` | any authenticated | newest first, cursor pagination, excludes deleted |
| DELETE | `/chat/messages/{id}` | sender or ADMIN | soft delete → 204; broadcasts `/topic/class` `{type:"DELETED", id}` |
| GET | `/chat/online` | any | the current online users |

### E4-S4 Observer: live notifications
- Spring events act as the **Observer** pattern. `common.event.ContentPublishedEvent{type: NOTICE|ASSIGNMENT|FORM, id, title, authorName}` is published by `NoticeService` (E2) and `AssignmentService.publish` / `FormService.create` (E3) via `ApplicationEventPublisher`.
- `chat.notify.WebSocketNotificationListener` (`@EventListener`, `@Async` optional) broadcasts to `/topic/notifications`.
- Add an `interface NotificationChannel { void notify(ContentPublishedEvent e); }` with `WebSocketChannel`, and a `LoggingChannel` decorator (optional, Decorator pattern) that logs and then delegates.
- If E2/E3 are not merged yet, add only the publisher calls in a follow-up PR once they are.

### E4-S5 Frontend
- Add the STOMP client from a CDN with a pinned version, e.g. `https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js`, and include it in the `sw.js` cache.
- Chat page: load history from `/chat/messages`, connect with the JWT in the connect headers, subscribe to `/topic/class`, send to `/app/chat.send`, show the online count from `/topic/presence`, and offer a delete button on your own messages.
- A global subscription to `/topic/notifications` shows a toast ("New assignment: …").
- Offline: disable the input and show "Chat unavailable offline"; history is shown from the last cache.
- Remove `chat` from `data.js`.

### E4-S6 Tests (quota: 3 unit + 2 integration)
Unit tests:
1. `ClassChatRoomTest.send_persistsAndBroadcasts` (mock repository + `SimpMessagingTemplate`; verify `convertAndSend("/topic/class", …)`)
2. `ClassChatRoomTest.send_blankContent_throwsBadRequest`
3. `ChatServiceTest.delete_byNonSender_throwsForbidden`
4. `WebSocketNotificationListenerTest.onEvent_broadcastsToNotificationsTopic`

Integration tests:
1. `ChatControllerIT.history_withoutToken_returns401`
2. `ChatControllerIT.history_returnsNewestFirst`
3. (optional) `ChatWebSocketIT`: connect with `WebSocketStompClient` on a random port, send a message and receive it on `/topic/class`

## Definition of Done
- [ ] Two browsers logged in as different users chat in real time; history survives a reload
- [ ] An unauthenticated WebSocket connect is rejected
- [ ] Publishing a notice or assignment shows a toast to other users
- [ ] ≥ 3 unit + 2 IT green; `docs/agent.md` updated
