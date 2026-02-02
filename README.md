# Blog Application (Spring Framework, no Spring Boot)

## 🧩 Технологии

* Java
* Spring Framework (Core, Context, Web, Test)
* Spring JDBC (`JdbcTemplate`)
* PostgreSQL
* JUnit 5
* Mockito
* Maven

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

## ▶️ Запуск

1. Запустить PostgreSQL
2. Создать базу данных `blog`
3. Добавить `application.properties`
4. Собрать проект:

```bash
mvn clean test
```

--- 

5. Запустить приложение через servlet-container (Tomcat / Jetty)

---

## 📦 Сборка WAR

Проект предназначен для развёртывания во **внешнем servlet-контейнере** (Tomcat / Jetty), поэтому собирается в формате *
*WAR**.

### Maven

Сборка WAR-файла:

```bash
mvn clean package
```

Готовый файл появится в директории:

```
target/blog.war
```

WAR-файл можно скопировать в директорию `webapps` Tomcat или задеплоить любым другим способом.

