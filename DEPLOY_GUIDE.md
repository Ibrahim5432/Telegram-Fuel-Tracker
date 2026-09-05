# 📖 دليل النشر الشامل - Telegram Fuel Tracker

## المحتويات
1. [متطلبات النشر](#متطلبات-النشر)
2. [الخطوة 1: إعداد حساب Oracle Cloud](#الخطوة-1-إعداد-حساب-oracle-cloud)
3. [الخطوة 2: إنشاء مفتاح تطبيق تيليجرام](#الخطوة-2-إنشاء-مفتاح-تطبيق-تيليجرام)
4. [الخطوة 3: إعداد السيرفر](#الخطوة-3-إعداد-السيرفر)
5. [الخطوة 4: تشغيل النظام كخدمة](#الخطوة-4-تشغيل-النظام-كخدمة)
6. [الخطوة 5: نشر تطبيق الأندرويد](#الخطوة-5-نشر-تطبيق-الأندرويد)
7. [الاستكشاف والصيانة](#الاستكشاف-والصيانة)

---

## متطلبات النشر

### للسيرفر:
- خادم Linux (Ubuntu 20.04 أو أحدث) - مجاني على Oracle Cloud Always Free
- Python 3.9+
- Git

### لتطبيق الأندرويد:
- Android Studio
- JDK 11+
- Android SDK (API 24+)

---

## الخطوة 1: إعداد حساب Oracle Cloud

### 1.1 إنشاء حساب مجاني
```bash
# زيارة الموقع
https://www.oracle.com/cloud/free/

# اضغط على "Start for Free"
# ملئ البيانات الشخصية والدفع (بطاقة ائتمان، بدون تكاليف)
```

### 1.2 إنشاء آلة افتراضية (Compute Instance)
```
1. انتقل إلى: Compute → Instances
2. اضغط "Create Instance"
3. الإعدادات الموصى بها:
   - Image: Ubuntu 20.04 (مجاني)
   - Shape: VM.Standard.E2.1.Micro (مجاني دائماً)
   - Storage: 50 GB (كافي)
   - Public IP: Enable
4. اضغط Create
5. احفظ المفتاح الخاص (Private Key) - ستحتاجه للاتصال
```

### 1.3 الاتصال بالخادم عبر SSH
```bash
# في الجهاز المحلي
chmod 600 private_key.key
ssh -i private_key.key ubuntu@<PUBLIC_IP>
```

---

## الخطوة 2: إنشاء مفتاح تطبيق تيليجرام

### 2.1 الحصول على API_ID و API_HASH
```
1. انتقل إلى: https://my.telegram.org/apps
2. سجل دخولك برقم الهاتف
3. اضغط "Create new application"
4. ملأ البيانات:
   - App title: "Telegram Fuel Tracker"
   - Short name: "fuel_tracker"
5. انسخ:
   - api_id (مثال: 1234567)
   - api_hash (مثال: abcdef1234...)
```

---

## الخطوة 3: إعداد السيرفر

### 3.1 تحديث النظام
```bash
sudo apt-get update
sudo apt-get upgrade -y
sudo apt-get install -y python3-pip python3-venv git
```

### 3.2 استنساخ المستودع
```bash
cd ~
git clone https://github.com/Ibrahim5432/Telegram-Fuel-Tracker.git
cd Telegram-Fuel-Tracker/Telegram_Scraper
```

### 3.3 إنشاء بيئة Python الافتراضية
```bash
python3 -m venv venv
source venv/bin/activate
```

### 3.4 تثبيت المكتبات
```bash
pip install -r requirements.txt
```

### 3.5 إعداد متغيرات البيئة
```bash
# نسخ الملف المثالي
cp .env.example .env

# تحرير الملف
nano .env
```

**أضف البيانات التالية:**
```env
API_ID=YOUR_API_ID_HERE
API_HASH=YOUR_API_HASH_HERE
PHONE_NUMBER=+1234567890
CHANNEL_USERNAME=kazyat_halab

SERVER_HOST=0.0.0.0
SERVER_PORT=8000
DATABASE_PATH=fuel_tracker.db
FETCH_INTERVAL=300
```

اضغط `Ctrl+X` ثم `Y` ثم `Enter` للحفظ.

### 3.6 اختبار تشغيل السيرفر يدوياً
```bash
python main.py
```

ستظهر رسالة تطلب منك إدخال رمز التحقق من تيليجرام:
```
📱 أدخل رمز التحقق من تيليجرام: 123456
```

أدخل الرمز الذي ستستقبله على تطبيق تيليجرام.

اضغط `Ctrl+C` للإيقاف بعد التأكد من أن الكاشط يعمل بنجاح.

---

## الخطوة 4: تشغيل النظام كخدمة

### 4.1 إنشاء ملف خدمة systemd
```bash
sudo nano /etc/systemd/system/fuel-tracker.service
```

**أضف المحتوى التالي:**
```ini
[Unit]
Description=Telegram Fuel Tracker API Server
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/Telegram-Fuel-Tracker/Telegram_Scraper
Environment="PATH=/home/ubuntu/Telegram-Fuel-Tracker/Telegram_Scraper/venv/bin"
ExecStart=/home/ubuntu/Telegram-Fuel-Tracker/Telegram_Scraper/venv/bin/python main.py
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

اضغط `Ctrl+X` ثم `Y` ثم `Enter` للحفظ.

### 4.2 تفعيل الخدمة
```bash
sudo systemctl daemon-reload
sudo systemctl enable fuel-tracker
sudo systemctl start fuel-tracker
```

### 4.3 فحص حالة الخدمة
```bash
sudo systemctl status fuel-tracker
```

يجب أن ترى:
```
● fuel-tracker.service - Telegram Fuel Tracker API Server
     Loaded: loaded (...; enabled; vendor preset: enabled)
     Active: active (running) since ...
```

### 4.4 عرض السجلات
```bash
sudo journalctl -u fuel-tracker -f
```

---

## الخطوة 5: فتح المنافذ على جدار الحماية

### 5.1 فتح المنفذ 8000 (للـ API)
```bash
# على Oracle Cloud Console:
# 1. انتقل إلى: Networking → Virtual Cloud Networks
# 2. اختر VCN الخاص بك
# 3. اختر Security List
# 4. أضف Ingress Rule جديد:
#    - Protocol: TCP
#    - Source Port: All
#    - Destination Port: 8000
#    - Source CIDR: 0.0.0.0/0
```

### 5.2 اختبار الاتصال من المحلي
```bash
# استبدل <PUBLIC_IP> بـ IP الخادم
curl http://<PUBLIC_IP>:8000/
curl http://<PUBLIC_IP>:8000/health
```

يجب أن ترى استجابة JSON.

---

## الخطوة 6: نشر تطبيق الأندرويد

### 6.1 تحديث عنوان الخادم
افتح `Android_App/app/src/main/java/com/fuel/tracker/MainActivity.kt`

غيّر:
```kotlin
private const val API_BASE_URL = "http://<YOUR_SERVER_IP>:8000/"
```

استبدل `<YOUR_SERVER_IP>` بـ IP الخادم الفعلي.

### 6.2 بناء التطبيق
```bash
cd Android_App
./gradlew build
```

### 6.3 تثبيت على جهازك
```bash
./gradlew installDebug
```

أو استخدم Android Studio لبناء وتشغيل التطبيق مباشرة.

---

## الاستكشاف والصيانة

### عرض السجلات الحية
```bash
sudo journalctl -u fuel-tracker -f --lines=50
```

### إعادة تشغيل الخدمة
```bash
sudo systemctl restart fuel-tracker
```

### إيقاف الخدمة
```bash
sudo systemctl stop fuel-tracker
```

### حذف الرسائل القديمة (اختياري)
```bash
cd ~/Telegram-Fuel-Tracker/Telegram_Scraper
source venv/bin/activate
python3
```

ثم:
```python
from database import db
import sqlite3

conn = db.get_connection()
cursor = conn.cursor()

# حذف الرسائل الأقدم من 30 يوم
cursor.execute("""
    DELETE FROM messages 
    WHERE date < datetime('now', '-30 days')
""")

conn.commit()
print(f"تم حذف {cursor.rowcount} رسالة قديمة")
conn.close()
```

### نسخ احتياطية
```bash
# نسخ قاعدة البيانات
cp ~/Telegram-Fuel-Tracker/Telegram_Scraper/fuel_tracker.db ~/fuel_tracker.db.backup
```

---

## اختبار نقاط النهاية (Endpoints)

### الحصول على آخر الرسائل
```bash
curl http://<PUBLIC_IP>:8000/get-latest-messages?limit=10
```

### البحث عن رسائل
```bash
curl "http://<PUBLIC_IP>:8000/search?keyword=بنزين&limit=5"
```

### فحص صحة الخادم
```bash
curl http://<PUBLIC_IP>:8000/health
```

### الإحصائيات
```bash
curl http://<PUBLIC_IP>:8000/stats
```

---

## ملاحظات مهمة

⚠️ **أمان:**
- لا تشارك ملف `.env` على GitHub
- استخدم HTTPS في الإنتاج (استخدم Let's Encrypt مع Nginx)
- اقصر الوصول إلى المنفذ 8000 على عنوان IP الخاص بتطبيقك

💾 **البيانات:**
- قاعدة البيانات تنمو تلقائياً - احذف الرسائل القديمة دورياً
- احتفظ بنسخ احتياطية منتظمة

🔄 **التحديثات:**
```bash
# سحب آخر التحديثات
cd ~/Telegram-Fuel-Tracker
git pull
sudo systemctl restart fuel-tracker
```

---

## الدعم والمساعدة

إذا واجهت مشاكل:
1. تحقق من السجلات: `sudo journalctl -u fuel-tracker -f`
2. تأكد من توفر الإنترنت على الخادم
3. تحقق من بيانات التحقق من تيليجرام في `.env`
4. أعد تشغيل الخدمة: `sudo systemctl restart fuel-tracker`

🎉 **مبروك!** تم نشر النظام بنجاح!
