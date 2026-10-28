# KỊCH BẢN THUYẾT TRÌNH CHỨC NĂNG — KHOẢNG 10 PHÚT

Đọc phần **Lời trình bày**, thực hiện phần *Thao tác*. Chuẩn bị dữ liệu trước; không nhập lại toàn bộ biểu mẫu trong giờ trình bày. Các mốc thời gian là mục tiêu khi tập nói.

**1. Giới thiệu — 0:00–0:25**

*Thao tác: Đứng tại màn hình đăng nhập.*

**Lời trình bày:**

“Em chào thầy. Em xin trình bày bài tập lớn hệ thống quản lý rạp chiếu phim. Ứng dụng hỗ trợ quản lý phim, sắp lịch, bán vé, bắp nước, thành viên và doanh thu. Em sẽ đi theo quy trình sử dụng: đăng nhập, chuẩn bị phim và lịch, thực hiện giao dịch, sau đó kiểm tra báo cáo.”

**2. Đăng nhập và phân quyền — 0:25–0:55**

*Thao tác: Giới thiệu hai vai trò, đăng nhập Admin bằng tài khoản chuẩn bị sẵn.*

**Lời trình bày:**

“Có hai vai trò là Staff và Admin. Staff sử dụng các tab bán vé và lấy vé online. Admin có thêm tab quản lý phim, lịch chiếu và thống kê doanh thu. Hệ thống kiểm tra tên đăng nhập, mật khẩu và trạng thái hoạt động; nhập sai hoặc tài khoản bị vô hiệu hóa sẽ không đăng nhập được. Em dùng Admin để trình bày đầy đủ chức năng.”

**3. Quản lý phim — 0:55–2:00**

*Thao tác: Mở Quản lý phim và lịch chiếu; tìm một phim, chọn dòng để hiện biểu mẫu và poster. Chỉ vào các nút thêm, cập nhật, xóa, làm mới.*

**Lời trình bày:**

“Đầu tiên, quản trị viên chuẩn bị danh mục phim. Bảng hiển thị mã, tên, thể loại, thời lượng, đạo diễn và phân loại tuổi. Có tìm kiếm và lọc thể loại để tìm nhanh phim cần xử lý. Chọn một dòng sẽ đưa dữ liệu lên biểu mẫu kèm poster.

Khi thêm phim, người dùng nhập thông tin và chọn ảnh từ máy để xem trước. Mã và tên không được trống, mã không trùng, thời lượng phải là số nguyên dương. Sau khi lưu, phim xuất hiện trong danh mục và danh sách lên lịch.

Cập nhật dùng để sửa thông tin hoặc thay poster. Nếu phim đã có lịch, hệ thống chặn đổi thời lượng để tránh sai thời gian chiếu. Xóa phim cần xác nhận và bị chặn nếu có suất đã bán vé. Nút làm mới đưa biểu mẫu về trạng thái thêm.”

**4. Tạo suất và thuật toán chống trùng giờ — 2:00–3:10**

*Thao tác: Chỉ các ô phim, ngày, giờ, phòng, giá. Dùng biểu mẫu đã chuẩn bị để thử tạo một suất trùng phòng và giờ; đóng cảnh báo.*

**Lời trình bày:**

“Để tạo suất, quản trị viên chọn phim, ngày giờ, phòng và giá cơ sở. Giá phải hữu hạn, lớn hơn 0; giờ mới phải ở tương lai. Hệ thống tính thời gian kết thúc bằng giờ bắt đầu cộng thời lượng phim và 15 phút đệm.

Ví dụ phim dài 120 phút bắt đầu lúc 18 giờ thì phòng được tính bận đến 20 giờ 15.

Trước khi lưu, hệ thống duyệt các suất cùng phòng. Hai suất trùng khi giờ bắt đầu suất mới sớm hơn giờ kết thúc suất cũ, đồng thời giờ kết thúc suất mới muộn hơn giờ bắt đầu suất cũ.

Vì vậy, suất 19 giờ ở phòng này bị từ chối; bắt đầu đúng 20 giờ 15 thì được phép nếu không vướng suất khác. Cùng giờ nhưng khác phòng vẫn hợp lệ. Khi xung đột, cảnh báo nêu phim và khung giờ đang chiếm phòng. Phép so sánh dùng cả ngày và giờ nên xét được suất qua nửa đêm.”

**5. Tra cứu, sửa và xóa suất — 3:10–3:45**

*Thao tác: Mở Quản lý và chỉnh sửa lịch chiếu; chọn một suất chưa bán vé.*

**Lời trình bày:**

“Danh sách suất có bộ lọc phim, phòng và ngày; hiển thị giờ, giá, số ghế trống và trạng thái. Chọn suất để sửa hoặc xóa. Khi sửa, thuật toán kiểm tra lại lịch nhưng bỏ qua chính suất đang sửa để tránh báo trùng với bản thân. Đổi phòng sẽ tạo lại sơ đồ ghế. Suất đã bán vé bị chặn sửa và xóa qua chức năng này. Mỗi suất có sơ đồ riêng, nên trạng thái ghế giữa các suất độc lập.”

**6. Tìm phim và chọn suất để bán — 3:45–4:20**

*Thao tác: Quay sang Bán vé; nhập một phần tên phim, xóa từ khóa, giới thiệu bộ lọc và chọn suất tương lai.*

**Lời trình bày:**

“Khi phục vụ khách, nhân viên xem phim qua poster, tìm theo tên hoặc lọc thể loại và độ tuổi. Chọn phim sẽ mở lịch theo ngày, có giờ, phòng và số ghế trống. Hệ thống ưu tiên ngày gần nhất còn suất chưa bắt đầu. Nhân viên chọn suất phù hợp để mở sơ đồ. Phân loại tuổi hiện là thông tin tham khảo, chưa tự xác minh tuổi khách.”

**7. Chọn ghế và giữ chỗ — 4:20–5:00**

*Thao tác: Chọn ghế VIP; chọn rồi bỏ một cặp Sweetbox; chỉ đồng hồ và tiền tạm tính.*

**Lời trình bày:**

“Sơ đồ thể hiện ghế thường, VIP và Sweetbox. Màu sắc phân biệt ghế trống, đang giữ và đã bán. Bấm ghế sẽ cập nhật danh sách lựa chọn và tiền tạm tính; có thể bỏ chọn hoặc hủy lượt giữ. Sweetbox thao tác theo cặp hai chỗ, có hiển thị giá cả cặp.

Số hàng, cột lấy theo dữ liệu thực tế của suất. Lượt chọn được giữ tối đa 5 phút. Hết hạn, ghế được giải phóng; trước thanh toán, hệ thống kiểm tra lại để không xuất vé từ lượt đã hết hạn.”

**8. Bắp nước — 5:00–5:20**

*Thao tác: Bấm Tiếp tục, chọn một combo, thay số lượng rồi chuyển thanh toán.*

**Lời trình bày:**

“Khách có thể chọn thêm bắp, nước hoặc combo. Mỗi món có giá, mô tả và nút tăng giảm số lượng. Tiền được cộng vào cùng giao dịch vé; không mua thêm vẫn tiếp tục được. Phần này là bán sản phẩm, chưa quản lý tồn kho.”

**9. Thành viên, giá và voucher — 5:20–6:25**

*Thao tác: Tra cứu thành viên theo số điện thoại; chỉ nút cấp thẻ; bật/tắt sinh viên và áp dụng voucher còn hạn.*

**Lời trình bày:**

“Tra cứu số điện thoại sẽ hiện tên, hạng và điểm thành viên. Khách mới có thể đăng ký bằng chức năng cấp thẻ. Có ba hạng: Standard dưới 1 triệu đồng chi tiêu, VIP từ 1 đến dưới 3 triệu, Diamond từ 3 triệu; tỷ lệ tích điểm tương ứng là 3, 5 và 7 phần trăm. Điểm có thể dùng trừ tiền; giao dịch thành công sẽ cập nhật chi tiêu và hạng.

Giá vé xét loại ghế, cuối tuần hoặc sinh viên. Theo chính sách bài tập, sinh viên giảm 20 phần trăm từ giá cơ sở rồi nhân hệ số ghế, thay thế giá cuối tuần.

Voucher kiểm tra mã, hoạt động, hạn và giá trị đơn tối thiểu; có giới hạn giảm tối đa và nút hủy. Tổng tiền được tính theo thứ tự vé cộng F&B, trừ voucher, trừ điểm rồi làm tròn đến đồng.”

**10. Thanh toán và hóa đơn — 6:25–7:15**

*Thao tác: Nhập tiền lớn hơn tổng phải trả để hiện tiền thừa; xác nhận giao dịch.*

**Lời trình bày:**

“Màn hình tách riêng tiền vé, F&B và từng khoản giảm để đối chiếu. Với tiền mặt, nhập tiền khách đưa sẽ hiện tiền thiếu hoặc tiền thừa. Nút Khách đưa đủ điền nhanh đúng tổng cần trả. Tiền không hợp lệ hoặc chưa đủ thì không xác nhận được.

QR hiện chỉ mô phỏng, chưa kết nối ngân hàng. Khi thanh toán tại quầy được lưu thành công, ghế chuyển sang đã bán, thành viên được cập nhật và hóa đơn xuất ra TXT. Hóa đơn có phim, phòng, suất, ghế, bắp nước, giảm giá, phương thức và tiền thừa. Nếu lưu thất bại, giao dịch không được báo thành công.”

**11. Lấy vé online mô phỏng — 7:15–8:00**

*Thao tác: Mở Lấy vé online; tìm đơn đã chuẩn bị bằng mã hoặc điện thoại; xem chi tiết và in vé.*

**Lời trình bày:**

“Phân hệ này minh họa khách đặt trước rồi đến nhận vé. Nhân viên tìm theo mã, điện thoại hoặc tên; lọc đơn chưa in và đã in. Chọn đơn sẽ hiện phim, phòng, suất, ghế, khách và F&B.

Xác nhận in sẽ xuất phiếu TXT, ghi nhận trạng thái và thời gian in. Đơn đã in có cảnh báo khi in lại. Có chức năng tạo đơn online giả để trình diễn, chưa có website hoặc app đặt vé thực. Trạng thái đã in chưa có nghĩa khách đã vào rạp.”

**12. Thống kê, tra cứu và in lại hóa đơn — 8:00–9:10**

*Thao tác: Mở thống kê, lọc Hôm nay, mở hóa đơn vừa bán và chỉ nút In lại. Quay về bảng, chọn sắp xếp rồi xuất báo cáo.*

**Lời trình bày:**

“Quản trị viên theo dõi doanh thu thực trả, số vé, F&B trước giảm giá và phim bán nhiều vé nhất. Đây chưa phải lợi nhuận vì chưa có chi phí.

Danh sách có mã hóa đơn, phim, thu ngân, khách, số vé, thời gian và số tiền. Mở chi tiết để kiểm tra hoặc in lại. In lại dùng hóa đơn có sẵn, không tạo giao dịch, tăng doanh thu hay tính điểm thêm lần nữa.

Bộ lọc gồm tất cả, hôm nay và 7 ngày gần nhất — tính hôm nay cùng 6 ngày trước. Có sắp xếp theo tiền hoặc ngày và xuất TXT. KPI, bảng, sắp xếp và file xuất dùng chung phạm vi lọc.

Nút xóa lịch sử dùng đặt lại dữ liệu demo, có xác nhận và trả ghế về trống; em không dùng trong lượt trình bày vì sẽ xóa giao dịch vừa tạo.”

**13. Lưu dữ liệu và kết thúc — 9:10–10:00**

*Thao tác: Giữ màn hình báo cáo, không mở thêm chức năng.*

**Lời trình bày:**

“Dữ liệu được lưu bằng JSON để sử dụng ở lần mở sau. Có tự lưu định kỳ, lưu khi thoát bình thường và thông tin phục hồi cho lần ghi bị gián đoạn. Trong thanh toán tại quầy, lưu thất bại sẽ hoàn tác cập nhật ghế, điểm và hóa đơn trong bộ nhớ; nếu chỉ xuất file lỗi sau khi giao dịch đã lưu thì có thể in lại.

Bài tập tập trung vào một máy, một tiến trình. Các chức năng liên kết từ quản lý phim và lịch đến bán vé, hóa đơn và báo cáo. QR, online và in phiếu đang mô phỏng; chưa kết nối ngân hàng hoặc máy in nhiệt thật. Hướng phát triển tiếp là soát vé và hoàn, đổi vé. Em cảm ơn thầy đã theo dõi.”

---

**Chuẩn bị trước khi trình bày — không đọc thành lời**

- Tài khoản Admin; thông tin Staff để giới thiệu, không cần đăng nhập qua lại.
- Một phim có suất tương lai, còn ghế thường, VIP và Sweetbox.
- Một suất đã có để thử tạo lịch trùng; chọn ngày tương lai, tránh lỗi giờ quá khứ xuất hiện trước cảnh báo trùng.
- Một thành viên có điểm và voucher còn hạn; bảo đảm đơn đạt mức tối thiểu kể cả khi chọn sinh viên.
- Một đơn ONLINE chưa in, đã kiểm tra trước thông tin.
- File poster nếu muốn minh họa chọn ảnh; không nhập cả phim mới trong giờ báo cáo.
- Thử quyền ghi hóa đơn/báo cáo trước buổi trình bày. Không chờ đủ 5 phút hoặc bấm xóa lịch sử khi demo.
- Nếu chậm thời gian, chỉ giới thiệu nút sửa/xóa và chức năng tạo đơn online; vẫn giữ demo cảnh báo trùng lịch và thanh toán.
