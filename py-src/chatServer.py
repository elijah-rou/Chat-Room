# Chat Server

import socket
import select

CONNECTIONS = []


def send_message(sock, current_socket, server_socket, message):
    "Send a message to a destination socket"
    if sock != server_socket and sock != current_socket:
        try:
            sock.send(message)
        except Exception:
            sock.close()
            CONNECTIONS.remove(current_socket)


def broadcast(current_socket, server_socket, message):
    "Send message to all connnections"
    for conn in CONNECTIONS:
        send_message(conn, current_socket, server_socket, message)


def send_receive(current_socket, server_socket, buff):
    "Accept new connections or send messages"
    s, address = server_socket.accept()
    if current_socket == server_socket:
        # new connection
        CONNECTIONS.append(s)
        print("(%s, %s) is connected" % address)
        broadcast(s, server_socket, "[%s:%s] entered\n" % address)
    else:
        # A client sent a message
        try:
            msg = current_socket.recv(buff)
            if msg:
                broadcast(current_socket, "\r" + "|" + str(current_socket.getpeername()) + "> " + msg.decode('utf-8'))
        except Exception:
            broadcast(current_socket, "Client (%s, %s) offline" % address)
            current_socket.close()
            CONNECTIONS.remove(current_socket)


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
        for k in r_sock:
            send_receive(k, server_socket, BUFFER)
        continue
    server_socket.close()




