# AGENTS.md

## tiAuth At A Glance

tiAuth is a Minecraft authentication plugin for **BungeeCord** and **Velocity**.
Authentication features include registration, login, password changes, premium mode, sessions, two-factor authentication, database migration, and a PicoLimbo-based virtual auth server.

When changing behavior:

- keep platform-specific code in the owning platform module
- prefer extending shared code in `common` over duplicating logic in `bungee` and `velocity`
- keep public API changes deliberate, because `bungee/api` and `velocity/api` are consumed by plugin code

---

## Repository Orientation

### Module Map

- `common`
  Shared runtime logic, configuration, database access, hashing, caches, utilities, and PicoLimbo integration.
- `bungee`
  BungeeCord bootstrap, commands, listeners, managers, and Bungee-specific API classes.
- `velocity`
  Velocity bootstrap, commands, listeners, managers, and Velocity-specific API classes.
- `build.gradle.kts`
  Root dependency setup, shading, relocation, and Java version configuration.
- `README.md`
  User-facing feature and command summary.

### Where To Look First

- For shared config, database, hash, cache, or utility behavior, start in `common`.
- For BungeeCord command, listener, or bootstrap behavior, start in `bungee`.
- For Velocity command, listener, or bootstrap behavior, start in `velocity`.
- For build and packaging behavior, check `build.gradle.kts`.
- For user-visible command or feature changes, update `README.md` in the same work when needed.

---

## Architecture Rules

### Module Boundaries

- Keep logic in the narrowest owning module.
- Avoid copying the same behavioral change into both platform modules when the shared code can own it.
- Keep shared domain behavior in `common`; keep Bungee and Velocity wiring thin.
- Do not add platform-only dependencies to `common` unless the code already depends on them there.

### Public API Surfaces

- Treat `bungee/api` and `velocity/api` as public API surfaces.
- API changes are compatibility-sensitive.
- Public API classes and methods should have clear Javadocs when they are intended for external use.

### Existing Patterns First

- Follow neighboring package, naming, and registration patterns before introducing a new style.
- Reuse the existing command, listener, manager, cache, and repository conventions already present in the repo.
- Prefer small edits that fit the existing structure over broad cleanup unless cleanup is the actual task.

---

## Runtime And Library Conventions

### Messaging And Text

- Use the existing Adventure and MiniMessage patterns already used in the project.
- Avoid mixing unrelated text systems or ad-hoc formatting when an existing serializer path already exists.

### Dependencies

- Use Java 21-compatible code.
- Prefer the versions already declared in `build.gradle.kts`.
- Keep dependencies in the narrowest module that actually needs them.
- Be careful with shaded or relocated libraries, since the root build already relocates several packages.

---

## Build, Test, And Validation

### Common Commands

- `./gradlew test`
- `./gradlew build`

### Verification Guidance

- Prefer targeted verification for the module you changed before running broader checks.
- Add or update tests when behavior changes and the codebase already has a good place for them.
- If a change affects database access, hashing, or authentication flow, validate the affected path more carefully than a simple compile.

---

## Working Style

- Keep changes scoped to the module that owns the behavior.
- Match the surrounding implementation style instead of reworking unrelated code.
- If a fix affects both platforms, make sure the shared logic is correct first and then wire each platform to it cleanly.
