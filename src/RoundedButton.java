import javax.swing.JButton;
import java.awt.*;
import javax.swing.border.EmptyBorder;

public class RoundedButton extends JButton {
    public RoundedButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setForeground(Color.WHITE);
        setFont(new Font("SansSerif", Font.BOLD, 13));
        setBorder(new EmptyBorder(10, 20, 10, 20));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color softPurple = new Color(190, 170, 255); // Ніжно-фіолетовий
        Color hoverPurple = new Color(160, 140, 230);
        if (getModel().isPressed()) {
            g2.setColor(hoverPurple.darker());
        } else if (getModel().isRollover()) {
            g2.setColor(hoverPurple);
        } else {
            g2.setColor(softPurple);
        }

        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
        g2.dispose();
        super.paintComponent(g);
    }
}

