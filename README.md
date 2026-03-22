# File transporter

## TL;DR

A simple file transfer service by Full Stack of Kotlin.

## Used

- [Compose for Web](https://compose-web.ui.pages.jetbrains.team/)
- [Ktor](https://ktor.io/)
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)

## How to run

Edit `config/application.yaml` first if you want to change the shared root folder or upload size limit.

Run the following command:

```bash
./gradlew run
```

Then, you can access to http://localhost:8080 or http://0.0.0.0:8080.

## Configuration

The server reads its external YAML config from `config/application.yaml` by default.

```yaml
storage:
  rootDirectory: "."

upload:
  maxFileSizeBytes: 1073741824
```

- `storage.rootDirectory`: base directory exposed by the file browser
- `upload.maxFileSizeBytes`: maximum allowed upload size in bytes

You can also point to a different config file:

```bash
./gradlew run -Dfile.transporter.config=/absolute/path/to/application.yaml
```

For temporary overrides, these system properties are also supported:

```bash
./gradlew run -Dfile.transporter.root=/absolute/path -Dfile.transporter.upload.maxFileSizeBytes=52428800
```

## References

- [Full Stack JVM & JS App Hands-On Lab](https://github.com/kotlin-hands-on/jvm-js-fullstack/tree/final)
- [Compose Web Landing Page](https://github.com/JetBrains/compose-jb/tree/master/examples/web-landing)
