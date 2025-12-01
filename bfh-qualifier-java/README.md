# Bajaj Finserv Health Qualifier - JAVA (Spring Boot)

This Spring Boot application:

1. On startup, calls the `generateWebhook` API to obtain a webhook URL and access token.
2. Chooses the SQL problem based on the last two digits of `regNo`.
3. Stores the final SQL query into an in-memory H2 database.
4. Sends the final SQL query to the returned webhook URL using the JWT access token in the `Authorization` header.

## How to use

1. Edit `src/main/resources/application.properties` and set:
   - `candidate.name`
   - `candidate.regNo`
   - `candidate.email`

   If your last two digits are **even**, Question 2 SQL (`sql.solution.q2`) will be used.
   If **odd**, Question 1 SQL (`sql.solution.q1`) will be used.

2. Build:
   ```bash
   mvn clean package
   ```

3. Run:
   ```bash
   java -jar target/bfh-qualifier-java-0.0.1-SNAPSHOT.jar
   ```
