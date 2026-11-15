#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <errno.h>

int main(int argc, char *argv[]) {
    if (argc < 2) return 1;

    const char *sock_path = getenv("DCLID_SOCK");
    if (!sock_path) {
        fprintf(stderr, "Error: DCLID_SOCK environment variable not set.\n");
        return 1;
    }

    char cmd[4096] = {0};
    for (int i = 1; i < argc; i++) {
        strcat(cmd, argv[i]);
        if (i < argc - 1) strcat(cmd, " ");
    }

    int sock = socket(AF_UNIX, SOCK_STREAM, 0);
    if (sock < 0) return 1;

    struct sockaddr_un server_addr;
    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sun_family = AF_UNIX;
    strncpy(server_addr.sun_path, sock_path, sizeof(server_addr.sun_path) - 1);

    if (connect(sock, (struct sockaddr*)&server_addr, sizeof(server_addr)) < 0) {
        fprintf(stderr, "Error: cannot connect to %s: %s\n", sock_path, strerror(errno));
        return 1;
    }

    write(sock, cmd, strlen(cmd));
    shutdown(sock, SHUT_WR);

    char buffer[4096];
    int n;
    while ((n = read(sock, buffer, sizeof(buffer) - 1)) > 0) {
        buffer[n] = '\0';
        printf("%s", buffer);
    }
    close(sock);
    return 0;
}
