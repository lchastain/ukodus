import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created with IntelliJ IDEA.
 * User: lee
 */
public class InitialInfo extends JPanel {

    public InitialInfo(ActionListener al)
    {
        setLayout(new BorderLayout());

        add(new Spacer(1,10), "North");
        add(new Spacer(1,10), "South");
        JPanel centerPanel = new JPanel(new GridLayout(0,1,0,14));

        JButton jbDefine = new JButton(Values.defineButton);
        JButton jbLast = new JButton(Values.lastButton);
        JButton jbLoad = new JButton(Values.loadButton);
        JButton jbHelp = new JButton(Values.helpButton);

        Font f = new Font("Dialog", Font.PLAIN, 24);
        jbDefine.setFont(f);
        jbLast.setFont(f);
        jbLoad.setFont(f);
        jbHelp.setFont(f);

        jbDefine.addActionListener(al);
        jbLast.addActionListener(al);
        jbLoad.addActionListener(al);
        jbHelp.addActionListener(al);

        centerPanel.add(jbDefine);
        centerPanel.add(jbLast);
        centerPanel.add(jbLoad);
        centerPanel.add(jbHelp);

        add(centerPanel, "Center");
    }
}