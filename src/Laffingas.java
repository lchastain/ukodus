
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.Vector;
import javax.swing.*;

/**
 * @author D. Lee Chastain
 */

/* Graphical makeup of the BorderLayout at the 'top' level:
    Center panel is a 3x3 grid (of 3x3 grids)
    South panel contains a centered line of buttons
*/


// Add:  
//  Keyboard entry.  Need key listener, also a way to indicate which is active,
//    and a way to move to another square.
//
//  Show explanations of pending reductions -
//          X-wing, Naked Pair, Hidden Pair
//
// A Save and Restore that provides a way to 'go back'
//      to a user-selected point.  AND/OR - an 'undo' ?
//
// A way to select the reduction methodologies that are to be used, 
//      and the order that they are to be used in.
//
// Recognize when the reductions have removed all possibilities -
//   show an invalid puzzle and stop.
//


public class Laffingas extends JPanel implements ActionListener {
    public static final int DEFINING = Values.DEFINING;
    public static final int SOLVING = Values.SOLVING;
    public static final int SOLVED = Values.SOLVED;

    public static final int ROBOX = Values.ROBOX;
    public static final int COBOX = Values.COBOX;
    public static final int BOXRO = Values.BOXRO;
    public static final int BOXCO = Values.BOXCO;
    public static final int BOX = Values.BOX;
    public static final int COL = Values.COL;
    public static final int ROW = Values.ROW;

    private static JFrame theFrame;
    private static Laffingas theMatrix;
    private Vector<JLabel> squares;
    private int intState;
    private Color theOriginalBackground;
    private Color theHighlightBackground;
    private JButton jbSave;
    private JButton jbStart;   // After Start, text changes from 'Start' to 'Next'
    private JButton jbReset;   // 'Reset' becomes visible after Start
    private JButton jbAuto;    // Becomes visible after Start
    private int intLastFoundIndex;
    private int intNakedIndex1;
    private int intNakedIndex2;
    private int intNakedIndex3;
    private int foundArray[];   // The indexes of each 'found' square (will never be 81)
    private String referenceArray[]; // The square texts after the last 'Next'.
    private boolean pendingReductions;
    private boolean blnAutoSolve;
    private boolean hadPendingReductions;  // Need to know if we ever did.

    //---------------------------------------------------------------------
    // The values in the indices declared here rely on the placement of the
    //   labels in the squares array.  The labels are entered in a specific
    //   order and are never reordered after that, so this major dependency
    //   can be considered to be stable, if not necessarily obvious.
    //---------------------------------------------------------------------
    private int boxIndices[][];  // see 'initializeIndices' for more info.
    private int colIndices[][];
    private int rowIndices[][];

    private int theArray[][];   // Will 'point' to one of the other three after 'setTheArray'.

    public Laffingas() {
        super(new BorderLayout());

        // Initializations
        intState = DEFINING;
        initializeIndices(); // Values do not change after initial setting.
        intLastFoundIndex = -1;
        foundArray = new int[81];
        referenceArray = new String[81];
        hadPendingReductions = false;
        blnAutoSolve = false;
        resetReductionVars();  // In this case, it is a 'set', not 'reset'.

        MouseAdapter ma = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {

                String theValues = " 123456789";
                JLabel jl = (JLabel) e.getSource();
                String startText = jl.getText();
                String ttText;
                int adjust = 1;

                ttText = jl.getToolTipText(); // Note - no trim here.
                if (ttText != null) {
                    if (ttText.length() == 1) return; // Original definition, or solution.
                    // User-defined answers will have length 2 since a blank is
                    //   another possibility, to allow them to un-set the value.
                } // end if

                // Look for right mouse press.
                int m = e.getModifiers();
                if ((m & InputEvent.BUTTON3_MASK) != 0) adjust = -1;

                if (intState == DEFINING) {
                    int index = theValues.indexOf(startText);
                    index += adjust;
                    if (index > 9) index = 0;
                    if (index < 0) index = 9;
                    jl.setText(theValues.substring(index, index + 1));
                } // end if

                if (intState == SOLVING) {
                    theValues = jl.getToolTipText();
                    int theMax = theValues.length() - 1;
                    int index = theValues.indexOf(startText);
                    index += adjust;
                    if (index > theMax) index = 0;
                    if (index < 0) index = theMax;
                    jl.setText(theValues.substring(index, index + 1));
                    jl.setForeground(Color.blue);
                } // end if
            } // end mousePressed
        };

        JPanel centerPanel = new JPanel(new GridLayout(3, 3, 0, 0));
        JPanel southPanel = new JPanel(new FlowLayout());

        // Initialize the vector of squares
        squares = new Vector<JLabel>();

        // Fill the 3x3 grid with 9 Sbox
        for (int i = 1; i <= 9; i++) {
            Sbox s = new Sbox(ma);
            centerPanel.add(s);
            squares.addAll(s.getSquares());
        } // end for

        // Preserve the original background color
        theOriginalBackground = (squares.elementAt(0)).getBackground();

        jbSave = new JButton("Save");
        jbSave.addActionListener(this);

        jbStart = new JButton("Start");
        jbStart.addActionListener(this);

        jbAuto = new JButton("Auto On");
        jbAuto.addActionListener(this);
        jbAuto.setVisible(false);

        JButton jbNew = new JButton("New");
        jbNew.addActionListener(this);

        jbReset = new JButton("Reset");
        jbReset.addActionListener(this);

        southPanel.add(jbSave);
        southPanel.add(jbStart);
        southPanel.add(jbAuto);
        southPanel.add(jbNew);
        southPanel.add(jbReset);

        add(centerPanel, "Center");
        add(southPanel, "South");
    } // end constructor


    public void actionPerformed(ActionEvent e) {
        JButton jb = (JButton) e.getSource();

        // Clear previous highlighting, if any.
        highlightOff();

        //------------------------------------------------------------
        if (e.getActionCommand().equals("Start")) {
            // Check validity of entered squares.

            // First look to see if ALL are empty
            boolean allBlank = true;
            for (JLabel jl : squares) {
                if (!jl.getText().trim().equals("")) {
                    allBlank = false;
                    break;
                } // end if
            } // end for
            if (allBlank) {
                showMessage("Cannot start without some initial squares");
                return;
            } // end if

            // If any of the basic rules are violated, highlight the
            //   row, column, or box and pop up a complaint.  When they
            //   OK the dialog, leave the highlighting and continue DEFINING.
            if (!checkDuplicates(ROW)) return;
            if (!checkDuplicates(COL)) return;
            if (!checkDuplicates(BOX)) return;

            // Allow 'Auto' to show now.
            jbAuto.setVisible(true);

            // Write out the current matrix into the 'last.txt' file.
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter("last.txt"));
                out.write(stringifyMatrix());
                out.close();
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            } // end try/catch

            intState = SOLVING;
            jb.setText("Next");

            // Put the possibilities for the empty squares into their tool tips.
            //   The values at this point will only be limited by the visible intersects.
            sweep();

            // Shouldn't happen in the 'defining' phase, but someone may
            //   just be contrary and go ahead and not only enter the initial
            //   values but all the others as well.
            if (checkComplete()) {
                showMessage("Solved!");
                intState = SOLVED;
                ((JButton) e.getSource()).setText("Quit");
                jbAuto.setText("Auto On"); // Restore text to its initial state
                jbAuto.setVisible(false);  //   but the initial state is non-visible.
                blnAutoSolve = false;
                return;
            } // end if

            // Considered not doing this at the start, but even if someone
            //   does not want the help, it seems best to give them the
            //   first one and then let them turn it off.  Otherwise, not
            //   much point in using this app in the first place, and what
            //   else did they get for clicking 'Start' ?
            findNext();
        } // end if 'Start'

        //------------------------------------------------------------
        if (e.getActionCommand().equals("Quit")) {
            System.exit(0);
        } // end if

        //------------------------------------------------------------
        if (e.getActionCommand().equals("Auto On")) setAutomatic(true);
        if (e.getActionCommand().equals("Auto Off")) setAutomatic(false);

        //------------------------------------------------------------
        if (e.getActionCommand().equals("Next")) handleNextClick();

        //------------------------------------------------------------
        if (e.getSource() == jbReset) {
            loadLast();
            jbAuto.setVisible(false);
        } // end if

        if (e.getActionCommand().equals(Values.lastButton)) {
            loadLast();
            theFrame.setContentPane(theMatrix);
        } // end if

        if (e.getActionCommand().equals("Save")) {
            try {
                //Create a file chooser
                final JFileChooser fc = new JFileChooser("data");

                int returnVal = fc.showSaveDialog(theFrame);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    System.out.println("Saving: " + file.getName() + ".");
                    BufferedWriter out = new BufferedWriter(new FileWriter(file.getAbsolutePath()));
                    out.write(stringifyMatrix());
                    out.close();
                } else {
                    System.out.println("Save operation cancelled by user.");
                }
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            } // end try/catch
        }


        //------------------------------------------------------------
        if (e.getActionCommand().equals("New")) {
            intState = DEFINING;
            jbStart.setText("Start");
            clear();
            for (JLabel jl : squares) {
                jl.setToolTipText(null);
            }
        } // end if

        if (e.getActionCommand().equals(Values.defineButton)) {
            theFrame.setContentPane(theMatrix);
            // 'New'
            intState = DEFINING;
            jbStart.setText("Start");
            clear();
            for (JLabel jl : squares) {
                jl.setToolTipText(null);
            }
        }

        if (e.getActionCommand().equals(Values.loadButton)) {
            handleMenuBar("Open");
        }

        if (e.getActionCommand().equals(Values.helpButton)) {
            try {
                Runtime.getRuntime().exec("hh help" + File.separatorChar + "ukodus.chm");
            } catch(IOException ioe) {
                System.out.println(ioe.getMessage());
            } // end try/catch
        }

        System.out.println("Handled " + e.getActionCommand());
    } // end actionPerformed


    // Create and return a string that represents the current makeup of the matrix
    public String stringifyMatrix() {
        String returnString = "";
        JLabel jl;
        for (int i = 0; i < (9 * 9); i++) {
            jl = squares.elementAt(i);
            String s = jl.getText().trim();
            if (s.equals("")) {
                returnString += "0";
            } else {
                returnString += s;
            } // end if
        } // end for

        return returnString;
    }

    public void clear() {
        highlightOff();
        resetReductionVars();
        hadPendingReductions = false;
        foundArray = new int[81];
        referenceArray = new String[81];
        for (JLabel jl : squares) {
            jl.setText(" ");
            jl.setForeground(Color.black);
        } // end for
    } // end clear


    public boolean checkComplete() {
        for (JLabel jl : squares) {
            if (jl.getText().trim().equals("")) return false;
        } // end for
        return true;
    } // end checkComplete


    public boolean checkDuplicates(int intType) {
        JLabel jl;
        String values = " 123456789";
        String tmpVals;
        String boxVal;
        int intTmp;

        setTheArray(intType);
        if (theArray == null) return false;

        //System.out.println("squares: " + squares.size());
        for (int i = 0; i < 9; i++) {  // Dimension 1 iterator
            tmpVals = values;
            for (int j = 0; j < 9; j++) { // Dimension 2 iterator
                jl = squares.elementAt(theArray[i][j]);
                boxVal = jl.getText();
                if (boxVal.trim().equals("")) continue;
                intTmp = tmpVals.indexOf(boxVal.trim());
                if (intTmp >= 0) {
                    // If found, remove the value from the complete list
                    tmpVals = tmpVals.substring(0, intTmp) + tmpVals.substring(intTmp + 1);
                } else {
                    // This means we looked for this value a second time -
                    theHighlightBackground = Color.pink;
                    highlightOn(intType, i);
                    showMessage("There are duplicates within the highlighted area!\nPlease correct and try again.");
                    return false;
                } // end if
            } // end for
        } // end for
        return true;
    } // end checkDuplicates


    //----------------------------------------------------------------------------
    // Method Name: checkReferences
    //
    // This method compares the current values with the ones that were present
    //   after the user last clicked 'Next'.  It will prevent 'crossover' from
    //   a first pass of one solution to the second pass of a different one, in
    //   the case where the user has made changes in between.  If a previously
    //   set value has now been cleared by the user, it will remove reductions
    //   other than the intersects for ALL squares.
    //----------------------------------------------------------------------------
    private void checkReferences() {
        boolean changeWasMade = false;
        String strCurrentVal;
        JLabel jl;
        String sweepRepeatMessage = null;

        for (int i = 0; i < 81; i++) {
            jl = squares.elementAt(i);
            strCurrentVal = jl.getText();
            if (!strCurrentVal.equals(referenceArray[i])) {
                referenceArray[i] = strCurrentVal;
                changeWasMade = true;
                if (strCurrentVal.equals(" ")) {
                    if (sweepRepeatMessage == null) {
                        sweepRepeatMessage = "One or more previously set squares has been cleared.  This requires a reset to";
                        sweepRepeatMessage += "\n the available possibilities for every open square in the matrix.  Consequently,";
                        sweepRepeatMessage += "\n you may see a repeat of one or more of the reduction methodologies.";
                        if (hadPendingReductions) {
                            showMessage(sweepRepeatMessage);
                        } // end if
                    } // end if the message has not yet been shown
                } // end if a square was un-set
            } // end if
        } // end for i - each square

        if (changeWasMade) resetReductionVars();
        if (sweepRepeatMessage != null) sweep();
    } // end checkReferences


    // Return a count of the unique squares for which the app has found a value.
    // If intLastFoundIndex is valid and not among them, add it.
    public int countFound() {
        int theCount = 0;
        boolean alreadyThere = false;
        for (int i = 0; i < 81; i++) {
            if (foundArray[i] == 0) break;
            theCount++;

            if (foundArray[i] == intLastFoundIndex) alreadyThere = true;
        } // end for i

        if (intLastFoundIndex >= 0) {
            if (!alreadyThere) {
                foundArray[theCount] = intLastFoundIndex;
                theCount++;
            } // end if not already there
        } // end if intLastFoundIndex has a valid value
        return theCount;
    } // end countFound


    // Used in development to quickly skip the 'DEFINING' stage
    public void define(int index, int val) {

        JLabel jl;
        jl = squares.elementAt(index);

        jl.setText(String.valueOf(val));
        jl.setToolTipText(String.valueOf(val));
    } // end define


    //-------------------------------------------------------------------------
    // Method Name: findNext
    //
    //-------------------------------------------------------------------------
    public void findNext() {
        intLastFoundIndex = -1;

        findSimple();

        if (intLastFoundIndex >= 0) return;  //  We found one!

        // We only get to this point if we couldn't find a simple solution.

        // Non-simple solutions all involve the reduction of the remaining
        //   possibilities for the squares.  They do not identify the next
        //   solution square, but may make it possible to do so, after the
        //   next resweep.

        // The non-simple solutions all work in a two-pass fashion, if they
        //   'find' anything.
        //
        // The first pass will highlight the solution area and may pop up an
        //   explanatory dialog.  Control returns from this method back to
        //   the user, who may mouse over the squares to see remaining
        //   possibilities, or make changes with mouse clicks.  The second
        //   pass does not come until the user clicks 'Next' again.  If the
        //   user explicitly turns off the highlight or makes a change to a
        //   square before clicking on 'Next', the second pass is cancelled
        //   and 'Next' will function as though it is conducting an entirely
        //   new search, after making updates to remaining possibilities due
        //   to the changes that were made.
        //
        // The second pass will find the
        //   same area that the first pass highlighted and will make the
        //   reductions and return control to the user.  Highlighting will
        //   already be off, by virtue of the fact that it goes off with
        //   every click of 'Next'.

        boolean b;

        b = findNakedPair(BOX);
        if (!b) b = findNakedPair(COL);
        if (!b) b = findNakedPair(ROW);

        if (!b) b = findX();
        // could add a 'row' version

        if (!b) b = findHiddenPair(BOX);
        if (!b) b = findHiddenPair(COL);
        if (!b) b = findHiddenPair(ROW);

        if (!b) b = findInsideBox(ROBOX);
        if (!b) b = findInsideBox(COBOX);
//    if(!b) b = findOutsideBox(BOXRO);
//    if(!b) b = findOutsideBox(BOXCO);

        if (!b) {
            showMessage("Cannot find the next location; you must make one manually, " +
                    "then try again.");
            blnAutoSolve = false;
            jbAuto.setText("Auto On");
        } // end if
    } // end findNext


    // A Hidden Pair is by definition reducible, so unlike the other methods
    //   that first use one method to test for reducibility and then a different
    //   method to actually perform the reduction, this method does both.  BUT -
    //   not at the same time; it will only reduce if there are pendingReductions.
    private boolean findHiddenPair(int intAreaType) {
        int theIndex; // temporary holding var
        int index1;   // Index of the first square of the potential pair
        int index2;   // Index of the second square of the potential pair
        String strValues;     // All values in the area
        String strPairValues; // Values that occur twice
        String strValue1;
        String strValue2;

        int aCount;
        String allValues = "123456789";
        int counterIndex;

        setTheArray(intAreaType);
        if (theArray == null) return false;

        //------------------------------------------------------------------
        // Scan each 9-square area for possible values that appear two
        //   and only two times in the area, then check to see if there
        //   is another such value that also appears in the same two
        //   squares where the first value was found.  If so, and there is at
        //   least one other value in either of the two squares, then these
        //   two values are the hidden pair, located in those two squares.
        //------------------------------------------------------------------
        for (int i = 0; i < 9; i++) { // for each area
            strValues = "";
            strPairValues = "";

            // Build a composite string of all possible values
            for (int j = 0; j < 9; j++) { // for each square in the area
                theIndex = theArray[i][j];

                // If the square already has a value in it then skip it.
                if (!squares.elementAt(theIndex).getText().trim().equals("")) continue;

                // Add this square's possible answers to the composite string.
                strValues += squares.elementAt(theIndex).getToolTipText().trim();
            } // end for j

            // To see all the possibilities in the area (strValues), decomment below:
            //System.out.print("The possibilities for all open squares for ");
            //System.out.print(getAreaTypeString(intAreaType)); // Box, Row, or Col
            //System.out.println(" " + String.valueOf(i+1) + " are: [" + strValues + "]");

            // For each possible value
            for (int k = 0; k < 9; k++) {
                strValue1 = allValues.substring(k, k + 1); // Converts 0-8 to 1-9.
                aCount = 0;
                counterIndex = 0;

                // Count the occurrences of the value under consideration.
                while (counterIndex >= 0) {
                    counterIndex = strValues.indexOf(strValue1, counterIndex);
                    if (counterIndex != -1) {
                        aCount++;

                        // counterIndex is already adjusted to the location of the
                        //   value; need to nudge it one further along.
                        counterIndex++;
                    } // end if
                } // end while

                // To see every count, decomment below:
                //System.out.print("In " + getAreaTypeString(intAreaType) + " " + String.valueOf(i+1));
                //System.out.println(" possible value " + aValue + " found " + aCount + " times");

                // If the count is 2 then we have a potential half of a pair.
                //   Add this value to a list to consider further (strPairValues).
                if (aCount == 2) {
                    // To see each possible pair half, decomment below:
                    //System.out.print("In " + getAreaTypeString(intAreaType) + " " + String.valueOf(i+1));
                    //System.out.println(" the value: " + aValue + " was found twice.");
                    strPairValues += strValue1;
                } // end if
            } // end for k

            // At this point we now have a string of potential pair halves.  If
            //   the string length is not at least 2, we cannot have a pair.
            if (strPairValues.length() < 2) continue; // Go on to the next area.

            // To see a string containing all possible pair halves, decomment below:
            //System.out.print(getAreaTypeString(intAreaType) + " " + String.valueOf(i+1));
            //System.out.println(" has potential pairs for the following values : " + strPairValues);

            // For each possible half pair value -
            for (int k = 0; k < strPairValues.length() - 1; k++) {
                strValue1 = strPairValues.substring(k, k + 1);
                index1 = -1;
                index2 = -1;
                boolean otherValues = false;

                // Get the indices of the two squares that hold this value
                for (int j = 0; j < 9; j++) {
                    theIndex = theArray[i][j];

                    // If the square already has a value in it then skip it.
                    if (!squares.elementAt(theIndex).getText().trim().equals("")) continue;

                    // Get the possible answers for this square
                    strValues = squares.elementAt(theIndex).getToolTipText().trim();

                    if (strValues.contains(strValue1)) {
                        // Set the first index, if not yet set
                        if (index1 == -1) {
                            index1 = theIndex;
                            if (strValues.length() > 2) otherValues = true;
                            continue;
                        } // end if

                        // Set the second index, if not yet set
                        if (index2 == -1) {
                            index2 = theIndex;
                            if (strValues.length() > 2) otherValues = true;
                            break;
                        } // end if
                    } // end if the value was found in this square
                } // end for j - for each square in the area

                // If there are only two values in each of the two squares,
                //   regardless of whether or not this is a pair, it cannot
                //   be a 'hidden' pair.  This is the 'reducible' test.
                if (!otherValues) continue; // Go on to consider the next value.

                // Examine the rest of the potential pair halves to see if one
                //   occurs in the same squares as strValue1.
                for (int q = k + 1; q < strPairValues.length(); q++) {
                    boolean foundPair = true; // Initialize true; must be in both squares.
                    strValue2 = strPairValues.substring(q, q + 1);

                    if (!squares.elementAt(index1).getToolTipText().contains(strValue2)) foundPair = false;
                    if (!squares.elementAt(index2).getToolTipText().contains(strValue2)) foundPair = false;

                    if (foundPair) {
                        if (pendingReductions) {
                            squares.elementAt(index1).setToolTipText(" " + strValue1 + strValue2);
                            squares.elementAt(index2).setToolTipText(" " + strValue1 + strValue2);
                            resetReductionVars();
                        } else {
                            System.out.print("Found a Hidden Pair for values ");
                            System.out.print(strValue1 + ", " + strValue2);
                            System.out.print(" at indexes " + index1 + " & " + index2);
                            System.out.println(" in " + getAreaTypeString(intAreaType) + " " + String.valueOf(i + 1));
                            setPendingReductions(true);

                            theHighlightBackground = Color.gray.brighter();
                            highlightOn(intAreaType, i);
                            squares.elementAt(index1).setBackground(Color.gray);
                            squares.elementAt(index2).setBackground(Color.gray);
                        } // end if/else

                        return true;
                    } // end if

                } // end for q
            } // end for k

        } // end for i - each area
        //--------------------------------------------------------------

        return false;
    } // end findHiddenPair


    //-----------------------------------------------------------------------
    // Method Name: findHighlander
    //
    // Looks for a square where there is only one possible unused value
    //   remaining, based on the intersection of all other values showing.
    //-----------------------------------------------------------------------
    public boolean findHighlander() {
        JLabel jl;
        for (int i = 0; i < (9 * 9); i++) {
            jl = squares.elementAt(i);
            if (!jl.getText().trim().equals("")) continue;
            if (jl.getToolTipText().trim().length() == 1) {
                showIntersects(i); // highlight the row, col, box
                jl.setBorder(BorderFactory.createLineBorder(Color.red, 2));
                jl.setBackground(Color.white);
                if (blnAutoSolve) {
                    jl.setForeground(Color.green.darker());
                    jl.setText(jl.getToolTipText().trim());
                } // end if
                intLastFoundIndex = i;
                return true;
            } // end if
        } // end for
        return false;
    } // end findHighlander


    // When a value in a given Row or Column is restricted to a single box,
    //   if that box has other squares with that possible value that are
    //   not in that row or col, the value may be removed as a possibility
    //   from those squares that are in the box but outside the row or col.
    private boolean findInsideBox(int intInteractionType) {
        JLabel jl;
        String theValues;
        int theIndex;
        int theBox;

        // Set our array of vector indices to the one that matches
        //   the interaction type we're searching for.
        setTheArray(intInteractionType);
        if (theArray == null) return false;

        for (int i = 0; i < 9; i++) { // Look at each of the 9 areas
            for (int j = 1; j <= 9; j++) { // Look at each possible value
                theBox = -1; // initialization

                for (int k = 0; k < 9; k++) { // Look at each of the 9 squares in the area

                    // Use the array of vector indices to select the correct square.
                    theIndex = theArray[i][k];
                    jl = squares.elementAt(theIndex);

                    // If the square is not unsolved, skip it.
                    if (!jl.getText().trim().equals("")) continue;

                    theValues = jl.getToolTipText().trim();

                    // This should not happen because we are not supposed to be here
                    //   if the square can be solved or the puzzle is incorrect.
                    if (theValues.length() < 2) continue;

                    // But now, we can be certain that there are 2 or more possible
                    //   values for the square, and they can be examined.

                    // Does this square contain the value we're looking for?
                    if (theValues.contains(String.valueOf(j))) {
                        // If this is the first time we have considered this value,
                        //   determine which box it is contained within.
                        if (theBox == -1) theBox = getPosition(BOX, theIndex);
                        else { // Otherwise, make sure this subsequent occurrence
                            // is in the same box as any that were encountered earlier.
                            // If not, then look at the next value.
                            if (theBox != getPosition(BOX, theIndex)) {
                                theBox = -1;
                                break; // do not continue to examine more squares
                            } // end if it is in a different box

                        } // end else
                    } // end if the value is in the square
                } // end for k - for each square

                // At this point, we have either found no occurence of the value under
                //   consideration, or it has been found in one and only one box.  If
                //   not found, we move on to consider the next value.  Otherwise -
                if (theBox != -1) { // Found in one and only one box.
                    //System.out.print("The value " + String.valueOf(j) + " in ");
                    //System.out.print(getAreaTypeString(intInteractionType) + " " + String.valueOf(i+1));
                    //System.out.println(" may interact with Box " + String.valueOf(theBox+1));

                    // Now, look for other instances of the value that are outside
                    //   of the area we're considering, but INSIDE the same box.
                    int tmpIndex;
                    JLabel jlTmp;
                    String strTmp;
                    boolean reductionsWereMade = false;
                    for (int q = 0; q < 9; q++) { // For each square in the box
                        tmpIndex = boxIndices[theBox][q];

                        // If this square of the box is also contained
                        //   in the same area we're working in, skip it.
                        if (getPosition(intInteractionType, tmpIndex) == i) continue;
                        // Note that this WILL happen, three times.

                        jlTmp = squares.elementAt(tmpIndex);
                        strTmp = jlTmp.getToolTipText();
                        if (strTmp.contains(String.valueOf(j))) {
                            if (pendingReductions) {
                                reductionsWereMade = true;
                                jlTmp.setToolTipText(strTmp.replace(String.valueOf(j), ""));
                            } else {
                                System.out.print("The value " + String.valueOf(j) + " in ");
                                System.out.print(getAreaTypeString(intInteractionType) + " " + String.valueOf(i + 1));
                                System.out.println(" interacts with Box " + String.valueOf(theBox + 1));

                                theHighlightBackground = Color.yellow.darker();
                                highlightOn(intInteractionType, i);
                                highlightOn(BOX, theBox);

                                // Now show the intersecting squares that have the value -
                                for (int hl = 0; hl < 9; hl++) {
                                    jlTmp = squares.elementAt(boxIndices[theBox][hl]);

                                    // Leave alone, if value not here.
                                    if (!jlTmp.getToolTipText().contains(String.valueOf(j))) continue;

                                    // Squares in the intersection that contain the value.
                                    jlTmp.setBackground(Color.yellow);

                                    // The squares in the area we've been considering,
                                    //   that will not be reduced.
                                    if (getPosition(intInteractionType, boxIndices[theBox][hl]) == i)
                                        jlTmp.setBackground(Color.orange);
                                } // end for

                                setPendingReductions(true);
                                return true;
                            } // end if/else
                        } // end if - if this square contains the value

                    } // end for q - for each square in the box

                    // Above was one square at a time, doing the reductions where
                    //   possible.  Here, we detect that this has been done for
                    //   all squares in the Box, and return.
                    if (reductionsWereMade) {
                        resetReductionVars();
                        return true;
                    } // end if

                } // end if we have a box
            } // end for j - for each value
        } // end for i - for each area

        return false;
    } // end findInsideBox


    //-----------------------------------------------------------------------
    // Method Name: findLoneRanger
    //
    // Examines the possible values for the square and determines if they
    //   appear more than once throughout the rest of the empty squares in
    //   the same area (area type [box, col, or row] is set via input parameter).
    //   If not, then the value is a 'Lone Ranger' and regardless of the other
    //   possible values for that square, the LR is the correct one because it
    //   has nowhere else to go.
    //-----------------------------------------------------------------------
    public boolean findLoneRanger(int intAreaType) {
        JLabel jl;
        JLabel jlTmp;
        int theArea;      // Which box, row, or col (0-8)
        String theValues; // Possible values for the square
        String theChar;   // One of the possible values
        int theCount;     // How many times it appears in theValues of other squares

        setTheArray(intAreaType);
        if (theArray == null) return false;

        for (int i = 0; i < (9 * 9); i++) {
            jl = squares.elementAt(i);
            if (!jl.getText().trim().equals("")) continue;

            theValues = jl.getToolTipText().trim();
            theArea = getPosition(intAreaType, i);

            // For each character in the list of possible values for the square -
            for (int j = 0; j < theValues.length(); j++) {
                theChar = theValues.substring(j, j + 1);
                theCount = 0;

                for (int k = 0; k < 9; k++) {
                    jlTmp = squares.elementAt(theArray[theArea][k]);
                    if (!jlTmp.getText().trim().equals("")) continue;
                    if (jlTmp.getToolTipText().contains(theChar)) theCount++;
                } // end for k
                //System.out.println("Looking for " + theChar + "; found " + theCount);

                if (theCount == 1) { // We found the LR!
                    System.out.println("The Lone Ranger is: " + theChar);
                    theHighlightBackground = Color.magenta;
                    highlightOn(intAreaType, theArea);
                    jl.setBorder(BorderFactory.createLineBorder(Color.red, 2));
                    jl.setBackground(Color.white);
                    if (blnAutoSolve) {
                        jl.setForeground(Color.magenta.darker());
                        jl.setText(theChar);
                    } // end if
                    intLastFoundIndex = i;
                    return true;
                } // end if
            } // end for j
        } // end for i

        return false;
    } // end findLoneRanger


    //-----------------------------------------------------------------------------
    // Method Name: findNakedReductions
    //
    // Examines the possible values for a square in the indicated area
    //   and if there are exactly two then look for another square in the
    //   same area with the same two possibilities (and no others besides
    //   those two; this is where the 'naked' comes from).
    // If a pair is found, further processing will depend whether it is
    //   a new discovery or one that is already in progress, as determined
    //   by the value of 'pendingReductions'.
    //
    // There are 3 possible outcomes:
    // 1.  A pair was not found, or it was found to be irrelevant - returns false.
    // 2.  A pair was found for the first time - Area highlighted, returns true.
    // 3.  A pair was found for the second time - Reductions made, returns true.
    //-----------------------------------------------------------------------------
    public boolean findNakedPair(int intAreaType) {
        JLabel jl;
        JLabel jl1, jl2;

        String str1, str2;
        String theValues; // Possible values for the square
        int theIndex;
        int theArea;  // Area 0-8 for the relevant BOX, COL, or ROW type.
        int j;

        //  A vector of 2-possibility square indices
        Vector<Integer> theTwins;

        setTheArray(intAreaType);
        if (theArray == null) return false;

        for (int i = 0; i < 9; i++) { // For each area
            theTwins = new Vector<Integer>();
            // Look for squares with two (and only two) possibilities.
            // When found, add to 'theTwins' vector.
            for (j = 0; j < 9; j++) { // For each square in the area
                theIndex = theArray[i][j];
                jl = squares.elementAt(theIndex);

                // If the square under examination is not empty, keep looking.
                if (!jl.getText().trim().equals("")) continue;

                theValues = jl.getToolTipText().trim();
                if (theValues.length() != 2) continue;  // Need two possibilities

                theTwins.addElement(theIndex);
            } // end for j

            if (theTwins.size() < 2) continue; // Cannot have a pair here; move on to next area

            // Now cycle thru the Vector, looking for pairs.
            // We need both an outer and inner loop to do this.
            for (j = 0; j < theTwins.size() - 1; j++) {
                jl1 = squares.elementAt(theTwins.elementAt(j));
                str1 = jl1.getToolTipText().trim();

                // Search the remaining twins for a matching value
                for (int k = j + 1; k < theTwins.size(); k++) {
                    jl2 = squares.elementAt(theTwins.elementAt(k));
                    str2 = jl2.getToolTipText().trim();

                    if (str1.equals(str2)) { // We have a Naked Pair!
                        intNakedIndex1 = theTwins.elementAt(j);
                        intNakedIndex2 = theTwins.elementAt(k);

                        // But is it reducible?
                        if (!isAreaNakedreducible(i, str1)) {
                            intNakedIndex1 = -1;
                            intNakedIndex2 = -1;
                            continue;
                        } // end if

                        // Now we have a reducible Naked Pair.

                        if (!pendingReductions) {
                            setPendingReductions(true);
                            System.out.print("Found a Naked Pair for values ");
                            System.out.print(str1.substring(0, 1) + ", " + str1.substring(1));
                            System.out.print(" at indexes " + intNakedIndex1 + " & " + intNakedIndex2);
                            System.out.println(" in " + getAreaTypeString(intAreaType) + " " + String.valueOf(i + 1));

                            theHighlightBackground = Color.cyan.darker();
                            highlightOn(intAreaType, i);

                            // Now, if this is a BOX, we'll need to see if
                            //   the ROW or COL also needs highlighting.
                            if (intAreaType == BOX) {
                                theArea = getPosition(COL, intNakedIndex1);
                                // System.out.println("   the COL for N1 is: " + theArea);
                                // System.out.println("   the COL for N2 is: " + getPosition(COL, intNakedIndex2));
                                if (theArea == getPosition(COL, intNakedIndex2)) {
                                    setTheArray(COL);
                                    if (isAreaNakedreducible(theArea, str1)) highlightOn(COL, theArea);
                                } // end if also a COL
                                theArea = getPosition(ROW, intNakedIndex1);
                                // System.out.println("   the ROW for N1 is: " + theArea);
                                // System.out.println("   the ROW for N2 is: " + getPosition(ROW, intNakedIndex2));
                                if (theArea == getPosition(ROW, intNakedIndex2)) {
                                    setTheArray(ROW);
                                    if (isAreaNakedreducible(theArea, str1)) highlightOn(ROW, theArea);
                                } // end if also a ROW
                            } // end if BOX

                            jl1.setBackground(Color.cyan.brighter());
                            jl2.setBackground(Color.cyan.brighter());
                            return true;
                        } else { // There are pending reductions
                            stripNaked(i, str1);

                            if (intAreaType == BOX) {
                                theArea = getPosition(COL, intNakedIndex1);
                                if (theArea == getPosition(COL, intNakedIndex2)) {
                                    setTheArray(COL);
                                    stripNaked(theArea, str1);
                                } // end if also a COL
                                theArea = getPosition(ROW, intNakedIndex1);
                                if (theArea == getPosition(ROW, intNakedIndex2)) {
                                    setTheArray(ROW);
                                    stripNaked(theArea, str1);
                                } // end if also a ROW
                            } // end if BOX

                            resetReductionVars();
                            return true;
                        } // end if
                    } // end if we have a pair
                } // end for k
            } // end for j (again)
        } // end for i
        return false;
    } // end findNakedPair


    //------------------------------------------------------------------------
    // Method Name: findX
    //
    // Look for a row where a potential value appears in two and only two
    //   squares.  Then, continue looking until you find a second such row,
    //   for the same value and in the same two columns.  Since that value
    //   cannot be both places in the row or both places in the column, it will
    //   be in either the (top left and bottom right), or (top right and bottom
    //   left) squares.  If you drew lines between the alternative locations,
    //   you would see an 'X'.  The methodology is known as 'X-wing'.  Once
    //   such a value is found, this value can be eliminated as a possibility
    //   in all other squares in either of the two columns.
    //
    // There are 3 possible outcomes:
    // 1.  An X was not found, or it was found to be irrelevant - returns false.
    // 2.  An X was found for the first time - Area highlighted, returns true.
    // 3.  An X was found for the second time - Reductions made, returns true.
    //------------------------------------------------------------------------
    public boolean findX() {
        int row1, row2;
        int col1, col2;
        String strValues;
        String aValue;
        int theIndex;
        int aCount;
        String allValues = "123456789";
        int counterIndex;
        int theCounts[][] = new int[9][9];

        //--------------------------------------------------------------
        // Scan the matrix and get a count of ALL possibilities in each row.
        //   Place the results into 'theCounts[][]', where the
        //   first dimension is the row, the second is the value
        //   that is being counted.
        //--------------------------------------------------------------
        for (int i = 0; i < 9; i++) { // for each row
            strValues = "";
            for (int j = 0; j < 9; j++) { // for each square on the row
                theIndex = rowIndices[i][j];

                // If the square is not empty -
                if (!squares.elementAt(theIndex).getText().trim().equals("")) continue;

                // Get all the possible answers for the entire row
                strValues += squares.elementAt(theIndex).getToolTipText().trim();
            } // end for j

            //System.out.print("The possibilities for all open squares on row ");
            //System.out.println(String.valueOf(i+1) + " are " + strValues);

            // Get a count for each possible value
            for (int k = 0; k < 9; k++) { // For each possible value
                aValue = allValues.substring(k, k + 1);
                aCount = 0;
                counterIndex = 0;
                while (counterIndex >= 0) {
                    counterIndex = strValues.indexOf(aValue, counterIndex);
                    if (counterIndex != -1) {
                        aCount++;
                        counterIndex++;
                    } // end if
                } // end while

                theCounts[i][k] = aCount;
                //  System.out.print("In row " + String.valueOf(i+1));
                //  System.out.println(" possible value " + aValue + " found " + aCount + " times");
            } // end for k
        } // end for i
        //--------------------------------------------------------------

        // Now scan 'theCounts' for values of 2.  If we find two rows for
        //   the same value, we have our rows.
        for (int i = 0; i < 8; i++) {  // For each row (except the last)
            for (int j = 0; j < 9; j++) { // For each value
                row1 = -1;
                row2 = -1;
                if (theCounts[i][j] != 2) continue;

                // The count is 2; we need to look further...
                for (int k = i + 1; k < 9; k++) { // For each subsequent row
                    if (theCounts[k][j] == 2) {
                        row1 = i;
                        row2 = k;
                    } // end if
                } // end for

                if (row2 == -1) continue; // We found a first row but not a second.

                //System.out.println("Found a possible X on rows " + String.valueOf(row1+1) + ", " + String.valueOf(row2+1) + " for value " + String.valueOf(j+1));

                // Now look to see that the values appear in the same columns.
                col1 = -1;
                col2 = -1;

                // For row1 we can just set the columns; no checking required.
                for (int k = 0; k < 9; k++) { // For each square in row1
                    if (squares.elementAt(rowIndices[row1][k]).getToolTipText().contains(String.valueOf(j + 1))) {
                        if (col1 == -1) {
                            col1 = k;
                            //System.out.print("  Col1: " + String.valueOf(k+1));
                            continue;
                        } // end if
                        if (col2 == -1) {
                            col2 = k;
                            //System.out.print("\tCol2: " + String.valueOf(k+1));
                            break; // This is the 2nd on this row and there are only 2.
                        } // end if
                    } // end if the square possibilities contain the value
                } // end for k - each square

                // For row2 we need to check that the value appears in the same cols.
                //   If it does not, we keep looking.
                if (!squares.elementAt(rowIndices[row2][col1]).getToolTipText().contains(String.valueOf(j + 1))) {
                    //System.out.println("\tRow 2 Col 1 - NO match.");
                    continue; // on to the next value
                } // end if the square possibilities do not contain the value

                if (!squares.elementAt(rowIndices[row2][col2]).getToolTipText().contains(String.valueOf(j + 1))) {
                    //System.out.println("\tRow 2 Col 2 - NO match.");
                    continue; // on to the next value
                } // end if the square possibilities do not contain the value

                // If we made it here, we have an X.

                // But it does us no good if it is not reducible -
                String theValue = String.valueOf(j + 1);
                if (!isAreaXreducible(row1, row2, col1, col2, theValue)) continue;

                // So - if we're here, we have a reducible X
                if (!pendingReductions) { // Highlight the X and return true
                    setPendingReductions(true);
                    // System.out.println("  Reducible: " + pendingXReductions);
                    System.out.print("Found an X for value [" + theValue + "] on ");
                    System.out.print("Rows " + String.valueOf(row1 + 1) + ", " + String.valueOf(row2 + 1));
                    System.out.println(" & Cols " + String.valueOf(col1 + 1) + ", " + String.valueOf(col2 + 1));

                    JLabel jl;

                    theHighlightBackground = Color.yellow.darker();
                    highlightOn(COL, col1);
                    highlightOn(COL, col2);

                    theHighlightBackground = Color.yellow.brighter();
                    // Top Left
                    jl = squares.elementAt(colIndices[col1][row1]);
                    jl.setBackground(theHighlightBackground);

                    // Bottom Right
                    jl = squares.elementAt(colIndices[col2][row2]);
                    jl.setBackground(theHighlightBackground);

                    // Bottom Left
                    jl = squares.elementAt(colIndices[col1][row2]);
                    jl.setBackground(theHighlightBackground);

                    // Top Right
                    jl = squares.elementAt(colIndices[col2][row1]);
                    jl.setBackground(theHighlightBackground);

                    return true;
                } else { // There are pending reductions
                    stripX(row1, row2, col1, col2, theValue);
                    resetReductionVars();
                    return true;
                } // end if pendingReductions or not
            } // end for j - each value
        } // end for i - each row

        return false;
    } // end findX


    //-----------------------------------------------------------------------
    // This method calls the methods that can identify the one correct value
    //   for a square, if possible.  Those methods will not only highlight
    //   the relevant area in a method-specific color code, but they will
    //   also identify that one square and return true.  If the simple
    //   versions cannot find a solvable square, they return false.
    //-----------------------------------------------------------------------
    private void findSimple() {
        boolean b;

        // This is our first method of attack; if we have a 'hit' here then
        //   it is the easiest solution for the user to see.
        b = findHighlander();

        // If that didn't work, we become Tonto -
        if (!b) b = findLoneRanger(BOX);
        if (!b) b = findLoneRanger(COL);
        if (!b) findLoneRanger(ROW);

        if (intLastFoundIndex >= 0) {
            System.out.println("\t\t\tFound Count: " + countFound() + "\t\tLast Index: " + intLastFoundIndex);
        } // end if
    } // end findSimple


    //-----------------------------------------------------------------------
    // Method Name: getAreaTypeString
    //
    // A convenience method to convert the int value
    //   for area type to meaningful text.
    //-----------------------------------------------------------------------
    public String getAreaTypeString(int intAreaType) {
        switch (intAreaType) {
            case BOX:
            case BOXRO:
            case BOXCO:
                return "Box";
            case ROW:
            case ROBOX:
                return "Row";
            case COL:
            case COBOX:
                return "Column";
            default:
                return "";
        } // end switch
    } // end getAreaTypeString


    //------------------------------------------------------------------
    // Method Name: getIntersects
    //
    // Given an index into the squares vector, return a string containing
    //   all non-empty values at the row, col, and box intersects.
    //   Duplicate values are ok but no spaces.
    //------------------------------------------------------------------
    public String getIntersects(int index) {
        String strReturn = "";
        JLabel jl;

        int theRow = getPosition(ROW, index);
        int theCol = getPosition(COL, index);
        int theBox = getPosition(BOX, index);

        // Get the ROW intersects
        for (int i = 0; i < 9; i++) {
            jl = squares.elementAt(rowIndices[theRow][i]);
            strReturn += jl.getText().trim();
        } // end for

        // Get the COL intersects
        for (int i = 0; i < 9; i++) {
            jl = squares.elementAt(colIndices[theCol][i]);
            strReturn += jl.getText().trim();
        } // end for

        // Get the BOX intersects
        for (int i = 0; i < 9; i++) {
            jl = squares.elementAt(boxIndices[theBox][i]);
            strReturn += jl.getText().trim();
        } // end for

        //System.out.print("Row: " + theRow + "\tCol: " + theCol + "\tBox:" + theBox);
        //System.out.println("\tIntersects: " + strReturn );

        return strReturn;
    } // end getIntersects


    //---------------------------------------------------------------
    // Method Name: getPosition
    //
    // Given the 0-80 index into the squares vector, convert to a 0-8
    //   value of the corresponding row, column, or box.
    //---------------------------------------------------------------
    private int getPosition(int intType, int index) {
        int[][] tmp = theArray;

        // We do this as a shortcut but if we do not undo it before
        //   leaving, it would mean that this method is not just
        //   a passive query; it needs to be undone before we leave.
        setTheArray(intType);

        for (int i = 0; i < 9; i++) {  // Dimension 1 iterator
            for (int j = 0; j < 9; j++) { // Dimension 2 iterator
                if (theArray[i][j] == index) {
                    theArray = tmp; // put it back to whatever it was.
                    return i;
                }
            } // end for
        } // end for

        theArray = tmp; // put it back to whatever it was.
        return -1;
    } // end getPosition


    private void handleMenuBar(String what) {
        if (what.equals("Open")) {
            //Create a file chooser
            final JFileChooser fc = new JFileChooser("data");

            //In response to a button click:
            int returnVal = fc.showOpenDialog(theFrame);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                theFrame.setContentPane(theMatrix);
                File file = fc.getSelectedFile();
                System.out.println("Opening: " + file.getName() + ".");
                loadFile(file.getAbsolutePath());
            } else {
                System.out.println("Open command cancelled by user.");
            }
        } // end if

        if (what.equals("Exit")) System.exit(0);

        if (what.equals("Remove Highlighting")) {
            highlightOff();
            intLastFoundIndex = -1;
            resetReductionVars();
        } // end if

        if (what.equals("Getting Started")) {
            theFrame.setContentPane(new InitialInfo(theMatrix));
            ((JPanel)theFrame.getContentPane()).revalidate();
            theFrame.repaint();
        } // end if

    } // end handleMenuBar


    //----------------------------------------------------------------------
    // Method Name: handleNextClick
    //
    // This method has been separated out from actionPerformed so that
    //   it can be called again from another location; a 'programmatic'
    //   'Next' button click.
    //----------------------------------------------------------------------
    private void handleNextClick() {
        // Clear previous highlighting, if any.
        highlightOff();

        // Check the answer we just left; only continue if correct.
        if (!checkDuplicates(ROW)) return;
        if (!checkDuplicates(COL)) return;
        if (!checkDuplicates(BOX)) return;

        if (checkComplete()) {
            sweep(); // Locks in the solution
            showMessage("Solved!");
            intState = SOLVED;
            jbStart.setText("Quit");
            jbAuto.setText("Auto On"); // Restore text to its initial state
            jbAuto.setVisible(false);  //   but the initial state is non-visible.
            blnAutoSolve = false;
            return;
        } // end if

        // Check the reference array to see if any values have changed.
        // If so, cancel any in-progress two-pass methodology and reset
        //   the reference array.
        checkReferences();

        // Look to see if they pressed 'Next' without filling in the most
        //   recently found square.  If so, then we interpret that action
        //   to mean that they want to turn off the highlight and we've
        //   already done that at this point so we'll just bail out now.
        if (intLastFoundIndex != -1) { // If we previously found a solvable square
            if (squares.elementAt(intLastFoundIndex).getText().trim().equals("")) {
                intLastFoundIndex = -1;
                return;
            } // end if
        } // end if

        // Reduce the remaining possible values for every square,
        //   based on intersects only.
        resweep();

        // Look for the next square or area to consider,
        //   based on a succession of strategies.
        findNext();

        if (blnAutoSolve) {
            // By recursing as a separate thread, we can
            //   return from this one, which allows the 'Auto Off'
            //   button to be used, if desired.
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(700);
                    } catch (InterruptedException ie) {
                        System.out.println(ie.getMessage());
                    }
                    handleNextClick();
                }
            }).start();
        } // end if

    } // end handleNextClick


    // Uncolor the background of all squares
    public void highlightOff() {
        JLabel jl;

        for (int i = 0; i < (9 * 9); i++) {
            jl = squares.elementAt(i);
            jl.setBackground(theOriginalBackground);
            jl.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        } // end for
    } // end highlightOff


    // Color the background of the specified row, column, or box
    public void highlightOn(int intType, int intNum) {
        JLabel jl;

        // This may be ok for now, since highlighting is a last step in
        //   any reduction methodology.
        setTheArray(intType);

        for (int i = 0; i < 9; i++) {  // 2nd dimension iterator
            jl = squares.elementAt(theArray[intNum][i]);
            jl.setBackground(theHighlightBackground);
        } // end for
    } // end highlightOn


    //------------------------------------------------------------------
    // These index assignments could also be done via calculations
    //   but this 'brute force' method is more readable / understandable.
    //   The values were obtained by printing out a 9x9 matrix of squares
    //   and then filling in the values (top left = 0) in the same
    //   order that they are created in the constructor.  Since that
    //   sequence is by Boxes rather than rows or columns, this first
    //   group is very straightforward; just 0-80.  The next two groups
    //   are a bit more jumbled.
    //------------------------------------------------------------------
    public void initializeIndices() {
        boxIndices = new int[9][9];
        boxIndices[0][0] = 0;
        boxIndices[0][1] = 1;
        boxIndices[0][2] = 2;
        boxIndices[0][3] = 3;
        boxIndices[0][4] = 4;
        boxIndices[0][5] = 5;
        boxIndices[0][6] = 6;
        boxIndices[0][7] = 7;
        boxIndices[0][8] = 8;
        boxIndices[1][0] = 9;
        boxIndices[1][1] = 10;
        boxIndices[1][2] = 11;
        boxIndices[1][3] = 12;
        boxIndices[1][4] = 13;
        boxIndices[1][5] = 14;
        boxIndices[1][6] = 15;
        boxIndices[1][7] = 16;
        boxIndices[1][8] = 17;
        boxIndices[2][0] = 18;
        boxIndices[2][1] = 19;
        boxIndices[2][2] = 20;
        boxIndices[2][3] = 21;
        boxIndices[2][4] = 22;
        boxIndices[2][5] = 23;
        boxIndices[2][6] = 24;
        boxIndices[2][7] = 25;
        boxIndices[2][8] = 26;
        boxIndices[3][0] = 27;
        boxIndices[3][1] = 28;
        boxIndices[3][2] = 29;
        boxIndices[3][3] = 30;
        boxIndices[3][4] = 31;
        boxIndices[3][5] = 32;
        boxIndices[3][6] = 33;
        boxIndices[3][7] = 34;
        boxIndices[3][8] = 35;
        boxIndices[4][0] = 36;
        boxIndices[4][1] = 37;
        boxIndices[4][2] = 38;
        boxIndices[4][3] = 39;
        boxIndices[4][4] = 40;
        boxIndices[4][5] = 41;
        boxIndices[4][6] = 42;
        boxIndices[4][7] = 43;
        boxIndices[4][8] = 44;
        boxIndices[5][0] = 45;
        boxIndices[5][1] = 46;
        boxIndices[5][2] = 47;
        boxIndices[5][3] = 48;
        boxIndices[5][4] = 49;
        boxIndices[5][5] = 50;
        boxIndices[5][6] = 51;
        boxIndices[5][7] = 52;
        boxIndices[5][8] = 53;
        boxIndices[6][0] = 54;
        boxIndices[6][1] = 55;
        boxIndices[6][2] = 56;
        boxIndices[6][3] = 57;
        boxIndices[6][4] = 58;
        boxIndices[6][5] = 59;
        boxIndices[6][6] = 60;
        boxIndices[6][7] = 61;
        boxIndices[6][8] = 62;
        boxIndices[7][0] = 63;
        boxIndices[7][1] = 64;
        boxIndices[7][2] = 65;
        boxIndices[7][3] = 66;
        boxIndices[7][4] = 67;
        boxIndices[7][5] = 68;
        boxIndices[7][6] = 69;
        boxIndices[7][7] = 70;
        boxIndices[7][8] = 71;
        boxIndices[8][0] = 72;
        boxIndices[8][1] = 73;
        boxIndices[8][2] = 74;
        boxIndices[8][3] = 75;
        boxIndices[8][4] = 76;
        boxIndices[8][5] = 77;
        boxIndices[8][6] = 78;
        boxIndices[8][7] = 79;
        boxIndices[8][8] = 80;

        colIndices = new int[9][9];
        colIndices[0][0] = 0;
        colIndices[0][1] = 3;
        colIndices[0][2] = 6;
        colIndices[0][3] = 27;
        colIndices[0][4] = 30;
        colIndices[0][5] = 33;
        colIndices[0][6] = 54;
        colIndices[0][7] = 57;
        colIndices[0][8] = 60;
        colIndices[1][0] = 1;
        colIndices[1][1] = 4;
        colIndices[1][2] = 7;
        colIndices[1][3] = 28;
        colIndices[1][4] = 31;
        colIndices[1][5] = 34;
        colIndices[1][6] = 55;
        colIndices[1][7] = 58;
        colIndices[1][8] = 61;
        colIndices[2][0] = 2;
        colIndices[2][1] = 5;
        colIndices[2][2] = 8;
        colIndices[2][3] = 29;
        colIndices[2][4] = 32;
        colIndices[2][5] = 35;
        colIndices[2][6] = 56;
        colIndices[2][7] = 59;
        colIndices[2][8] = 62;
        colIndices[3][0] = 9;
        colIndices[3][1] = 12;
        colIndices[3][2] = 15;
        colIndices[3][3] = 36;
        colIndices[3][4] = 39;
        colIndices[3][5] = 42;
        colIndices[3][6] = 63;
        colIndices[3][7] = 66;
        colIndices[3][8] = 69;
        colIndices[4][0] = 10;
        colIndices[4][1] = 13;
        colIndices[4][2] = 16;
        colIndices[4][3] = 37;
        colIndices[4][4] = 40;
        colIndices[4][5] = 43;
        colIndices[4][6] = 64;
        colIndices[4][7] = 67;
        colIndices[4][8] = 70;
        colIndices[5][0] = 11;
        colIndices[5][1] = 14;
        colIndices[5][2] = 17;
        colIndices[5][3] = 38;
        colIndices[5][4] = 41;
        colIndices[5][5] = 44;
        colIndices[5][6] = 65;
        colIndices[5][7] = 68;
        colIndices[5][8] = 71;
        colIndices[6][0] = 18;
        colIndices[6][1] = 21;
        colIndices[6][2] = 24;
        colIndices[6][3] = 45;
        colIndices[6][4] = 48;
        colIndices[6][5] = 51;
        colIndices[6][6] = 72;
        colIndices[6][7] = 75;
        colIndices[6][8] = 78;
        colIndices[7][0] = 19;
        colIndices[7][1] = 22;
        colIndices[7][2] = 25;
        colIndices[7][3] = 46;
        colIndices[7][4] = 49;
        colIndices[7][5] = 52;
        colIndices[7][6] = 73;
        colIndices[7][7] = 76;
        colIndices[7][8] = 79;
        colIndices[8][0] = 20;
        colIndices[8][1] = 23;
        colIndices[8][2] = 26;
        colIndices[8][3] = 47;
        colIndices[8][4] = 50;
        colIndices[8][5] = 53;
        colIndices[8][6] = 74;
        colIndices[8][7] = 77;
        colIndices[8][8] = 80;

        rowIndices = new int[9][9];
        rowIndices[0][0] = 0;
        rowIndices[0][1] = 1;
        rowIndices[0][2] = 2;
        rowIndices[0][3] = 9;
        rowIndices[0][4] = 10;
        rowIndices[0][5] = 11;
        rowIndices[0][6] = 18;
        rowIndices[0][7] = 19;
        rowIndices[0][8] = 20;
        rowIndices[1][0] = 3;
        rowIndices[1][1] = 4;
        rowIndices[1][2] = 5;
        rowIndices[1][3] = 12;
        rowIndices[1][4] = 13;
        rowIndices[1][5] = 14;
        rowIndices[1][6] = 21;
        rowIndices[1][7] = 22;
        rowIndices[1][8] = 23;
        rowIndices[2][0] = 6;
        rowIndices[2][1] = 7;
        rowIndices[2][2] = 8;
        rowIndices[2][3] = 15;
        rowIndices[2][4] = 16;
        rowIndices[2][5] = 17;
        rowIndices[2][6] = 24;
        rowIndices[2][7] = 25;
        rowIndices[2][8] = 26;
        rowIndices[3][0] = 27;
        rowIndices[3][1] = 28;
        rowIndices[3][2] = 29;
        rowIndices[3][3] = 36;
        rowIndices[3][4] = 37;
        rowIndices[3][5] = 38;
        rowIndices[3][6] = 45;
        rowIndices[3][7] = 46;
        rowIndices[3][8] = 47;
        rowIndices[4][0] = 30;
        rowIndices[4][1] = 31;
        rowIndices[4][2] = 32;
        rowIndices[4][3] = 39;
        rowIndices[4][4] = 40;
        rowIndices[4][5] = 41;
        rowIndices[4][6] = 48;
        rowIndices[4][7] = 49;
        rowIndices[4][8] = 50;
        rowIndices[5][0] = 33;
        rowIndices[5][1] = 34;
        rowIndices[5][2] = 35;
        rowIndices[5][3] = 42;
        rowIndices[5][4] = 43;
        rowIndices[5][5] = 44;
        rowIndices[5][6] = 51;
        rowIndices[5][7] = 52;
        rowIndices[5][8] = 53;
        rowIndices[6][0] = 54;
        rowIndices[6][1] = 55;
        rowIndices[6][2] = 56;
        rowIndices[6][3] = 63;
        rowIndices[6][4] = 64;
        rowIndices[6][5] = 65;
        rowIndices[6][6] = 72;
        rowIndices[6][7] = 73;
        rowIndices[6][8] = 74;
        rowIndices[7][0] = 57;
        rowIndices[7][1] = 58;
        rowIndices[7][2] = 59;
        rowIndices[7][3] = 66;
        rowIndices[7][4] = 67;
        rowIndices[7][5] = 68;
        rowIndices[7][6] = 75;
        rowIndices[7][7] = 76;
        rowIndices[7][8] = 77;
        rowIndices[8][0] = 60;
        rowIndices[8][1] = 61;
        rowIndices[8][2] = 62;
        rowIndices[8][3] = 69;
        rowIndices[8][4] = 70;
        rowIndices[8][5] = 71;
        rowIndices[8][6] = 78;
        rowIndices[8][7] = 79;
        rowIndices[8][8] = 80;

    } // end initializeIndices


    //-------------------------------------------------------------------
    // Method Name: isAreareducible
    //
    // Given an area (0-8) in the already-specified 'theArray', check
    //   to see if there are possibilities that can be removed.
    //-------------------------------------------------------------------
    private boolean isAreaNakedreducible(int which, String strStrip) {

        JLabel jl;
        String strChoices;
        String aChar;            // A temporary char
        int theIndex;

        // For each square in the area,
        for (int i = 0; i < 9; i++) {
            theIndex = theArray[which][i];
            if (theIndex == intNakedIndex1) continue; // Only considering OTHER squares.
            if (theIndex == intNakedIndex2) continue; // Only considering OTHER squares.
            if (theIndex == intNakedIndex3) continue; // Only considering OTHER squares.
            jl = squares.elementAt(theIndex);
            if (!jl.getText().trim().equals("")) continue; // Only looking at unsolved squares
            strChoices = jl.getToolTipText().trim();

            //System.out.println("  Examining [" + strChoices + "] on index " + theIndex);
            // For each character in the strChoices -
            for (int j = 0; j < strChoices.length(); j++) {
                aChar = strChoices.substring(j, j + 1);
                if (strStrip.contains(aChar)) {
                    //System.out.println("    strStrip: " + strStrip + " contains " + aChar);
                    return true; // only need one
                }
            } // end for j - each char of strChoices
        } // end for i
        //System.out.println("Returning false from isAreaNakedReducible");
        return false;
    } // end isAreaNakedReducible


    // Given an X as implied by the supplied rows, columns and value,
    //   the return value will indicate if the value is anywhere
    //   else in the two columns.
    private boolean isAreaXreducible(int row1, int row2, int col1, int col2, String theVal) {
        String theValues;

        for (int i = 0; i < 9; i++) { // Check Column 1
            if (i == row1) continue;
            if (i == row2) continue;
            theValues = squares.elementAt(colIndices[col1][i]).getToolTipText().trim();
            if (theValues.length() == 1) continue;
            if (theValues.contains(theVal)) return true;
        } // end for

        for (int i = 0; i < 9; i++) { // Check Column 2
            if (i == row1) continue;
            if (i == row2) continue;
            theValues = squares.elementAt(colIndices[col2][i]).getToolTipText().trim();
            if (theValues.length() == 1) continue;
            if (theValues.contains(theVal)) return true;
        } // end for

        return false;
    } // end isAreaXreducible


    //--------------------------------------------------------------------------
    // Method Name: loadLast
    //
    // Loads in the last matrix that was evaluated, if it was possible
    //   to save it upon definition and if it is still there.
    //--------------------------------------------------------------------------
    public void loadLast() {
        loadFile("last.txt");
    } // end loadLast


    //--------------------------------------------------------------------------
    // Method Name: loadFile
    //
    // Loads in the specified file.  Full file/path should be provided.
    //--------------------------------------------------------------------------
    public void loadFile(String filename) {
        clear();
        intState = DEFINING;
        jbStart.setText("Start");
        String theString = "";

        int theValue;
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            theString = in.readLine();
            in.close();

            if (theString.length() > 81) theString = theString.substring(0, 81);
            for (int i = 0; i < theString.length(); i++) {
                theValue = Integer.parseInt(theString.substring(i, i + 1));
                if (theValue != 0) define(i, theValue);
            } // end for i
            //System.out.println(str);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } // end try/catch
    } // end loadFile


    // These settings need to be done in multiple places
    private void resetReductionVars() {
        pendingReductions = false;

        intNakedIndex1 = -1;
        intNakedIndex2 = -1;
        intNakedIndex3 = -1;

    } // end resetStateVars


    // Resweep the vector and update the tool tip text for every square
    //   to show the remaining values that are not in any of the
    //   intersects.  Unlike 'sweep', do this by removing new intersects
    //   from the possibilities rather than starting from no possibilities
    //   and only adding non-intersects.  This is to allow the more
    //   complex reducing methodologies to have a cumulative effect and
    //   not undo them with each sweep.
    private void resweep() {
        JLabel jl;
        String theIntersects;
        String strChoices;
        String strFewerChoices;
        String aChar;

        // First, look to see if a value for the square has been set and if so,
        //   reduce its choices to only that value and the option to clear it.
        for (int i = 0; i < (9 * 9); i++) {
            jl = squares.elementAt(i);

            // If there is a value selected for the square
            if (!jl.getText().trim().equals("")) {
                // Original definition tool tips were set at the start; do not change.
                if (jl.getForeground() != Color.black) { // Not an original definition
                    // So, set his choices to either clear, or remain as-is.
                    jl.setToolTipText(" " + jl.getText());
                } // end if
                continue;
            } // end if

            theIntersects = getIntersects(i);
            strChoices = jl.getToolTipText().trim();
            strFewerChoices = " ";

            // Examine the choices that were available after the last
            //   resweep and compare to (possibly new) intersects.
            for (int j = 0; j < strChoices.length(); j++) {
                aChar = strChoices.substring(j, j + 1);
                // If this possibility is now an intersect, do not keep it.
                if (theIntersects.contains(aChar)) continue;
                strFewerChoices += aChar;
            } // end for i - each char of strChoices

            jl.setToolTipText(strFewerChoices);
        } // end for
    } // end resweep


    // The pendingReductions variable is not so tightly held that we
    //   need a 'set' and a 'get' for it.  This method is here because
    //   we may need to know (later) if it had ever been true.
    private void setPendingReductions(boolean b) {
        pendingReductions = b;
        if (b) hadPendingReductions = true;
    } // end setPendingReductions


    public void setTheArray(int intAreaType) {
        switch (intAreaType) {
            case BOX:
            case BOXRO:
            case BOXCO:
                theArray = boxIndices;
                break;
            case COL:
            case COBOX:
                theArray = colIndices;
                break;
            case ROW:
            case ROBOX:
                theArray = rowIndices;
                break;
            default:
                theArray = null;
        } // end switch
    } // end setTheArray


    //------------------------------------------------------------------
    // Method Name: showMessage
    //

    /**
     * This method is called to show the user an informational message.
     * It will display a dialog with the string parameter as its message,
     * then allow the app to continue when the user presses OK.
     */
    //------------------------------------------------------------------
    public static void showMessage(String s) {
        JOptionPane.showMessageDialog(theFrame, s, "Important Information",
                JOptionPane.INFORMATION_MESSAGE);
    } // end showMessage


    // Given a square identified by its index into the squares
    // vector, highlight the correct row, column, and box.
    public void showIntersects(int index) {
        int theRow = getPosition(ROW, index);
        int theCol = getPosition(COL, index);
        int theBox = getPosition(BOX, index);

        theHighlightBackground = Color.green;
        highlightOn(ROW, theRow);
        highlightOn(COL, theCol);
        highlightOn(BOX, theBox);
    } // end showIntersects


    //-------------------------------------------------------------------
    // Method Name: stripNaked
    //
    // This method supports both Naked Twins and Triplets.
    // do not call unless pendingReductions
    //
    //   Given an area to work in and some values to reject, remove those
    //     possibilities and return.
    //-------------------------------------------------------------------
    private void stripNaked(int which, String strStrip) {

        JLabel jl;
        String strChoices;
        String strFewerChoices;
        String aChar;            // A temporary char
        int theIndex;

        // For each square in the area,
        for (int i = 0; i < 9; i++) {
            theIndex = theArray[which][i];
            if (theIndex == intNakedIndex1) continue; // Only stripping OTHER squares.
            if (theIndex == intNakedIndex2) continue; // Only stripping OTHER squares.
            if (theIndex == intNakedIndex3) continue; // Only stripping OTHER squares.
            jl = squares.elementAt(theIndex);
            if (!jl.getText().trim().equals("")) continue; // Only looking at unsolved squares
            strChoices = jl.getToolTipText().trim();
            strFewerChoices = " "; // First choice is 'empty'

            // Develop the strFewerChoices string -
            for (int j = 0; j < strChoices.length(); j++) {
                aChar = strChoices.substring(j, j + 1);
                // If this possibility is one of the 'strip' values, do not keep it.
                if (strStrip.contains(aChar)) continue;
                strFewerChoices += aChar;
            } // end for i - each char of strChoices

            jl.setToolTipText(strFewerChoices);

        } // end for i
    } // end stripNaked


    // Given an X as implied by the supplied rows, columns and value,
    //   remove other instances of the value from the columns.
    private void stripX(int row1, int row2, int col1, int col2, String theVal) {
        String theValues;
        int theIndex;

        //System.out.print("In stripX for val: [" + theVal + "] on rows: ");
        //System.out.print(row1 + ", " + row2);
        //System.out.println(" & cols: " + col1 + ", " + col2);

        for (int i = 0; i < 9; i++) { // Check Column 1
            if (i == row1) continue;
            if (i == row2) continue;
            theValues = squares.elementAt(colIndices[col1][i]).getToolTipText().trim();
            if (theValues.length() == 1) continue;

            if (theValues.contains(theVal)) {
                theIndex = theValues.indexOf(theVal);
                theValues = theValues.substring(0, theIndex) + theValues.substring(theIndex + 1);
                squares.elementAt(colIndices[col1][i]).setToolTipText(" " + theValues);
            } // end if
        } // end for

        for (int i = 0; i < 9; i++) { // Check Column 2
            if (i == row1) continue;
            if (i == row2) continue;
            theValues = squares.elementAt(colIndices[col2][i]).getToolTipText().trim();
            if (theValues.length() == 1) continue;

            if (theValues.contains(theVal)) {
                theIndex = theValues.indexOf(theVal);
                theValues = theValues.substring(0, theIndex) + theValues.substring(theIndex + 1);
                squares.elementAt(colIndices[col2][i]).setToolTipText(" " + theValues);
            } // end if
        } // end for
    } // end stripX


    //---------------------------------------------------------------------
    // Method Name: sweep
    //
    // Sweep the vector and update the tool tip text for every square to
    //   show the remaining values that are not in any of the intersects.
    //   This method is usually only called once, after the user has pressed
    //   'Start' and the definition has passed the 'duplicates' check.
    //---------------------------------------------------------------------
    public void sweep() {
        JLabel jl;
        String strChoices;
        String theIntersects;

        for (int i = 0; i < (9 * 9); i++) {
            jl = squares.elementAt(i);
            referenceArray[i] = jl.getText();

            // If the text is not empty, set the tool tip text to that text, only.
            if (!jl.getText().trim().equals("")) {
                jl.setToolTipText(jl.getText());
                continue;
            } // end if

            theIntersects = getIntersects(i);
            strChoices = " ";
            if (theIntersects.indexOf('1') < 0) strChoices += "1";
            if (theIntersects.indexOf('2') < 0) strChoices += "2";
            if (theIntersects.indexOf('3') < 0) strChoices += "3";
            if (theIntersects.indexOf('4') < 0) strChoices += "4";
            if (theIntersects.indexOf('5') < 0) strChoices += "5";
            if (theIntersects.indexOf('6') < 0) strChoices += "6";
            if (theIntersects.indexOf('7') < 0) strChoices += "7";
            if (theIntersects.indexOf('8') < 0) strChoices += "8";
            if (theIntersects.indexOf('9') < 0) strChoices += "9";

            jl.setToolTipText(strChoices);
        } // end for
    } // end sweep


    private void setAutomatic(boolean b) {

        if (!b) {
            blnAutoSolve = false;
            jbAuto.setText("Auto On");

            // Highlighting is off already anyway, but this method will
            //   also clear controlling flags.
            handleMenuBar("Remove Highlighting");
        } else {
            blnAutoSolve = true;
            jbAuto.setText("Auto Off");

            // Highlighting is off already anyway, but this method will
            //   also clear controlling flags.
            handleMenuBar("Remove Highlighting");

            handleNextClick();
        } // end if
    } // end setAutomatic

    public void showMatrix() {

    }

    public static void main(String args[]) {
        JMenu jmFile = new JMenu("File");
        jmFile.add(new JMenuItem("Open"));
        jmFile.add(new JMenuItem("Exit"));

        JMenu jmView = new JMenu("View");
        jmView.add(new JMenuItem("Remove Highlighting"));

        JMenu jmHelp = new JMenu("Help");
        jmHelp.add(new JMenuItem("Getting Started"));

        JMenuBar jmb = new JMenuBar();
        jmb.add(jmFile);
        jmb.add(jmView);
        jmb.add(Box.createHorizontalGlue());
        jmb.add(jmHelp);

        theMatrix = new Laffingas();
        theFrame = new JFrame("Laffingas");
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
} // end class
