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
            writer.write(String.format("Thời gian   : %s\n", invoice.getCreatedAt().format(dtf)));
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
                writer.write(String.format("Voucher giảm giá : %s (-%,.0f VNĐ)\n", invoice.getVoucherCode(), invoice.getDiscountAmount()));
            }
            writer.write(String.format("THANH TOÁN       : %,12.0f VNĐ\n", invoice.getFinalAmount()));
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

    /**
     * In vé xem phim nhiệt dành riêng cho đơn đặt online nhận tại quầy (Kiosk / POS Ticket Pickup)
     */
    public static File printOnlineTicket(Invoice invoice) {
        File dir = new File("receipts");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File ticketFile = new File(dir, "Ticket_Online_" + invoice.getInvoiceId() + ".txt");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        try (FileWriter writer = new FileWriter(ticketFile, StandardCharsets.UTF_8)) {
            writer.write("*****************************************************\n");
            writer.write("         VÉ XEM PHIM ĐIỆN TỬ - PTIT CINEMA ONLINE    \n");
            writer.write("        (PHIẾU XÁC NHẬN VÀO CỬA PHÒNG CHIẾU PHIM)    \n");
            writer.write("*****************************************************\n");
            writer.write(String.format("MÃ ĐẶT VÉ   : %s\n", invoice.getInvoiceId()));
            writer.write(String.format("KÊNH ĐẶT    : %s (Đã thanh toán Online)\n",
                    invoice.getBookingChannel() != null ? invoice.getBookingChannel() : "ONLINE"));
            writer.write(String.format("KHÁCH HÀNG  : %s (%s)\n", invoice.getCustomerName(), invoice.getCustomerPhone()));
            writer.write(String.format("THỜI ĐIỂM IN: %s\n",
                    invoice.getPrintedAt() != null ? invoice.getPrintedAt().format(dtf) : dtf.format(java.time.LocalDateTime.now())));
            writer.write("-----------------------------------------------------\n");
            writer.write(String.format("PHIM        : %s\n", invoice.getMovieTitle()));
            writer.write(String.format("PHÒNG CHIẾU : %s\n", invoice.getRoomName()));
            writer.write(String.format("SUẤT CHIẾU  : %s\n",
                    invoice.getShowStartTime() != null ? invoice.getShowStartTime().format(dtf) : "N/A"));
            writer.write("-----------------------------------------------------\n");
            writer.write("VỊ TRÍ GHẾ NGỒI ĐÃ ĐẶT:\n");
            for (Ticket t : invoice.getTickets()) {
                writer.write(String.format("  >>> GHẾ: %-6s | VÉ CHUẨN CGV/BETA | GIÁ: %,8.0f VNĐ\n",
                        t.getSeatCode(), t.getPrice()));
            }

            if (!invoice.getFoods().isEmpty()) {
                writer.write("-----------------------------------------------------\n");
                writer.write("BẮP NƯỚC NHẬN TẠI QUẦY F&B:\n");
                for (OrderItem item : invoice.getFoods()) {
                    writer.write(String.format("  + %-20s x%-2d %,10.0f VNĐ\n",
                            item.getFoodName(), item.getQuantity(), item.getTotalPrice()));
                }
            }

            writer.write("=====================================================\n");
            writer.write(String.format("TỔNG TIỀN ĐÃ TRẢ: %,12.0f VNĐ (ĐÃ THANH TOÁN)\n", invoice.getFinalAmount()));
            writer.write("=====================================================\n");
            writer.write("MÃ SOÁT VÉ CỬA RẠP: [BARCODE: *PTIT-" + invoice.getInvoiceId() + "*]\n");
            writer.write("Vui lòng xuất trình phiếu này cho nhân viên soát vé tại cửa.\n");
            writer.write("       CHÚC QUÝ KHÁCH CÓ TRẢI NGHIỆM XEM PHIM TUYỆT VỜI!    \n");
            writer.write("*****************************************************\n");

            writer.flush();
            System.out.println("[ReceiptPrinter] Đã xuất vé online: " + ticketFile.getAbsolutePath());
            return ticketFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
