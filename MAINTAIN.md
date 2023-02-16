## Building locally

In order to prepare userlib folder to run the project locally, use `./gradlew prepareTest`. To keep only jars that are needed for the release, use `./gradlew prepareMpk`.

`./gradlew mxBuild` builds the app locally.

`./gradlew exportModule` creates an mpk to be released with only necessary jars included in userlib.
