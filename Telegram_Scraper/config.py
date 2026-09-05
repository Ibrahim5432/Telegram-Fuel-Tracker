# ملف إعدادات التطبيق - قراءة متغيرات البيئة
import os
from dotenv import load_dotenv

# تحميل متغيرات البيئة من ملف .env
load_dotenv()

# بيانات تطبيق تيليجرام (يجب الحصول عليها من https://my.telegram.org/apps)
API_ID = int(os.getenv("API_ID", 0))
API_HASH = os.getenv("API_HASH", "")
PHONE_NUMBER = os.getenv("PHONE_NUMBER", "")

# اسم القناة المراد كشطها (بدون @)
CHANNEL_USERNAME = os.getenv("CHANNEL_USERNAME", "kazyat_halab")

# إعدادات الخادم
SERVER_HOST = os.getenv("SERVER_HOST", "0.0.0.0")
SERVER_PORT = int(os.getenv("SERVER_PORT", 8000))

# إعدادات قاعدة البيانات
DATABASE_PATH = os.getenv("DATABASE_PATH", "fuel_tracker.db")

# الفاصل الزمني لجلب البيانات (بالثواني)
FETCH_INTERVAL = int(os.getenv("FETCH_INTERVAL", 300))  # 5 دقائق كقيمة افتراضية

# التحقق من البيانات الأساسية
if not API_ID or not API_HASH:
    print("⚠️ تحذير: لم يتم تعيين API_ID و API_HASH في ملف .env")
