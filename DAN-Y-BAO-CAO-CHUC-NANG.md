# DÀN Ý BÁO CÁO ĐẦY ĐỦ — QUẢN LÝ RẠP CHIẾU PHIM

Bản gạch ý để tự thuyết trình và thao tác, không phải lời thoại. Đi theo thứ tự dưới đây. Các lưu ý cuối tài liệu dùng để tránh giới thiệu quá phạm vi thực tế.

**1. Giới thiệu chung**

- Bài tập lớn quản lý rạp chiếu phim bằng Java desktop.
- Đối tượng: nhân viên bán vé và quản trị viên.
- Phạm vi một máy, một tiến trình ứng dụng.
- Các nhóm chức năng: tài khoản; phim; lịch chiếu; bán vé; F&B; thẻ tích điểm; ưu đãi; thanh toán/hóa đơn; nhận vé online mô phỏng; báo cáo; lưu dữ liệu.
- Luồng tổng thể: chuẩn bị phim/lịch → tiếp nhận khách → chọn phim/suất/ghế → F&B → thành viên/ưu đãi → thanh toán → xuất phiếu → thống kê.

**2. Đăng nhập, phân quyền, đăng xuất**

- Nhập tên đăng nhập, mật khẩu; có hiện/ẩn mật khẩu.
- Bấm Đăng nhập hoặc nhấn Enter; có nút thoát.
- Kiểm tra thiếu thông tin, sai tài khoản/mật khẩu, tài khoản bị vô hiệu hóa.
- Staff: tab bán vé, tab lấy vé online; mở được quản lý thẻ hội viên.
- Admin: thêm tab quản lý phim/lịch chiếu và thống kê/doanh thu; được mở quản lý sửa lịch.
- Màn hình chính hiển thị người dùng và vai trò.
- Đăng xuất để quay về đăng nhập và đổi tài khoản.
- Demo chính dùng Admin; không cần đăng nhập qua lại nếu giới hạn 10 phút.

**3. Màn hình chính và các lối truy cập**

- Tab Bán vé và sơ đồ rạp.
- Tab Lấy vé online — Kiosk & POS.
- Tab Quản lý phim và lịch chiếu, dành cho Admin.
- Tab Thống kê và doanh thu, dành cho Admin.
- Nút Thẻ hội viên ở thanh trên.
- Menu Hệ thống: thẻ tích điểm, nhận vé online, quản lý lịch, lưu ngay, đăng xuất, thoát.
- Phím tắt: Ctrl+M mở thẻ; Ctrl+O chuyển nhận vé online; Ctrl+L quản lý lịch; Ctrl+S lưu ngay.
- Menu Trợ giúp: thông tin giới thiệu ứng dụng.

**4. Quản lý phim — chuẩn bị dữ liệu trước khi bán**

Danh sách và tìm kiếm:

- Bảng có mã phim, tên, thể loại, thời lượng, đạo diễn, phân loại tuổi.
- Tìm phim bằng từ khóa và lọc thể loại.
- Chọn dòng: đưa thông tin lên biểu mẫu và xem poster.

Thêm phim:

- Nhập mã, tên, thể loại, thời lượng, đạo diễn, độ tuổi.
- Chọn ảnh poster từ máy và xem trước.
- Kiểm tra mã/tên không trống; mã không trùng; thời lượng là số nguyên dương.
- Thêm xong cập nhật danh mục và danh sách chọn phim khi tạo lịch.

Cập nhật phim:

- Chọn phim → sửa thông tin → cập nhật.
- Có thể thay poster.
- Chặn đổi thời lượng khi phim đã có lịch, tránh sai khoảng thời gian chiếu.

Xóa và làm mới:

- Xác nhận trước khi xóa phim cùng các suất liên quan được phép xóa.
- Chặn xóa nếu có suất đã bán vé.
- Làm mới biểu mẫu để trở về chế độ thêm, tránh nhầm thêm/cập nhật.

Demo: tìm một phim, chọn dòng, chỉ các trường và nút; có thể mở chọn poster, không cần thêm thật toàn bộ phim.

**5. Tạo suất chiếu và dữ liệu phòng**

- Chọn phim, phòng, ngày, giờ/phút và giá vé cơ sở.
- Giá phải là số hữu hạn lớn hơn 0; thời gian bắt đầu ở tương lai.
- Mỗi suất có mã và sơ đồ ghế riêng.
- Thời gian kết thúc khi tạo/sửa trên giao diện = bắt đầu + thời lượng phim + 15 phút đệm.
- Phòng xác định số hàng/cột và sức chứa; dữ liệu mẫu có phòng 80 và 120 ghế.
- Tạo sơ đồ cho suất mới theo phòng, ban đầu các ghế trống.
- Cùng một ghế vật lý nhưng khác suất có trạng thái đặt riêng.
- Phòng hiện được sử dụng từ dữ liệu có sẵn; chưa có đầy đủ giao diện quản lý hoặc thiết kế phòng tùy ý.

**6. Thuật toán kiểm tra không trùng giờ chiếu — điểm nhấn nghiệp vụ**

- Chạy trước khi lưu suất mới hoặc lưu sửa.
- Duyệt danh sách suất đã có.
- Chỉ so sánh các suất cùng phòng; khác phòng được phép cùng giờ.
- Khi sửa: bỏ qua chính suất đang sửa.
- Xung đột khi đồng thời có hai điều kiện:
  - Bắt đầu suất mới sớm hơn kết thúc suất đã có.
  - Kết thúc suất mới muộn hơn bắt đầu suất đã có.
- Phát hiện chồng đầu, chồng cuối, nằm trong hoặc bao phủ suất khác.
- So sánh cả ngày và giờ, bao gồm trường hợp qua nửa đêm.
- Khoảng thời gian kiểm tra gồm thời lượng phim và phần đệm.
- Nếu xung đột: không lưu; nêu phim/khung giờ đang chiếm phòng, yêu cầu đổi giờ hoặc phòng.
- Nếu không xung đột: cho lưu.
- Với N suất, một lần kiểm tra duyệt tối đa N suất; phù hợp quy mô bài tập.

Ví dụ để tự giải thích:

| Phòng 1 đang bận | Suất mới | Kết quả |
|---|---|---|
| 18:00–20:15 | Phòng 1, 19:00–21:15 | Trùng |
| 18:00–20:15 | Phòng 1, 17:00–19:15 | Trùng |
| 18:00–20:15 | Phòng 1, 18:30–19:30 | Trùng, nằm bên trong |
| 18:00–20:15 | Phòng 1, 17:00–22:00 | Trùng, bao phủ |
| 18:00–20:15 | Phòng 1, 20:15–22:30 | Không chồng suất này; còn phải kiểm tra các suất khác |
| 18:00–20:15 | Phòng 2, 19:00–21:15 | Không xung đột với phòng 1 |

Demo: chuẩn bị ngày tương lai và một suất đã có; thử tạo cùng phòng trong khoảng giờ đó để hiện đúng cảnh báo trùng.

**7. Xem, lọc, sửa và xóa suất chiếu**

- Mở từ menu quản lý lịch hoặc Ctrl+L, cần Admin.
- Danh sách: mã suất, phim, phòng, ngày, khung giờ, giá, ghế trống/tổng và trạng thái thời gian.
- Lọc theo phim, phòng, ngày.
- Chọn dòng để đưa thông tin vào biểu mẫu.
- Tạo mới, lưu cập nhật, xóa hoặc đặt lại biểu mẫu.
- Sửa suất chưa bán: kiểm tra lại giá, giờ tương lai và trùng lịch.
- Đổi phòng của suất chưa bán: tạo lại sơ đồ theo phòng mới.
- Chặn sửa/xóa suất đã bán vé qua chức năng này.
- Xóa suất chưa bán có xác nhận; sau xóa không còn suất để chọn bán.

**8. Sảnh phim và tìm kiếm khi bán vé**

- Hiển thị phim dạng poster và thông tin nhận diện.
- Tìm theo tên; danh sách cập nhật khi nhập.
- Lọc thể loại và phân loại độ tuổi; kết hợp với từ khóa.
- Chọn phim để xem ngày và suất chiếu.
- Hiển thị giờ, phòng, số ghế trống; mặc định ngày gần nhất còn suất hợp lệ.
- Luồng bán tại quầy không cho mua suất đã bắt đầu.
- Phim không còn suất phù hợp có thông báo thay vì cho chọn tùy ý.
- Nhãn tuổi giúp tư vấn; không phải xác minh tuổi tự động.

**9. Sơ đồ ghế và giữ chỗ**

- Kiểm tra phim, ngày/giờ và phòng trước khi chọn.
- Hiển thị màn hình chiếu, hàng/cột, lối đi và chú thích ghế.
- Kích thước sơ đồ dựa theo dữ liệu ghế.
- Hạng ghế: thường, VIP, Sweetbox.
- Trạng thái: trống, đang giữ, đã bán; phân biệt bằng màu/chú thích.
- Chọn ghế → đổi trạng thái giữ, thêm mã ghế vào danh sách, cập nhật tiền tạm tính.
- Bỏ ghế bằng bấm lại hoặc nút x trên danh sách; có hủy lượt chọn.
- Sweetbox chọn/bỏ theo cặp hai chỗ; tooltip hiển thị giá cả cặp.
- Không chọn được ghế đã bán.
- Lượt chọn có chung thời hạn giữ 5 phút; đồng hồ tiếp tục khi chuyển tab.
- Hết hạn trả ghế trống; kiểm tra lại hạn khi thanh toán.
- Đổi suất để chọn giờ khác; khi chuyển suất, lượt giữ đang chọn được giải phóng.
- Không giới thiệu đây là hệ thống đồng bộ giữ ghế nhiều máy.

**10. Chọn bắp nước và combo**

- Mở sau bước chọn ghế.
- Sản phẩm có tên, mô tả, giá; gồm bắp, nước và combo.
- Nút +/− điều chỉnh số lượng; tự cập nhật tổng.
- Xác nhận để đưa danh sách món sang thanh toán.
- Bỏ qua nếu khách không mua.
- Hóa đơn ghi tên món, số lượng, đơn giá/thành tiền.
- Chưa có nhập/xuất tồn kho hoặc giao diện quản trị sản phẩm đầy đủ.

**11. THẺ TÍCH ĐIỂM VÀ QUẢN LÝ HỘI VIÊN — màn hình riêng**

Mở màn hình:

- Nút Thẻ hội viên trên thanh trên.
- Menu Quản lý thẻ tích điểm & hội viên hoặc Ctrl+M.
- Từ thanh toán còn có nút Cấp thẻ mới để đăng ký nhanh.
- Nhân viên và Admin đều có thể dùng.

Danh sách thẻ:

- Các cột: STT, số điện thoại, họ tên, hạng thẻ, điểm tích lũy, tổng chi tiêu, tiến độ nâng hạng.
- Tìm theo số điện thoại hoặc tên; kết quả cập nhật khi nhập.
- Phân biệt hạng bằng màu chữ.
- Tổng chi tiêu và số điểm là hai thông tin khác nhau: chi tiêu dùng xác định hạng, điểm dùng giảm tiền.
- Tiến độ hiển thị còn cần chi bao nhiêu để lên hạng tiếp theo; Diamond hiển thị đã đạt hạng cao nhất.

Cấp thẻ mới:

- Nhập số điện thoại và họ tên.
- Có ô điểm tặng khởi tạo, mặc định 10.000 theo giá trị quy đổi trong ứng dụng.
- Biểu mẫu còn có lựa chọn hạng ban đầu; xem lưu ý về hành vi thực tế ở cuối tài liệu.
- Kiểm tra hai trường bắt buộc và số điện thoại đã đăng ký, tránh cấp trùng.
- Xác nhận cấp thẻ để thêm vào danh sách và lưu dữ liệu; có hủy đăng ký.
- Thẻ ở đây là hồ sơ hội viên quản lý bằng số điện thoại, chưa phải thẻ nhựa hoặc thẻ QR vật lý.

Hạng và quyền lợi:

| Hạng | Tổng chi tiêu | Tỷ lệ tích điểm |
|---|---|---|
| STANDARD | Dưới 1.000.000 đồng | 3% |
| VIP | Từ 1.000.000 đến dưới 3.000.000 đồng | 5% |
| DIAMOND | Từ 3.000.000 đồng | 7% |

Tại thanh toán:

- Nhập số điện thoại, tra cứu hoặc rời ô để tự tra cứu.
- Nếu có thành viên: điền tên, hiển thị hạng và điểm.
- Nếu có điểm: cho chọn dùng điểm để giảm tiền; không dùng thì giữ lại.
- Điểm được quy đổi trực tiếp sang khoản giảm trong bài tập; trừ tối đa khoản còn phải trả sau voucher.
- Thanh toán thành công: trừ điểm đã dùng, cộng điểm mới, cộng chi tiêu và cập nhật hạng.
- Điểm mới tính theo số tiền thực trả và tỷ lệ hạng trước giao dịch.
- Ví dụ: Standard trả 200.000 đồng thì được tích 6.000 điểm theo chính sách hiện tại.
- Mở lại danh sách thẻ để đối chiếu điểm, chi tiêu, hạng và tiến độ.
- Hiện chưa có đầy đủ sửa/xóa hội viên hoặc lịch sử biến động điểm riêng.

Demo riêng bắt buộc: mở danh sách → tìm hội viên → mở cấp thẻ → chỉ điểm khởi tạo → dùng hội viên khi thanh toán → quay lại kiểm tra số liệu.

**12. Giá vé và các ưu đãi**

- Giá cơ sở gắn với suất, không phải một giá cố định cho mọi phim.
- Hệ số ghế: thường 1; VIP 1,25; Sweetbox 2 trên mỗi chỗ, một cặp gồm hai chỗ.
- Ngày thường: giá cơ sở nhân hệ số ghế.
- Cuối tuần, thứ Bảy/Chủ nhật: phụ thu 20%.
- Sinh viên: giảm 20% từ giá cơ sở rồi nhân hệ số ghế, thay thế chiến lược cuối tuần.
- Không có phụ thu giờ vàng trong bản hiện tại.
- Sinh viên hiện chọn bằng checkbox, không có xác thực thẻ sinh viên tự động.

Voucher:

- Nhập mã → áp dụng; có nút hủy mã.
- Kiểm tra tồn tại, active, hạn dùng và mức đơn tối thiểu.
- Dùng được hết ngày ghi hạn; quá ngày đó là hết hạn.
- Giảm theo phần trăm, giới hạn giảm tối đa.
- Tính trên tổng vé và F&B sau khi đã xác định giá vé.
- Đổi ưu đãi làm thay đổi tiền đơn thì kiểm tra lại điều kiện mã.
- Thông báo lý do không áp dụng, không báo thành công khi đơn chưa đủ điều kiện.
- Có mã mẫu PTIT10, SINHVIEN20, VIPDISCOUNT; phải kiểm tra hạn và điều kiện trong dữ liệu đang dùng.
- Chưa có màn hình quản trị thêm/sửa/xóa voucher đầy đủ.

Thứ tự tổng tiền: tiền vé theo chính sách + F&B → trừ voucher → trừ điểm → làm tròn tổng đến đồng VNĐ.

**13. Thanh toán và hóa đơn tại quầy**

- Header: phim, suất, phòng, danh sách ghế.
- Thông tin khách: tên, điện thoại; có thể mua dưới dạng khách lẻ, không bắt buộc là hội viên.
- Kiểm tra độ dài tên và định dạng điện thoại khi có nhập.
- Tách tiền vé, tiền F&B, giảm voucher, trừ điểm, tổng phải trả.
- Quay lại khi cần điều chỉnh lựa chọn.

Tiền mặt:

- Nhập tiền khách đưa bằng chữ số VNĐ.
- Tự tính thiếu/thừa theo tổng phải trả hiện tại.
- Nút Khách đưa đủ điền nhanh số tiền bằng tổng cần trả.
- Chặn xác nhận nếu số tiền không hợp lệ hoặc chưa đủ.
- Lưu phương thức, tiền khách đưa, tiền thừa trên hóa đơn mới.

QR:

- Có lựa chọn QR mô phỏng và số tiền tương ứng.
- Chưa kết nối ngân hàng, không tự xác nhận đã nhận tiền.

Hoàn tất:

- Kiểm tra lại suất còn hợp lệ, danh sách ghế không rỗng và hạn giữ.
- Tạo vé, hóa đơn có mã UUID trong luồng tại quầy.
- Ghế chuyển sang đã bán; cập nhật hội viên.
- Lưu giao dịch rồi xuất hóa đơn TXT trong receipts.
- Nội dung phiếu: mã, thời gian, thu ngân, khách, phim, phòng, suất, ghế, F&B, giảm giá, tiền phải trả; thêm tiền khách đưa/thừa khi thanh toán tiền mặt.
- Nếu lưu thất bại: hoàn tác cập nhật ghế/điểm/hóa đơn trong RAM, không báo thanh toán thành công.
- Nếu đã lưu nhưng lỗi xuất phiếu: có thể in lại từ lịch sử, không cần thanh toán lại.

**14. Nhận vé online — mô phỏng Kiosk/POS**

- Mở tab hoặc Ctrl+O.
- Danh sách mặc định là đơn kênh ONLINE.
- Tìm theo mã, điện thoại hoặc tên; tìm kiếm còn có thể tìm đơn quầy phù hợp.
- Lọc tất cả, chưa in, đã in; có làm mới bộ lọc.
- Xem chi tiết: mã, khách, phim, suất, phòng, ghế, F&B, tiền và trạng thái in.
- Xác nhận in vé → xuất phiếu TXT, ghi nhận trạng thái và thời gian in.
- Đơn đã in: có cảnh báo trước in lại; không tuyên bố là cấm tuyệt đối in trùng.
- Mở file vé nhiệt đã xuất.
- Tạo đơn online mô phỏng: chọn phim, nhập khách/điện thoại, mã ghế, chọn combo và xác nhận để có dữ liệu demo.
- Biểu mẫu mô phỏng hiện tự lấy một suất của phim, chưa có bước chọn suất đầy đủ như bán tại quầy.
- Chưa có website/app đặt vé thật, quét mã thực hoặc tích hợp thanh toán online.
- Đã in vé không đồng nghĩa đã vào rạp; chưa có soát vé tại cửa.

**15. Thống kê, lịch sử giao dịch và in lại**

Các chỉ số:

- Doanh thu: tổng số tiền thực trả trên hóa đơn.
- Số vé: tổng số vé trên các hóa đơn được lọc.
- F&B: tổng giá trị món trước giảm giá, nhãn có ghi rõ.
- Phim bán chạy: xét số vé bán, không phải xếp theo doanh thu phim.
- Chưa phải lợi nhuận vì không có chi phí.

Lọc và tra cứu:

- Tất cả thời gian, hôm nay, 7 ngày gần nhất gồm hôm nay và 6 ngày trước.
- Bảng: mã hóa đơn, phim, thu ngân, khách, số vé, thời gian, thành tiền.
- Sắp xếp theo tiền giảm dần hoặc ngày mới trước.
- KPI, bảng, sắp xếp, xuất báo cáo dùng cùng phạm vi thời gian.

Chi tiết và in lại:

- Mở chi tiết hóa đơn để xem thông tin giao dịch.
- Mở file TXT; có nút in lại hóa đơn.
- In lại không tạo giao dịch, tăng doanh thu hoặc tính điểm thêm lần nữa.
- Xuất báo cáo TXT có phạm vi lọc và tổng doanh thu.

Xóa lịch sử:

- Có nút và cảnh báo xác nhận.
- Xóa hóa đơn, đặt lại doanh thu theo hóa đơn còn lại và trả trạng thái ghế về trống.
- Không phải đóng ca, hoàn tiền hay hoàn điểm. Chưa có hoàn tác từ giao diện.
- Chỉ giới thiệu để đặt lại dữ liệu demo; không bấm trong lượt báo cáo chính.

**16. Lưu dữ liệu, đăng xuất và thoát**

- Dữ liệu JSON: người dùng, phim, phòng, suất/ghế, hóa đơn, voucher, F&B, hội viên.
- UTF-8 hỗ trợ tiếng Việt.
- Tự lưu mỗi 30 giây; lưu khi thoát bình thường.
- Ctrl+S/menu lưu ngay để yêu cầu lưu chủ động.
- Lưu nhiều file theo đợt, có journal giữ dữ liệu trước để phục hồi lần ghi dở.
- Giữ ghế dở dang được trả về trống khi khởi động lại.
- Có đăng xuất về màn hình đăng nhập và thoát ứng dụng.
- Không đảm bảo lưu khi cưỡng bức tắt hoặc mất điện ở mọi thời điểm; không gọi JSON là cơ sở dữ liệu nhiều quầy.

**17. Các ý thuật toán và kỹ thuật nếu thầy hỏi thêm — không trình bày code**

| Nội dung | Ý cần nắm |
|---|---|
| Chống trùng lịch | So sánh hai khoảng thời gian cùng phòng; bỏ qua chính suất khi sửa |
| Tìm kiếm | Lọc dữ liệu bằng từ khóa; tìm tên không phân biệt hoa/thường ở các màn hình có áp dụng |
| Giá vé | Chọn chính sách ngày thường/cuối tuần/sinh viên, sau đó tính hệ số ghế |
| Giữ ghế | So sánh thời gian hiện tại với hạn lượt giữ; đếm ngược và tự giải phóng |
| Voucher | Kiểm tra điều kiện trước, tính phần trăm, chặn mức giảm bằng giới hạn tối đa |
| Hạng thẻ | Đối chiếu tổng chi tiêu với mốc 1 triệu và 3 triệu |
| Điểm | Trừ điểm đã dùng, cộng tỷ lệ trên thực trả, cập nhật chi tiêu/hạng |
| Báo cáo | Lọc hóa đơn, cộng doanh thu/vé/F&B; gom vé theo phim để tìm phim bán chạy |
| Sắp xếp | Theo tiền hoặc thời điểm tạo, sau khi lọc |
| Lưu phục hồi | Lưu bản trước trong journal; nếu ghi dở thì phục hồi |
| Factory | Chuẩn hóa tạo đối tượng ghế |
| Strategy | Tách các cách tính giá |
| Observer | Thông báo đổi trạng thái ghế để cập nhật giao diện |
| Singleton | Kho dữ liệu và phiên đăng nhập dùng chung trong tiến trình |

**18. Các điểm cần nói đúng, tránh giới thiệu quá mức**

- Cấp thẻ có ô chọn hạng ban đầu, nhưng hạng đang được tính lại theo tổng chi tiêu. Khách mới có chi tiêu 0 sẽ về STANDARD; không giới thiệu chọn VIP/Diamond là giữ được vĩnh viễn.
- Cấp thẻ kiểm tra bắt buộc và trùng điện thoại; chưa có kiểm tra định dạng chặt như màn hình thanh toán.
- Thẻ tích điểm là hồ sơ hội viên, không phải tính năng in thẻ vật lý.
- Chính sách giá là quy tắc của bài tập, không khẳng định áp dụng đúng bảng giá rạp ngoài thực tế.
- Luồng online mô phỏng chưa áp dụng đầy đủ mọi kiểm tra của bán tại quầy; không khẳng định hai luồng đã tương đương.
- In TXT mô phỏng phiếu POS, chưa điều khiển máy in nhiệt vật lý.
- Chưa có hoàn/đổi vé, soát vé, tồn kho, nhiều quầy hoặc quản lý tài khoản đầy đủ trên giao diện.
- 26 kiểm tra hồi quy đã chạy không thay thế thử mọi thao tác đầu cuối trên UI.

**19. Phân bổ 10 phút gợi ý**

| Phần | Thời lượng |
|---|---|
| Giới thiệu, đăng nhập, các lối truy cập | 0:45 |
| Quản lý phim, suất, demo trùng lịch | 2:30 |
| Tìm phim, chọn suất/ghế, F&B | 1:30 |
| Màn hình thẻ tích điểm riêng, cấp thẻ, hạng và điểm | 1:15 |
| Voucher, giá, thanh toán và hóa đơn | 1:30 |
| Nhận vé online mô phỏng | 0:50 |
| Báo cáo, in lại | 1:00 |
| Lưu dữ liệu, phạm vi, kết thúc | 0:40 |

Đây là danh sách đầy đủ để nắm chức năng; không cần đọc từng gạch đầu dòng. Khi demo chỉ thao tác các điểm nhấn: tìm phim, cảnh báo trùng lịch, chọn ghế, tìm/cấp thẻ, áp voucher, tiền thừa, nhận vé online, in lại và báo cáo. Các trường hợp còn lại giới thiệu hoặc dùng trả lời câu hỏi.
