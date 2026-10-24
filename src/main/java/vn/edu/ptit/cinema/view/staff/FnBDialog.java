package vn.edu.ptit.cinema.view.staff;

import com.formdev.flatlaf.FlatClientProperties;
import vn.edu.ptit.cinema.model.booking.OrderItem;
import vn.edu.ptit.cinema.model.fnb.FoodItem;
import vn.edu.ptit.cinema.service.DataStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [STEP 2: F&B DIALOG - CHỌN BẮP NƯỚC COMBO]
 * Cửa sổ Popup (JDialog) độc lập, thiết kế dạng thẻ Card hiện đại.
 * Hỗ trợ tăng/giảm (+/-) số lượng trực quan, không bị co cụm hay tràn viền.
 */
public class FnBDialog extends JDialog {

    private final List<OrderItem> selectedOrderItems = new ArrayList<>();
    private final Map<String, Integer> quantityMap = new HashMap<>();
    private final Map<String, JLabel> qtyLabelMap = new HashMap<>();

    private JLabel lblTotalFoodPrice;
    private boolean proceedToPayment = false;

    public FnBDialog(Frame parent) {
        super(parent, "BẮP NƯỚC & COMBO ƯU ĐÃI (F&B)", true);
        initUI();
    }

    private void initUI() {
        setSize(740, 590);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout(15, 15));

        // 1. Header Dialog
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(15, 20, 10, 20));
        headerPanel.setBackground(new Color(248, 250, 252));

        JLabel lblTitle = new JLabel("COMBO BẮP NƯỚC RẠP CHIẾU PHIM");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(220, 38, 38));

        JLabel lblDesc = new JLabel("Chọn thêm bắp rang bơ giòn rụm & nước ngọt có ga mát lạnh thưởng thức cùng phim!");
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDesc.setForeground(Color.GRAY);

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblDesc, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Trung tâm: Danh sách mặt hàng (Grid Card Layout)
        JPanel itemsContainer = new JPanel();
        itemsContainer.setLayout(new BoxLayout(itemsContainer, BoxLayout.Y_AXIS));
        itemsContainer.setBorder(new EmptyBorder(10, 20, 10, 20));

        List<FoodItem> foodList = DataStore.getInstance().getFoodList();
        for (FoodItem item : foodList) {
            quantityMap.put(item.getId(), 0);
            itemsContainer.add(createFoodCard(item));
            itemsContainer.add(Box.createVerticalStrut(10));
        }

        JScrollPane scrollPane = new JScrollPane(itemsContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // 3. Footer: Tổng tiền F&B và Nút bấm Action
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        footerPanel.setBackground(new Color(248, 250, 252));

        lblTotalFoodPrice = new JLabel("Tiền bắp nước: 0 VNĐ");
        lblTotalFoodPrice.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTotalFoodPrice.setForeground(new Color(220, 38, 38));
        footerPanel.add(lblTotalFoodPrice, BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnGroup.setOpaque(false);

        JButton btnSkip = new JButton("Bỏ qua (Không mua)");
        btnSkip.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnSkip.putClientProperty(FlatClientProperties.STYLE, "arc: 10; margin: 8,16,8,16;");
        btnSkip.addActionListener(e -> {
            selectedOrderItems.clear();
            proceedToPayment = true;
            dispose();
        });

        JButton btnConfirm = new JButton("Xác nhận & Sang Thanh Toán >");
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConfirm.setBackground(new Color(22, 163, 74));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.putClientProperty(FlatClientProperties.STYLE, "arc: 10; margin: 8,20,8,20;");
        btnConfirm.addActionListener(e -> {
            collectOrderItems();
            proceedToPayment = true;
            dispose();
        });

        btnGroup.add(btnSkip);
        btnGroup.add(btnConfirm);
        footerPanel.add(btnGroup, BorderLayout.EAST);

        add(footerPanel, BorderLayout.SOUTH);
    }

    private JPanel createFoodCard(FoodItem item) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(8, 16, 8, 16)
        ));
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(0, 88));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));

        // Bên trái: Tên & Mô tả (Dùng GridLayout 3 dòng để không bao giờ bị cắt chữ)
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 0, 2));
        infoPanel.setOpaque(false);

        JLabel lblName = new JLabel(item.getName());
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel lblPrice = new JLabel(String.format("%,.0f VNĐ", item.getPrice()));
        lblPrice.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPrice.setForeground(new Color(220, 38, 38));

        JLabel lblDesc = new JLabel(item.getDescription());
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDesc.setForeground(new Color(100, 116, 139));

        infoPanel.add(lblName);
        infoPanel.add(lblPrice);
        infoPanel.add(lblDesc);
        card.add(infoPanel, BorderLayout.CENTER);

        // Bên phải: Bộ điều khiển (+ / -) số lượng
        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 12));
        qtyPanel.setOpaque(false);

        JButton btnMinus = new JButton("-");
        btnMinus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnMinus.setPreferredSize(new Dimension(36, 32));
        btnMinus.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");

        JLabel lblQty = new JLabel("0", JLabel.CENTER);
        lblQty.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblQty.setPreferredSize(new Dimension(32, 32));
        qtyLabelMap.put(item.getId(), lblQty);

        JButton btnPlus = new JButton("+");
        btnPlus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPlus.setPreferredSize(new Dimension(36, 32));
        btnPlus.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");

        btnMinus.addActionListener(e -> {
            int current = quantityMap.get(item.getId());
            if (current > 0) {
                current--;
                quantityMap.put(item.getId(), current);
                lblQty.setText(String.valueOf(current));
                updateTotal();
            }
        });

        btnPlus.addActionListener(e -> {
            int current = quantityMap.get(item.getId());
            if (current < 20) {
                current++;
                quantityMap.put(item.getId(), current);
                lblQty.setText(String.valueOf(current));
                updateTotal();
            }
        });

        qtyPanel.add(btnMinus);
        qtyPanel.add(lblQty);
        qtyPanel.add(btnPlus);
        card.add(qtyPanel, BorderLayout.EAST);

        return card;
    }

    private void updateTotal() {
        double total = 0;
        List<FoodItem> foodList = DataStore.getInstance().getFoodList();
        for (FoodItem item : foodList) {
            int qty = quantityMap.getOrDefault(item.getId(), 0);
            total += qty * item.getPrice();
        }
        lblTotalFoodPrice.setText(String.format("Tiền bắp nước: %,.0f VNĐ", total));
    }

    private void collectOrderItems() {
        selectedOrderItems.clear();
        List<FoodItem> foodList = DataStore.getInstance().getFoodList();
        for (FoodItem item : foodList) {
            int qty = quantityMap.getOrDefault(item.getId(), 0);
            if (qty > 0) {
                selectedOrderItems.add(new OrderItem(item.getId(), item.getName(), qty, item.getPrice()));
            }
        }
    }

    public List<OrderItem> getSelectedOrderItems() {
        return selectedOrderItems;
    }

    public boolean isProceedToPayment() {
        return proceedToPayment;
    }
}
