package vn.edu.ptit.cinema.service;

import com.google.gson.reflect.TypeToken;
import vn.edu.ptit.cinema.dao.BaseDAO;
import vn.edu.ptit.cinema.model.account.Customer;
import vn.edu.ptit.cinema.model.account.Role;
import vn.edu.ptit.cinema.model.account.User;
import vn.edu.ptit.cinema.model.booking.Invoice;
import vn.edu.ptit.cinema.model.booking.Voucher;
import vn.edu.ptit.cinema.model.cinema.Room;
import vn.edu.ptit.cinema.model.cinema.ShowTime;
import vn.edu.ptit.cinema.model.fnb.FoodItem;
import vn.edu.ptit.cinema.model.movie.Movie;
import vn.edu.ptit.cinema.model.seat.Seat;
import vn.edu.ptit.cinema.model.seat.SeatStatus;
import vn.edu.ptit.cinema.model.seat.SeatType;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * [SINGLETON PATTERN - TRUNG TÂM QUẢN TRỊ DỮ LIỆU IN-MEMORY VÀ ĐA LUỒNG]
 * Class DataStore đóng vai trò là "Single Source of Truth" (Kho dữ liệu duy nhất) của toàn hệ thống:
 * 
 * ĐIỂM SÁNG KỸ THUẬT PHÔ DIỄN KỸ NĂNG OOP & SENIOR PRACTICES:
 * 1. Singleton Pattern: Áp dụng Bill Pugh Singleton (Holder Idiom) - Đạt chuẩn thread-safety,
 *    lazy loading tuyệt đối mà không cần dùng synchronized block làm nghẽn cổ chai performance.
 * 2. Cấu trúc dữ liệu Concurrency-safe: Sử dụng ConcurrentHashMap và CopyOnWriteArrayList
 *    để cho phép luồng Swing UI và luồng chạy ngầm (Auto-Save, Seat Countdown) truy cập đồng thời mà không bị
 *    ConcurrentModificationException.
 * 3. Luồng chạy ngầm (Multithreading): 
 *    - Tự động lưu dữ liệu xuống file JSON theo chu kỳ (Auto-Save Daemon Thread) mỗi 30s.
 *    - Tự động quét và nhả ghế hết hạn giữ chỗ (5 phút) về trạng thái AVAILABLE.
 * 4. JVM Shutdown Hook: Đảm bảo dữ liệu luôn được flush xuống file an toàn trước khi thoát ứng dụng.
 * 
 * @author Senior Java Developer (Hướng dẫn BTL OOP - PTIT)
 */
public class DataStore {

    // =========================================================================
    // IN-MEMORY DATA STORES (Bộ nhớ RAM truy xuất tốc độ cao O(1))
    // =========================================================================
    private final Map<String, User> userMap = new ConcurrentHashMap<>();
    private final Map<String, Movie> movieMap = new ConcurrentHashMap<>();
    private final Map<String, Room> roomMap = new ConcurrentHashMap<>();
    private final Map<String, ShowTime> showTimeMap = new ConcurrentHashMap<>();
    private final Map<String, Invoice> invoiceMap = new ConcurrentHashMap<>();
    private final Map<String, Voucher> voucherMap = new ConcurrentHashMap<>();
    private final Map<String, Customer> customerMap = new ConcurrentHashMap<>();
    private final List<FoodItem> foodList = new CopyOnWriteArrayList<>();

    // DAO đa năng phục vụ lưu trữ file JSON
    private final JsonStorageDAO storageDAO = new JsonStorageDAO();

    // Scheduler quản lý các luồng chạy ngầm (Auto-Save và Seat Timeout Checker)
    private ScheduledExecutorService backgroundExecutor;

    // Thời gian tối đa giữ ghế chờ thanh toán (5 phút = 300,000 milliseconds)
    public static final long SEAT_HOLD_TIMEOUT_MILLIS = 5 * 60 * 1000L;

    // =========================================================================
    // SINGLETON PATTERN: BILL PUGH SINGLETON HOLDER
    // =========================================================================
    private DataStore() {
        System.out.println("[DataStore] Đang khởi tạo trung tâm dữ liệu In-Memory...");
        loadAllData();
        initBackgroundTasks();
        registerShutdownHook();
    }

    /**
     * Static Helper Class chịu trách nhiệm khởi tạo instance khi được gọi lần đầu
     */
    private static class InstanceHolder {
        private static final DataStore INSTANCE = new DataStore();
    }

    /**
     * Điểm truy cập toàn cục duy nhất vào DataStore
     */
    public static DataStore getInstance() {
        return InstanceHolder.INSTANCE;
    }

    // =========================================================================
    // FILE I/O - ĐỌC VÀ LƯU DỮ LIỆU JSON
    // =========================================================================

    /**
     * Đọc toàn bộ dữ liệu từ các file JSON vào bộ nhớ RAM.
     * Nếu là lần đầu chạy và file chưa có, tự động tạo dữ liệu mẫu (Seed Data) chuẩn thực tế.
     */
    public synchronized void loadAllData() {
        System.out.println("[DataStore] Bắt đầu nạp dữ liệu từ File JSON...");

        // 1. Tải Users
        Type userMapType = new TypeToken<Map<String, User>>() {}.getType();
        Map<String, User> loadedUsers = storageDAO.readMapFromFile("users.json", userMapType);
        if (loadedUsers.isEmpty()) {
            seedInitialUsers();
        } else {
            userMap.putAll(loadedUsers);
        }

        // 2. Tải Movies
        Type movieMapType = new TypeToken<Map<String, Movie>>() {}.getType();
        Map<String, Movie> loadedMovies = storageDAO.readMapFromFile("movies.json", movieMapType);
        if (loadedMovies.isEmpty()) {
            seedInitialMovies();
        } else {
            movieMap.putAll(loadedMovies);
        }

        // 3. Tải Rooms
        Type roomMapType = new TypeToken<Map<String, Room>>() {}.getType();
        Map<String, Room> loadedRooms = storageDAO.readMapFromFile("rooms.json", roomMapType);
        if (loadedRooms.isEmpty()) {
            seedInitialRooms();
        } else {
            roomMap.putAll(loadedRooms);
        }

        // 4. Tải ShowTimes
        Type showTimeMapType = new TypeToken<Map<String, ShowTime>>() {}.getType();
        Map<String, ShowTime> loadedShowTimes = storageDAO.readMapFromFile("showtimes.json", showTimeMapType);
        if (loadedShowTimes.isEmpty()) {
            seedInitialShowTimes();
        } else {
            showTimeMap.putAll(loadedShowTimes);
        }

        // [STRESS TEST FIX - VÒNG ĐỜI GHẾ]: Tắt hẳn app bật lại, toàn bộ ghế PENDING
        // chưa thanh toán bắt buộc được reset về AVAILABLE (Trống - Xanh)
        for (ShowTime st : showTimeMap.values()) {
            if (st.getSeats() != null) {
                if (!(st.getSeats() instanceof ConcurrentHashMap)) {
                    st.setSeats(new ConcurrentHashMap<>(st.getSeats()));
                }
                for (Seat s : st.getSeats().values()) {
                    if (s.getStatus() == SeatStatus.PENDING) {
                        s.setStatus(SeatStatus.AVAILABLE);
                        s.setHoldTimestamp(0);
                        s.setHeldByUserId(null);
                    }
                }
            }
        }

        // 5. Tải Invoices
        Type invoiceMapType = new TypeToken<Map<String, Invoice>>() {}.getType();
        Map<String, Invoice> loadedInvoices = storageDAO.readMapFromFile("invoices.json", invoiceMapType);
        invoiceMap.putAll(loadedInvoices);

        // 6. Tải Vouchers
        Type voucherMapType = new TypeToken<Map<String, Voucher>>() {}.getType();
        Map<String, Voucher> loadedVouchers = storageDAO.readMapFromFile("vouchers.json", voucherMapType);
        if (loadedVouchers.isEmpty()) {
            seedInitialVouchers();
        } else {
            voucherMap.putAll(loadedVouchers);
        }

        // 7. Tải F&B items
        Type foodListType = new TypeToken<List<FoodItem>>() {}.getType();
        List<FoodItem> loadedFoods = storageDAO.readListFromFile("foods.json", foodListType);
        if (loadedFoods.isEmpty()) {
            seedInitialFoods();
        } else {
            foodList.addAll(loadedFoods);
        }

        // 8. Tải Khách hàng thành viên & Chuẩn hóa tự động nâng hạng theo tổng chi tiêu
        Type customerMapType = new TypeToken<Map<String, Customer>>() {}.getType();
        Map<String, Customer> loadedCustomers = storageDAO.readMapFromFile("customers.json", customerMapType);
        if (loadedCustomers.isEmpty()) {
            seedInitialCustomers();
        } else {
            customerMap.putAll(loadedCustomers);
            boolean hasTierChange = false;
            for (Customer c : customerMap.values()) {
                if (c.checkAndUpdateTier()) {
                    hasTierChange = true;
                }
            }
            if (hasTierChange) {
                storageDAO.writeToFile("customers.json", customerMap);
            }
        }

        System.out.println("[DataStore] Nạp dữ liệu hoàn tất! Users: " + userMap.size()
                + " | Movies: " + movieMap.size()
                + " | Rooms: " + roomMap.size()
                + " | ShowTimes: " + showTimeMap.size()
                + " | Foods: " + foodList.size()
                + " | Customers: " + customerMap.size());
    }

    /**
     * Ghi toàn bộ dữ liệu RAM xuống các file JSON tương ứng (Thread-safe)
     */
    public synchronized void saveAllData() {
        System.out.println("[DataStore Background] Đang lưu dữ liệu xuống file JSON...");
        storageDAO.writeToFile("users.json", userMap);
        storageDAO.writeToFile("movies.json", movieMap);
        storageDAO.writeToFile("rooms.json", roomMap);
        storageDAO.writeToFile("showtimes.json", showTimeMap);
        storageDAO.writeToFile("invoices.json", invoiceMap);
        storageDAO.writeToFile("vouchers.json", voucherMap);
        storageDAO.writeToFile("foods.json", foodList);
        storageDAO.writeToFile("customers.json", customerMap);
        System.out.println("[DataStore Background] Lưu dữ liệu thành công lúc " + LocalDateTime.now());
    }

    // =========================================================================
    // MULTITHREADING: LUỒNG CHẠY NGẦM AUTO-SAVE VÀ KIỂM TRA TIMEOUT GHẾ
    // =========================================================================

    /**
     * Khởi tạo luồng nền Daemon Thread bằng ScheduledExecutorService
     */
    private void initBackgroundTasks() {
        backgroundExecutor = Executors.newScheduledThreadPool(2, runnable -> {
            Thread t = new Thread(runnable);
            t.setName("DataStore-BackgroundWorker");
            t.setDaemon(true); // Daemon thread tự động giải phóng khi app đóng
            return t;
        });

        // Tác vụ 1: Auto-save dữ liệu mỗi 30 giây một lần
        backgroundExecutor.scheduleWithFixedDelay(this::saveAllData, 30, 30, TimeUnit.SECONDS);

        // Tác vụ 2: Kiểm tra và giải phóng ghế Pending quá hạn (5 phút) mỗi 5 giây một lần
        backgroundExecutor.scheduleWithFixedDelay(this::releaseExpiredSeatsTask, 5, 5, TimeUnit.SECONDS);

        System.out.println("[DataStore] Đã khởi chạy Daemon Threads: Auto-Save (30s) và Seat Timeout Watcher (5s).");
    }

    /**
     * Tác vụ luồng phụ tự động giải phóng ghế đang Pending nếu quá 5 phút chưa thanh toán
     */
    private void releaseExpiredSeatsTask() {
        long current = System.currentTimeMillis();
        boolean hasChanged = false;

        for (ShowTime showTime : showTimeMap.values()) {
            if (showTime.getSeats() == null) continue;

            for (Seat seat : showTime.getSeats().values()) {
                if (seat.getStatus() == SeatStatus.PENDING) {
                    long elapsed = current - seat.getHoldTimestamp();
                    if (elapsed >= SEAT_HOLD_TIMEOUT_MILLIS) {
                        // Quá thời hạn giữ ghế -> Tự động chuyển về AVAILABLE (Xanh)
                        seat.setStatus(SeatStatus.AVAILABLE);
                        seat.setHoldTimestamp(0);
                        seat.setHeldByUserId(null);
                        hasChanged = true;
                        System.out.println("[Auto-Release] Ghế " + seat.getCode() + " tại Suất chiếu "
                                + showTime.getId() + " đã hết hạn 5 phút -> Tự động trả về Trống!");
                        
                        // Kích hoạt Observer Pattern cập nhật tức thì lên Swing UI
                        vn.edu.ptit.cinema.pattern.observer.SeatEventBus.notifySeatChanged(showTime.getId(), seat.getCode());
                    }
                }
            }
        }
    }

    /**
     * Đăng ký JVM Shutdown Hook: Đảm bảo khi người dùng nhấn tắt ứng dụng thì dữ liệu luôn được ghi đầy đủ
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[DataStore Shutdown Hook] Ứng dụng đang đóng, thực hiện lưu dữ liệu lần cuối...");
            saveAllData();
            if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
                backgroundExecutor.shutdown();
            }
        }));
    }

    // =========================================================================
    // DỮ LIỆU MẪU (SEED DATA) BAN ĐẦU CHO SINH VIÊN DEMO BẢO VỆ ĐỒ ÁN
    // =========================================================================

    private void seedInitialUsers() {
        // Tài khoản Admin quản lý rạp
        User admin = new User("U001", "admin", "admin123", "Quản Lý Rạp PTIT", "0988888888", Role.ADMIN, true);
        // Tài khoản Nhân viên bán vé
        User staff1 = new User("U002", "staff01", "123456", "Nguyễn Văn Bán Vé", "0977777777", Role.STAFF, true);
        User staff2 = new User("U003", "staff02", "123456", "Trần Thị Thu Ngân", "0966666666", Role.STAFF, true);

        userMap.put(admin.getUsername(), admin);
        userMap.put(staff1.getUsername(), staff1);
        userMap.put(staff2.getUsername(), staff2);

        storageDAO.writeToFile("users.json", userMap);
    }

    private void seedInitialMovies() {
        Movie m1 = new Movie("MOV01", "Đào, Phở và Piano", "Lịch Sử / Chiến Tranh", 100,
                LocalDate.of(2024, 2, 10), "posters/dao_pho_piano.jpg",
                "Bộ phim lịch sử tái hiện không khí hào hùng của Hà Nội mùa đông năm 1946.",
                "Phi Tiến Sơn", "C13");

        Movie m2 = new Movie("MOV02", "Dune: Part Two", "Khoa Học Viễn Tưởng", 166,
                LocalDate.of(2024, 3, 1), "posters/dune2.jpg",
                "Hành trình vĩ đại của Paul Atreides trong việc giải phóng Arrakis.",
                "Denis Villeneuve", "C16");

        Movie m3 = new Movie("MOV03", "Exhuma: Quật Mộ Trùng Ma", "Kinh Dị / Bí Ẩn", 134,
                LocalDate.of(2024, 3, 15), "posters/exhuma.jpg",
                "Câu chuyện rùng rợn về nghi thức khai quật ngôi mộ cổ bí ẩn tại Hàn Quốc.",
                "Jang Jae-hyun", "C18");

        movieMap.put(m1.getId(), m1);
        movieMap.put(m2.getId(), m2);
        movieMap.put(m3.getId(), m3);

        storageDAO.writeToFile("movies.json", movieMap);
    }

    private void seedInitialRooms() {
        Room r1 = new Room("ROOM01", "Phòng Chiếu 1 (Cinema Star)", 8, 10); // 8 hàng x 10 cột = 80 ghế
        Room r2 = new Room("ROOM02", "Phòng Chiếu 2 (IMAX Laser)", 10, 12); // 10 hàng x 12 cột = 120 ghế

        roomMap.put(r1.getId(), r1);
        roomMap.put(r2.getId(), r2);

        storageDAO.writeToFile("rooms.json", roomMap);
    }

    private void seedInitialShowTimes() {
        // Tạo sơ đồ ghế mẫu cho Room 01 (8 hàng: A -> H, 10 cột: 1 -> 10)
        Map<String, Seat> seats = generateRoomSeats(8, 10);

        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0);
        ShowTime st1 = new ShowTime("ST01", "MOV01", "ROOM01",
                now.plusHours(2), now.plusHours(4), 85000.0, seats);

        ShowTime st2 = new ShowTime("ST02", "MOV02", "ROOM01",
                now.plusHours(5), now.plusHours(8), 90000.0, generateRoomSeats(8, 10));

        showTimeMap.put(st1.getId(), st1);
        showTimeMap.put(st2.getId(), st2);

        storageDAO.writeToFile("showtimes.json", showTimeMap);
    }

    /**
     * Tạo ma trận ghế mẫu theo hàng và cột (Hàng A,B: Ghế Thường | Hàng C-F: Ghế VIP | Hàng G,H: Sweetbox)
     */
    public static Map<String, Seat> generateRoomSeats(int rows, int cols) {
        Map<String, Seat> seatMap = new ConcurrentHashMap<>();
        for (int r = 0; r < rows; r++) {
            char rowChar = (char) ('A' + r);
            for (int c = 1; c <= cols; c++) {
                String code = String.format("%c%02d", rowChar, c);
                SeatType type;
                if (r < 2) {
                    type = SeatType.STANDARD; // 2 hàng đầu là ghế thường
                } else if (r < rows - 2) {
                    type = SeatType.VIP;      // Các hàng giữa là ghế VIP
                } else {
                    type = SeatType.SWEETBOX; // 2 hàng cuối là Sweetbox
                }
                Seat seat = new Seat(code, r, c - 1, type);
                seatMap.put(code, seat);
            }
        }
        return seatMap;
    }

    private void seedInitialVouchers() {
        Voucher v1 = new Voucher("PTIT10", 10.0, 30000.0, 100000.0, LocalDate.now().plusMonths(6), true);
        Voucher v2 = new Voucher("SINHVIEN20", 20.0, 50000.0, 80000.0, LocalDate.now().plusMonths(3), true);
        Voucher v3 = new Voucher("VIPDISCOUNT", 25.0, 100000.0, 200000.0, LocalDate.now().plusMonths(12), true);

        voucherMap.put(v1.getCode(), v1);
        voucherMap.put(v2.getCode(), v2);
        voucherMap.put(v3.getCode(), v3);

        storageDAO.writeToFile("vouchers.json", voucherMap);
    }

    private void seedInitialFoods() {
        foodList.add(new FoodItem("F01", "Bắp Rang Bơ (Ngọt/Mặn) - Size M", 45000.0, "Bắp rang bơ truyền thống thơm lừng", "POPCORN"));
        foodList.add(new FoodItem("F02", "Bắp Phô Mai - Size L", 65000.0, "Bắp lắc phô mai béo ngậy đặc biệt", "POPCORN"));
        foodList.add(new FoodItem("F03", "Coca Cola Tươi - Size L", 35000.0, "Nước ngọt có ga mát lạnh sảng khoái", "DRINK"));
        foodList.add(new FoodItem("F04", "Combo Đơn (1 Bắp L + 1 Nước L)", 85000.0, "Combo tiết kiệm cho 1 người", "COMBO"));
        foodList.add(new FoodItem("F05", "Combo Đôi Couple (1 Bắp L + 2 Nước L)", 115000.0, "Combo hoàn hảo cho cặp đôi hẹn hò", "COMBO"));

        storageDAO.writeToFile("foods.json", foodList);
    }

    private void seedInitialCustomers() {
        Customer c1 = new Customer("0912345678", "Nguyễn Văn Tuấn", 50000.0, "VIP", 1500000.0);
        Customer c2 = new Customer("0988888888", "Trần Thị Mai", 120000.0, "DIAMOND", 3200000.0);
        Customer c3 = new Customer("0977112233", "Lê Hoàng Nam", 15000.0, "STANDARD", 450000.0);

        customerMap.put(c1.getPhone(), c1);
        customerMap.put(c2.getPhone(), c2);
        customerMap.put(c3.getPhone(), c3);

        storageDAO.writeToFile("customers.json", customerMap);
    }

    // =========================================================================
    // GETTERS CHO CÁC COLLECTION (Truy xuất từ Controller & Service)
    // =========================================================================

    public Map<String, User> getUserMap() {
        return userMap;
    }

    public Map<String, Movie> getMovieMap() {
        return movieMap;
    }

    public Map<String, Room> getRoomMap() {
        return roomMap;
    }

    public Map<String, ShowTime> getShowTimeMap() {
        return showTimeMap;
    }

    public Map<String, Invoice> getInvoiceMap() {
        return invoiceMap;
    }

    public Map<String, Voucher> getVoucherMap() {
        return voucherMap;
    }

    public Map<String, Customer> getCustomerMap() {
        return customerMap;
    }

    public List<FoodItem> getFoodList() {
        return foodList;
    }

    // =========================================================================
    // INNER DAO THỰC THI BASEDAO
    // =========================================================================
    private static class JsonStorageDAO extends BaseDAO<Object> {
        // Tái sử dụng toàn bộ phương thức generic đọc/ghi từ BaseDAO
    }
}
