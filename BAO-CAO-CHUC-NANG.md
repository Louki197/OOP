# BÁO CÁO CHỨC NĂNG — HỆ THỐNG QUẢN LÝ RẠP CHIẾU PHIM

**Phạm vi:** Bài tập lớn Java, ứng dụng desktop phục vụ thao tác tại một máy. Nội dung mô tả bản mã nguồn hiện tại, không trình bày code.

## 1. Mục tiêu và đối tượng sử dụng

Ứng dụng hỗ trợ các công việc chính tại quầy rạp: tra cứu phim và lịch chiếu, chọn ghế, bán bắp nước, áp dụng ưu đãi, thanh toán và xuất hóa đơn. Ngoài ra, ứng dụng có chức năng quản lý phim, sắp xếp suất chiếu, theo dõi doanh thu và mô phỏng lấy vé đã đặt online.

Hai vai trò sử dụng:

| Vai trò | Chức năng trên giao diện chính |
|---|---|
| Nhân viên | Bán vé tại quầy; tra cứu và xử lý lấy vé online |
| Quản trị viên | Các chức năng của nhân viên; thêm tab quản lý phim/lịch chiếu và thống kê/doanh thu |

Ứng dụng kiểm tra thông tin đăng nhập và trạng thái hoạt động của tài khoản. Sau khi đăng nhập, giao diện hiển thị thông tin người dùng và các tab tương ứng.

## 2. Bán vé tại quầy

### 2.1. Tra cứu và chọn phim

- Hiển thị danh sách phim bằng poster và thông tin nhận diện.
- Tìm theo tên phim, lọc theo thể loại và phân loại độ tuổi.
- Chọn phim để mở danh sách ngày và suất chiếu tương ứng.
- Phân loại tuổi là thông tin tham khảo trên giao diện; ứng dụng chưa tự xác minh tuổi khách.

**Ý nghĩa:** Giúp nhân viên nhanh chóng tìm phim khách yêu cầu và tư vấn lựa chọn.

### 2.2. Chọn suất chiếu

- Hiển thị ngày chiếu, giờ bắt đầu, phòng và số ghế còn trống.
- Mặc định chọn ngày gần nhất còn suất chưa bắt đầu.
- Luồng bán tại quầy không cho chọn mua suất đã bắt đầu.
- Cho phép đổi suất để đáp ứng nhu cầu khách.

**Thông tin liên kết:** Một suất gắn với một phim, một phòng, thời gian bắt đầu/kết thúc, giá cơ sở và sơ đồ ghế riêng.

### 2.3. Chọn ghế và giữ chỗ

Ba loại ghế gồm ghế thường, VIP và Sweetbox. Sweetbox được chọn và bỏ chọn theo cặp hai chỗ. Sơ đồ lấy kích thước từ dữ liệu ghế của suất, không cố định một kích thước cho mọi phòng.

Các trạng thái được phân biệt bằng màu và chú thích:

| Trạng thái | Ý nghĩa |
|---|---|
| Trống | Có thể chọn mua |
| Đang giữ | Đã chọn, đang chờ hoàn tất thanh toán |
| Đã bán | Không thể tiếp tục chọn mua |

- Ghế đã chọn xuất hiện trong danh sách tóm tắt và được cộng vào tiền vé tạm tính.
- Người dùng có thể bỏ từng ghế, bỏ cả cặp Sweetbox hoặc hủy lượt chọn.
- Một lượt chọn có thời hạn giữ 5 phút và đồng hồ đếm ngược.
- Khi hết hạn, ghế được giải phóng. Luồng thanh toán kiểm tra lại hạn giữ để tránh xuất vé từ lượt chọn đã hết hạn.

**Ý nghĩa:** Thể hiện rõ quá trình từ chọn chỗ đến bán vé, đồng thời xử lý trường hợp khách không tiếp tục mua.

### 2.4. Bắp nước và combo

- Hiển thị sản phẩm với tên, mô tả và giá.
- Chọn bắp, nước hoặc combo; tăng/giảm số lượng.
- Tổng tiền F&B được cộng vào cùng giao dịch mua vé.
- Khách có thể tiếp tục thanh toán mà không chọn thêm F&B.

Phần này đang hỗ trợ chọn bán sản phẩm trong đơn; chưa có quản lý nhập kho hoặc tồn kho.

## 3. Tính giá, khuyến mãi và thành viên

### 3.1. Chính sách giá vé

| Trường hợp | Chính sách hiện tại |
|---|---|
| Ghế thường | Hệ số 1 lần giá cơ sở |
| VIP | Hệ số 1,25 lần giá cơ sở |
| Sweetbox | Hệ số 2 lần giá cơ sở trên mỗi chỗ; một cặp có hai chỗ |
| Cuối tuần | Phụ thu 20% vào thứ Bảy và Chủ nhật |
| Sinh viên | Giảm 20% từ giá cơ sở, sau đó nhân hệ số ghế; thay thế chiến lược cuối tuần |

Không trình bày rằng ứng dụng có phụ thu giờ vàng vì phần này chưa được triển khai.

### 3.2. Voucher

- Nhập mã và áp dụng hoặc hủy voucher.
- Kiểm tra mã tồn tại, đang hoạt động, còn hạn và đơn đạt mức tối thiểu.
- Mức giảm được tính theo phần trăm và có giới hạn số tiền giảm tối đa.
- Khi thay đổi ưu đãi sinh viên, điều kiện voucher được kiểm tra lại.
- Nếu không hợp lệ, giao diện nêu lý do thay vì chỉ báo thành công.

Mã mẫu được khởi tạo gồm PTIT10, SINHVIEN20 và VIPDISCOUNT. Khi demo cần dùng mã thực tế còn hợp lệ trong dữ liệu.

### 3.3. Thành viên

- Tra cứu khách bằng số điện thoại.
- Đăng ký thành viên mới và xem thông tin hội viên.
- Theo dõi tổng chi tiêu, điểm hiện có và tiến độ lên hạng.
- Dùng điểm để trừ vào khoản phải trả.
- Sau thanh toán, cập nhật chi tiêu, cộng điểm và tự xác định lại hạng.

| Hạng | Tổng chi tiêu | Tỷ lệ tích điểm |
|---|---|---|
| STANDARD | Dưới 1.000.000 đồng | 3% |
| VIP | Từ 1.000.000 đến dưới 3.000.000 đồng | 5% |
| DIAMOND | Từ 3.000.000 đồng | 7% |

Trong ứng dụng, điểm được sử dụng theo giá trị quy đổi trực tiếp để trừ tiền. Điểm mới được tính trên số tiền thực trả theo hạng trước giao dịch; hạng được cập nhật sau khi cộng chi tiêu.

### 3.4. Thứ tự tổng hợp số tiền

1. Tính tiền vé theo suất, loại ghế và chính sách đang chọn.
2. Cộng tiền F&B.
3. Trừ phần giảm voucher hợp lệ.
4. Trừ điểm thành viên được sử dụng.
5. Làm tròn số tiền phải trả đến đồng VNĐ.

## 4. Thanh toán và hóa đơn

### 4.1. Tiền mặt

- Hiển thị tiền vé, F&B, voucher, điểm và tổng phải trả.
- Nhập tiền khách đưa để tính số tiền thiếu hoặc tiền thừa.
- Nút **Khách đưa đủ** điền nhanh đúng số tiền cần thanh toán.
- Không cho xác nhận khi tiền khách đưa không hợp lệ hoặc chưa đủ.
- Khi hoàn tất: ghế chuyển sang đã bán, tạo hóa đơn và cập nhật thành viên.
- Hóa đơn mới lưu cả phương thức thanh toán, tiền khách đưa và tiền thừa.

### 4.2. QR mô phỏng

Giao diện có lựa chọn QR để minh họa bước thanh toán không dùng tiền mặt. Đây là hình mô phỏng, chưa tạo giao dịch ngân hàng, chưa xác minh đã nhận tiền và chưa kết nối cổng thanh toán.

### 4.3. Xuất và in lại hóa đơn

- Xuất hóa đơn dạng file TXT trong thư mục receipts, mô phỏng phiếu POS.
- Nội dung gồm mã hóa đơn, thu ngân, khách hàng, phim, phòng, suất, ghế, bắp nước, giảm giá và tổng thanh toán.
- Từ lịch sử giao dịch có thể mở chi tiết, mở file hoặc chọn **In lại hóa đơn**.
- In lại không tạo đơn mới, không cộng doanh thu và không cộng/trừ điểm lần nữa.
- Nếu giao dịch đã lưu nhưng xuất file thất bại, có thể in lại từ lịch sử.

## 5. Lấy vé đặt online — mô phỏng

Phân hệ này minh họa tình huống khách đã đặt vé trước và đến quầy nhận vé.

- Danh sách hiển thị các đơn thuộc kênh ONLINE.
- Tra cứu bằng mã đơn, số điện thoại hoặc tên khách; khi nhập tìm kiếm còn có thể tìm đơn tại quầy phù hợp.
- Lọc tất cả, chưa in hoặc đã in vé.
- Xem chi tiết khách, phim, phòng, suất, ghế, F&B và số tiền trên đơn.
- Xác nhận in vé, ghi nhận trạng thái và thời gian in.
- Nếu đơn đã in, ứng dụng cảnh báo trước khi cho in lại.
- Xuất phiếu vé TXT và mở file đã xuất.
- Có hộp thoại tạo đơn online giả để phục vụ trình diễn.

**Giới hạn cần nói rõ:** Chưa có website/app khách hàng hoặc kết nối đặt vé online thực tế. Trạng thái “đã in vé” cũng chưa phải trạng thái “đã vào phòng chiếu”; ứng dụng chưa có soát vé tại cửa rạp.

## 6. Quản lý phim, phòng và lịch chiếu

### 6.1. Quản lý phim

- Thêm phim với mã, tên, thể loại, thời lượng, đạo diễn, phân loại tuổi và poster.
- Tìm kiếm/lọc danh sách; chọn phim để xem và cập nhật.
- Kiểm tra mã phim không trùng và thời lượng lớn hơn 0.
- Không đổi thời lượng phim đã có lịch để tránh làm sai khoảng thời gian chiếu.
- Xóa phim cần xác nhận; chặn xóa nếu phim có suất đã bán vé.

### 6.2. Phòng và sơ đồ

Phòng được sử dụng khi tạo suất và xác định số hàng/cột ghế. Dữ liệu mẫu có phòng 80 ghế và phòng 120 ghế. Ứng dụng chưa có đầy đủ giao diện thêm/sửa/xóa phòng hoặc thiết kế sơ đồ tùy ý.

### 6.3. Lịch chiếu

- Tạo suất bằng cách chọn phim, phòng, ngày, giờ và giá cơ sở.
- Giờ mới phải ở tương lai; giá phải hữu hạn và lớn hơn 0.
- Tính thời gian kết thúc dựa trên thời lượng phim và khoảng đệm 15 phút khi tạo/sửa qua giao diện quản lý.
- Kiểm tra hai suất có chồng thời gian trong cùng phòng hay không.
- Nếu trùng, thông báo suất đang xung đột để người dùng chọn lại giờ/phòng.
- Lọc, xem, sửa và xóa suất chưa bán vé.
- Đổi phòng của suất chưa bán sẽ tạo lại sơ đồ tương ứng.
- Suất đã bán vé được bảo vệ khỏi sửa/xóa qua chức năng quản lý suất thông thường.

## 7. Thống kê và lịch sử giao dịch

Các chỉ số chính:

- Doanh thu theo số tiền thực trả trên hóa đơn.
- Tổng số vé bán theo số vé trong hóa đơn.
- Tổng giá trị F&B trước giảm giá.
- Phim bán nhiều vé nhất trong phạm vi được lọc.

Các thao tác hỗ trợ:

- Lọc toàn bộ thời gian, hôm nay hoặc 7 ngày gần nhất — gồm hôm nay và 6 ngày trước.
- Sắp xếp hóa đơn theo số tiền hoặc ngày tạo.
- Xem chi tiết, mở file và in lại hóa đơn.
- Xuất báo cáo TXT theo bộ lọc đang chọn.

Không gọi doanh thu là lợi nhuận vì hệ thống chưa tính chi phí. Chỉ số F&B trước giảm giá không cùng cơ sở tính với doanh thu thực trả sau giảm giá.

Bản hiện tại có nút xóa lịch sử để đặt lại dữ liệu demo, đồng thời trả ghế về trống. Đây là thao tác xóa dữ liệu, không nên giới thiệu là quy trình đóng ca thực tế hoặc thực hiện trong lượt demo chính.

## 8. Lưu dữ liệu và phạm vi hoàn thành

Dữ liệu được lưu bằng JSON, có tự lưu định kỳ và lưu khi thoát bình thường. Lưu theo đợt có thông tin phục hồi để xử lý lần ghi bị gián đoạn. Trong luồng thanh toán tại quầy, nếu lưu thất bại, ứng dụng hoàn tác cập nhật ghế, điểm và hóa đơn trong bộ nhớ, không thông báo giao dịch thành công.

Đây là ứng dụng một máy, một tiến trình. Chưa triển khai đồng bộ nhiều quầy, ngân hàng thật, máy in nhiệt vật lý, tồn kho, hoàn/đổi vé hoặc soát vé vào rạp. Các kiểm tra hồi quy đã chạy không thay thế cho việc thử toàn bộ thao tác trên giao diện.

## 9. Kịch bản thuyết trình và demo 10 phút

| Thời gian | Nội dung | Thao tác minh họa |
|---|---|---|
| 0:00–0:40 | Giới thiệu mục tiêu và vai trò | Đăng nhập Admin để thấy các tab |
| 0:40–1:30 | Danh sách phim và suất | Tìm một phim, mở suất tương lai, chỉ rõ phòng/ghế trống |
| 1:30–2:40 | Chọn ghế | Chọn ghế thường/VIP, minh họa Sweetbox theo cặp và đồng hồ giữ |
| 2:40–3:20 | F&B | Chọn combo, thay số lượng |
| 3:20–5:10 | Thành viên, ưu đãi, thanh toán | Tra cứu khách, áp voucher, giải thích tổng, nhập tiền và xem tiền thừa |
| 5:10–6:00 | Hóa đơn | Thanh toán, mở lịch sử và in lại |
| 6:00–7:10 | Nhận vé online mô phỏng | Tra cứu đơn đã chuẩn bị, xem chi tiết, in và chỉ trạng thái đã in |
| 7:10–8:35 | Quản lý phim và lịch | Mở danh sách, tạo thử một suất trùng để hiện cảnh báo |
| 8:35–9:30 | Báo cáo | Lọc hôm nay, xem KPI và xuất báo cáo |
| 9:30–10:00 | Phạm vi và hướng phát triển | Nêu phần mô phỏng và 2 hướng nâng cấp |

### Lời mở đầu có thể dùng trực tiếp

“Em xin trình bày bài tập lớn hệ thống quản lý rạp chiếu phim. Ứng dụng tập trung vào quy trình làm việc tại quầy, từ chọn phim, suất và ghế đến bán bắp nước, áp dụng ưu đãi, thanh toán và xuất hóa đơn. Bên cạnh đó có quản lý phim, lịch chiếu, thống kê và mô phỏng nhận vé đặt online. Hệ thống có hai vai trò là nhân viên và quản trị viên.”

### Lời dẫn khi demo bán vé

“Đầu tiên, nhân viên tìm phim theo yêu cầu của khách. Khi chọn phim, hệ thống hiển thị các suất chưa bắt đầu cùng phòng và số ghế trống. Trên sơ đồ, các màu phân biệt ghế trống, đang giữ và đã bán. Ghế VIP có mức giá khác ghế thường, còn Sweetbox được chọn theo cặp. Các ghế trong một lượt được giữ tối đa 5 phút.”

“Sau khi chọn ghế, khách có thể mua thêm bắp nước. Ở thanh toán, nhân viên tra cứu thành viên và áp dụng voucher. Hệ thống tách tiền vé, tiền F&B và từng khoản giảm để người dùng đối chiếu. Với tiền mặt, nhập số tiền khách đưa sẽ thấy số tiền thiếu hoặc tiền thừa. Sau khi xác nhận và lưu thành công, ghế chuyển sang đã bán và hóa đơn được xuất ra file.”

### Lời dẫn khi chuyển sang quản lý

“Đối với quản trị viên, hệ thống cung cấp quản lý phim và suất chiếu. Điểm kiểm tra quan trọng là không cho hai suất chồng thời gian trong cùng phòng. Khi suất đã bán vé, thao tác sửa hoặc xóa qua màn hình quản lý suất bị chặn để tránh làm thay đổi thông tin khách đã mua.”

### Lời kết có thể dùng trực tiếp

“Bài tập đã mô phỏng được luồng bán vé tại quầy và các chức năng quản lý liên quan. Phần QR và đặt vé online hiện phục vụ minh họa, chưa kết nối dịch vụ bên ngoài. Nếu phát triển tiếp, em ưu tiên bổ sung soát vé và xử lý hoàn, đổi vé để hoàn thiện hơn vòng đời của một vé.”

## 10. Chuẩn bị trước khi báo cáo

- Chạy lại ứng dụng từ mã nguồn mới nhất bằng run.bat.
- Có ít nhất một suất tương lai vào ngày trình bày, đủ ghế trống.
- Chuẩn bị sẵn một tài khoản Admin, một thành viên có điểm và một voucher còn hiệu lực.
- Chọn đơn đủ điều kiện voucher; không đợi đến lúc trình bày mới kiểm tra.
- Chuẩn bị sẵn một đơn ONLINE chưa in để tiết kiệm thời gian.
- Thử trước thư mục xuất hóa đơn và báo cáo có ghi được file.
- Không chờ hết 5 phút trực tiếp; chỉ giới thiệu đồng hồ và quy tắc hết hạn.
- Không bấm xóa lịch sử khi đang demo giao dịch và thống kê.

## 11. Một số câu hỏi thầy có thể hỏi

**QR đã thanh toán thật được chưa?**

“Chưa, đây là giao diện mô phỏng. Ứng dụng chưa tích hợp ngân hàng hoặc xác nhận giao dịch.”

**Đặt vé online có website riêng không?**

“Chưa có. Em có hộp thoại tạo đơn giả và phân hệ nhận/in vé tại quầy để minh họa luồng xử lý.”

**Hết thời gian giữ ghế thì sao?**

“Ghế được trả về trống; khi xác nhận thanh toán tại quầy, chương trình kiểm tra lại thời hạn, không xuất vé từ lượt giữ đã hết hạn.”

**Nếu khách là sinh viên vào cuối tuần?**

“Theo chính sách hiện tại, lựa chọn sinh viên sử dụng giá cơ sở giảm 20% rồi nhân hệ số ghế, thay thế chiến lược cuối tuần. Đây là quy tắc của bài tập, chưa phải bảng giá của một rạp cụ thể.”

**In lại có tăng doanh thu không?**

“Không. In lại dùng dữ liệu hóa đơn có sẵn, không tạo thêm giao dịch hoặc cập nhật điểm.”

**Đã in vé có nghĩa đã vào rạp chưa?**

“Chưa. Trạng thái này chỉ ghi nhận việc in; soát vé ở cửa là chức năng chưa triển khai.”

**Ứng dụng có dùng cho nhiều quầy cùng lúc không?**

“Phạm vi hiện tại là một máy, một tiến trình. Đồng bộ nhiều quầy là hướng mở rộng, chưa nằm trong bản này.”
