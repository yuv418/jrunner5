import asyncio
import websockets

async def frame(ws, path):
    print("well, someone connected")
    await ws.send(greeting)
    name = await ws.recv()
    print(f"< {name}")

    greeting = f"Hello {name}!"
    await print(greeting)

    # print("
     
start_server = websockets.serve(frame, "localhost", 5791)

asyncio.get_event_loop().run_until_complete(start_server)
asyncio.get_event_loop().run_forever()
