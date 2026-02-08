# QuickHelp Backend (Spring Boot)

This is the backend service for the QuickHelp application, built with **Java** and **Spring Boot**. It handles data persistence, user authentication, and service management.

## Run Locally

1.  **Prerequisites**:
    *   Java 17 or higher installed.
    *   Maven installed (or use the included wrapper).
    *   A running PostgreSQL instance (update `application.properties` with credentials).

2.  **Clone the repository**:
    ```bash
    git clone https://github.com/NilakanthaPradhan/QuickHelp_Backend_SpringBoot.git
    cd quickhelp_backend
    ```

3.  **Configure Database**:
    Update `src/main/resources/application.properties` with your local database details:
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/quickhelp_db
    spring.datasource.username=your_username
    spring.datasource.password=your_password
    ```

4.  **Run the application**:
    ```bash
    ./mvnw spring-boot:run
    ```
    The server will start on `http://localhost:8080`.

## Deploy to Render

You can deploy this Spring Boot application on Render using their native **Web Service** support for Docker or Java.

### Option 1: Native Java
1.  Create a **Web Service** on Render.
2.  Connect this repository.
3.  **Environment**: `Java`.
4.  **Build Command**: `./mvnw clean package -DskipTests`
5.  **Start Command**: `java -jar target/*.jar`

### Option 2: Docker
(Ensure a `Dockerfile` exists in the root)
1.  Create a **Web Service**.
2.  Connect this repository.
3.  **Environment**: `Docker`.
4.  Render will automatically build using the `Dockerfile`.

### Environment Variables
Set these in your Render Dashboard:
*   `DB_URL`: The internal or external URL of your PostgreSQL database.
*   `DB_USERNAME`: Database user.
*   `DB_PASSWORD`: Database password.
