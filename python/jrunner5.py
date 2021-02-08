import reqres_pb2
import socket
import select
import time


class JRunner5Client():
    def __init__(self, client_ip, client_port):
        self.client_ip = client_ip
        self.client_port = client_port

    def send_java(self, input_method, input_method_name, solution_method, inputs):
        req = reqres_pb2.Request()
        req.inputMethod = input_method
        req.inputMethodName = input_method_name
        req.solutionMethod = solution_method
        req.inputs.extend(inputs)

        # Turn into byte array
        req_bytes = req.SerializeToString()

        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.setblocking(True)
        s.connect((self.client_ip, self.client_port))
        s.sendall(req_bytes)

        # Receive
        data = s.recv(4096)

        response = reqres_pb2.Response()
        response.ParseFromString(data)

        return response


# Test things

if __name__ == "__main__":
    client = JRunner5Client("127.0.0.1", 5001)
    stt = time.time()

    inputMethod = """

int b = 6;
java.util.ArrayList<Double> ret = new java.util.ArrayList<Double>();
public java.util.ArrayList<Double> myMethod(double a) {
 if(a<1) return ret;
 ret.add(a);
 return myMethod(a/1.05);
}


		
    """

    inputMethodName = "myMethod"

    solutionMethod = """
    public int solution (int a) {
        return a + 11;
     }
    """

    response = client.send_java(inputMethod, inputMethodName, solutionMethod, [str(i) for i in range(5, 15)])

    print(f"Server returned response\n{response}")
    print(f"TIME {time.time() - stt} seconds")


# print(res.solutionOutputs)
