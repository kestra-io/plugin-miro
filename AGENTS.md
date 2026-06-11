# Kestra Miro Plugin

## What

- Provides plugin components under `io.kestra.plugin.miro`.
- Task and trigger subpackage: `boards`.

## Why

- Teams need to manage Miro boards programmatically from orchestrated workflows without custom Script tasks.
- The `boards` package covers the core Miro board lifecycle: list, get, create, update, delete, copy, and poll for new boards.

## How

### Architecture

Single-module plugin built on Kestra's internal HTTP client. Source packages under `io.kestra.plugin`:

- `miro.boards`

`AbstractMiroConnection` holds the shared `token` property and HTTP helper methods (bearer auth, JSON body serialization). All tasks and the trigger extend it.

Infrastructure dependencies (Docker Compose services):

- `app`

### Key Plugin Classes

- `io.kestra.plugin.miro.boards.AbstractMiroConnection`
- `io.kestra.plugin.miro.boards.List`
- `io.kestra.plugin.miro.boards.Get`
- `io.kestra.plugin.miro.boards.Create`
- `io.kestra.plugin.miro.boards.Update`
- `io.kestra.plugin.miro.boards.Delete`
- `io.kestra.plugin.miro.boards.Copy`
- `io.kestra.plugin.miro.boards.Trigger`

### Project Structure

```
plugin-miro/
├── src/main/java/io/kestra/plugin/miro/
│   ├── package-info.java
│   └── boards/
│       ├── AbstractMiroConnection.java
│       ├── BoardResponse.java
│       ├── BoardListResponse.java
│       ├── List.java
│       ├── Get.java
│       ├── Create.java
│       ├── Update.java
│       ├── Delete.java
│       ├── Copy.java
│       ├── Trigger.java
│       └── package-info.java
├── src/test/java/io/kestra/plugin/miro/boards/
│   └── BoardsTest.java   (WireMock-backed unit tests)
├── src/main/resources/
│   ├── icons/plugin-icon.svg
│   ├── icons/io.kestra.plugin.miro.boards.svg
│   └── metadata/
│       ├── index.yaml
│       └── boards.yaml
├── build.gradle
└── README.md
```

### Testing

WireMock-backed unit tests (`BoardsTest`) cover the happy path and failure scenarios for all 6 tasks. One integration test is `@Disabled` and requires `MIRO_TOKEN` and `MIRO_TEAM_ID` env vars.

## References

- https://developers.miro.com/docs/miro-rest-api-introduction
- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
