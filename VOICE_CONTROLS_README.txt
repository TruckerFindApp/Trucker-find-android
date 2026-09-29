Trucker Find voice controls - source prepared for APK build

Use the microphone below the floating app logo, then say:
- Find fuel / food / truck parking / repair / laundry / truck wash / hotels
- Find 24 hour truck stops / chrome shops / weigh stations
- Go to saved area Home (replace Home with your saved name)
- Show saved areas
- Cancel

Saved Areas use the existing web app's signed-in Supabase session. No database credentials or saved places are copied into Android storage. Exact names take priority. Multiple matches show a choice. The saved address is re-read before opening directions.

The supplied index.html was used as the integration reference and is unchanged. No web deployment is required for this Android integration, provided the hosted app has the same supabaseClient and mapsDirections definitions as that file.

Validation:
- 11 Saved Areas JavaScript tests passed locally.
- Java command tests were added to GitHub Actions but have not run locally (no JDK available).
- Android compilation and physical phone testing remain pending.
- Source code is not an installable APK. Download the APK from a successful GitHub Actions build.

Build: upload the archive contents to the root of TruckerFindApp/Trucker-find-android, commit, and run Build Trucker Find APK in GitHub Actions. This runs voice checks, builds the app, and uploads Trucker-Find-Debug-APK.

The phone's speech recognition provider handles the listening screen. Voice starts only when the microphone button is tapped. A supported speech provider is required and may use a network connection.
