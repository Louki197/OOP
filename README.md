# HỆ THỐNG QUẢN LÝ RẠP CHIẾU PHIM TOÀN DIỆN (PTIT CINEMA)
> **BÀI TẬP LỚN MÔN LẬP TRÌNH HƯỚNG ĐỐI TƯỢNG (OOP) - HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG (PTIT)**  
> **Kiến trúc:** Pure Java Swing (FlatLaf) • In-Memory Datastore • Persistent JSON Storage • 5 GoF Design Patterns • Multithreading

---

## 📌 GIỚI THIỆU DỰ ÁN

**PTIT Cinema Management System** là hệ thống phần mềm máy tính (Desktop Application) mô phỏng trọn vẹn quy trình vận hành và bán vé của các cụm rạp chiếu phim hiện đại hàng đầu hiện nay như **CGV Cinemas, Lotte Cinema, Galaxy Cinema**. 

Dự án được xây dựng với mục tiêu phô diễn trọn vẹn các nguyên lý lập trình hướng đối tượng (**Encapsulation, Inheritance, Polymorphism, Abstraction**), kết hợp các chuẩn thiết kế phần mềm doanh nghiệp (**SOLID principles, GoF Design Patterns, Multithreading Concurrency**), vận hành hoàn toàn độc lập mà **không cần cài đặt cơ sở dữ liệu SQL bên ngoài**.

---

## 🌟 ĐIỂM SÁNG KỸ THUẬT & RÀNG BUỘC KIẾN TRÚC

1. **100% Pure Java Swing (Zero HTML/CSS):**
   * Toàn bộ các thành phần giao diện (`JLabel`, `JButton`, `JTable`, Dialogs) đều được tạo hình và định dạng bằng các lớp thuần Java (`java.awt.Font`, `Color`, `BorderFactory`, `LayoutManager`).
   * Tuyệt đối không sử dụng thẻ nhúng HTML/CSS (`<html>`, `<b>`, `<br>`), giúp ứng dụng chạy cực nhẹ, không rò rỉ bộ nhớ rendering engine và tương thích đa nền tảng.
   * Giao diện FlatLaf hiện đại, loại bỏ hoàn toàn các ký tự Unicode gây lỗi font ô vuông (`Tofu []`) trên hệ điều hành Windows.

2. **Kiến Trúc In-Memory DataStore & Lưu Trữ JSON File I/O (No-SQL):**
   * Bộ nhớ RAM đóng vai trò là **Single Source of Truth** với các cấu trúc dữ liệu đa luồng an toàn (`ConcurrentHashMap`, `CopyOnWriteArrayList`), cho tốc độ đọc/ghi thời gian thực đạt độ phức tạp $O(1)$.
   * Lưu trữ bền vững (Persistence) qua Google Gson (`gson-2.10.1.jar`).

3. **Cơ Chế I/O Chống Vỡ Font & Chống Khóa File Trên Windows:**
   * Ép kiểu tường minh bảng mã `StandardCharsets.UTF_8` qua `InputStreamReader` và `OutputStreamWriter`, bọc qua `BufferedReader` / `BufferedWriter`. Đảm bảo tiếng Việt có dấu luôn hiển thị sắc nét 100%.
   * **Atomic Safe-Write**: Dữ liệu ghi ra file tạm `.tmp` trước khi sử dụng `java.nio.file.Files.move` với cờ `StandardCopyOption.REPLACE_EXISTING`, loại trừ hoàn toàn nguy cơ file bị rỗng (0 KB) do crash hoặc Windows File Indexer đang quét.
   * **Auto-Seeding & Self-Healing (Test "Trắng tinh")**: Nếu toàn bộ thư mục `data/` bị xóa hoặc file rỗng, hệ thống tự động khởi tạo lại bộ dữ liệu mẫu chuẩn thực tế mà không bao giờ bị Crash.

4. **Xử Lý Đa Luồng Thời Gian Thực (Multithreading & Concurrency):**
   * **Auto-Save Daemon Thread**: Tự động đồng bộ toàn bộ dữ liệu RAM xuống file JSON định kỳ mỗi 30 giây.
   * **Seat Timeout Watcher Thread**: Tự động quét và giải phóng các ghế giữ tạm thời (`PENDING`) quá 5 phút về trạng thái trống (`AVAILABLE`) định kỳ mỗi 5 giây.
   * **EDT Safe**: Mọi thao tác cập nhật giao diện từ luồng ngầm đều được bọc qua `SwingUtilities.invokeLater()` để bảo vệ hàng đợi Event Dispatch Thread.
   * **State Restoration & Debounce**: Bảo lưu đồng hồ đếm ngược 5 phút khi nhân viên chuyển qua lại giữa các Tab, tích hợp cơ chế Debounce 150ms chống spam-click.

---

## 🏛️ 5 DESIGN PATTERNS (GoF) ĐƯỢC ÁP DỤNG

| Design Pattern | Lớp Thực Thi | Vai Trò & Mục Đích Kỹ Thuật |
| :--- | :--- | :--- |
| **1. Singleton** | `DataStore`, `SessionManager` | Áp dụng kỹ thuật **Bill Pugh Singleton (Holder Idiom)** để đảm bảo tính duy nhất, thread-safe tuyệt đối và lazy loading không cần `synchronized` block gây nghẽn hiệu năng. |
| **2. DAO Pattern** | `BaseDAO<T>` | Tách biệt hoàn toàn tầng logic truy xuất dữ liệu file I/O với tầng nghiệp vụ và giao diện. Tái sử dụng code Generic cho mọi thực thể (`User`, `Movie`, `ShowTime`, `Invoice`). |
| **3. Strategy** | `PricingStrategy`, `PricingStrategies` | Đa hình hóa thuật toán tính giá vé linh hoạt: Giá vé cơ sở kết hợp phụ thu theo loại ghế (Ghế thường, VIP, Sweetbox đôi), phụ thu giờ cao điểm/cuối tuần, và chiết khấu ưu đãi Sinh viên PTIT (-20%). |
| **4. Factory** | `SeatFactory` | Đóng gói logic khởi tạo các đối tượng ghế phức tạp (`Seat`) theo tọa độ ma trận hàng/cột và loại ghế tương ứng. |
| **5. Observer** | `SeatEventBus`, `SeatUpdateListener` | Cơ chế thông báo sự kiện hướng sự kiện (Pub/Sub): Khi luồng đếm ngược giải phóng ghế hết hạn, `SeatEventBus` lập tức bắn thông báo để giao diện đổi màu nút ghế từ Vàng sang Xanh lá tức thì. |

---

## 🎬 TÍNH NĂNG CHI TIẾT CỦA HỆ THỐNG

### 1. Đăng Nhập & Phân Quyền Bảo Mật
* Hỗ trợ 2 vai trò phân quyền rõ ràng:
  * **Quản trị viên (Admin)**: Toàn quyền truy cập cả 3 phân hệ (Bán vé, Quản lý phim & Lịch chiếu, Báo cáo doanh thu).
  * **Nhân viên bán vé (Staff)**: Chỉ truy cập màn hình Bán vé và Thống kê; Tab Quản lý phim tự động bị khóa và ẩn đi.
* Phím tắt `Ctrl + S` lưu dữ liệu nhanh, tính năng hiện/ẩn mật khẩu an toàn.

### 2. Tab 1: Bán Vé & Sơ Đồ Rạp (CardLayout Chuẩn CGV)
* **Sảnh Chọn Phim (Movie Lobby)**: Hiển thị danh mục phim đang chiếu dưới dạng lưới thẻ (Grid Cards) với đầy đủ thông tin: Poster, tên phim, thời lượng, đạo diễn, số suất chiếu hiện có và badge phân loại độ tuổi sắc nét (**P** xanh lá, **C13** vàng, **C16** cam, **C18** đỏ).
* **Phòng Chiếu 3D**:
  * Màn hình chiếu cong 3D (`CurvedScreenPanel`) với dải ánh sáng phát xạ Ambient Light Glow Gradient vẽ bằng Graphics2D khử răng cưa.
  * Trục tọa độ ma trận chuẩn: Cột chữ cái (A..H) ở hai bên và hàng số (1..10) ở trên.
  * Phân chia lối đi bộ thực tế (Aisles): Dãy trái (cột 1-2), Lối đi, Dãy trung tâm (cột 3-8), Lối đi, Dãy phải (cột 9-10).
  * Phân hạng ghế đồ họa: Ghế thường (xanh lá), Ghế VIP (viền vàng Gold Accent), Ghế đôi Sweetbox (nút đôi hình chữ nhật viền hồng tím).
  * Bộ lọc Cascading ComboBox độc lập: Chọn Ngày $\rightarrow$ tự lọc Giờ chiếu và Phòng chiếu rộng rãi, không bị cắt chữ.
* **Sidebar Đặt Vé**:
  * Đồng hồ đếm ngược 5 phút giữ ghế (tính theo miliseconds thời gian thực, không bị dừng khi chuyển tab).
  * Danh sách thẻ tag chips `[ MãGhế x ]` nằm trong thanh cuộn `JScrollPane`, cho phép hủy lẻ từng ghế linh hoạt.
  * Nút `< Đổi Phim Khác` tích hợp hộp thoại xác nhận (`ConfirmDialog`) tránh làm mất ghế đang chọn của khách.

### 3. Pop-up Bắp Nước (FnBDialog)
* Giao diện Card danh mục bắp rang bơ, nước ngọt có ga và combo tiết kiệm.
* Bộ điều khiển số lượng `+` / `-` trực quan, văn bản mô tả hiển thị sắc nét đầy đủ, không bị cắt cụt.
* Tự động tổng hợp danh sách món ăn và tính tiền bắp nước sang hóa đơn.

### 4. Pop-up Thanh Toán & Xuất Hóa Đơn (PaymentDialog)
* **Xác thực dữ liệu (Regex Validation)**: Kiểm tra chuẩn định dạng số điện thoại Việt Nam (`^0[0-9]{9,10}$`) và giới hạn độ dài tên khách hàng.
* **Hệ thống Khuyến mãi (Voucher)**: Nhập mã (`PTIT10`, `SINHVIEN20`), tự động khóa ô nhập và chuyển nút thành *"Đã áp dụng"*, bổ sung nút *"Hủy"* để hoàn tác tiện lợi. Checkbox ưu đãi Sinh viên PTIT (-20%).
* **Đa dạng thanh toán**:
  * Tiền mặt tại quầy (hướng dẫn giao dịch thu ngân).
  * Chuyển khoản VietQR Napas 24/7: Vẽ mô phỏng thẻ ngân hàng Napas kèm ma trận QR Code sinh động.
* **Mô phỏng máy in nhiệt POS**: Tự động xuất file hóa đơn nhiệt `.txt` chuẩn UTF-8 lưu vào thư mục `receipts/`.

### 5. Tab 2: Quản Lý Phim & Xếp Lịch Chiếu (Admin)
* Thanh tìm kiếm tên phim thời gian thực + lọc thể loại + nút *"Làm mới"* + nhãn đếm tổng số phim.
* Bảng danh sách phim: Tối ưu độ rộng cột, căn giữa mã và thời lượng, hỗ trợ click chọn dòng để tự động binding thông tin ngược lên Form.
* **Form 1: Thêm Phim Mới** (Bố cục `GridBagLayout` 4 cột cân xứng): Nhập thông tin tác phẩm kèm bộ nút *"Nhập lại"*, *"Xóa Phim"*, *"+ Lưu Phim"*.
* **Form 2: Lên Lịch Suất Chiếu** (Bố cục `GridBagLayout` 2 cột thẳng hàng): Chủ động ghép phim với ngày chiếu (7 ngày tới), giờ/phút bắt đầu (JSpinner), phòng chiếu (Standard 80 ghế hoặc IMAX Laser 120 ghế), và cài đặt giá vé cơ sở.
* **Chặn xung đột lịch chiếu**: Tự động phát hiện và cảnh báo nếu phòng chiếu đã có phim khác đang chiếu trong cùng khung giờ.

### 6. Tab 3: Thống Kê & Báo Cáo Doanh Thu
* 4 Thẻ Dashboard KPI trực quan: Tổng doanh thu, Tổng số vé bán, Doanh thu F&B, Top 1 Phim ăn khách nhất.
* Bộ lọc thời gian: *Tất cả thời gian, Hôm nay, 7 ngày gần nhất*.
* Bảng lịch sử hóa đơn chuẩn tài chính: Cột tiền tệ căn lề phải in đậm màu đỏ.
* Sắp xếp giao dịch theo doanh thu giảm dần hoặc theo thời gian tạo.
* Tính năng xuất toàn bộ báo cáo doanh thu ra file văn bản `BaoCaoDoanhThu_PTIT.txt`.

---

## 📂 CẤU TRÚC THƯ MỤC NGUỒN (PROJECT STRUCTURE)

```text
c:\OOP
├── bin/                                # Thư mục chứa bytecode (.class) sau khi biên dịch
├── data/                               # Thư mục lưu trữ các file JSON (No-SQL Database)
│   ├── foods.json                      # Dữ liệu bắp nước combo
│   ├── invoices.json                   # Lịch sử giao dịch hóa đơn
│   ├── movies.json                     # Danh mục phim
│   ├── rooms.json                      # Danh mục phòng chiếu
│   ├── showtimes.json                  # Lịch chiếu và sơ đồ ghế thời gian thực
│   ├── users.json                      # Danh sách tài khoản nhân viên & admin
│   └── vouchers.json                   # Danh sách mã giảm giá
├── lib/                                # Thư mục chứa thư viện bên thứ ba (.jar)
│   ├── flatlaf-3.4.1.jar               # Giao diện phẳng FlatLaf hiện đại
│   └── gson-2.10.1.jar                 # Thư viện tuần tự hóa/giải tuần tự hóa JSON
├── receipts/                           # Thư mục chứa hóa đơn nhiệt xuất ra (.txt)
├── src/main/java/vn/edu/ptit/cinema/   # Mã nguồn chính của dự án
│   ├── App.java                        # Entry point chính của ứng dụng
│   ├── dao/
│   │   └── BaseDAO.java                # Lớp cơ sở trừu tượng xử lý File I/O JSON UTF-8
│   ├── model/
│   │   ├── account/ (Role, User)       # Thực thể tài khoản và phân quyền
│   │   ├── booking/ (Invoice, Ticket, Voucher, OrderItem) # Thực thể giao dịch và vé
│   │   ├── cinema/ (Room, ShowTime)    # Thực thể phòng chiếu và suất chiếu
│   │   ├── fnb/ (FoodItem)             # Thực thể bắp nước
│   │   ├── movie/ (Movie)              # Thực thể phim
│   │   └── seat/ (Seat, SeatType, SeatStatus) # Thực thể ghế ngồi
│   ├── pattern/
│   │   ├── factory/ (SeatFactory)      # Factory Pattern tạo ghế
│   │   ├── observer/ (SeatEventBus, SeatUpdateListener) # Observer Pattern bắn sự kiện
│   │   └── strategy/ (PricingStrategy, PricingStrategies) # Strategy Pattern tính giá
│   ├── service/
│   │   ├── DataStore.java              # Singleton trung tâm quản trị RAM & luồng ngầm
│   │   └── SessionManager.java         # Singleton quản lý phiên đăng nhập hiện tại
│   ├── util/
│   │   └── ReceiptPrinter.java         # Tiện ích mô phỏng in hóa đơn POS nhiệt
│   └── view/
│       ├── MainFrame.java              # Cửa sổ chính điều khiển 3 Tab nghiệp vụ
│       ├── auth/ (LoginFrame)          # Cửa sổ đăng nhập người dùng
│       ├── component/                  # Custom Graphics2D components (Màn hình 3D, VietQR)
│       └── staff/ (FnBDialog, PaymentDialog) # Hộp thoại bán bắp nước & thanh toán
├── pom.xml                             # Cấu hình dự án Apache Maven
├── run.bat                             # Script chạy nhanh 1-Click trên Windows
└── README.md                           # Tài liệu hướng dẫn đồ án
```

---

## 🔑 TÀI KHOẢN MẪU THỬ NGHIỆM (DEMO ACCOUNTS)

Hệ thống đã chuẩn bị sẵn các tài khoản demo chuẩn trong cơ sở dữ liệu:

| Tên Đăng Nhập | Mật Khẩu | Họ Và Tên | Vai Trò (Role) | Quyền Hạn |
| :--- | :--- | :--- | :--- | :--- |
| **admin** | `admin123` | Quản Lý Rạp PTIT | **ADMIN** | Toàn quyền 3 Tab (Bán vé, Quản lý phim/lịch chiếu, Thống kê) |
| **staff01** | `123456` | Nguyễn Văn Bán Vé | **STAFF** | Bán vé, thanh toán, xem thống kê (Khóa Tab Quản lý phim) |
| **staff02** | `123456` | Trần Thị Thu Ngân | **STAFF** | Bán vé, thanh toán, xem thống kê (Khóa Tab Quản lý phim) |

> **Voucher mẫu thử nghiệm khi thanh toán:**
> * `PTIT10`: Giảm 10% (áp dụng đơn từ 100k, giảm tối đa 30k).
> * `SINHVIEN20`: Giảm 20% (áp dụng đơn từ 80k, giảm tối đa 50k).
> * `VIPDISCOUNT`: Giảm 25% (áp dụng đơn từ 200k, giảm tối đa 100k).

---

## 🚀 HƯỚNG DẪN BIÊN DỊCH & KHỞI CHẠY (QUICK START)

### Cách 1: Chạy 1-Click bằng file `run.bat` (Khuyên dùng trên Windows)
Chỉ cần nhấp đúp chuột vào file [`run.bat`](file:///c:/OOP/run.bat) ở thư mục gốc của dự án. Script sẽ tự động:
1. Tạo thư mục `bin/` nếu chưa có.
2. Quét toàn bộ mã nguồn Java và biên dịch tự động bằng `javac` với bảng mã UTF-8.
3. Nạp đầy đủ thư viện `lib/flatlaf-3.4.1.jar` và `lib/gson-2.10.1.jar`.
4. Khởi chạy ứng dụng đồ họa ngay lập tức.

### Cách 2: Biên dịch thủ công bằng Command Line (cmd / PowerShell)
Mở cửa sổ dòng lệnh tại thư mục dự án `c:\OOP` và chạy các lệnh sau:

```cmd
:: 1. Tạo thư mục chứa file biên dịch
if not exist bin mkdir bin

:: 2. Quét danh sách file nguồn Java
dir /s /b src\main\java\*.java > sources.txt

:: 3. Biên dịch toàn bộ mã nguồn với encoding UTF-8
javac -cp "lib\gson-2.10.1.jar;lib\flatlaf-3.4.1.jar" -encoding UTF-8 -d bin @sources.txt

:: 4. Xóa file tạm
del sources.txt

:: 5. Khởi chạy ứng dụng
java -cp "bin;lib\gson-2.10.1.jar;lib\flatlaf-3.4.1.jar" vn.edu.ptit.cinema.App
```

### Cách 3: Chạy thông qua Maven
Nếu môi trường máy tính của bạn đã cài đặt sẵn Apache Maven:
```bash
# Biên dịch và đóng gói dự án
mvn clean compile

# Chạy ứng dụng qua exec-maven-plugin
mvn exec:java -Dexec.mainClass="vn.edu.ptit.cinema.App"
```

---

## 🧪 KẾT QUẢ KIỂM THỬ STRESS TEST CHỨNG THỰC

* [x] **Test Vòng đời ghế & Reboot**: Bấm giữ ghế chuyển sang màu Vàng (Pending). Tắt hẳn ứng dụng rồi bật lại ngay $\rightarrow$ Ghế tự động hoàn trả về màu Xanh (Available/Trống), không bị kẹt màu vàng vĩnh viễn trong file JSON.
* [x] **Test Độ lỳ ComboBox**: Khi đổi Ngày chiếu, toàn bộ Giờ chiếu của ngày cũ lập tức được làm rỗng, không lưu rác suất chiếu cũ.
* [x] **Test Phá hoại (Spam Click)**: Click chuột dồn dập vào ghế được bộ lọc Debounce 150ms chặn lại mượt mà, không gây nghẽn Thread.
* [x] **Test "Trắng tinh" (Empty Data Directory)**: Xóa trắng toàn bộ file trong thư mục `data/` $\rightarrow$ Ứng dụng tự động kích hoạt chế độ Seed Data mẫu, tạo lại đầy đủ dữ liệu phim, phòng và người dùng, không bao giờ văng Crash.
* [x] **Test Nhập liệu âm / Ký tự lạ**: Bộ lọc biểu thức chính quy (Regex) chặn đứng mọi chuỗi text siêu dài hoặc số điện thoại rác tại cổng thanh toán.

---

## 👨‍💻 BẢN QUYỀN ĐỒ ÁN
* **Đề tài:** Hệ Thống Quản Lý Rạp Chiếu Phim Toàn Diện (Comprehensive Cinema Management System).
* **Đơn vị:** Học viện Công nghệ Bưu chính Viễn thông (PTIT).
* **Học phần:** Lập trình Hướng đối tượng (Object-Oriented Programming).
* **Năm thực hiện:** 2026.
