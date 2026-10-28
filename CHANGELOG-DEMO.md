# Cập nhật bản thuyết trình

## Tính năng bổ sung

- Thanh toán tiền mặt: nhập tiền khách đưa, hiển thị thiếu/thừa, nút **Khách đưa đủ**. Chỉ nhập chữ số, ví dụ `200000`.
- Lưu phương thức, tiền khách đưa và tiền thừa trên hóa đơn mới; hóa đơn cũ vẫn đọc được.
- Nút **In lại hóa đơn** trong chi tiết lịch sử giao dịch. Không tạo giao dịch mới, không trừ điểm hoặc cộng doanh thu lần nữa.

## Các lỗi đã xử lý

- Chọn suất chỉ hiện ngày có suất chưa bắt đầu; mặc định ngày gần nhất và kiểm tra lại trước khi mua.
- Hóa đơn lấy đúng tên phòng; đổi phòng của suất chưa bán tạo lại sơ đồ ghế.
- Sơ đồ lấy số hàng/cột từ dữ liệu ghế thay vì cố định 8×10; phòng 2 hiển thị đủ sơ đồ. Tooltip Sweetbox hiển thị giá cả cặp theo chiến lược ngày chiếu.
- Voucher kiểm tra hoạt động, hạn sử dụng (dùng được hết ngày hạn), giá trị đơn tối thiểu và mức giảm. Đổi ưu đãi sẽ kiểm tra lại điều kiện.
- KPI, sắp xếp, xuất báo cáo dùng chung bộ lọc; 7 ngày gần nhất gồm hôm nay và 6 ngày trước. KPI F&B được ghi rõ là trước giảm giá.
- Ghế được chọn trong cùng lượt có chung thời điểm hết hạn; kiểm tra lại khi xác nhận thanh toán. Không cho hóa đơn rỗng hoặc thanh toán sau hạn.
- Sweetbox ghi nhận người giữ; bỏ một ghế trên danh sách sẽ bỏ cả cặp; cập nhật lại sơ đồ khi hết hạn.
- Chặn sửa/xóa suất đã bán, chặn xóa phim có suất đã bán, xác nhận trước khi xóa phim.
- Mã vé/hóa đơn dùng UUID. Tổng phải trả làm tròn đến đồng VNĐ.
- Lưu theo đợt có journal phục hồi khi ghi dở. Lưu thanh toán thất bại sẽ hoàn tác ghế/điểm/hóa đơn trong bộ nhớ; lỗi in file có thể xử lý bằng in lại.
- Giá vé phải hữu hạn và lớn hơn 0; lịch mới phải ở tương lai; mã phim không trùng, thời lượng dương. Không đổi thời lượng phim đã có lịch.
- Luồng tạo ghế dùng SeatFactory; QR ghi rõ mô phỏng. Sửa script chạy PowerShell thiếu FlatLaf và ưu tiên JDK cài thực tế thay vì shim Java.

## Chính sách giá giữ nguyên

- Sinh viên dùng chiến lược giảm 20% từ giá cơ sở, thay thế giá cuối tuần.
- Voucher áp dụng sau ưu đãi sinh viên trên tổng vé + F&B; điểm trừ sau voucher.
- Chưa triển khai phụ thu giờ vàng hoặc chuyển khoản ngân hàng thật.
- Hệ số Sweetbox hiện áp dụng trên từng chỗ, chọn theo cặp gồm hai chỗ. Không đổi bảng giá hiện tại.

## Kiểm tra

Chạy `./verify.ps1`: biên dịch tương thích Java 17 và chạy các kiểm tra hồi quy trong thư mục riêng dưới `target`. Không sửa thư mục `data` thật.

Trước thuyết trình, chạy `run.bat`, tạo suất tương lai cho ngày trình bày, thử mua vé phòng 2, voucher, tiền thừa và in lại từ lịch sử. Dữ liệu hiện có không tự dời ngày để bảo toàn lịch sử của bạn.

Giới hạn: chưa xác nhận bố cục trực quan hoặc toàn bộ thao tác đầu cuối trên giao diện. Lưu JSON hỗ trợ một tiến trình ứng dụng; không chạy hai bản ứng dụng chung thư mục data.
