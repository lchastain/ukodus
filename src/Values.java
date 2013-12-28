import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

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
    public static final JMenuItem mi_view_3 = new JMenuItem("Explanation");

    public static ArrayList<String> methodChoices = new ArrayList<String> (Arrays.asList(
            "Highlander",
            "Lone Ranger in a Box",
            "Lone Ranger in a Column",
            "Lone Ranger in a Row",
            "Naked Pair in a Box",
            "Naked Pair in a Column",
            "Naked Pair in a Row",
            "Hidden Pair in a Box",
            "Hidden Pair in a Column",
            "Hidden Pair in a Row",
            "X-wing for Columns",
            "X-wing for Rows",
            "Box interaction with Column",
            "Box interaction with Row",
            "Column interaction with Box",
            "Row interaction with Box"            ));

}