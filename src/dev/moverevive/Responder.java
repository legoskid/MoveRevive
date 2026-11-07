package dev.moverevive;

import de.robv.android.xposed.XposedBridge;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loopback port of mock/server.py. Request bodies carry access_token, so they are never logged.
 * Responses are newline-delimited JSON, which is what Move's Relay layer parses.
 */
final class Responder implements Runnable {
    private static final Pattern DOC_ID = Pattern.compile("(?:^|&)doc_id=(\\d+)");
    private static final String EMPTY = "{\"data\":{}}";
    private static final Map<String, String> RESPONSES = new HashMap<>();

    // FitnessTrackerNuxStepBirthdayQuery. A null date leaves that step spinning forever.
    private static final String BIRTHDAY_DOC_ID = "6344756408886152";
    private static final File BIRTHDAY_FILE = new File("/sdcard/MoveRevive/birthday.txt");
    private static final Pattern BIRTHDAY =
            Pattern.compile("^(\\d{4})-(0?[1-9]|1[0-2])-(0?[1-9]|[12]\\d|3[01])$");

    static {
        // FitnessTrackerNuxBirthdayAccountCheckerQuery
        RESPONSES.put("5900752513340345",
                "{\"data\":{\"viewer\":{\"user\":{\"has_valid_frl_account\":true,\"id\":\"1\"}}}}");
        // FitnessTrackerDashboardQuery
        RESPONSES.put("6460631687397224",
                "{\"data\":{\"viewer\":{\"fitness_sharing_requests_received\":{\"count\":0}}}}");
    }

    private final ServerSocket server;

    Responder() throws IOException {
        server = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
        Thread thread = new Thread(this, "MoveRevive-responder");
        thread.setDaemon(true);
        thread.start();
    }

    int port() {
        return server.getLocalPort();
    }

    @Override
    public void run() {
        while (true) {
            try (Socket socket = server.accept()) {
                socket.setSoTimeout(5000);
                handle(socket);
            } catch (IOException ignored) {
                // A dropped client only loses its own request. Keep serving.
            }
        }
    }

    private static void handle(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        String headers = readHeaders(in);
        String body = new String(readBody(in, contentLength(headers)), StandardCharsets.UTF_8);

        Matcher match = DOC_ID.matcher(body);
        String docId = match.find() ? match.group(1) : null;
        String json = BIRTHDAY_DOC_ID.equals(docId) ? birthdayJson() : RESPONSES.get(docId);
        byte[] payload = ((json != null ? json : EMPTY) + "\n").getBytes(StandardCharsets.UTF_8);

        String head = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: application/json\r\n"
                + "Content-Length: " + payload.length + "\r\n"
                + "Connection: close\r\n\r\n";
        OutputStream out = socket.getOutputStream();
        out.write(head.getBytes(StandardCharsets.US_ASCII));
        out.write(payload);
        out.flush();
    }

    /** Read on every request so edits apply without a rebuild. The date itself is never logged. */
    private static String birthdayJson() {
        try {
            for (String line : Files.readAllLines(BIRTHDAY_FILE.toPath(), StandardCharsets.UTF_8)) {
                Matcher date = BIRTHDAY.matcher(line.trim());
                if (date.matches()) {
                    return birthdayJson(date.group(1), date.group(2), date.group(3));
                }
            }
            XposedBridge.log("MoveRevive: no valid date in " + BIRTHDAY_FILE + ", using default");
        } catch (IOException | RuntimeException e) {
            XposedBridge.log("MoveRevive: can't read " + BIRTHDAY_FILE + ": " + e.getClass().getName());
        }
        return birthdayJson("1990", "1", "1");
    }

    private static String birthdayJson(String year, String month, String day) {
        return String.format(Locale.ROOT,
                "{\"data\":{\"viewer\":{\"user_birthdate\":{\"day\":%d,\"month\":%d,\"year\":%d}}}}",
                Integer.parseInt(day), Integer.parseInt(month), Integer.parseInt(year));
    }

    private static String readHeaders(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int matched = 0;
        while (matched < 4) {
            int b = in.read();
            if (b < 0) {
                throw new IOException("closed during headers");
            }
            buffer.write(b);
            matched = b == "\r\n\r\n".charAt(matched) ? matched + 1 : (b == '\r' ? 1 : 0);
        }
        return buffer.toString("US-ASCII");
    }

    private static int contentLength(String headers) {
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
                return Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
            }
        }
        return 0;
    }

    private static byte[] readBody(InputStream in, int length) throws IOException {
        byte[] body = new byte[length];
        int read = 0;
        while (read < length) {
            int count = in.read(body, read, length - read);
            if (count < 0) {
                throw new IOException("closed during body");
            }
            read += count;
        }
        return body;
    }
}
