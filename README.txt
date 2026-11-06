ROBLOX CHECKER - ANDROID PROJECT

WHAT IT DOES
- Loads a .txt file with one Roblox username per line (up to 500).
- Looks up public Roblox usernames using Roblox's public Users API.
- Shows public profile avatar, username, display name, user ID, account creation date,
  description, friends, followers, following, and a link to the public profile.
- Does not ask for Roblox passwords, cookies, or account login.

BUILD THE APK WITHOUT INSTALLING ANDROID STUDIO (GitHub Actions)
1. Extract this ZIP on your phone or computer.
2. Sign in to GitHub and create a NEW repository.
3. Upload the CONTENTS of this folder to the repository (not the ZIP file itself).
4. Open the repository's Actions tab.
5. If GitHub asks, enable Actions.
6. Choose "Build Roblox Checker APK" and tap "Run workflow".
   (A push to the main branch also starts the workflow.)
7. Wait for the workflow to finish with a green check.
8. Open the completed workflow run, scroll to Artifacts, and download
   "RobloxChecker-debug-apk".
9. Extract the downloaded artifact ZIP. It contains app-debug.apk.
10. Open app-debug.apk on Android and install it. Android may ask you to allow
    installs from that browser/file manager.

USE THE APP
1. Create a text file named usernames.txt.
2. Put one Roblox username on each line, for example:
   Builderman
   Roblox
   Robloxian
3. Open Roblox Checker and tap SELECT .TXT FILE.
4. Choose usernames.txt.
5. Tap START CHECKING.
6. Scroll down to see profile cards and public information.

NOTES
- Public API requests can be rate-limited or temporarily unavailable.
- A missing username match is reported as NOT FOUND; network/API failures are ERROR.
- Some profile counts or avatar images may be unavailable.
- This is a debug APK build, suitable for testing. It is not Play Store signed.
- Only use public profile information. Never enter or upload passwords or session cookies.
