package vn.edu.ptit.cinema.view.staff;

import com.formdev.flatlaf.FlatClientProperties;
import vn.edu.ptit.cinema.model.cinema.Room;
import vn.edu.ptit.cinema.model.cinema.ShowTime;
import vn.edu.ptit.cinema.model.movie.Movie;
import vn.edu.ptit.cinema.model.seat.Seat;
import vn.edu.ptit.cinema.model.seat.SeatStatus;
import vn.edu.ptit.cinema.service.DataStore;

import vn.edu.ptit.cinema.service.BookingRules;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * [SHOWTIME MANAGEMENT & EDIT DIALOG]
 * Phân hệ Quản lý & Chỉnh sửa Suất Chiếu toàn diện dành cho Quản Trị Viên:
 * 1. Bảng danh sách toàn bộ suất chiếu của các phim, hỗ trợ lọc theo Phim, Phòng, Ngày.
 * 2. Click vào bất kỳ dòng nào để đổ dữ liệu lên Form chỉnh sửa (Giờ chiếu, Ngày chiếu, Phòng, Giá vé).
 * 3. Kiểm tra chống xung đột trùng lịch phòng chiếu thông minh (loại trừ chính suất chiếu đang sửa).
 * 4. Xóa suất chiếu an toàn (cảnh báo nếu đã có vé được bán ra).
 * 5. Tự động đồng bộ và lưu vĩnh viễn vào showtimes.json.
 */
@SuppressWarnings("serial")
public class ShowtimeManagementDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final DataStore dataStore = DataStore.getInstance();
    private final Runnable onDataChangedCallback;

    private JTable tableShowtimes;
    private DefaultTableModel modelShowtimes;

    private JComboBox<String> cbFilterMovie;
    private JComboBox<String> cbFilterRoom;
    private JComboBox<String> cbFilterDate;

    // Form chỉnh sửa / thêm mới
    private JPanel formBorderPanel;
    private JTextField txtStId;
    private JComboBox<String> cbEditMovie;
    private JComboBox<String> cbEditRoom;
    private JComboBox<String> cbEditDate;
    private JSpinner spEditHour;
    private JSpinner spEditMinute;
    private JTextField txtEditPrice;

    private JButton btnSaveUpdate;
    private JButton btnDeleteShowtime;
    private JButton btnCreateNew;
    private JButton btnResetForm;

    private ShowTime currentEditingShowtime = null;

    public ShowtimeManagementDialog(Frame parent, Runnable onDataChangedCallback) {
        super(parent, "QUẢN LÝ & CHỈNH SỬA TOÀN BỘ LỊCH CHIẾU - PTIT CINEMA", true);
        this.onDataChangedCallback = onDataChangedCallback;
        initUI();
        reloadShowtimeTable();
    }

    public ShowtimeManagementDialog(Frame parent) {
        this(parent, null);
    }

    private void initUI() {
        setSize(1080, 720);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(new Color(248, 250, 252));

        // 1. HEADER
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(14, 22, 14, 22)
        ));

        JLabel lblTitle = new JLabel("DANH SÁCH & ĐIỀU CHỈNH SUẤT CHIẾU PHIM");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(15, 23, 42));

        JLabel lblSub = new JLabel("Chọn một suất chiếu trên bảng để điều chỉnh lại Giờ chiếu, Phòng chiếu, Giá vé hoặc Xóa suất chiếu nhầm lẫn");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(Color.GRAY);

        header.add(lblTitle, BorderLayout.NORTH);
        header.add(lblSub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // 2. BODY CONTAINER
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(12, 20, 12, 20));

        // Toolbar: Bộ lọc đa năng
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setOpaque(false);

        toolbar.add(new JLabel("Lọc phim:"));
        cbFilterMovie = new JComboBox<>();
        cbFilterMovie.addItem("Tất cả phim");
        for (Movie m : dataStore.getMovieMap().values()) {
            cbFilterMovie.addItem(m.getTitle());
        }
        cbFilterMovie.addActionListener(e -> reloadShowtimeTable());
        toolbar.add(cbFilterMovie);

        toolbar.add(new JLabel("Phòng:"));
        cbFilterRoom = new JComboBox<>(new String[]{"Tất cả phòng", "ROOM01", "ROOM02"});
        cbFilterRoom.addActionListener(e -> reloadShowtimeTable());
        toolbar.add(cbFilterRoom);

        toolbar.add(new JLabel("Ngày:"));
        cbFilterDate = new JComboBox<>();
        cbFilterDate.addItem("Tất cả ngày");
        LocalDate today = LocalDate.now();
        DateTimeFormatter dtfDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (int i = 0; i < 14; i++) {
            cbFilterDate.addItem(today.plusDays(i).format(dtfDate));
        }
        cbFilterDate.addActionListener(e -> reloadShowtimeTable());
        toolbar.add(cbFilterDate);

        JButton btnResetFilter = new JButton("Làm mới bộ lọc");
        btnResetFilter.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnResetFilter.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnResetFilter.addActionListener(e -> {
            cbFilterMovie.setSelectedIndex(0);
            cbFilterRoom.setSelectedIndex(0);
            cbFilterDate.setSelectedIndex(0);
            reloadShowtimeTable();
        });
        toolbar.add(btnResetFilter);

        body.add(toolbar, BorderLayout.NORTH);

        // Bảng dữ liệu suất chiếu
        String[] columns = {"Mã Suất", "Tên Phim", "Phòng Chiếu", "Ngày Chiếu", "Khung Giờ Chiếu", "Giá Vé", "Ghế Trống / Tổng", "Trạng Thái"};
        modelShowtimes = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tableShowtimes = new JTable(modelShowtimes);
        tableShowtimes.setRowHeight(30);
        tableShowtimes.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableShowtimes.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tableShowtimes.getTableHeader().setBackground(new Color(241, 245, 249));
        tableShowtimes.setAutoCreateRowSorter(true);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        tableShowtimes.getColumnModel().getColumn(0).setPreferredWidth(75);
        tableShowtimes.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        tableShowtimes.getColumnModel().getColumn(1).setPreferredWidth(210);

        tableShowtimes.getColumnModel().getColumn(2).setPreferredWidth(130);
        tableShowtimes.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        tableShowtimes.getColumnModel().getColumn(3).setPreferredWidth(100);
        tableShowtimes.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        tableShowtimes.getColumnModel().getColumn(4).setPreferredWidth(130);
        tableShowtimes.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setForeground(new Color(2, 132, 199)); // Xanh dương đậm
                return lbl;
            }
        });

        tableShowtimes.getColumnModel().getColumn(5).setPreferredWidth(100);
        tableShowtimes.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(JLabel.RIGHT);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setForeground(new Color(220, 38, 38));
                return lbl;
            }
        });

        tableShowtimes.getColumnModel().getColumn(6).setPreferredWidth(130);
        tableShowtimes.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        tableShowtimes.getColumnModel().getColumn(7).setPreferredWidth(110);
        tableShowtimes.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String status = value != null ? value.toString() : "";
                if ("Sắp chiếu".equals(status)) {
                    lbl.setForeground(new Color(22, 163, 74));
                } else if ("Đang chiếu".equals(status)) {
                    lbl.setForeground(new Color(217, 119, 6));
                } else {
                    lbl.setForeground(new Color(148, 163, 184));
                }
                return lbl;
            }
        });

        // Click 1 dòng để đổ ngược dữ liệu vào Form chỉnh sửa bên dưới
        tableShowtimes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = tableShowtimes.getSelectedRow();
                if (selectedRow >= 0) {
                    int modelRow = tableShowtimes.convertRowIndexToModel(selectedRow);
                    String stId = (String) modelShowtimes.getValueAt(modelRow, 0);
                    ShowTime st = dataStore.getShowTimeMap().get(stId);
                    if (st != null) {
                        loadShowtimeIntoForm(st);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tableShowtimes);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Danh Sách Suất Chiếu Tại Rạp (Click vào 1 dòng để sửa)"));
        body.add(scrollPane, BorderLayout.CENTER);

        // FORM ĐIỀU CHỈNH & CHỈNH SỬA SUẤT CHIẾU Ở PHÍA DƯỚI
        JPanel editFormPanel = createEditFormPanel();
        body.add(editFormPanel, BorderLayout.SOUTH);

        add(body, BorderLayout.CENTER);

        // 3. FOOTER
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnClose = new JButton("Đóng lại");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 6,18,6,18;");
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose);

        add(footer, BorderLayout.SOUTH);
    }

    private JPanel createEditFormPanel() {
        formBorderPanel = new JPanel(new BorderLayout(10, 8));
        formBorderPanel.setBackground(Color.WHITE);
        formBorderPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Thông Tin Suất Chiếu (Chế độ: Thêm suất mới)"),
                new EmptyBorder(8, 14, 8, 14)
        ));

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtStId = new JTextField();
        txtStId.setEditable(false);
        txtStId.setBackground(new Color(241, 245, 249));

        cbEditMovie = new JComboBox<>();
        for (Movie m : dataStore.getMovieMap().values()) {
            cbEditMovie.addItem(String.format("[%s] %s", m.getId(), m.getTitle()));
        }

        cbEditRoom = new JComboBox<>(new String[]{"ROOM01 (Phòng 1 - Standard)", "ROOM02 (Phòng 2 - IMAX Laser)"});

        cbEditDate = new JComboBox<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (int i = 0; i < 14; i++) {
            cbEditDate.addItem(now.plusDays(i).format(dtf));
        }

        JPanel timePicker = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        timePicker.setOpaque(false);
        spEditHour = new JSpinner(new SpinnerNumberModel(18, 7, 23, 1));
        spEditMinute = new JSpinner(new SpinnerNumberModel(0, 0, 55, 5));
        timePicker.add(spEditHour);
        timePicker.add(new JLabel("giờ"));
        timePicker.add(Box.createHorizontalStrut(6));
        timePicker.add(spEditMinute);
        timePicker.add(new JLabel("phút"));

        txtEditPrice = new JTextField("85000");

        // Dòng 0: Mã Suất + Phim + Phòng
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        fields.add(new JLabel("Mã Suất:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.2;
        fields.add(txtStId, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0;
        fields.add(new JLabel("Phim:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.45;
        fields.add(cbEditMovie, gbc);

        gbc.gridx = 4; gbc.gridy = 0; gbc.weightx = 0;
        fields.add(new JLabel("Phòng Chiếu:"), gbc);
        gbc.gridx = 5; gbc.gridy = 0; gbc.weightx = 0.35;
        fields.add(cbEditRoom, gbc);

        // Dòng 1: Ngày Chiếu + Giờ Bắt Đầu + Giá Vé
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        fields.add(new JLabel("Ngày Chiếu:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.2;
        fields.add(cbEditDate, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0;
        fields.add(new JLabel("Giờ Bắt Đầu:"), gbc);
        gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 0.45;
        fields.add(timePicker, gbc);

        gbc.gridx = 4; gbc.gridy = 1; gbc.weightx = 0;
        fields.add(new JLabel("Giá Vé (VNĐ):"), gbc);
        gbc.gridx = 5; gbc.gridy = 1; gbc.weightx = 0.35;
        fields.add(txtEditPrice, gbc);

        formBorderPanel.add(fields, BorderLayout.CENTER);

        // Hàng nút bấm: Nhập Mới, Xóa, Lưu Cập Nhật, Tạo Mới
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);

        btnResetForm = new JButton("Nhập Mới / Hủy Chọn");
        btnResetForm.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnResetForm.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnResetForm.addActionListener(e -> resetFormToCreateMode());

        btnDeleteShowtime = new JButton("Xóa Suất Chiếu");
        btnDeleteShowtime.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDeleteShowtime.setForeground(new Color(220, 38, 38));
        btnDeleteShowtime.putClientProperty(FlatClientProperties.STYLE, "arc: 8; borderWidth: 1;");
        btnDeleteShowtime.setEnabled(false);
        btnDeleteShowtime.addActionListener(e -> handleDeleteShowtime());

        btnSaveUpdate = new JButton("Lưu Cập Nhật Suất Chiếu");
        btnSaveUpdate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSaveUpdate.setBackground(new Color(37, 99, 235));
        btnSaveUpdate.setForeground(Color.WHITE);
        btnSaveUpdate.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnSaveUpdate.setEnabled(false);
        btnSaveUpdate.addActionListener(e -> handleSaveUpdateShowtime());

        btnCreateNew = new JButton("+ Tạo Suất Chiếu Mới");
        btnCreateNew.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCreateNew.setBackground(new Color(22, 163, 74));
        btnCreateNew.setForeground(Color.WHITE);
        btnCreateNew.putClientProperty(FlatClientProperties.STYLE, "arc: 8;");
        btnCreateNew.addActionListener(e -> handleCreateNewShowtime());

        btnBar.add(btnResetForm);
        btnBar.add(btnDeleteShowtime);
        btnBar.add(btnSaveUpdate);
        btnBar.add(btnCreateNew);
        formBorderPanel.add(btnBar, BorderLayout.SOUTH);

        resetFormToCreateMode();
        return formBorderPanel;
    }

    private void reloadShowtimeTable() {
        modelShowtimes.setRowCount(0);

        String selMovie = (String) cbFilterMovie.getSelectedItem();
        String selRoom = (String) cbFilterRoom.getSelectedItem();
        String selDate = (String) cbFilterDate.getSelectedItem();

        boolean filterMovie = selMovie != null && !selMovie.equals("Tất cả phim");
        boolean filterRoom = selRoom != null && !selRoom.equals("Tất cả phòng");
        boolean filterDate = selDate != null && !selDate.equals("Tất cả ngày");

        DateTimeFormatter dtfDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dtfTime = DateTimeFormatter.ofPattern("HH:mm");

        List<ShowTime> list = new ArrayList<>(dataStore.getShowTimeMap().values());
        list.sort(Comparator.comparing(ShowTime::getStartTime));

        LocalDateTime now = LocalDateTime.now();

        for (ShowTime st : list) {
            Movie m = dataStore.getMovieMap().get(st.getMovieId());
            String movieTitle = m != null ? m.getTitle() : st.getMovieId();

            if (filterMovie && !movieTitle.equalsIgnoreCase(selMovie)) continue;
            if (filterRoom && !st.getRoomId().equalsIgnoreCase(selRoom)) continue;
            if (filterDate && st.getStartTime() != null) {
                if (!st.getStartTime().format(dtfDate).equals(selDate)) continue;
            }

            // Tính số ghế trống / đã đặt
            int totalSeats = (st.getSeats() != null) ? st.getSeats().size() : 80;
            int bookedSeats = 0;
            if (st.getSeats() != null) {
                bookedSeats = (int) st.getSeats().values().stream()
                        .filter(s -> s.getStatus() == SeatStatus.BOOKED)
                        .count();
            }
            int availableSeats = totalSeats - bookedSeats;

            String statusStr = "Sắp chiếu";
            if (st.getStartTime() != null && st.getEndTime() != null) {
                if (now.isAfter(st.getEndTime())) {
                    statusStr = "Đã chiếu";
                } else if (now.isAfter(st.getStartTime())) {
                    statusStr = "Đang chiếu";
                }
            }

            String dateStr = st.getStartTime() != null ? st.getStartTime().format(dtfDate) : "N/A";
            String timeStr = (st.getStartTime() != null && st.getEndTime() != null)
                    ? String.format("%s - %s", st.getStartTime().format(dtfTime), st.getEndTime().format(dtfTime))
                    : "N/A";

            Room room = dataStore.getRoomMap().get(st.getRoomId());
            String roomName = room != null ? room.getName() : st.getRoomId();

            modelShowtimes.addRow(new Object[]{
                    st.getId(),
                    movieTitle,
                    roomName,
                    dateStr,
                    timeStr,
                    String.format("%,.0f VNĐ", st.getBasePrice()),
                    String.format("%d / %d ghế", availableSeats, totalSeats),
                    statusStr
            });
        }
    }

    private void loadShowtimeIntoForm(ShowTime st) {
        this.currentEditingShowtime = st;

        txtStId.setText(st.getId());
        formBorderPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Thông Tin Suất Chiếu (Đang sửa: " + st.getId() + ")"),
                new EmptyBorder(8, 14, 8, 14)
        ));

        // Chọn movie trong dropdown
        for (int i = 0; i < cbEditMovie.getItemCount(); i++) {
            if (cbEditMovie.getItemAt(i).contains("[" + st.getMovieId() + "]")) {
                cbEditMovie.setSelectedIndex(i);
                break;
            }
        }

        // Chọn phòng
        if (st.getRoomId() != null && st.getRoomId().contains("ROOM02")) {
            cbEditRoom.setSelectedIndex(1);
        } else {
            cbEditRoom.setSelectedIndex(0);
        }

        // Chọn ngày
        if (st.getStartTime() != null) {
            String dateFormatted = st.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            boolean found = false;
            for (int i = 0; i < cbEditDate.getItemCount(); i++) {
                if (cbEditDate.getItemAt(i).equals(dateFormatted)) {
                    cbEditDate.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                cbEditDate.addItem(dateFormatted);
                cbEditDate.setSelectedItem(dateFormatted);
            }

            spEditHour.setValue(st.getStartTime().getHour());
            spEditMinute.setValue(st.getStartTime().getMinute());
        }

        txtEditPrice.setText(String.format("%.0f", st.getBasePrice()));

        btnSaveUpdate.setEnabled(true);
        btnDeleteShowtime.setEnabled(true);
        btnCreateNew.setEnabled(false);
    }

    private void resetFormToCreateMode() {
        this.currentEditingShowtime = null;
        tableShowtimes.clearSelection();

        int maxSt = 0;
        for (String k : dataStore.getShowTimeMap().keySet()) {
            if (k.startsWith("ST")) {
                try {
                    int num = Integer.parseInt(k.substring(2));
                    if (num > maxSt) maxSt = num;
                } catch (Exception ignored) {}
            }
        }
        String nextStId = String.format("ST%02d", maxSt + 1);
        txtStId.setText(nextStId);

        formBorderPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Thông Tin Suất Chiếu (Chế độ: Thêm suất mới)"),
                new EmptyBorder(8, 14, 8, 14)
        ));

        if (cbEditMovie.getItemCount() > 0) cbEditMovie.setSelectedIndex(0);
        cbEditRoom.setSelectedIndex(0);
        if (cbEditDate.getItemCount() > 0) cbEditDate.setSelectedIndex(0);
        spEditHour.setValue(18);
        spEditMinute.setValue(0);
        txtEditPrice.setText("85000");

        btnSaveUpdate.setEnabled(false);
        btnDeleteShowtime.setEnabled(false);
        btnCreateNew.setEnabled(true);
    }

    private void handleSaveUpdateShowtime() {
        if (currentEditingShowtime == null) return;
        if (BookingRules.hasBookedSeats(currentEditingShowtime)) {
            JOptionPane.showMessageDialog(this, "Suất đã bán vé, không thể sửa hoặc xóa.");
            return;
        }

        String movieItem = (String) cbEditMovie.getSelectedItem();
        String dateStr = (String) cbEditDate.getSelectedItem();
        String roomItem = (String) cbEditRoom.getSelectedItem();

        if (movieItem == null || dateStr == null || roomItem == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đầy đủ thông tin!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double price;
        try {
            price = Double.parseDouble(txtEditPrice.getText().trim());
            if (!Double.isFinite(price) || price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Giá vé phải là số hữu hạn lớn hơn 0!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String movieId = movieItem.substring(movieItem.indexOf('[') + 1, movieItem.indexOf(']'));
        String roomId = roomItem.startsWith("ROOM02") ? "ROOM02" : "ROOM01";

        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        int hour = (int) spEditHour.getValue();
        int minute = (int) spEditMinute.getValue();
        LocalDateTime startTime = LocalDateTime.of(date, LocalTime.of(hour, minute, 0));

        Movie m = dataStore.getMovieMap().get(movieId);
        int dur = (m != null) ? m.getDurationMinutes() : 120;
        if (!startTime.isAfter(LocalDateTime.now())) {
            JOptionPane.showMessageDialog(this, "Giờ chiếu phải ở tương lai.");
            return;
        }
        LocalDateTime endTime = startTime.plusMinutes(dur + 15);

        // KIỂM TRA TRÙNG LỊCH PHÒNG CHIẾU (LOẠI TRỪ CHÍNH SUẤT CHIẾU ĐANG SỬA)
        for (ShowTime other : dataStore.getShowTimeMap().values()) {
            if (other.getId().equals(currentEditingShowtime.getId())) {
                continue; // Bỏ qua chính nó
            }
            if (other.getRoomId().equals(roomId) && other.getStartTime() != null && other.getEndTime() != null) {
                if (startTime.isBefore(other.getEndTime()) && endTime.isAfter(other.getStartTime())) {
                    Movie conflictMovie = dataStore.getMovieMap().get(other.getMovieId());
                    String conflictTitle = (conflictMovie != null) ? conflictMovie.getTitle() : other.getMovieId();
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
                    JOptionPane.showMessageDialog(this,
                            "XUNG ĐỘT LỊCH CHIẾU TẠI " + roomId + "!\n"
                                    + "Phòng này đã có phim: '" + conflictTitle + "'\n"
                                    + "Khung giờ chiếu: " + other.getStartTime().format(fmt) + " - " + other.getEndTime().format(fmt) + " ngày " + dateStr + ".\n"
                                    + "Vui lòng chọn khung giờ khác hoặc phòng chiếu khác!",
                            "Cảnh Báo Trùng Lịch", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        }

        boolean roomChanged = !roomId.equals(currentEditingShowtime.getRoomId());
        // Cập nhật thông tin vào đối tượng
        currentEditingShowtime.setMovieId(movieId);
        currentEditingShowtime.setRoomId(roomId);
        currentEditingShowtime.setStartTime(startTime);
        currentEditingShowtime.setEndTime(endTime);
        currentEditingShowtime.setBasePrice(price);

        // Nếu phòng chiếu thay đổi, tạo lại sơ đồ ghế theo phòng mới
        if (roomChanged || currentEditingShowtime.getSeats() == null || currentEditingShowtime.getSeats().isEmpty()) {
            Room room = dataStore.getRoomMap().get(roomId);
            int rows = (room != null) ? room.getTotalRows() : 8;
            int cols = (room != null) ? room.getTotalCols() : 10;
            currentEditingShowtime.setSeats(DataStore.generateRoomSeats(rows, cols));
        }

        dataStore.saveAllData();
        reloadShowtimeTable();

        if (onDataChangedCallback != null) {
            onDataChangedCallback.run();
        }

        JOptionPane.showMessageDialog(this,
                "CẬP NHẬT SUẤT CHIẾU THÀNH CÔNG!\n"
                        + "• Mã suất   : " + currentEditingShowtime.getId() + "\n"
                        + "• Phim      : " + (m != null ? m.getTitle() : movieId) + "\n"
                        + "• Khung giờ : " + startTime.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                        + " đến " + endTime.format(DateTimeFormatter.ofPattern("HH:mm")) + "\n"
                        + "• Phòng     : " + roomId + "\n"
                        + "• Giá vé    : " + String.format("%,.0f VNĐ", price),
                "Thành công", JOptionPane.INFORMATION_MESSAGE);

        resetFormToCreateMode();
    }

    private void handleDeleteShowtime() {
        if (currentEditingShowtime == null) return;
        if (BookingRules.hasBookedSeats(currentEditingShowtime)) {
            JOptionPane.showMessageDialog(this, "Suất đã bán vé, không thể sửa hoặc xóa.");
            return;
        }
        String stId = currentEditingShowtime.getId();

        int bookedCount = 0;
        if (currentEditingShowtime.getSeats() != null) {
            bookedCount = (int) currentEditingShowtime.getSeats().values().stream()
                    .filter(s -> s.getStatus() == SeatStatus.BOOKED)
                    .count();
        }

        String confirmMsg = (bookedCount > 0)
                ? "CẢNH BÁO: Suất chiếu này đã có " + bookedCount + " vé đã được bán ra!\nBạn có chắc chắn muốn XÓA vĩnh viễn suất chiếu " + stId + " không?"
                : "Bạn có chắc chắn muốn xóa suất chiếu " + stId + " không?";

        int confirm = JOptionPane.showConfirmDialog(this, confirmMsg, "Xác nhận xóa suất chiếu",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            dataStore.getShowTimeMap().remove(stId);
            dataStore.saveAllData();
            reloadShowtimeTable();

            if (onDataChangedCallback != null) {
                onDataChangedCallback.run();
            }

            JOptionPane.showMessageDialog(this, "Đã xóa suất chiếu " + stId + " thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            resetFormToCreateMode();
        }
    }

    private void handleCreateNewShowtime() {
        String movieItem = (String) cbEditMovie.getSelectedItem();
        String dateStr = (String) cbEditDate.getSelectedItem();
        String roomItem = (String) cbEditRoom.getSelectedItem();

        if (movieItem == null || dateStr == null || roomItem == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đầy đủ thông tin để tạo suất chiếu!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double price;
        try {
            price = Double.parseDouble(txtEditPrice.getText().trim());
            if (!Double.isFinite(price) || price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Giá vé phải là số hữu hạn lớn hơn 0!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String movieId = movieItem.substring(movieItem.indexOf('[') + 1, movieItem.indexOf(']'));
        String roomId = roomItem.startsWith("ROOM02") ? "ROOM02" : "ROOM01";

        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        int hour = (int) spEditHour.getValue();
        int minute = (int) spEditMinute.getValue();
        LocalDateTime startTime = LocalDateTime.of(date, LocalTime.of(hour, minute, 0));

        Movie m = dataStore.getMovieMap().get(movieId);
        int dur = (m != null) ? m.getDurationMinutes() : 120;
        if (!startTime.isAfter(LocalDateTime.now())) {
            JOptionPane.showMessageDialog(this, "Giờ chiếu phải ở tương lai.");
            return;
        }
        LocalDateTime endTime = startTime.plusMinutes(dur + 15);

        // KIỂM TRA TRÙNG LỊCH PHÒNG CHIẾU
        for (ShowTime st : dataStore.getShowTimeMap().values()) {
            if (st.getRoomId().equals(roomId) && st.getStartTime() != null && st.getEndTime() != null) {
                if (startTime.isBefore(st.getEndTime()) && endTime.isAfter(st.getStartTime())) {
                    Movie conflictMovie = dataStore.getMovieMap().get(st.getMovieId());
                    String conflictTitle = (conflictMovie != null) ? conflictMovie.getTitle() : st.getMovieId();
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
                    JOptionPane.showMessageDialog(this,
                            "XUNG ĐỘT LỊCH CHIẾU TẠI " + roomId + "!\n"
                                    + "Phòng này đã có phim: '" + conflictTitle + "'\n"
                                    + "Khung giờ chiếu: " + st.getStartTime().format(fmt) + " - " + st.getEndTime().format(fmt) + " ngày " + dateStr + ".\n"
                                    + "Vui lòng chọn khung giờ khác hoặc phòng chiếu khác!",
                            "Cảnh Báo Trùng Lịch", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        }

        int maxSt = 0;
        for (String k : dataStore.getShowTimeMap().keySet()) {
            if (k.startsWith("ST")) {
                try {
                    int num = Integer.parseInt(k.substring(2));
                    if (num > maxSt) maxSt = num;
                } catch (Exception ignored) {}
            }
        }
        String stId = String.format("ST%02d", maxSt + 1);

        Room room = dataStore.getRoomMap().get(roomId);
        int rows = (room != null) ? room.getTotalRows() : 8;
        int cols = (room != null) ? room.getTotalCols() : 10;
        Map<String, Seat> seats = DataStore.generateRoomSeats(rows, cols);

        ShowTime newSt = new ShowTime(stId, movieId, roomId, startTime, endTime, price, seats);
        dataStore.getShowTimeMap().put(stId, newSt);
        dataStore.saveAllData();

        reloadShowtimeTable();

        if (onDataChangedCallback != null) {
            onDataChangedCallback.run();
        }

        JOptionPane.showMessageDialog(this,
                "Tạo suất chiếu mới thành công!\n"
                        + "• Mã suất   : " + stId + "\n"
                        + "• Phim      : " + (m != null ? m.getTitle() : movieId) + "\n"
                        + "• Thời gian : " + startTime.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) + "\n"
                        + "• Phòng     : " + roomId,
                "Thành công", JOptionPane.INFORMATION_MESSAGE);

        resetFormToCreateMode();
    }
}
