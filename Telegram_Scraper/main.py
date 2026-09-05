# ملف تشغيل خادم FastAPI
from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware
import logging
import asyncio
from threading import Thread
from config import SERVER_HOST, SERVER_PORT, CHANNEL_USERNAME
from database import db
from scraper import start_scraper

# إعداد نظام السجلات
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# إنشاء تطبيق FastAPI
app = FastAPI(
    title="🚗 Telegram Fuel Tracker API",
    description="API لجلب بيانات محطات الوقود من تيليجرام",
    version="1.0.0"
)

# السماح بطلبات CORS من جميع الأصول (للتطبيقات الخارجية)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
async def startup_event():
    """تشغيل كاشط البيانات عند بدء الخادم"""
    logger.info("🟢 بدء تشغيل الخادم...")
    
    # تشغيل الكاشط في خيط منفصل ليعمل بالتوازي مع الخادم
    scraper_thread = Thread(target=start_scraper, daemon=True)
    scraper_thread.start()
    
    logger.info("✅ تم بدء كاشط البيانات في الخلفية")


@app.on_event("shutdown")
async def shutdown_event():
    """تنظيف الموارد عند إيقاف الخادم"""
    logger.info("🔴 إيقاف الخادم...")


@app.get("/")
async def root():
    """نقطة الدخول الرئيسية"""
    return {
        "message": "🚗 مرحباً بك في API تتبع محطات الوقود",
        "channel": f"@{CHANNEL_USERNAME}",
        "endpoints": {
            "/get-latest-messages": "الحصول على آخر 50 رسالة",
            "/search": "البحث عن رسائل معينة",
            "/health": "فحص صحة الخادم"
        }
    }


@app.get("/get-latest-messages")
async def get_latest_messages(limit: int = Query(50, ge=1, le=500)):
    """
    الحصول على آخر الرسائل من القاعدة البيانات
    
    المعاملات:
    - limit: عدد الرسائل المطلوب إرجاعها (افتراضي: 50)
    """
    try:
        messages = db.get_latest_messages(limit=limit)
        
        return {
            "status": "success",
            "count": len(messages),
            "channel": CHANNEL_USERNAME,
            "data": messages
        }
    except Exception as e:
        logger.error(f"❌ خطأ في جلب الرسائل: {e}")
        return {
            "status": "error",
            "message": str(e),
            "data": []
        }


@app.get("/search")
async def search_messages(keyword: str = Query(...), limit: int = Query(50, ge=1, le=500)):
    """
    البحث عن رسائل تحتوي على كلمة مفتاحية معينة
    
    المعاملات:
    - keyword: الكلمة المفتاحية للبحث (مطلوبة)
    - limit: عدد النتائج (افتراضي: 50)
    """
    try:
        if not keyword or len(keyword.strip()) == 0:
            return {
                "status": "error",
                "message": "الكلمة المفتاحية لا يمكن أن تكون فارغة",
                "data": []
            }
        
        messages = db.search_messages(keyword=keyword, limit=limit)
        
        return {
            "status": "success",
            "count": len(messages),
            "search_keyword": keyword,
            "data": messages
        }
    except Exception as e:
        logger.error(f"❌ خطأ في البحث: {e}")
        return {
            "status": "error",
            "message": str(e),
            "data": []
        }


@app.get("/health")
async def health_check():
    """فحص صحة الخادم والاتصال بقاعدة البيانات"""
    try:
        # اختبار الاتصال بقاعدة البيانات
        test_messages = db.get_latest_messages(limit=1)
        
        return {
            "status": "healthy",
            "server": "online",
            "database": "connected",
            "messages_count": len(test_messages)
        }
    except Exception as e:
        logger.error(f"❌ خطأ في فحص الصحة: {e}")
        return {
            "status": "unhealthy",
            "server": "online",
            "database": "disconnected",
            "error": str(e)
        }


@app.get("/stats")
async def get_stats():
    """الحصول على إحصائيات عن البيانات المجمعة"""
    try:
        # جلب جميع الرسائل للعد
        conn = db.get_connection()
        cursor = conn.cursor()
        
        # عد الرسائل الكلي
        cursor.execute("SELECT COUNT(*) as total FROM messages")
        total_messages = cursor.fetchone()[0]
        
        # عد الرسائل التي تحتوي على وسائط
        cursor.execute("SELECT COUNT(*) as media_count FROM messages WHERE media_path IS NOT NULL")
        media_messages = cursor.fetchone()[0]
        
        # آخر تحديث
        cursor.execute("SELECT last_sync_time FROM sync_status WHERE id = 1")
        last_sync = cursor.fetchone()[0]
        
        conn.close()
        
        return {
            "status": "success",
            "channel": CHANNEL_USERNAME,
            "total_messages": total_messages,
            "messages_with_media": media_messages,
            "last_sync": last_sync
        }
    except Exception as e:
        logger.error(f"❌ خطأ في جلب الإحصائيات: {e}")
        return {
            "status": "error",
            "message": str(e)
        }


if __name__ == "__main__":
    import uvicorn
    
    logger.info(f"🚀 بدء الخادم على {SERVER_HOST}:{SERVER_PORT}")
    
    uvicorn.run(
        app,
        host=SERVER_HOST,
        port=SERVER_PORT,
        log_level="info"
    )
