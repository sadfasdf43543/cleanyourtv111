# الواجهة الإسلامية: ما أُضيف وكيف يعمل

## ما أُضيف
- **شريط الصفحة الرئيسية**: خمس صلوات (التالية بالأخضر مع الوقت المتبقي)، وتحتها آية أو حديث يتبدلان كل 45 ثانية، والتاريخ الهجري.
- **مواقيت الصلاة بدون إنترنت** للإحداثيات 41.0605086 / 28.8355530، بطريقة رئاسة الشؤون الدينية التركية، توقيت الجهاز.
- **إشعار فوق أي تطبيق** (20 ثانية، لا يأخذ التركيز ولا الأزرار).
- **الأذان**: شاشة كاملة في مهمة مستقلة، تطلب تركيز الصوت الحصري، تبتلع كل أزرار الريموت عدا الصوت، و**Back/Escape/Delete/Stop** يخرج ويعود للتطبيق السابق.
- خط **ثمانية** بدل Outfit في كل الواجهة، والأرقام **123** دائماً.
- الإعدادات: افتح إعدادات الواجهة (ضغط مطوّل OK على «Settings») ثم «الصلاة والأذان».

## التثبيت (مرة واحدة بعد تثبيت الـ APK)
```
adb shell appops set com.gathin.tvhome SYSTEM_ALERT_WINDOW allow
adb shell appops set com.gathin.tvhome SCHEDULE_EXACT_ALARM allow
adb shell pm grant com.gathin.tvhome android.permission.POST_NOTIFICATIONS
adb shell cmd deviceidle whitelist +com.gathin.tvhome
```
الإذن الأول ضروري: بدونه لا يظهر الإشعار ولا تُفتح شاشة الأذان من الخلفية.

## صوت الأذان
لا يوجد ملف صوتي داخل المشروع. اختر واحدة:
1. انسخ ملفات mp3 إلى `Android/data/com.gathin.tvhome/files/adhan/` على التلفزيون
   `adb push adhan.mp3 /sdcard/Android/data/com.gathin.tvhome/files/adhan/`
2. أو أنشئ في المجلد نفسه ملف `sources.txt` بسطور: `الاسم|https://رابط-الملف.mp3` (https فقط).
3. أو ضع ملفاً باسم `adhan_default.mp3` في `app/src/main/res/raw/` قبل البناء.
ثم اختر الصوت من «الصلاة والأذان». إن لم يوجد صوت يعمل الإشعار فقط.

## البناء
`TV_HOST=<ip> launcher/build-and-install.sh` كما في README الأصلي (JDK 17–22 و Android SDK 34).
