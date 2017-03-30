# chat client

import socket
import select
import sys


def query():
    "Query client"
    sys.stdout.write("|YOU| ")
    sys.stdout.flush()


def send_receive(socket, client):
    # receiving a message
    if socket == client:
        msg = socket.recv(4096)
        if not msg:
            print("Disconnected")
            sys.exit()
        else:
            sys.stdout.write(msg)
            query()
    else:
        msg = sys.stdin.readline()
        client.send(msg)
        query()


if __name__ == "__main__":
    if(len(sys.argv) != 3):
        print("Error: incorrect arguments (-> host ip, port no.)")
        sys.exit()

    ip = sys.argv[1]
    port = int(sys.argv[2])
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    socket.setdefaulttimeout(2)

    # attempt connection else quit program
    try:
        socket.connect((ip, port))
    except Exception:
        print("Connection failed")
        sys.exit()

    # Connected!
    print("Connected to " + ip)
    query()

    # broadcast loop
    while(True):
        sockets = [sys.stdin, sock]

        # get readable sockets
        r_sock, w_sock, e_sock = select.select(sockets, [], [])
        map(lambda x: send_receive(x, sock), r_sock)

