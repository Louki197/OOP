import vn.edu.ptit.cinema.view.component.RevenueChartsPanel;
import vn.edu.ptit.cinema.model.booking.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.io.File;

public class ChartPreviewCheck {
  static void layout(Container c) { c.doLayout(); for(Component child:c.getComponents()) if(child instanceof Container) layout((Container)child); }
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        com.formdev.flatlaf.FlatLightLaf.setup();
        RevenueChartsPanel panel = new RevenueChartsPanel();
        LocalDate day = LocalDate.of(2026,10,28);
        java.util.List<Invoice> invoices = new ArrayList<>();
        for(int i=0;i<7;i++) invoices.add(new Invoice("I"+i,"staff","Khách","","S","Dune: Part Two","Phòng 1",day.atStartOfDay(),
          java.util.List.of(new Ticket("T"+i,"S","A01",100000)), java.util.List.of(),null,100000,0,(i+1)*150000,day.minusDays(6-i).atTime(12,0)));
        invoices.add(invoices.get(6));
        panel.setInvoices(invoices,day.minusDays(6),day);
        java.lang.reflect.Field revenueField=RevenueChartsPanel.class.getDeclaredField("revenue"); revenueField.setAccessible(true);
        Object bars=revenueField.get(panel);
        java.lang.reflect.Field valuesField=bars.getClass().getDeclaredField("values"); valuesField.setAccessible(true);
        Map<?,?> values=(Map<?,?>)valuesField.get(bars);
        if(values.size()!=7 || ((Double)values.get("28/10/26"))!=2100000) throw new AssertionError("Daily sums incorrect");
        panel.setSize(1080,255); layout(panel);
        BufferedImage image=new BufferedImage(1080,255,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();
        ImageIO.write(image,"png",new File("target/chart-preview.png"));
        panel.setInvoices(java.util.List.of(),day,day);layout(panel);
        g=image.createGraphics();panel.printAll(g);g.dispose();
        System.out.println("PASS: daily aggregation, repeated-day sum, filled dates, populated and empty chart rendering");
      } catch(Exception ex) {throw new RuntimeException(ex);}
    });
  }
}
