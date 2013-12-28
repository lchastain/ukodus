import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Main implements Values {
    public static void main(String args[]) {
        JMenu jmFile = new JMenu("File");
        jmFile.add(new JMenuItem("Open..."));
        jmFile.add(new JMenuItem("Exit"));

        JMenu jmView = new JMenu("View");
        jmView.add(mi_view_1); // Menu Item - Restart
        jmView.add(mi_view_2); // Menu Item - Remove Highlighting
        jmView.add(mi_view_3); // Explanation
        jmView.add(new JMenuItem("Set Auto-solution delay..."));
        jmView.add(new JMenuItem("Reorder the methodologies..."));
        mi_view_1.setEnabled(false);
        mi_view_2.setEnabled(false);
        mi_view_3.setEnabled(false);

        JMenu jmHelp = new JMenu("Help");
        jmHelp.add(new JMenuItem("Documentation"));
        jmHelp.add(new JMenuItem("About"));

        JMenuBar jmb = new JMenuBar();
        jmb.add(jmFile);
        jmb.add(jmView);
        jmb.add(Box.createHorizontalGlue());
        jmb.add(jmHelp);

        JFrame theFrame = new JFrame(baseTitle);
        final Laffingas theMatrix = new Laffingas(theFrame);
        theFrame.setJMenuBar(jmb);

        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String what = ae.getActionCommand();
                theMatrix.handleMenuBar(what);
            } // end actionPerformed
        };

        //---------------------------------------------------------
        // Add the above handler to all menu items.
        //---------------------------------------------------------
        int numMenus = jmb.getMenuCount();
        // log.debug("Number of menus found: " + numMenus);
        for (int i = 0; i < numMenus; i++) {
            JMenu jm = jmb.getMenu(i);
            if (jm == null) continue;

            for (int j = 0; j < jm.getItemCount(); j++) {
                JMenuItem jmi = jm.getItem(j);
                if (jmi == null) continue; // Separator
                jmi.addActionListener(al);
            } // end for j
        } // end for i
        //---------------------------------------------------------

        theFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent we) {
                System.exit(0);
            }
        });
        theFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        theFrame.setContentPane(theMatrix);
        theFrame.pack();  // This sets the frame size to fit the solution matrix.
        theFrame.setLocationRelativeTo(null);
        theFrame.setContentPane(new InitialInfo(theMatrix));
        theFrame.setVisible(true);

    } // end of the main method
}
