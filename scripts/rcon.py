"""A minimal RCON client, for driving a real running server without a human at the console.

Why this exists: the operator-facing upgrade path has to be demonstrated in a live world, not only in
unit tests, and a scripted client cannot type into a window. RCON is Minecraft's own authenticated admin
protocol, so this uses the supported route rather than working around anything. The password is read from
the server's own `server.properties` and is never written into this repository.

Usage:
    python scripts/rcon.py "hex status"
    python scripts/rcon.py --server-properties run/probe/server.properties "setblock 0 70 0 minecraft:gold_block"
"""
import argparse
import pathlib
import socket
import struct
import sys

LOGIN = 3
COMMAND = 2


def read_properties(path):
    values = {}
    for line in pathlib.Path(path).read_text(encoding='utf-8').splitlines():
        line = line.strip()
        if not line or line.startswith('#') or '=' not in line:
            continue
        key, value = line.split('=', 1)
        values[key.strip()] = value.strip()
    return values


def send(sock, request_id, packet_type, payload):
    body = struct.pack('<ii', request_id, packet_type) + payload.encode('utf-8') + b'\x00\x00'
    sock.sendall(struct.pack('<i', len(body)) + body)


def receive(sock):
    raw_length = sock.recv(4)
    if len(raw_length) < 4:
        raise ConnectionError('the server closed the connection')
    length = struct.unpack('<i', raw_length)[0]
    data = b''
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            raise ConnectionError('the server closed the connection mid-packet')
        data += chunk
    request_id, packet_type = struct.unpack('<ii', data[:8])
    return request_id, packet_type, data[8:-2].decode('utf-8', errors='replace')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('command', nargs='+', help='the command to run, without a leading slash')
    parser.add_argument('--server-properties', default='run/probe/server.properties')
    parser.add_argument('--host', default='127.0.0.1')
    parser.add_argument('--timeout', type=float, default=20.0)
    args = parser.parse_args()

    properties = read_properties(args.server_properties)
    port = int(properties.get('rcon.port', '25575'))
    password = properties.get('rcon.password', '')
    if not password:
        print('no rcon.password in %s' % args.server_properties, file=sys.stderr)
        return 2

    command = ' '.join(args.command)
    if command.startswith('/'):
        command = command[1:]

    with socket.create_connection((args.host, port), timeout=args.timeout) as sock:
        sock.settimeout(args.timeout)
        send(sock, 1, LOGIN, password)
        request_id, _, _ = receive(sock)
        if request_id == -1:
            print('RCON authentication was refused', file=sys.stderr)
            return 3
        send(sock, 2, COMMAND, command)
        _, _, body = receive(sock)
        print(body)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
