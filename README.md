# Market App

Веб-приложение «интернет-магазин» на Spring Boot

## Стек

- Java 21
- Spring Boot 4.0.5 (Web, Thymeleaf, Data JPA)
- H2 (in-memory)
- Liquibase
- Lombok
- Maven

## Требования

| Среда | Версия |
|--------|--------|
| JDK | 21 |
| Maven | 3.9+ (или встроенный в IDE) |
| Docker | опционально, для запуска в контейнере |


## Локальный запуск (без Docker)

### Через Maven

Из корня проекта:

```bash
mvn spring-boot:run
```

Приложение: **http://localhost:8080**


### Сборка JAR и запуск

```bash
mvn clean package
java -jar target/my-market-app-0.0.1-SNAPSHOT.jar
```
---

## Запуск в Docker

Нужен установленный и запущенный **Docker Desktop** (или Docker Engine).

### Вариант 1: Docker Compose

```bash
docker compose up --build
```

Остановка:

```bash
docker compose down
```

### Вариант 2: Docker вручную

Сборка образа:

```bash
docker build -t my-market-app .
```

Запуск контейнера:

```bash
docker run -p 8080:8080 my-market-app
```

Приложение: **http://localhost:8080**

---
