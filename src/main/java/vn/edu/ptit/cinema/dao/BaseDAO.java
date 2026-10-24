package vn.edu.ptit.cinema.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [BASE DAO PATTERN - CHUẨN XỬ LÝ FILE I/O VỚI UTF-8 & GSON]
 * Class trừu tượng đảm bảo việc đọc/ghi dữ liệu JSON luôn tuân thủ bảng mã UTF-8,
 * ngăn chặn triệt để lỗi vỡ font tiếng Việt (kí tự dấu chấm hỏi '?').
 *
 * ĐIỂM SÁNG KỸ THUẬT:
 * 1. Ép kiểu tường minh StandardCharsets.UTF_8 qua InputStreamReader / OutputStreamWriter.
 * 2. Kết hợp BufferedReader và BufferedWriter để tối ưu hiệu năng I/O bộ đệm.
 * 3. Kỹ thuật Atomic File Replacement (ghi ra file .tmp rồi dùng Files.move ghi đè an toàn trên Windows).
 * 4. Đồng bộ hóa thread-safe tránh Race Condition giữa luồng Auto-save ngầm và luồng UI.
 */
public abstract class BaseDAO<T> {

    protected static final String DATA_DIR = "data" + File.separator;
    protected final Gson gson;
    private static final Object FILE_LOCK = new Object();

    public BaseDAO() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();

        ensureDataDirectoryExists();
    }

    private void ensureDataDirectoryExists() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Đọc danh sách List<E> từ file JSON, ép chặt mã hóa UTF-8
     */
    public <E> List<E> readListFromFile(String fileName, Type typeToken) {
        synchronized (FILE_LOCK) {
            File file = new File(DATA_DIR + fileName);
            if (!file.exists() || file.length() == 0) {
                return new ArrayList<>();
            }

            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {

                List<E> data = gson.fromJson(reader, typeToken);
                return data != null ? data : new ArrayList<>();
            } catch (Exception e) {
                System.err.println("[BaseDAO Error] Lỗi đọc List từ " + fileName + ": " + e.getMessage());
                return new ArrayList<>();
            }
        }
    }

    /**
     * Đọc bảng dữ liệu Map<K, V> từ file JSON, ép chặt mã hóa UTF-8
     */
    public <K, V> Map<K, V> readMapFromFile(String fileName, Type typeToken) {
        synchronized (FILE_LOCK) {
            File file = new File(DATA_DIR + fileName);
            if (!file.exists() || file.length() == 0) {
                return new HashMap<>();
            }

            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {

                Map<K, V> data = gson.fromJson(reader, typeToken);
                return data != null ? data : new HashMap<>();
            } catch (Exception e) {
                System.err.println("[BaseDAO Error] Lỗi đọc Map từ " + fileName + ": " + e.getMessage());
                return new HashMap<>();
            }
        }
    }

    /**
     * Ghi dữ liệu xuống file JSON theo nguyên tắc Atomic Safe-Write với UTF-8
     */
    public boolean writeToFile(String fileName, Object data) {
        synchronized (FILE_LOCK) {
            File targetFile = new File(DATA_DIR + fileName);
            File tempFile = new File(DATA_DIR + fileName + ".tmp");

            try (FileOutputStream fos = new FileOutputStream(tempFile);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 BufferedWriter writer = new BufferedWriter(osw)) {

                gson.toJson(data, writer);
                writer.flush();
            } catch (IOException e) {
                System.err.println("[BaseDAO Error] Lỗi ghi file tạm " + tempFile.getName() + ": " + e.getMessage());
                return false;
            }

            // Di chuyển và ghi đè file tạm lên file chính qua NIO Files.move
            try {
                Path tempPath = tempFile.toPath();
                Path targetPath = targetFile.toPath();
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException ex) {
                System.err.println("[BaseDAO Warning] NIO move thất bại, dùng fallback rename: " + ex.getMessage());
                if (targetFile.exists() && !targetFile.delete()) {
                    System.err.println("[BaseDAO Error] Không thể xóa file cũ: " + targetFile.getName());
                }
                return tempFile.renameTo(targetFile);
            }
        }
    }

    // =========================================================================
    // TYPE ADAPTERS CHO JAVA TIME (Định dạng thời gian chuẩn ISO/Việt Nam)
    // =========================================================================

    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(FORMATTER));
            }
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return LocalDateTime.parse(in.nextString(), FORMATTER);
        }
    }

    private static class LocalDateAdapter extends TypeAdapter<LocalDate> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public void write(JsonWriter out, LocalDate value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(FORMATTER));
            }
        }

        @Override
        public LocalDate read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return LocalDate.parse(in.nextString(), FORMATTER);
        }
    }
}
