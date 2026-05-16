# AllInOneManager Android

Native Android version of the Windows AllInOneManager client/session register.

## What is included

- Client create, update, delete, search, and selection.
- Session scheduling and deletion per selected client.
- SQLite persistence using the same core table names and fields as the Windows app.
- WhatsApp reminder notifications that open prefilled `wa.me` messages for the psychologist and optionally the client.
- A settings screen for reminder timing, WhatsApp phone fields, country prefix, and saved Twilio fields.

## Build

Open this folder in Android Studio and sync the Gradle project. This machine currently does not have Java, Gradle, the Android SDK, or the .NET MAUI workload installed, so the project could not be built locally here.

The app stores its Android database in the app sandbox. It does not automatically import the Windows `psychologist_clients.db` file.
