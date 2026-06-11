import asyncio
import os
from motor.motor_asyncio import AsyncIOMotorClient
from dotenv import load_dotenv

load_dotenv()
uri = os.getenv("MONGODB_URI")
print("URI:", uri)

async def test():
    client = AsyncIOMotorClient(uri, serverSelectionTimeoutMS=5000)
    try:
        info = await client.server_info()
        print("Connected!", info.get("version"))
    except Exception as e:
        print("Error:", e)

asyncio.run(test())
