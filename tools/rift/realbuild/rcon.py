#!/usr/bin/env python3
"""Минимальный RCON-клиент (Source RCON), чтобы «играть» командами из скрипта."""
import socket, struct, sys, time

class Rcon:
    def __init__(self, host="127.0.0.1", port=25575, password="rift"):
        self.sock = socket.create_connection((host, port), timeout=15)
        self.rid = 0
        self._send(3, password)
        kind, _ = self._recv()
        if kind == -1:
            raise SystemExit("RCON: неверный пароль")

    def _send(self, kind, body):
        self.rid += 1
        data = struct.pack("<ii", self.rid, kind) + body.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(data)) + data)
        return self.rid

    def _recv(self):
        raw = self._read(4)
        (size,) = struct.unpack("<i", raw)
        data = self._read(size)
        rid, kind = struct.unpack("<ii", data[:8])
        return kind, data[8:-2].decode("utf-8", "replace")

    def _read(self, n):
        buf = b""
        while len(buf) < n:
            chunk = self.sock.recv(n - len(buf))
            if not chunk:
                raise ConnectionError("RCON: соединение закрыто")
            buf += chunk
        return buf

    def cmd(self, command):
        rid = self._send(2, command)
        # сервер с broadcast-rcon-to-ops=true шлёт по пакету на каждую строку вывода:
        # читаем до пакета с нашим request id
        parts = []
        while True:
            raw = self._read(4)
            (size,) = struct.unpack("<i", raw)
            data = self._read(size)
            got, kind = struct.unpack("<ii", data[:8])
            body = data[8:-2].decode("utf-8", "replace")
            if got == rid:
                parts.append(body)
                return "\n".join(parts)
            parts.append(body)

    def close(self):
        self.sock.close()

if __name__ == "__main__":
    r = Rcon(*([sys.argv[1]] if len(sys.argv) > 1 else []))
    for c in sys.argv[2:]:
        print(f"> {c}\n{r.cmd(c)}")
