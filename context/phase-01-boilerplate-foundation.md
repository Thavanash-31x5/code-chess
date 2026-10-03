# Phase 1: Boilerplate/Foundation

## Purpose

This phase established the technical foundation for a new web application. The application/product idea and business logic were intentionally not defined. The phase was limited to environment verification, Gradle configuration, a plain Java/JDBC foundation, minimal frontend files, and a successful project build.

Authentication, registration, application-specific business logic, and the user database schema were explicitly deferred.

## Starting State

The workspace was initially empty. No existing application or business logic was confirmed at the start of the implementation conversation. The fixed target architecture was:

```text
HTML/CSS/Vanilla JavaScript
            -> Plain Java backend
            -> JDBC
            -> MySQL

Build tool: Gradle
```

## Fixed Architecture and Restrictions

The project uses HTML, CSS, Vanilla JavaScript, Java, JDBC, MySQL, and Gradle only.

The implementation conversation explicitly prohibited frameworks and unnecessary dependencies, including Spring, Spring Boot, Spring MVC, Hibernate, JPA, Jakarta EE frameworks, React, Angular, Vue, Node.js, Express, Bootstrap, Tailwind, ORM frameworks, authentication frameworks, and third-party web frameworks.

No HTTP server was introduced because the application purpose and server requirement had not yet been decided.

## Environment

The following versions were supplied by the user during the environment checks:

```text
JDK: OpenJDK 21.0.12.1
Gradle: 9.8.0
MySQL client: 8.0.46
Operating system: Ubuntu/Linux x86_64
```

The Gradle output reported Linux kernel `7.0.0-31-generic` and Java home `/usr/lib/jvm/java-21-openjdk-amd64`.

## Gradle Configuration

The current repository contains:

- `settings.gradle`
- `build.gradle`

The current `settings.gradle` is:

```groovy
rootProject.name = "plain-java-application"
```

The current `build.gradle` applies the Java plugin, configures a Java 21 toolchain, uses Maven Central, and declares MySQL Connector/J as a runtime-only dependency:

```groovy
plugins {
    id 'java'
}

java {
    toolchain{
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories{
    mavenCentral()
}

dependencies{
    runtimeOnly 'com.mysql:mysql-connector-j:8.4.0'
}
```

The dependency coordinate is:

```text
com.mysql:mysql-connector-j:8.4.0
```

The `runtimeOnly` scope supplies the official MySQL JDBC driver for runtime connectivity without adding a framework or ORM.

## Java Foundation

The current repository contains:

```text
src/main/java/app/database/DatabaseConfig.java
src/main/java/app/database/JdbcConnectionFactory.java
```

`DatabaseConfig` is a Java record containing `url`, `username`, and `password`. Its `fromEnvironment()` factory reads the following environment variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Missing or blank variables cause an `IllegalStateException`. Database secrets are not hardcoded in the source.

`JdbcConnectionFactory` imports `java.sql.Connection`, `java.sql.DriverManager`, and `java.sql.SQLException`. Its static connection method passes the configured URL, username, and password to `DriverManager.getConnection(...)` and returns a JDBC `Connection`. The current repository method is named `openCOnnection` with an uppercase `O` in `COnnection`.

## Frontend Foundation

The current repository contains:

```text
src/main/resources/static/index.html
src/main/resources/static/styles.css
src/main/resources/static/app.js
```

`index.html` provides a minimal HTML document titled `Application Foundation`, links `styles.css`, displays a placeholder heading and paragraph, and loads `app.js`.

`styles.css` defines a small global foundation: a sans-serif font, text and background colors, `box-sizing: border-box`, a minimum body width, and constrained main-content spacing. The current source contains `width:min(100% -2rem , 60rem)`, which has not been browser-tested.

`app.js` enables strict mode and adds `data-js="enabled"` to the root HTML element after `DOMContentLoaded`.

No complete UI or product-specific interaction exists.

## Project-Structure Decision

The conversation considered separating the application into top-level folders:

```text
frontend/
backend/
database/
```

A proposed version would have placed Java under `backend/src/main/java`, frontend files under `frontend`, and database documentation under `database`. This approach was not continued. The user explicitly chose to return to the regular Gradle layout.

The final application layout is therefore:

```text
src/main/java/app/database/
src/main/resources/static/
```

The repository also currently contains `conversation.md`, a conversation transcript export created after the implementation phase. It is documentation, not application code.

## Implementation Journey

1. The empty workspace and required development environment were established.
2. Java 21, Gradle 9.8.0, and the MySQL 8.0.46 client were verified without installing software.
3. A minimal Gradle Java project was configured.
4. MySQL Connector/J `8.4.0` and environment-based JDBC configuration were added.
5. Minimal static frontend files were added under the conventional Gradle resources path.
6. A separate top-level frontend/backend/database layout was considered, then rejected in favor of the regular Gradle structure.
7. The Java JDBC factory was debugged after compilation failures.
8. The final repository build was run successfully.

## Debugging History

The implementation history recorded these compilation failures in `JdbcConnectionFactory.java`:

- `[` and `]` were used around the class body instead of `{` and `}`. The compiler reported `'{ ' expected`, class/record errors, and an unnamed-class preview-feature message caused by the malformed structure.
- The constructor was misspelled as `JdbcCOnnectionFactory`, producing `invalid method declaration; return type required` because it did not exactly match the class name.
- The method was initially misspelled as `openCOnnection`.

The conversation supplied a corrected version using `JdbcConnectionFactory`, `openConnection`, and normal braces. However, the actual current repository still contains `openCOnnection`. This contradiction is documented deliberately: the historical correction was proposed/reported, but the repository is authoritative for current state.

## Commands and Operations

Commands materially used during the phase included:

```bash
java -version
gradle --version
mysql --version
gradle tasks
gradle build
find src/main/resources/static -maxdepth 1 -type f -print
```

The user supplied the outputs for the environment commands and the failed and successful build attempts. The final repository inspection also ran `gradle build` against the current files.

## Verification

The final current-state verification produced:

```text
BUILD SUCCESSFUL in 6s
3 actionable tasks: 3 up-to-date
```

The successful Gradle build verifies that the current Gradle configuration is readable, the current Java source compiles, the declared dependency configuration is acceptable to Gradle, resources can participate in the build, and the test/build lifecycle completes.

The build does not prove actual MySQL connectivity, valid database credentials, an existing database schema, HTTP server operation, browser-to-Java communication, authentication, registration, or application business logic. No runtime JDBC connection test or browser test was performed.

## Final Repository State

The relevant current structure is:

```text
build.gradle
settings.gradle
conversation.md
src/
  main/
    java/
      app/
        database/
          DatabaseConfig.java
          JdbcConnectionFactory.java
    resources/
      static/
        index.html
        styles.css
        app.js
context/
  phase-01-boilerplate-foundation.md
```

There is no current `frontend/`, `backend/`, or `database/` top-level application folder in the repository. There is no HTTP server, schema file, authentication code, registration code, or test source file confirmed in the final tree.

## Important Architectural Decisions

- Use plain Java rather than a web framework.
- Use JDBC rather than an ORM.
- Use Gradle as the build system.
- Keep MySQL credentials in environment variables rather than source code.
- Keep the regular Gradle source/resource layout.
- Do not add product-specific business logic during the foundation phase.
- Do not add authentication during the foundation phase.
- Do not introduce an HTTP server until that architectural requirement is decided.

## Deferred / Out of Scope

The following were intentionally not implemented:

- HTTP/server layer
- Registration
- Authentication
- User database schema
- Login
- Sessions
- Authorization
- Application-specific business logic

No users, passwords, roles, sessions, JWTs, or authentication tables were created.

## Known Risks or Open Questions

- JDBC connectivity has not been tested against a running MySQL database.
- Database credentials and `DB_URL` have not been validated.
- No database schema exists yet.
- Frontend browser behavior has not been fully tested.
- No HTTP server or application endpoint exists.
- The current repository method is `openCOnnection`, while the historical corrected proposal used `openConnection`; future work should choose and consistently use the intended name.
- The current CSS `width:min(100% -2rem , 60rem)` has not been browser-tested.

## Commit Linkage

The project directory is not a Git repository. `git status` and `git log` could not provide branch, commit, or diff metadata. No commit hash or commit message is available, and the phase changes are therefore uncommitted from the perspective of this workspace.

## Resume From Here

The next planned implementation phase is **Registration**. The intended registration data is:

```text
name
phone number
email
password
```

This document does not implement or design registration. Before beginning that phase, preserve the no-framework architecture, decide the HTTP/server mechanism, decide the database schema deliberately, and resolve the current JDBC method-name discrepancy against the actual source. Do not treat the successful build as proof of database connectivity or application behavior.
