# ScrollBrake

ScrollBrake helps people spend less time on social media. After a time limit you choose (12 minutes by default) in **Instagram, TikTok, Facebook, Snapchat, X, Threads or Reddit**, it:

- puts a grey layer over the app that gets stronger every minute, so the feed looks dull and less tempting
- shows a **"Time for a break?"** card every 3 minutes ("Continue" only works after 10 seconds)
- shows how many minutes you've spent in each app today

**YouTube is not affected**, so long learning videos are never interrupted.

---

# Easy way: let GitHub build the app for you (no Android Studio)

GitHub is a free website for storing code. This project already includes instructions that tell GitHub to **build the app automatically** and give you a download link you can open on your phone. You only need a web browser.

### 1. Make a free GitHub account

Go to **github.com** and sign up. It's free.

### 2. Create a place for your app

1. Click the **+** at the top right and choose **New repository**.
2. **Repository name:** `ScrollBrake`
3. Choose **Public**. This makes the download link work without logging in.
4. Click **Create repository**.

### 3. Upload the files

1. On your computer, **unzip ScrollBrake.zip**: right-click it and choose **Extract All**.
2. Open the extracted **ScrollBrake** folder until you see `app`, `gradle`, `.github`, `README.md` and the other files.
3. On the GitHub page, click the link **"uploading an existing file"**.
4. On your computer, press **Ctrl + A** to select everything inside the folder and **drag it into the browser window**.
5. Wait until all the files are listed, then click the green **Commit changes** button.

### 4. Wait for the build (about 3–5 minutes)

1. Click the **Actions** tab at the top of your GitHub page.
2. You'll see **"Build ScrollBrake app"** with a yellow dot, which means it's building.
3. When the dot turns into a **green tick ✓**, your app is ready.

> **If the Actions tab is empty,** the hidden `.github` folder didn't upload. To fix it:
> 1. Click **Add file → Create new file**.
> 2. Type `.github/workflows/build.yml` as the name.
> 3. Open the same file from your computer in Notepad, copy everything and paste it in.
> 4. Click **Commit changes**.
>
> **If you see a red ✗,** click it, copy the error text and send it to Claude.

### 5. Install it on your phone

1. On your Android phone, open this page in Chrome:
   **`https://github.com/YOUR-USERNAME/ScrollBrake/releases/latest`**
   (Replace `YOUR-USERNAME` with your GitHub name.)
2. Tap **ScrollBrake.apk** to download it, then tap the downloaded file to open it.
3. If the phone asks, allow **"Install unknown apps"** for Chrome.
4. If **Play Protect** warns you, tap **More details → Install anyway**. This warning appears for any app that doesn't come from the Play Store.

**To share the app with others,** send them the same link.

### 6. Switch it on

1. Open **ScrollBrake** and tap **Turn on ScrollBrake**.
2. Find **ScrollBrake** in the Accessibility list (sometimes under "Installed apps" or "Downloaded apps") and turn it on.
3. **Android 13 and newer:** if the switch is greyed out or says "Restricted setting":
   1. Go back to ScrollBrake and tap **Open app info**.
   2. Tap the **⋮** menu at the top right and choose **Allow restricted settings**.
   3. Try turning it on again.

### 7. Try it

Set the slider to **5 minutes**, then open Instagram or TikTok and scroll. After 5 minutes the screen starts fading and the break card appears.

---

## Changing the app later

1. On GitHub, open any file and click the **pencil ✏️ icon** to edit it.
2. Click **Commit changes**. GitHub rebuilds the app automatically.
3. Download the new ScrollBrake.apk from the same link and install it. It replaces the old version, and your settings are kept.

**Easy things to change:**

| What | Where |
|---|---|
| Which apps are slowed down | `APPS` list in `app/src/main/java/com/scrollbrake/app/Prefs.java` |
| How fast the screen fades | `FADE_PER_MIN` in `ScrollBrakeService.java` |
| How often the break card appears | `PAUSE_EVERY_MIN` in `ScrollBrakeService.java` |
| Break suggestions | `BREAK_IDEAS` in `ScrollBrakeService.java` |

## How the code is organized

| File | What it does |
|---|---|
| `ScrollBrakeService.java` | Detects the open app, counts time, and draws the fade and the break card |
| `MainActivity.java` | The settings screen: on/off status, time slider, app list and today's minutes |
| `Prefs.java` | The list of social apps, plus saved settings and usage |
| `AndroidManifest.xml` | Tells Android about the app and its accessibility service |
| `.github/workflows/build.yml` | The instructions GitHub follows to build the app |

## Other way: Android Studio

If you later want to work like a professional Android developer:

1. Install **Android Studio** (developer.android.com/studio).
2. Use **File → Open** to open this folder.
3. Press **▶ Run** with your phone plugged in and USB debugging turned on.

## Before publishing on Google Play

- Google Play has strict rules for apps that use Accessibility features. You'll need a clear disclosure, a privacy policy and a declaration form.
- Replace `app/debug.keystore` with your own private signing key.

Sharing the APK link directly with friends needs none of this.
