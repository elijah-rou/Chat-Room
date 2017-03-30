# Chat Server

import socket
import select


def send_message(dest_socket, current_socket, message):
    "Send a message to a destination socket"
    if dest_socket != server_socket and dest_socket != current_socket:
        try:
            current_socket.send(message)
        except Exception:
            current_socket.close()
            CONNECTIONS.remove(current_socket)


def broadcast(current_socket, message):
    "Send message to all connnections"
    map(lambda x: send_message(x, current_socket, message), CONNECTIONS)


def send_receive(current_socket, server_socket, buff):
    "Accept new connections or send messages"
    if current_socket == server_socket:
        # new connection
        current_socket, address = server_socket.accept()
        CONNECTIONS.append(current_socket)
        print("(%s, %s) is connected" % address)
        broadcast(current_socket, "[%s:%s] entered\n" % address)
    else:
        # A client sent a message
        try:
            msg = current_socket.recv(buff)
            if msg:
                broadcast(current_socket, "\r" + "|" + str(current_socket.getpeername()) + "| " + msg)
        except Exception:
            broadcast(current_socket, "Client (%s, %s) offline" % address)
            current_socket.close()
            CONNECTIONS.remove(current_socket)


if __name__ == "__main__":
    CONNECTIONS = []
    BUFFER = 4096
    PORT = 5000

    # obtain server socket
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind(("0.0.0.0", PORT))
    server_socket.listen(10)

    # add server socket to current connections
    CONNECTIONS.append(server_socket)

    print("Server started on port " + str(PORT))

    # broadcast loop
    while(True):
        # get sockets to be read (Read, Write, Error)
        r_sock, w_sock, e_sock = select.select(CONNECTIONS, [], [])
        map(lambda x: send_receive(x, server_socket, BUFFER))
    server_socket.close()




