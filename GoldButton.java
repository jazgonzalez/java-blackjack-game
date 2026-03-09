package finalProject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GoldButton extends JButton {
    private static final long serialVersionUID = 1L;
    private boolean hover;

    public GoldButton(String text) {
        super(text);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(new Font("Segoe UI", Font.BOLD, 16));
        setForeground(Color.BLACK);
        setPreferredSize(new Dimension(120, 38));

        // visual flags (will also be re-applied in updateUI)
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
            @Override public void mouseExited (MouseEvent e) { hover = false; repaint(); }
        });
    }

    @Override public void updateUI() {
        // IMPORTANT: keep the L&F UI delegate so the button still works
        super.updateUI();
        // Re-apply our flags after L&F might change them
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight(), arc = 22;

        // drop shadow
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRoundRect(2, h/6 + 2, w - 4, h - 6, arc, arc);

        ButtonModel m = getModel();
        Color cTop, cBottom, cBorder;
        if (!isEnabled()) {
            cTop=new Color(210,210,210); cBottom=new Color(170,170,170); cBorder=new Color(140,140,140);
        } else if (m.isPressed()) {
            cTop=new Color(255,215,80); cBottom=new Color(205,150,20); cBorder=new Color(155,110,10);
        } else if (hover || m.isRollover()) {
            cTop=new Color(255,235,140); cBottom=new Color(240,190,40); cBorder=new Color(190,140,30);
        } else {
            cTop=new Color(255,230,120); cBottom=new Color(240,190,40); cBorder=new Color(180,130,25);
        }

        // body
        g2.setPaint(new GradientPaint(0, 0, cTop, 0, h, cBottom));
        g2.fillRoundRect(0, 0, w, h - 6, arc, arc);

        // gloss
        g2.setPaint(new GradientPaint(0, 0, new Color(255,255,255,180), 0, h/2f, new Color(255,255,255,20)));
        g2.fillRoundRect(4, 4, w - 8, (h - 8)/2, arc - 8, arc - 8);

        // rims
        g2.setColor(new Color(255,255,255,120));
        g2.drawRoundRect(1, 1, w - 3, h - 8, arc - 2, arc - 2);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(cBorder);
        g2.drawRoundRect(0, 0, w - 1, h - 6, arc, arc);

        // label
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(getText())) / 2;
        int ty = (h - 6 + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(new Color(0,0,0,120));
        g2.drawString(getText(), tx + 1, ty + 1);
        g2.setColor(Color.BLACK);
        g2.drawString(getText(), tx, ty);

        g2.dispose();
    }
}
