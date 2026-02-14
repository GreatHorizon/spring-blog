# Blog Application (Spring Framework, no Spring Boot)

## 🧩 Технологии

* Java
* Spring Framework (Core, Context, Web, Test)
* Spring JDBC (`JdbcTemplate`)
* PostgreSQL
* JUnit 5
* Mockito
* Gradle

---

## 🗂 Структура проекта (пример)

```
com.my.blog
 ├── config          # Spring конфигурация
 ├── controller      # Web / REST контроллеры
 ├── service         # Бизнес‑логика
 ├── repository      # JDBC репозитории
 ├── model           # Модели / DTO
 └── test            # Unit и Integration тесты
```

---

## ⚙️ Конфигурация базы данных

Проект **не содержит** готового `application.properties` с реальными данными.

Перед запуском **необходимо создать файл**:

```
src/main/resources/application.properties
```

И указать в нём **свои параметры подключения к PostgreSQL**:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/blog
spring.datasource.username=name
spring.datasource.password=pass

/// Путь для сохранения фото
com.my.blog.uploads.path=uploads/
```

> ⚠️ Порт, имя базы данных, пользователь и пароль могут отличаться — используйте свои значения.

---

## 🗄 База данных

* PostgreSQL должен быть запущен локально или доступен по сети
* База данных `blog` должна быть создана заранее
* Далее запускается `schema.sql`

---

## 🧪 Тестирование

В проекте используются:

* **Unit‑тесты** — с моками репозиториев (Mockito)
* **Integration‑тесты** — с реальной БД и JDBC

---

## ▶️ Запуск тестов

```bash
./gradlew test
```

---

## 📦 Сборка JAR

### Gradle

Сборка JAR-файла:

```bash
 ./gradlew clean bootJar
```

JAR-файл находится в:

```
build/libs/
```

Запуск собранного JAR-файла:
```
java -jar build/libs/blog-0.0.1-SNAPSHOT.jar
```

