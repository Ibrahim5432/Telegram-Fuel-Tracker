# ملف إدارة قاعدة البيانات SQLite
import sqlite3
from datetime import datetime
import logging
from config import DATABASE_PATH

# إعداد نظام السجلات
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class Database:
    """فئة للتعامل مع قاعدة البيانات SQLite"""
    
    def __init__(self, db_path: str = DATABASE_PATH):
        self.db_path = db_path
        self.init_database()
    
    def get_connection(self):
        """الحصول على اتصال بقاعدة البيانات"""
        try:
            conn = sqlite3.connect(self.db_path)
            conn.row_factory = sqlite3.Row  # للحصول على النتائج كقاموس
            return conn
        except Exception as e:
            logger.error(f"❌ خطأ في الاتصال بقاعدة البيانات: {e}")
            raise
    
    def init_database(self):
        """إنشاء جداول قاعدة البيانات إن لم تكن موجودة"""
        try:
            conn = self.get_connection()
            cursor = conn.cursor()
            
            # إنشاء جدول الرسائل
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    message_id INTEGER UNIQUE NOT NULL,
                    text TEXT NOT NULL,
                    date TIMESTAMP NOT NULL,
                    media_path TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """)
            
            # إنشاء جدول تتبع آخر معرّف تم جلبه (offset_id)
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS sync_status (
                    id INTEGER PRIMARY KEY,
                    last_message_id INTEGER,
                    last_sync_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """)
            
            # إدراج سجل البداية إن لم يكن موجوداً
            cursor.execute("SELECT COUNT(*) FROM sync_status")
            if cursor.fetchone()[0] == 0:
                cursor.execute("""
                    INSERT INTO sync_status (id, last_message_id)
                    VALUES (1, 0)
                """)
            
            conn.commit()
            conn.close()
            logger.info("✅ تم إنشاء قاعدة البيانات بنجاح")
            
        except Exception as e:
            logger.error(f"❌ خطأ في إنشاء قاعدة البيانات: {e}")
            raise
    
    def insert_message(self, message_id: int, text: str, date: datetime, media_path: str = None):
        """إدراج رسالة جديدة في قاعدة البيانات"""
        try:
            conn = self.get_connection()
            cursor = conn.cursor()
            
            cursor.execute("""
                INSERT INTO messages (message_id, text, date, media_path)
                VALUES (?, ?, ?, ?)
            """, (message_id, text, date, media_path))
            
            conn.commit()
            conn.close()
            logger.info(f"✅ تم إدراج الرسالة #{message_id}")
            return True
            
        except sqlite3.IntegrityError:
            # الرسالة موجودة بالفعل
            logger.debug(f"⚠️ الرسالة #{message_id} موجودة بالفعل")
            return False
        except Exception as e:
            logger.error(f"❌ خطأ في إدراج الرسالة: {e}")
            return False
    
    def get_latest_messages(self, limit: int = 50):
        """الحصول على آخر الرسائل من قاعدة البيانات"""
        try:
            conn = self.get_connection()
            cursor = conn.cursor()
            
            cursor.execute("""
                SELECT id, message_id, text, date, media_path
                FROM messages
                ORDER BY date DESC
                LIMIT ?
            """, (limit,))
            
            messages = []
            for row in cursor.fetchall():
                messages.append({
                    "id": row["id"],
                    "message_id": row["message_id"],
                    "text": row["text"],
                    "date": row["date"],
                    "media_path": row["media_path"]
                })
            
            conn.close()
            return messages
            
        except Exception as e:
            logger.error(f"❌ خطأ في جلب الرسائل: {e}")
            return []
    
    def get_last_message_id(self):
        """الحصول على آخر معرّف رسالة تم جلبها (للاستئناف من حيث انقطع)"""
        try:
            conn = self.get_connection()
            cursor = conn.cursor()
            
            cursor.execute("SELECT last_message_id FROM sync_status WHERE id = 1")
            result = cursor.fetchone()
            conn.close()
            
            return result["last_message_id"] if result else 0
            
        except Exception as e:
            logger.error(f"❌ خطأ في جلب آخر معرّف: {e}")
            return 0
    
    def update_last_message_id(self, message_id: int):
        """تحديث آخر معرّف رسالة تم جلبها"""
        try:
            conn = self.get_connection()
            cursor = conn.cursor()
            
            cursor.execute("""
                UPDATE sync_status
                SET last_message_id = ?, last_sync_time = CURRENT_TIMESTAMP
                WHERE id = 1
            """, (message_id,))
            
            conn.commit()
            conn.close()
            logger.info(f"✅ تم تحديث آخر معرّف: {message_id}")
            
        except Exception as e:
            logger.error(f"❌ خطأ في تحديث المعرّف: {e}")
    
    def search_messages(self, keyword: str, limit: int = 50):
        """البحث عن رسائل تحتوي على كلمة مفتاحية معينة"""
        try:
            conn = self.get_connection()
            cursor = conn.cursor()
            
            cursor.execute("""
                SELECT id, message_id, text, date, media_path
                FROM messages
                WHERE text LIKE ?
                ORDER BY date DESC
                LIMIT ?
            """, (f"%{keyword}%", limit))
            
            messages = []
            for row in cursor.fetchall():
                messages.append({
                    "id": row["id"],
                    "message_id": row["message_id"],
                    "text": row["text"],
                    "date": row["date"],
                    "media_path": row["media_path"]
                })
            
            conn.close()
            return messages
            
        except Exception as e:
            logger.error(f"❌ خطأ في البحث: {e}")
            return []


# إنشاء نسخة عامة من قاعدة البيانات
db = Database()
