import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.util.Vector;

/**
* Created with IntelliJ IDEA.
* User: lee
*/
class Sbox extends JPanel {
    public static final long serialVersionUID = 1L;
    private Vector<JLabel> squares = new Vector<JLabel>();

    public Sbox(MouseAdapter ma) {
        super(new GridLayout(3, 3, 0, 0));
        setBorder(BorderFactory.createLineBorder(Color.black, 2));

        for (int i = 1; i <= 9; i++) {
            // JLabel jl = new JLabel(String.valueOf(i));
            JLabel jl = new JLabel(" ");
            squares.addElement(jl);
            jl.setBorder(BorderFactory.createLineBorder(Color.black, 1));
            jl.setOpaque(true);
            jl.setFont(Font.decode("Dialog-38"));
            jl.setHorizontalAlignment(JLabel.CENTER);
            jl.setVerticalAlignment(JLabel.CENTER);
            jl.addMouseListener(ma);
            add(jl);
        } // end for
    } // end constructor

    public Vector<JLabel> getSquares() {
        return squares;
    }

    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    } // end getInsets

    public Dimension getPreferredSize() {
        return new Dimension(130, 130);
    } // end getPreferredSize
} // end class Sbox
