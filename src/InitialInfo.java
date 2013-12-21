import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Created with IntelliJ IDEA.
 * User: lee
 */
public class InitialInfo extends JPanel implements Values {

    public InitialInfo(ActionListener al)
    {
        setLayout(new BorderLayout());

        add(new Spacer(1,10), "North");
        add(new Spacer(1,10), "South");
        JPanel centerPanel = new JPanel(new GridLayout(0,1,0,14));

        JButton jbDefine = new JButton(defineButton);
        JButton jbLast = new JButton(lastButton);
        JButton jbLoad = new JButton(loadButton);
        JButton jbHelp = new JButton(helpButton);

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