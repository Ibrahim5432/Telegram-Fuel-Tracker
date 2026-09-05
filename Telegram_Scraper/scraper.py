# ملف كشط بيانات تيليجرام
import asyncio
import logging
from telethon import TelegramClient, events
from telethon.errors import SessionPasswordNeededError, FloodWaitError
from datetime import datetime
import time
from config import API_ID, API_HASH, PHONE_NUMBER, CHANNEL_USERNAME, FETCH_INTERVAL
from database import db

# إعداد نظام السجلات
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# إنشاء عميل تيليجرام
client = TelegramClient('session_name', API_ID, API_HASH)


def parse_fuel_data(text: str) -> dict:
    """
    تحليل نص الرسالة واستخراج كلمات مفتاحية عن البنزين والمازوت والحالة
    
    المدخل: نص الرسالة
    المخرج: قاموس يحتوي على الكلمات المفتاحية المستخرجة
    """
    result = {
        "original_text": text,
        "keywords": [],
        "has_benzene": False,
        "has_mazot": False,
        "has_diesel": False,
        "status": []  # متوفر، غير متوفر، مزدحم، إلخ
    }
    
    text_lower = text.lower()
    
    # البحث عن كلمات البنزين
    benzene_keywords = ["بنزين", "نافتا", "وقود", "غاز"]
    if any(keyword in text_lower for keyword in benzene_keywords):
        result["has_benzene"] = True
        result["keywords"].append("بنزين")
    
    # البحث عن كلمات المازوت
    mazot_keywords = ["مازوت", "ديزل", "وقود ثقيل", "فيول"]
    if any(keyword in text_lower for keyword in mazot_keywords):
        result["has_mazot"] = True
        result["keywords"].append("مازوت")
    
    # البحث عن كلمات الديزل
    diesel_keywords = ["ديزل", "احمر", "أحمر"]
    if any(keyword in text_lower for keyword in diesel_keywords):
        result["has_diesel"] = True
        result["keywords"].append("ديزل")
    
    # البحث عن حالات التوفر
    availability_keywords = {
        "متوفر": ["متوفر", "موجود", "كافي", "✅"],
        "غير متوفر": ["غير متوفر", "نفذ", "ما فيش", "❌"],
        "مزدحم": ["مزدحم", "طابور", "ازدحام", "📍"],
        "الإغلاق": ["مغلق", "مقفل", "توقف"]
    }
    
    for status, keywords_list in availability_keywords.items():
        if any(keyword in text_lower for keyword in keywords_list):
            result["status"].append(status)
    
    return result


async def authenticate():
    """
    مصادقة العميل مع تيليجرام
    يتم حفظ الجلسة تلقائياً لتجنب طلب بيانات الدخول في كل مرة
    """
    try:
        await client.connect()
        
        # التحقق من وجود جلسة صحيحة
        if not await client.is_user_authorized():
            logger.info("🔐 بدء عملية التحقق من الهوية...")
            await client.send_code_request(PHONE_NUMBER)
            
            code = input("📱 أدخل رمز التحقق من تيليجرام: ")
            
            try:
                await client.sign_in(PHONE_NUMBER, code)
            except SessionPasswordNeededError:
                password = input("🔒 أدخل كلمة المرور ثنائية التحقق: ")
                await client.sign_in(password=password)
        
        logger.info("✅ تم التحقق من الهوية بنجاح")
        return True
        
    except FloodWaitError as e:
        logger.error(f"⚠️ حد الطلبات تم تجاوزه. انتظر {e.seconds} ثانية")
        return False
    except Exception as e:
        logger.error(f"❌ خطأ في التحقق من الهوية: {e}")
        return False


async def fetch_channel_messages():
    """
    جلب أحدث الرسائل من القناة
    تستخدم offset_id لجلب الرسائل الجديدة فقط
    """
    try:
        last_message_id = db.get_last_message_id()
        
        logger.info(f"🔄 جاري جلب الرسائل من القناة @{CHANNEL_USERNAME}...")
        
        # جلب الرسائل الجديدة من آخر رسالة تم جلبها
        async for message in client.iter_messages(CHANNEL_USERNAME, offset_id=last_message_id, limit=100):
            if message.message_id <= last_message_id:
                continue
            
            # استخراج نص الرسالة
            text = message.text or ""
            
            # تحليل البيانات
            parsed_data = parse_fuel_data(text)
            
            # إنشاء مسار الوسائط (صورة/فيديو)
            media_path = None
            if message.media:
                # بناء رابط التحميل المباشر من تيليجرام
                media_path = f"https://t.me/{CHANNEL_USERNAME}/{message.message_id}"
            
            # إدراج الرسالة في قاعدة البيانات
            db.insert_message(
                message_id=message.message_id,
                text=text,
                date=message.date,
                media_path=media_path
            )
            
            # تحديث آخر معرّف تم جلبه
            if message.message_id > last_message_id:
                last_message_id = message.message_id
                db.update_last_message_id(message.message_id)
            
            logger.info(f"📨 تم جلب الرسالة #{message.message_id}: {parsed_data['keywords']}")
        
        logger.info("✅ انتهى جلب الرسائل بنجاح")
        return True
        
    except FloodWaitError as e:
        logger.warning(f"⚠️ حد الطلبات تم تجاوزه. سيتم الانتظار {e.seconds} ثانية...")
        await asyncio.sleep(e.seconds)
        return False
    except Exception as e:
        logger.error(f"❌ خطأ في جلب الرسائل: {e}")
        return False


@client.on(events.NewMessage(chats=CHANNEL_USERNAME))
async def handle_new_message(event):
    """
    معالج الرسائل الجديدة (الاستماع اللحظي)
    يتم تنفيذ هذه الدالة عند ورود رسالة جديدة للقناة
    """
    try:
        message = event.message
        text = message.text or ""
        
        logger.info(f"🔔 رسالة جديدة: {text[:50]}...")
        
        # تحليل البيانات
        parsed_data = parse_fuel_data(text)
        
        # إنشاء مسار الوسائط
        media_path = None
        if message.media:
            media_path = f"https://t.me/{CHANNEL_USERNAME}/{message.message_id}"
        
        # إدراج الرسالة في قاعدة البيانات
        db.insert_message(
            message_id=message.message_id,
            text=text,
            date=message.date,
            media_path=media_path
        )
        
        # تحديث آخر معرّف
        db.update_last_message_id(message.message_id)
        
        logger.info(f"💾 تم حفظ الرسالة الجديدة #{message.message_id}")
        
    except Exception as e:
        logger.error(f"❌ خطأ في معالجة الرسالة الجديدة: {e}")


async def periodic_fetch():
    """
    حلقة دورية لجلب الرسائل كل 5 دقائق (احتياطي)
    تعمل بالتوازي مع الاستماع اللحظي
    """
    while True:
        try:
            await asyncio.sleep(FETCH_INTERVAL)
            logger.info(f"⏰ جاري جلب الرسائل بشكل دوري...")
            await fetch_channel_messages()
        except Exception as e:
            logger.error(f"❌ خطأ في الجلب الدوري: {e}")
            await asyncio.sleep(FETCH_INTERVAL)


async def run_scraper():
    """
    تشغيل كاشط البيانات
    - يبدأ بالجلب الأولي للرسائل
    - ثم يستمع للرسائل الجديدة بشكل لحظي
    - وفي نفس الوقت يجلب الرسائل بشكل دوري كاحتياطي
    """
    # التحقق من الهوية
    if not await authenticate():
        logger.error("❌ فشل التحقق من الهوية. سيتم الخروج.")
        return
    
    logger.info("🚀 بدء تشغيل كاشط البيانات...")
    
    try:
        # الجلب الأولي
        await fetch_channel_messages()
        
        # بدء المراقب اللحظي والجلب الدوري بالتوازي
        await asyncio.gather(
            client.run_until_disconnected(),
            periodic_fetch()
        )
        
    except KeyboardInterrupt:
        logger.info("⏹️ تم التوقف بناءً على طلب المستخدم")
    except Exception as e:
        logger.error(f"❌ خطأ غير متوقع: {e}")
    finally:
        await client.disconnect()
        logger.info("👋 تم قطع الاتصال بتيليجرام")


def start_scraper():
    """
    نقطة الدخول الرئيسية لتشغيل الكاشط
    """
    try:
        asyncio.run(run_scraper())
    except KeyboardInterrupt:
        logger.info("⏹️ تم إيقاف الكاشط")
    except Exception as e:
        logger.error(f"❌ خطأ حرج: {e}")
