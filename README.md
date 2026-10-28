# HỆ THỐNG QUẢN LÝ RẠP CHIẾU PHIM PTIT (CINEMA MANAGEMENT SYSTEM)

Dự án Bài tập lớn môn **Lập trình Hướng đối tượng (OOP) - Học viện Công nghệ Bưu chính Viễn thông (PTIT)**. Hệ thống mô phỏng quy trình vận hành và bán vé rạp chiếu phim chuẩn CGV & Beta Cinemas, xây dựng trên nền tảng **Java Swing FlatLaf**, lưu trữ bền vững dạng **File JSON (No-SQL)**, áp dụng các **GoF Design Patterns** và cơ chế **đa luồng (Multithreading)** thời gian thực.

---

## 🌳 CẤU TRÚC DỰ ÁN (PROJECT TREE)

```text
src\main\java\vn\edu\ptit\cinema
│
├── App.java                                    # Điểm khởi chạy chính (Main Entry Point)
│
├── dao/                                        # TẦNG TRUY XUẤT DỮ LIỆU FILE I/O
│   └── BaseDAO.java                            # Lớp cơ sở trừu tượng đọc/ghi file JSON với UTF-8
│
├── model/                                      # TẦNG MÔ HÌNH DỮ LIỆU (DATA MODELS)
│   ├── account/
│   │   ├── Customer.java                       # Khách hàng thành viên & Tích điểm hội viên
│   │   ├── Role.java                           # Phân quyền: ADMIN và STAFF
│   │   └── User.java                           # Tài khoản đăng nhập hệ thống
│   ├── booking/
│   │   ├── Invoice.java                        # Hóa đơn thanh toán giao dịch
│   │   ├── OrderItem.java                      # Chi tiết món bắp nước đã gọi
│   │   ├── Ticket.java                         # Vé xem phim (ghế, suất chiếu, giá vé)
│   │   └── Voucher.java                        # Mã giảm giá khuyến mãi
│   ├── cinema/
│   │   ├── Room.java                           # Phòng chiếu (hàng, cột, loại phòng)
│   │   └── ShowTime.java                       # Suất chiếu phim & sơ đồ ghế
│   ├── fnb/
│   │   └── FoodItem.java                       # Mặt hàng bắp, nước, combo
│   ├── movie/
│   │   └── Movie.java                          # Thông tin phim, độ tuổi, đạo diễn
│   └── seat/
│       ├── Seat.java                           # Ghế ngồi (mã ghế, tọa độ hàng/cột)
│       ├── SeatStatus.java                     # Trạng thái ghế: AVAILABLE, PENDING, BOOKED
│       └── SeatType.java                       # Hạng ghế: STANDARD, VIP, SWEETBOX
│
├── pattern/                                    # CÁC DESIGN PATTERNS ÁP DỤNG
│   ├── factory/
│   │   └── SeatFactory.java                    # Factory Pattern: Khởi tạo các hạng ghế
│   ├── observer/
│   │   ├── SeatEventBus.java                   # Subject: Bắn tín hiệu đổi trạng thái ghế
│   │   └── SeatUpdateListener.java             # Observer Interface: Lắng nghe sự kiện ghế
│   └── strategy/
│       ├── PricingStrategy.java                # Strategy Interface: Tính giá vé linh hoạt
│       └── PricingStrategies.java              # Concrete Strategies: Ngày thường, Cuối tuần, Sinh viên
│
├── service/                                    # TẦNG DỊCH VỤ & QUẢN TRỊ BỘ NHỚ RAM
│   ├── DataStore.java                          # Bill Pugh Singleton: Trung tâm dữ liệu In-Memory
│   └── SessionManager.java                     # Singleton: Quản lý phiên đăng nhập hiện tại
│
├── util/                                       # CÁC TIỆN ÍCH BỔ TRỢ HỆ THỐNG
│   ├── PosterManager.java                      # Quản lý bộ nhớ đệm RAM & tải trước ảnh Poster
│   └── ReceiptPrinter.java                     # Tiện ích mô phỏng in hóa đơn nhiệt POS ra file .txt
│
└── view/                                       # TẦNG GIAO DIỆN NGƯỜI DÙNG (SWING UI)
    ├── MainFrame.java                          # Cửa sổ chính điều khiển 3 Tab nghiệp vụ
    ├── auth/
    │   └── LoginFrame.java                     # Cửa sổ đăng nhập người dùng
    ├── component/                              # CÁC CUSTOM COMPONENT ĐỒ HỌA
    │   ├── CurvedScreenPanel.java              # Màn hình cong 3D hiệu ứng Ambient Glow
    │   ├── QrCodePanel.java                    # Mô phỏng thẻ chuyển khoản VietQR Napas 24/7
    │   └── WrapLayout.java                     # Layout tự động rớt dòng tương thích JScrollPane
    └── staff/                                  # CÁC HỘP THOẠI NGHIỆP VỤ (DIALOGS)
        ├── BetaShowtimeDialog.java             # Popup chọn suất chiếu chuẩn Beta Cinemas & CGV
        ├── CustomerManagementDialog.java       # Quản lý & cấp thẻ tích điểm hội viên
        ├── ShowtimeManagementDialog.java       # Quản lý, thêm, sửa, xóa suất chiếu
        ├── FnBDialog.java                      # Hộp thoại chọn combo bắp nước
        └── PaymentDialog.java                  # Hộp thoại thanh toán hóa đơn & in vé
```

---

## 📖 GIẢI THÍCH CHI TIẾT TỪNG PACKAGE & TỪNG FILE MÃ NGUỒN

### 1. `vn.edu.ptit.cinema` (Main App)
* **`App.java`**:
  * **Chức năng**: Lớp chứa hàm `main()` khởi chạy chương trình.
  * **Xử lý chính**: Thiết lập giao diện phẳng hiện đại **FlatLaf Light** (`FlatLightLaf.setup()`), khởi tạo trung tâm dữ liệu `DataStore` và hiển thị cửa sổ đăng nhập `LoginFrame`.

---

### 2. `vn.edu.ptit.cinema.dao` (Tầng Truy Xuất File I/O)
* **`BaseDAO.java`**:
  * **Chức năng**: Lớp trừu tượng (Abstract Class) thực thi mẫu thiết kế **DAO Pattern**, đóng gói toàn bộ logic đọc/ghi dữ liệu JSON thông qua Google Gson.
  * **Điểm sáng**:
    * Ép kiểu tường minh bảng mã `StandardCharsets.UTF_8` qua `InputStreamReader` và `OutputStreamWriter`, chống triệt để lỗi vỡ font tiếng Việt (`?`).
    * **Atomic Safe-Write**: Ghi ra file `.tmp` trước rồi dùng `Files.move()` với cờ `REPLACE_EXISTING` để ghi đè an toàn, tránh file bị rỗng (0 KB) khi tắt ứng dụng đột ngột.
    * Bắt toàn diện ngoại lệ `Exception` (bao gồm `JsonSyntaxException`), tự động trả về collection rỗng nếu file bị xóa hoặc trống (Test Trắng Tinh).

---

### 3. `vn.edu.ptit.cinema.model` (Tầng Mô Hình Dữ Liệu)

#### 🔹 Package `model.account`:
* **`User.java`**: Biểu diễn tài khoản đăng nhập của nhân viên (`username`, `password`, `fullName`, `phone`, `role`, `active`).
* **`Role.java`**: Enum phân quyền người dùng gồm `ADMIN` (Quản trị viên) và `STAFF` (Nhân viên bán vé).
* **`Customer.java`**: Khách hàng thành viên tích điểm (**Loyalty Membership**). Tự động phân hạng hội viên dựa trên tổng chi tiêu (`totalSpent`):
  * **STANDARD**: Chi tiêu < 1.000.000 VNĐ (Tích điểm 3%).
  * **VIP**: Chi tiêu 1.000.000 - 3.000.000 VNĐ (Tích điểm 5%).
  * **DIAMOND**: Chi tiêu $\ge$ 3.000.000 VNĐ (Tích điểm 7%).

#### 🔹 Package `model.booking`:
* **`Ticket.java`**: Đại diện cho vé xem phim được phát hành (`ticketId`, `showTimeId`, `seatCode`, `price`).
* **`OrderItem.java`**: Chi tiết món bắp nước kèm theo (`foodId`, `foodName`, `quantity`, `unitPrice`, `totalPrice`).
* **`Voucher.java`**: Mã giảm giá khuyến mãi (`code`, `discountPercent`, `maxDiscountAmount`, `minOrderAmount`, `expiryDate`, `active`).
* **`Invoice.java`**: Hóa đơn thanh toán hoàn chỉnh tổng hợp danh sách vé, bắp nước, mã voucher, điểm tích lũy, tiền giảm giá và số tiền cuối cùng.

#### 🔹 Package `model.cinema`:
* **`Room.java`**: Biểu diễn phòng chiếu phim (`id`, `name`, `totalRows`, `totalCols`), tự tính tổng số ghế trong phòng.
* **`ShowTime.java`**: Suất chiếu phim (`id`, `movieId`, `roomId`, `startTime`, `endTime`, `basePrice`, `seats`). Sơ đồ ghế lưu trong `ConcurrentHashMap` để an toàn đa luồng.

#### 🔹 Package `model.fnb`:
* **`FoodItem.java`**: Sản phẩm bắp nước tại quầy rạp (`id`, `name`, `price`, `description`, `category`).

#### 🔹 Package `model.movie`:
* **`Movie.java`**: Thông tin bộ phim (`id`, `title`, `genre`, `durationMinutes`, `releaseDate`, `posterPath`, `director`, `ageRating`: P, C13, C16, C18).

#### 🔹 Package `model.seat`:
* **`Seat.java`**: Đối tượng ghế ngồi (`code` e.g. A01, `row`, `col`, `type`, `status`, `holdTimestamp`, `heldByUserId`).
* **`SeatType.java`**: Enum phân loại hạng ghế: `STANDARD` (Ghế thường), `VIP` (Ghế VIP viền vàng), `SWEETBOX` (Ghế đôi hồng tím).
* **`SeatStatus.java`**: Enum trạng thái ghế: `AVAILABLE` (Trống - Xanh), `PENDING` (Đang giữ 5p - Vàng), `BOOKED` (Đã bán - Đỏ).

---

### 4. `vn.edu.ptit.cinema.pattern` (Các Mẫu Thiết Kế GoF)

#### 🔹 Package `pattern.factory`:
* **`SeatFactory.java`**: **Factory Pattern** - Đóng gói logic tạo ghế theo tọa độ hàng/cột: 2 hàng đầu là Standard, hàng giữa là VIP, 2 hàng cuối là Sweetbox đôi.

#### 🔹 Package `pattern.observer`:
* **`SeatEventBus.java`**: **Observer Pattern (Subject / Event Bus)** - Nơi đăng ký và quản lý các `SeatUpdateListener`. Khi luồng ngầm giải phóng ghế hết hạn 5 phút, nó gọi `notifySeatChanged()` để đẩy sự kiện.
* **`SeatUpdateListener.java`**: Interface cho các thành phần giao diện muốn nhận thông báo khi có ghế thay đổi trạng thái.

#### 🔹 Package `pattern.strategy`:
* **`PricingStrategy.java`**: Interface chiến lược tính giá vé (`calculatePrice`).
* **`PricingStrategies.java`**: Các lớp thực thi chiến lược cụ thể:
  * `StandardDayStrategy`: Tính giá vé ngày thường.
  * `PeakWeekendStrategy`: Phụ thu 20% vào cuối tuần (Thứ 7, CN); chưa áp dụng phụ thu giờ vàng.
  * `StudentDiscountStrategy`: Giảm giá 20% cho học sinh, sinh viên PTIT.

---

### 5. `vn.edu.ptit.cinema.service` (Tầng Dịch Vụ & Bộ Nhớ RAM)
* **`DataStore.java`**:
  * Thực thi **Bill Pugh Singleton Pattern** (sử dụng `InstanceHolder`), là kho dữ liệu duy nhất trong bộ nhớ RAM truy xuất tốc độ cao $O(1)$.
  * Sử dụng `ConcurrentHashMap` và `CopyOnWriteArrayList` đảm bảo an toàn đa luồng giữa luồng giao diện và luồng chạy ngầm.
  * Khởi chạy 2 luồng Daemon ngầm (`ScheduledExecutorService`):
    * **Auto-Save**: Đồng bộ dữ liệu xuống file JSON mỗi 30 giây.
    * **Seat Timeout Watcher**: Tự động giải phóng ghế `PENDING` quá 5 phút mỗi 5 giây.
  * **Sanitize on Reboot**: Tự động reset toàn bộ ghế giữ dở dang về trạng thái trống (Xanh) khi ứng dụng khởi động lại.
* **`SessionManager.java`**: Singleton lưu trữ tài khoản người dùng đang đăng nhập (`currentUser`).

---

### 6. `vn.edu.ptit.cinema.util` (Tiện Ích Bổ Trợ)
* **`PosterManager.java`**:
  * Quản lý bộ nhớ đệm RAM (`CACHE`) lưu trữ hình ảnh poster đã scale chuẩn tỷ lệ 2:3.
  * Chạy luồng nền (Asynchronous Preload) tải ảnh ngầm, đảm bảo luồng EDT không bị giật lag khi cuộn sảnh phim.
* **`ReceiptPrinter.java`**:
  * Định dạng và xuất phiếu hóa đơn nhiệt POS khổ 80mm ra file văn bản `.txt` UTF-8 lưu trong thư mục `receipts/`.

---

### 7. `vn.edu.ptit.cinema.view` (Tầng Giao Diện Người Dùng)

#### 🔹 Cửa sổ chính:
* **`MainFrame.java`**:
  * Cửa sổ trung tâm chia làm 3 Tab:
    1. **Bán Vé & Sơ Đồ Rạp**: Sảnh chọn phim (Movie Lobby) chuyển cảnh mượt mà sang Sơ đồ ghế 3D bằng `CardLayout`. Đồng hồ 5 phút đếm ngược không bị reset khi chuyển Tab; Thẻ tag hủy ghế lẻ có thanh cuộn mượt mà; Chống spam click (Debounce 150ms).
    2. **Quản Lý Phim & Lịch Chiếu**: Bố cục `GridBagLayout` chuẩn, tìm kiếm phim, click dòng binding form, kiểm tra chống trùng phòng chiếu. (Chỉ Admin truy cập).
    3. **Thống Kê & Doanh Thu**: 4 thẻ KPI Dashboard, lọc theo thời gian, bảng hóa đơn căn chuẩn kế toán, xuất file `BaoCaoDoanhThu_PTIT.txt`.

#### 🔹 Package `view.auth`:
* **`LoginFrame.java`**: Giao diện đăng nhập phẳng FlatLaf, hỗ trợ phím Enter, xem mật khẩu, gợi ý tài khoản mẫu.

#### 🔹 Package `view.component` (Custom Java2D Components):
* **`CurvedScreenPanel.java`**: Vẽ màn hình chiếu uốn cong 3D hướng về phía khán giả kèm dải ánh sáng chuyển sắc Ambient Light Glow Gradient.
* **`QrCodePanel.java`**: Vẽ mô phỏng thẻ thanh toán VietQR Napas 24/7 với ma trận QR Code sinh động.
* **`WrapLayout.java`**: Mở rộng từ `FlowLayout`, tự động rớt dòng linh hoạt khi chiều ngang bị thu hẹp, tương thích hoàn hảo trong `JScrollPane`.

#### 🔹 Package `view.staff` (Các Hộp Thoại Chuyên Sâu):
* **`BetaShowtimeDialog.java`**: Popup chọn suất chiếu chuẩn phong cách Beta Cinemas & CGV (Tab ngày to bản, phân loại 2D Phụ đề / Lồng tiếng, các nút giờ chiếu kèm số ghế trống thực tế).
* **`CustomerManagementDialog.java`**: Phân hệ tra cứu, đăng ký cấp thẻ tích điểm mới cho khách và tự động nâng hạng hội viên (Standard, VIP, Diamond).
* **`ShowtimeManagementDialog.java`**: Quản lý toàn bộ danh sách suất chiếu, hỗ trợ lọc, chỉnh sửa giờ/ngày/phòng/giá vé, xóa suất chiếu an toàn và kiểm tra chống xung đột lịch chiếu.
* **`FnBDialog.java`**: Pop-up chọn bắp nước, combo có bộ đếm `+`/`-`, nhãn mô tả không bị cắt chữ.
* **`PaymentDialog.java`**: Pop-up thanh toán với xác thực Regex số điện thoại, áp dụng Voucher có nút "Hủy", chiết khấu sinh viên, chọn tiền mặt / VietQR và in hóa đơn nhiệt POS.

---

## 🚀 HƯỚNG DẪN KHỞI CHẠY (QUICK START)

1. Nhấp đúp chuột vào file [`run.bat`](run.bat) để tự động biên dịch và khởi chạy chương trình 1-click trên Windows.
2. **Tài khoản dùng thử**:
   * **Quản trị viên (Admin)**: `admin` / `admin123` (Full quyền 3 Tab).
   * **Nhân viên bán vé (Staff)**: `staff01` / `123456` (Chỉ mở Tab Bán Vé & Thống Kê).
3. **Mã Voucher mẫu khi thanh toán**: `PTIT10` (Giảm 10%), `SINHVIEN20` (Giảm 20%), `VIPDISCOUNT` (Giảm 25%).


## Cập nhật bản demo

Xem [CHANGELOG-DEMO.md](CHANGELOG-DEMO.md) để biết tính năng tiền khách đưa/tiền thừa, in lại hóa đơn, các lỗi đã sửa và cách chạy kiểm tra. QR chỉ là mô phỏng; không dùng để chuyển tiền.
