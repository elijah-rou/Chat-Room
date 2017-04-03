# chat client

import socket
import select
import sys


def query():
    "Query client"
    sys.stdout.write("\n|YOU> ")
    sys.stdout.flush()


def send_receive(socket, server):
    # receiving a message
    if socket == server:
        data = socket.recv(4096)
        if not data:
            print("Disconnected")
            sys.exit()
        else:
            sys.stdout.write(data.decode('utf-8'))
            query()
    else:
        msg = sys.stdin.readline()
        if msg == ":q":
            sys.exit(0)
        server.send(("\033[1m" + msg + "\033[0;0m").encode('utf-8'))
        query()


if __name__ == "__main__":
    if(len(sys.argv) != 3):
        print("Error: incorrect arguments (-> host ip, port no.)")
        sys.exit()

    ip = sys.argv[1]
    port = int(sys.argv[2])
    name = sys.argv[3]
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(2)

    # attempt connection else quit program
    try:
        sock.connect((ip, port))
    except Exception:
        print("Connection failed")
        sys.exit()

    # Connected!
    print("\nConnected to " + ip + " as " + name + "\n")
    
    query()

    # broadcast loop
    while(True):
        sockets = [sys.stdin, sock]

        # get readable sockets
        r_sock, w_sock, e_sock = select.select(sockets, [], [])
        for s in r_sock:
            send_receive(s, sock)

