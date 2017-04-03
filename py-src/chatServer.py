# Chat Server

import socket
import select

CONNECTIONS = []


def send_message(conn, current_socket, server_socket, message):
    "Send a message to a destination socket"
    print("ATTEMPTING TO SEND")
    if conn != server_socket and conn != current_socket:
        try:
            print("Sending: " + message + "to " + str(conn.getsockname()))
            conn.send(message.encode('utf-8'))
        except Exception:
            print("Failed: " + str(conn.getsockname()))
            conn.close()
            CONNECTIONS.remove(conn)


def broadcast(current_socket, server_socket, message):
    "Send message to all connnections"
    # print("Broadcast: " + message)
    for conn in CONNECTIONS:
        send_message(conn, current_socket, server_socket, message)


if __name__ == "__main__":
    BUFFER = 4096
    PORT = 5000

    # obtain server socket
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_socket.bind(("0.0.0.0", PORT))
    server_socket.listen(10)

    # add server socket to current connections
    CONNECTIONS.append(server_socket)

    print("Server started on port " + str(PORT))

    # broadcast loop
    while(True):
        # get sockets to be read (Read, Write, Error)
        r_sock, w_sock, e_sock = select.select(CONNECTIONS, [], [])
        print(r_sock)
        for k in r_sock:
            "Accept new connections or send messages"
            if k == server_socket:
                # new connection
                s, address = server_socket.accept()
                CONNECTIONS.append(s)
                print("(%s, %s) is connected" % address)
                broadcast(s, server_socket, "[%s:%s] entered\n" % address)
            else:
                # A client sent a message
                try:
                    msg = k.recv(BUFFER)
                    if msg:
                        broadcast(k, server_socket, "\r" + "|" + str(k.getpeername()) + "> " + msg.decode('utf-8'))
                except Exception:
                    broadcast(k, server_socket, "Client (%s, %s) offline" % address)
                    k.close("Client (%s, %s) offline" % address)
                    CONNECTIONS.remove(k)
                    continue
    server_socket.close()




