import reqres_pb2
import socket
import select
import time
import sys
import websockets

TIMEOUT = 30

import asyncio

class JRunner5Client():
    def __init__(self, client_ip, client_port, wsserver=False):
        self.client_ip = client_ip
        self.client_port = client_port
        self.ws = None

    async def ws_connect(self, ws, path):
        if path == "/frame":
            self.ws = ws
            if __name__ == "__main__":
                await self.test_send()
               

    async def send_java(self, id, input_method, input_method_name, solution_method, inputs, timeout=None):
        req = reqres_pb2.Request()
        req.id = id
        req.inputMethod = input_method
        req.inputMethodName = input_method_name
        req.solutionMethod = solution_method
        req.inputs.extend(inputs)
        if timeout:
            req.timeout = timeout

        # Turn into byte array
        req_bytes = req.SerializeToString()

        if not self.ws:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.setblocking(True)
            s.settimeout(TIMEOUT)
            s.connect((self.client_ip, self.client_port))
            s.sendall(req_bytes)

            # Receive
            data = s.recv(16192)
            response = reqres_pb2.Response()
            response.ParseFromString(data)

            return response
        else:
            # Websocket send
            stt = time.time()
            await self.ws.send(req_bytes)
            async for data in self.ws:
                response = reqres_pb2.Response()
                response.ParseFromString(data)

                print(f"Server returned response\n{response}")
                print(f"TIME {time.time() - stt} seconds")


    async def test_send(self):
        stt = time.time()

        inputMethod = """
        public int myMethod(int a){
            return a /0;
        }
        """

        inputMethodName = "myMethod"

        solutionMethod = """
        public int solution(int a) {
            return a + 1;
        }
        """

        response = await client.send_java("1", inputMethod, inputMethodName, solutionMethod, ["1", "2"], timeout=1)
        if response: # If TCP
            print(f"Server returned response\n{response}")
            print(f"TIME {time.time() - stt} seconds")


# Test things

if __name__ == "__main__":

    if len(sys.argv) > 1:
        if sys.argv[1] == 'ws':
            client = JRunner5Client("127.0.0.1", 5791, True)
            srv = websockets.serve(client.ws_connect, "localhost", 5791)
            asyncio.get_event_loop().run_until_complete(srv)
            asyncio.get_event_loop().run_forever()
    else:
        client = JRunner5Client("127.0.0.1", 5791, True)
        asyncio.get_event_loop().run_until_complete(client.test_send())
        asyncio.get_event_loop().close()
