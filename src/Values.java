import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: lee
 */
public interface Values {
    public static final int DEFINING = 88;
    public static final int SOLVING = 89;
    public static final int SOLVED = 90;

    public static final int BOX = 95;
    public static final int COL = 96;
    public static final int ROW = 97;

    public static final String defineButton = "Define a new puzzle";
    public static final String lastButton = "Load the previous puzzle";
    public static final String loadButton = "Load a saved puzzle";
    public static final String helpButton = "View the Help documentation";

    public static final String baseTitle = "ukodus";

    public static final JMenuItem mi_view_1 = new JMenuItem("Restart");
    public static final JMenuItem mi_view_2 = new JMenuItem("Remove Highlighting");
}