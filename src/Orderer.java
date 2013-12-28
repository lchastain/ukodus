import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;

public class Orderer extends JPanel implements Values {
    // This list itself is never reordered.
    private ArrayList<String> theItems;

    // This is the initial order in which to display theItems.
    private ArrayList<Integer> theOrder;

    // This is the new order in which to display theItems.
    private ArrayList<Integer> theNewOrder;

    private JPanel centerPanel;

    public Orderer(ArrayList<String> theItems, ArrayList<Integer> theOrder) {
        this.theItems = theItems;
        this.theOrder = theOrder;
        theNewOrder = new ArrayList<Integer> (theOrder);

        int theSize = theItems.size();
        if(theSize != theOrder.size()) {
            System.out.println("Sizes do not match!");
        }

        setLayout(new BorderLayout());
        JLabel prompt = new JLabel("Left click to move up one, Right click to move down one");
        JPanel jp = new JPanel(new FlowLayout());
        jp.add(prompt);
        jp.add(new Spacer(1, 30), "North");
        add(jp, "North");
        add(new Spacer(1,10), "South");
        centerPanel = new JPanel(new GridLayout(0,1,0,6));
        rebuildCenterPanel();
        add(centerPanel, "Center");
    }

    // Given a List of strings, return a numeric list.
    public static ArrayList<Integer> getDefaultOrder(ArrayList<String> theItems) {
        int i = 0;
        ArrayList<Integer> theOrder = new ArrayList<Integer>();
        for(String s: theItems) {
            theOrder.add(i++);
        }
        return theOrder;
    }

    public ArrayList<Integer> getNewOrder() {
        return theNewOrder;
    }

    public void moveDown(JLabel jl) {
        System.out.println("Move Down " + jl.getText());
        int highestIndex = centerPanel.getComponentCount() - 1;
        int myPlace = getComponentIndex(jl);
        int newPlace;

        if(myPlace < highestIndex) {
            newPlace = myPlace + 1;
            Collections.swap(theNewOrder, myPlace, newPlace);
        } else {
            int keepThis = theNewOrder.get(myPlace);
            theNewOrder.remove(highestIndex);
            theNewOrder.add(0, keepThis);
        }
        rebuildCenterPanel();
    }

    public void moveUp(JLabel jl) {
        System.out.println("Move Up " + jl.getText());
        int myPlace = getComponentIndex(jl);
        int newPlace;

        if(myPlace > 0) {
            newPlace = myPlace - 1;
            Collections.swap(theNewOrder, myPlace, newPlace);
        } else {
            theNewOrder.add(theNewOrder.get(myPlace));
            theNewOrder.remove(0);
        }
        rebuildCenterPanel();
    }

    public static int getComponentIndex(Component component) {
        if (component != null && component.getParent() != null) {
            Container c = component.getParent();
            for (int i = 0; i < c.getComponentCount(); i++) {
                if (c.getComponent(i) == component)
                    return i;
            }
        }
        return -1;
    }

    private MouseAdapter makeMouseAdapter() {
        return new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                JLabel jl = (JLabel) e.getSource();

                // Look for right mouse press.
                int m = e.getModifiers();
                if ((m & InputEvent.BUTTON3_MASK) != 0) moveDown(jl);
                else moveUp(jl);
            } // end mousePressed
        };
    }

    public void rebuildCenterPanel() {
        Font f = new Font("Dialog", Font.PLAIN, 18);
        MouseAdapter ma = makeMouseAdapter();
        centerPanel.removeAll();

        for(int i=0; i<theItems.size(); i++) {
            JLabel jl = new JLabel(theItems.get(theNewOrder.get(i)));
            jl.setFont(f);
            jl.addMouseListener(ma);

            centerPanel.add(jl);
        }
        centerPanel.revalidate();
    }

    public void setTheNewOrder(ArrayList<Integer> al) {
        theNewOrder = al;
        rebuildCenterPanel();
    }

}
