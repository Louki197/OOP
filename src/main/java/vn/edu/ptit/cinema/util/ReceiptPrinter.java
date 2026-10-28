package vn.edu.ptit.cinema.util;

import vn.edu.ptit.cinema.model.booking.Invoice;
import vn.edu.ptit.cinema.model.booking.OrderItem;
import vn.edu.ptit.cinema.model.booking.Ticket;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Tiện ích in vé và hóa đơn mô phỏng máy in nhiệt POS ra file .txt
 */
public class ReceiptPrinter {

    public static File printReceipt(Invoice invoice) {
        File dir = new File("receipts");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File receiptFile = new File(dir, "Receipt_" + invoice.getInvoiceId() + ".txt");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        try (FileWriter writer = new FileWriter(receiptFile, StandardCharsets.UTF_8)) {
            writer.write("=====================================================\n");
            writer.write("          RẠP CHIẾU PHIM PTIT CINEMA COMPLEX         \n");
            writer.write("       Đ/C: Km 10 Đ. Nguyễn Trãi, Q. Hà Đông, Hà Nội \n");
            writer.write("              HOTLINE: 1900 8888 - PTIT.EDU.VN       \n");
            writer.write("=====================================================\n");
            writer.write(String.format("Mã hóa đơn : %s\n", invoice.getInvoiceId()));
            writer.write(String.format("Thời gian   : %s\n", invoice.getCreatedAt() == null ? "N/A" : invoice.getCreatedAt().format(dtf)));
            writer.write(String.format("Thu ngân    : %s\n", invoice.getStaffUsername()));
            writer.write(String.format("Khách hàng  : %s (%s)\n", invoice.getCustomerName(), invoice.getCustomerPhone()));
            writer.write("-----------------------------------------------------\n");
            writer.write(String.format("Phim        : %s\n", invoice.getMovieTitle()));
            writer.write(String.format("Phòng chiếu : %s\n", invoice.getRoomName()));
            writer.write(String.format("Suất chiếu  : %s\n", invoice.getShowStartTime() != null ? invoice.getShowStartTime().format(dtf) : "N/A"));
            writer.write("-----------------------------------------------------\n");
            writer.write("DANH SÁCH VÉ:\n");
            for (Ticket t : invoice.getTickets()) {
                writer.write(String.format("  - Ghế: %-8s Đơn giá: %,10.0f VNĐ\n", t.getSeatCode(), t.getPrice()));
            }

            if (!invoice.getFoods().isEmpty()) {
                writer.write("-----------------------------------------------------\n");
                writer.write("BẮP NƯỚC (F&B):\n");
                for (OrderItem item : invoice.getFoods()) {
                    writer.write(String.format("  - %-20s x%-2d %,10.0f VNĐ\n",
                            item.getFoodName(), item.getQuantity(), item.getTotalPrice()));
                }
            }

            writer.write("=====================================================\n");
            writer.write(String.format("Tổng tiền gốc    : %,12.0f VNĐ\n", invoice.getOriginalAmount()));
            if (invoice.getVoucherCode() != null && !invoice.getVoucherCode().isEmpty()) {
                writer.write("Mã voucher      : " + invoice.getVoucherCode() + "\n");
            }
            writer.write(String.format("Tổng giảm giá   : %,.0f VNĐ\n", invoice.getDiscountAmount()));
            writer.write(String.format("THANH TOÁN       : %,12.0f VNĐ\n", invoice.getFinalAmount()));
            if (invoice.getPaymentMethod() != null) {
                writer.write("Phương thức     : " + ("CASH".equals(invoice.getPaymentMethod()) ? "Tiền mặt" : "QR mô phỏng") + "\n");
                if ("CASH".equals(invoice.getPaymentMethod())) {
                    writer.write(String.format("Khách đưa       : %,.0f VNĐ\nTiền thừa       : %,.0f VNĐ\n", invoice.getCashReceived(), invoice.getChangeAmount()));
                }
            }
            writer.write("=====================================================\n");
            writer.write("       CẢM ƠN QUÝ KHÁCH & CHÚC XEM PHIM VUI VẺ!      \n");
            writer.write("=====================================================\n");

            writer.flush();
            System.out.println("[ReceiptPrinter] Đã xuất hóa đơn: " + receiptFile.getAbsolutePath());
            return receiptFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
