/*****************************************************************************/
/*                                                                           */
/* File name: Spacer.java                                                    */
/*                                                                           */
/* Description:  This purpose of this custom component is to                 */
/*   create a specific amount of space, specified in                         */
/*   pixels, that it should take up on the screen.                           */
/*                                                                           */
/*****************************************************************************/

import javax.swing.*;
import java.awt.*;

public class Spacer extends JComponent {
  private static final long serialVersionUID = -6506429229180781162L;

  int Width;
  int Height;
  Color bColor;
  Color fColor;
  Color c = null;

  public Spacer() {  // constructor
    super();
    bColor = getBackground();
    fColor = getForeground();
  } // end constructor

  public Spacer(int width, int height) {  // constructor
    super();
    Width = width;
    Height = height;
    bColor = getBackground();
    fColor = getForeground();
  } // end constructor

  public Dimension getMaximumSize() { return getPreferredSize(); }
  public Dimension getMinimumSize() { return getPreferredSize(); }

  public Dimension getPreferredSize() {
    if(Width==0 || Height==0) return super.getPreferredSize();
    return new Dimension(Width, Height);
  } // end getPreferredSize

  public void resetColor() {
    c = null;
    setForeground(fColor);
    setBackground(bColor);
    repaint();
  } // end resetColor

  public void setColor(Color c) {
    this.c = c;
    setForeground(c);
    setBackground(c);
  } // end setColor

  public void paint(Graphics g) {
    super.paint(g);
    Dimension d = getSize();
    if(c != null) g.fillRect(0, 0, d.width, d.height);
  } // end paint
} // end class
