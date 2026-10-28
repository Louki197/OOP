package vn.edu.ptit.cinema;

import vn.edu.ptit.cinema.dao.BaseDAO;
import vn.edu.ptit.cinema.model.booking.*;
import vn.edu.ptit.cinema.model.cinema.ShowTime;
import vn.edu.ptit.cinema.model.seat.*;
import vn.edu.ptit.cinema.service.*;
import vn.edu.ptit.cinema.util.ReceiptPrinter;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Chạy trong thư mục kiểm thử riêng; không dùng data thật. */
public class RegressionChecks {
    private static int checks;
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    public static void main(String[] args) throws Exception {
        if (!Path.of(".isolated-test").toFile().exists())
            throw new IllegalStateException("Chỉ chạy trong thư mục có marker .isolated-test");
        LocalDate today = LocalDate.of(2026, 10, 28);
        Voucher v = new Voucher("TEST", 10, 30000, 100000, today, true);
        check(BookingRules.voucherError(v, 100000, today) == null, "Voucher dùng được hết ngày hạn");
        check(BookingRules.voucherError(v, 99999, today) != null, "Đơn dưới tối thiểu");
        check(BookingRules.voucherError(v, 100000, today.plusDays(1)) != null, "Voucher hết hạn");
        v.setActive(false);
        check(BookingRules.voucherError(v, 100000, today) != null, "Voucher vô hiệu");
        check(BookingRules.voucherError(null, 100000, today) != null, "Mã không tồn tại");
        check(BookingRules.inPeriod(today.minusDays(6), today, "7 ngày gần nhất"), "Ngày thứ bảy được tính");
        check(!BookingRules.inPeriod(today.minusDays(7), today, "7 ngày gần nhất"), "Không lấy ngày thứ tám");
        check(!BookingRules.inPeriod(today.plusDays(1), today, "7 ngày gần nhất"), "Không lấy tương lai");
        check(BookingRules.inPeriod(today, today, "Hôm nay"), "Lọc hôm nay");
        check(!BookingRules.inPeriod(today.minusDays(1), today, "Hôm nay"), "Bỏ hôm qua");
        Seat seat = new Seat("A01", 0, 0, SeatType.STANDARD);
        long now = System.currentTimeMillis();
        seat.setStatus(SeatStatus.PENDING); seat.setHoldTimestamp(now);
        ShowTime show = new ShowTime("TEST", "M", "R", LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(3), 85000, new HashMap<>(Map.of("A01", seat)));
        check(BookingRules.canSell(show, LocalDateTime.now()), "Suất tương lai");
        check(!BookingRules.canSell(show, show.getStartTime()), "Chặn đúng giờ bắt đầu");
        check(BookingRules.validHold(show, Set.of("A01"), now), "Ghế còn hạn");
        check(!BookingRules.validHold(show, Set.of("A01"), now + 300000), "Chặn đúng hạn 5 phút");
        check(!BookingRules.validHold(show, Set.of(), now), "Chặn đơn rỗng");
        check(!BookingRules.validHold(show, Set.of("A02"), now), "Chặn ghế không tồn tại");
        seat.setStatus(SeatStatus.BOOKED);
        check(!BookingRules.validHold(show, Set.of("A01"), now), "Không mua lại ghế đã bán");
        check(BookingRules.hasBookedSeats(show), "Bảo vệ suất đã bán");
        BaseDAO<Object> dao = new BaseDAO<>() {};
        LinkedHashMap<String, Object> batch = new LinkedHashMap<>();
        batch.put("one.json", Map.of("name", "Tiếng Việt"));
        batch.put("two.json", List.of(1,2));
        check(dao.writeBatch(batch), "Lưu nhiều file");
        String original = Files.readString(Path.of("data/one.json"));
        check(original.contains("Tiếng Việt"), "UTF-8");
        Files.createDirectory(Path.of("data/two.json.tmp"));
        batch.put("one.json", Map.of("changed", true));
        check(!dao.writeBatch(batch), "Giả lập lỗi sau khi file đầu đã đổi");
        check(Files.readString(Path.of("data/one.json")).equals(original), "Hoàn tác file đầu");
        check(!Files.exists(Path.of("data/save-journal.json")), "Dọn journal sau hoàn tác");
        Files.delete(Path.of("data/two.json.tmp"));
        Map<String, String> previous = Map.of("one.json", original);
        Files.writeString(Path.of("data/save-journal.json"), new com.google.gson.Gson().toJson(previous));
        Files.writeString(Path.of("data/one.json"), "{}");
        new BaseDAO<Object>() {};
        check(Files.readString(Path.of("data/one.json")).equals(original), "Phục hồi khi khởi động sau lưu dở");
        Invoice inv = new Invoice("TEST", "staff", "Khách", "", "ST", "Phim", "Phòng 2", show.getStartTime(),
                List.of(new Ticket("T1", "ST", "A01", 85000)), List.of(), null, 85000, 5000, 80000, LocalDateTime.now());
        inv.setPaymentDetails("CASH", 100000, 20000);
        var receipt = ReceiptPrinter.printReceipt(inv);
        String text = Files.readString(receipt.toPath());
        check(text.contains("Phòng 2") && text.contains("Tiền thừa") && text.contains("Tổng giảm giá"), "Hóa đơn phòng, tiền thừa, điểm giảm");
        check(ReceiptPrinter.printReceipt(inv) != null, "In lại hóa đơn");
        System.out.println("PASS: " + checks + " regression checks");
    }
}
