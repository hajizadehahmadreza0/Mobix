# ساخت APK موبیکس بدون Android Studio

این پروژه یک workflow برای GitHub Actions دارد که روی سرور GitHub:

1. Java 17 را نصب می‌کند.
2. Android SDK و پلتفرم Android 36 را نصب می‌کند.
3. Gradle 8.13 را آماده می‌کند.
4. APK دیباگ را با `:app:assembleDebug` می‌سازد.
5. فایل `app-debug.apk` را به‌عنوان Artifact قابل دانلود قرار می‌دهد.

## روش استفاده

1. در GitHub یک Repository جدید بسازید (Private هم می‌تواند باشد).
2. محتویات پوشه `Mobix_Android` را در Repository آپلود کنید؛ خود پوشه `Mobix_Android` را داخل Repository تو در تو نگذارید.
3. بعد از Push به شاخه `main`، از تب **Actions** اجرای `Build Mobix APK` را ببینید.
4. پس از موفقیت، وارد اجرای Workflow شوید و در بخش **Artifacts** فایل `Mobix-debug-apk` را دانلود کنید.

برای اجرای دستی هم در Actions، Workflow را باز کنید و **Run workflow** را بزنید.
