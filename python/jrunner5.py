import reqres_pb2
import socket
import select
import time

TIMEOUT = 30

class JRunner5Client():
    def __init__(self, client_ip, client_port):
        self.client_ip = client_ip
        self.client_port = client_port

    def send_java(self, id, input_method, input_method_name, solution_method, inputs, timeout=None):
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


# Test things

if __name__ == "__main__":
    client = JRunner5Client("127.0.0.1", 5791)
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

    response = client.send_java("1", inputMethod, inputMethodName, solutionMethod, ["1", "2"], timeout=1)

    print(f"Server returned response\n{response}")
    print(f"TIME {time.time() - stt} seconds")


# print(res.solutionOutputs)
