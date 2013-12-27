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

public class Laffingas extends JPanel implements Values, ActionListener {
    private static JFrame theFrame;
    private Vector<JLabel> squares;
    private int intState;
    private Color theOriginalBackground;
    private Color theHighlightBackground;
    private JButton jbStart;   // After Start, text changes from 'Start' to 'Next'
    private JButton jbSave;    // After Start, this one should go away.
    private JButton jbReset;   // 'Reset' becomes visible after Start
    private JButton jbAuto;    // 'Auto On' becomes visible after Start
    private int intNakedIndex1;
    private int intNakedIndex2;
    private int intNakedIndex3;
    private String referenceArray[]; // The square texts after the last 'Next'.
    private boolean pendingAssignment;
    private boolean pendingReductions;
    private boolean blnAutoSolve;
    private boolean hadPendingReductions;  // Need to know if we ever did.
    private String definitionName;
    private int autoDelay = 700;  // The default value.
    private boolean debug = false;

    //------------------------------------------------------------------
    // These index assignments could also be done via calculations
    //   but this 'brute force' method is more readable / understandable.
    //   The values were obtained by printing out a 9x9 matrix of squareFs
    //   and then filling in the index values (top left = 0) in the same
    //   order that they are created in the constructor.  Since that
    //   sequence is by Boxes rather than rows or columns, the first
    //   group is very straightforward; just 0-80.  The next two groups
    //   are a bit more jumbled as a result of extracting the columns
    //   and rows from each 'box' in the order that it first appeared in
    //   the 'squares' array.
    //------------------------------------------------------------------
    private final int[][] boxIndices = {  { 0, 1, 2, 3, 4, 5, 6, 7, 8},
            { 9,10,11,12,13,14,15,16,17}, {18,19,20,21,22,23,24,25,26},
            {27,28,29,30,31,32,33,34,35}, {36,37,38,39,40,41,42,43,44},
            {45,46,47,48,49,50,51,52,53}, {54,55,56,57,58,59,60,61,62},
            {63,64,65,66,67,68,69,70,71}, {72,73,74,75,76,77,78,79,80} };
    private final int[][] colIndices = {  { 0, 3, 6,27,30,33,54,57,60},
            { 1, 4, 7,28,31,34,55,58,61}, { 2, 5, 8,29,32,35,56,59,62},
            { 9,12,15,36,39,42,63,66,69}, {10,13,16,37,40,43,64,67,70},
            {11,14,17,38,41,44,65,68,71}, {18,21,24,45,48,51,72,75,78},
            {19,22,25,46,49,52,73,76,79}, {20,23,26,47,50,53,74,77,80} };
    private final int[][] rowIndices = {  { 0, 1, 2, 9,10,11,18,19,20},
            { 3, 4, 5,12,13,14,21,22,23}, { 6, 7, 8,15,16,17,24,25,26},
            {27,28,29,36,37,38,45,46,47}, {30,31,32,39,40,41,48,49,50},
            {33,34,35,42,43,44,51,52,53}, {54,55,56,63,64,65,72,73,74},
            {57,58,59,66,67,68,75,76,77}, {60,61,62,69,70,71,78,79,80} };

    private int theArray[][];   // Will 'point' to one of the other three after 'setTheArray'.

    public Laffingas(JFrame theFrame) {
        super(new BorderLayout());

        Laffingas.theFrame = theFrame;

        // Initializations
        intState = DEFINING;
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
        jbReset.setVisible(false);
        jbReset.addActionListener(this);

        southPanel.add(jbSave);
        southPanel.add(jbStart);
        southPanel.add(jbAuto);
        southPanel.add(jbNew);
        southPanel.add(jbReset);

        add(centerPanel, "Center");
        add(southPanel, "South");
    } // end constructor


    // Handles ALL button clicks - from the South panel as well as the startup view.
    public void actionPerformed(ActionEvent e) {
        JButton jb = (JButton) e.getSource();

        // Clear previous highlighting, if any.
        highlightOff();

        // Start
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

            // Allow 'Auto' and 'Reset' to show now.
            jbAuto.setVisible(true);
            jbReset.setVisible(true);

            // Do not allow saving of (partial) solutions; only puzzle definitions.
            jbSave.setVisible(false);

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

        //  Quit
        if (e.getActionCommand().equals("Quit")) {
            System.exit(0);
        } // end if

        // Auto settings
        if (e.getActionCommand().equals("Auto On")) setAutomatic(true);
        if (e.getActionCommand().equals("Auto Off")) setAutomatic(false);

        // Next
        if (e.getActionCommand().equals("Next")) handleNextClick();

        // Reset
        if (e.getSource() == jbReset) {
            if(blnAutoSolve) {
                setAutomatic(false);
                try {
                    Thread.sleep(autoDelay);
                } catch (InterruptedException ie) {
                    System.out.println(ie.getMessage());
                }
            }
            jbAuto.setVisible(false);
            jbReset.setVisible(false);
            jbSave.setVisible(true);
            loadLast();
        } // end if

        // Load the previous.. (same as 'Reset')
        if (e.getActionCommand().equals(lastButton)) {
            loadLast();
            jbAuto.setVisible(false);
            jbReset.setVisible(false);
            jbSave.setVisible(true);
            mi_view_1.setEnabled(true);
            theFrame.setContentPane(this);
        } // end if

        // Save
        if (e.getActionCommand().equals("Save")) {
            try {
                //Create a file chooser
                final JFileChooser fc = new JFileChooser("data");
                if(definitionName != null && !definitionName.trim().equals("")) {
                    fc.setSelectedFile(new File(definitionName));
                }

                int returnVal = fc.showSaveDialog(theFrame);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    String newName = file.getName();

                    // Flag to indicate whether of not we should issue a warning to the
                    // user, in the case that the 'Save' will overwrite an existing file.
                    boolean warnOverwrite = true;

                    // Are we re-saving the same file that we loaded?
                    // If so, no overwrite warning will be given.
                    if(definitionName != null && !definitionName.isEmpty()) {
                        if(newName.equals(definitionName)) {
                            warnOverwrite = false;
                        }
                    }

                    // This can become false, if there is an overwrite warning
                    // and it convinces the user to stop what they are doing.
                    boolean doTheSave = true;

                    // The two cases where the user should see an overwrite warning:
                    // 1.  No file was loaded; they just somehow chose a name that already
                    //      exists (the UI actually makes this easy to do).
                    // 2.  A file was loaded, but they are now saving with a different name
                    //      and the name they have changed to is a pre-existing file.
                    // Both these cases boil down to the fact that there is a pre-existing file,
                    // and the 'warnOverwrite' flag is still true.
                    if(warnOverwrite && file.exists()) {
                        String message = "A file with the chosen name already exists " +
                                "in this location." + System.lineSeparator() +
                                "Are you sure you want to overwrite it?";
                        String title = "Save / Overwrite";
                        int reply = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
                        if (reply != JOptionPane.YES_OPTION) {
                            doTheSave = false;
                            System.out.println("Save/overwrite operation cancelled by user.");
                        }
                    }

                    if(doTheSave) {
                        System.out.println("Saving: " + file.getName() + ".");
                        BufferedWriter out = new BufferedWriter(new FileWriter(file.getAbsolutePath()));
                        out.write(stringifyMatrix());
                        out.close();
                        definitionName = newName;
                        theFrame.setTitle(baseTitle + " - " + definitionName);
                    }

                } else {
                    System.out.println("Save operation cancelled by user.");
                }
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            } // end try/catch
        }

        // The 'New' button (on the South panel)
        if (e.getActionCommand().equals("New")) {
            intState = DEFINING;
            definitionName = "";
            theFrame.setTitle(baseTitle);
            jbStart.setText("Start");
            jbAuto.setVisible(false);
            jbReset.setVisible(false);
            jbSave.setVisible(true);
            clear();
            for (JLabel jl : squares) {
                jl.setToolTipText(null);
            }
        } // end if

        // The 'Define New' button, from the startup panel (almost same as above)
        if (e.getActionCommand().equals(defineButton)) {
            mi_view_1.setEnabled(true);
            theFrame.setContentPane(this);
            intState = DEFINING;
            jbStart.setText("Start");
            jbAuto.setVisible(false);
            jbReset.setVisible(false);
            jbSave.setVisible(true);
            clear();
            for (JLabel jl : squares) {
                jl.setToolTipText(null);
            }
        }

        // The 'Load a saved..' button, from the startup panel
        if (e.getActionCommand().equals(loadButton)) {
            handleMenuBar("Open...");
        }

        if (e.getActionCommand().equals(helpButton)) {
            try {
                Runtime.getRuntime().exec("hh help" + File.separatorChar + "ukodus.chm");
            } catch(IOException ioe) {
                System.out.println(ioe.getMessage());
            } // end try/catch
        }

        //System.out.println("Handled " + e.getActionCommand());
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
    // This method compares the current values (just the values, not the
    //   possibilities) in the matrix with the ones that were present during
    //   the previous pass.  The very first time we come here, we will fill
    //   our empty referenceArray with the initial puzzle values.  On
    //   subsequent visits, the array is updated to capture any/all changes.
    //
    // On those subsequent visits, if any change at all (to a value in the
    //   matrix) has been made (by either the user or the program), any pending
    //   reduction is cleared.  This will prevent 'crossover' from a first pass
    //   of one reduction method to the second pass of a different one.
    //
    // Then, if it turns out that something has changed and it includes a
    //   previously set value having now been cleared by the user, it will
    //   remove all reductions other than the intersects, for ALL squares.
    //   This is because one or more of those reductions may have been made due
    //   to the now-missing value.  But that alone may not fix things, if the
    //   cleared square gets a different value when it eventually comes back,
    //   because while it was present it may have led to an incorrect value
    //   assignment in another square, and we are not clearing values at this
    //   point; only possibilities.  We can recognize that this may have
    //   occurred and warn the user about it (including suggestions on what
    //   they can do to fix it) but no further programmatic action is taken and
    //   so the overall solution may go off the rails.
    //----------------------------------------------------------------------------
    private void checkReferences() {
        boolean changeWasMade = false;
        String strCurrentVal;
        JLabel jl;

        // A non-null value here is relevant even if the message is never shown.
        // Review the code at the end of this method, if you don't believe me.
        String clearedValMsg = null;

        for (int i = 0; i < 81; i++) {
            jl = squares.elementAt(i);
            strCurrentVal = jl.getText();
            if (!strCurrentVal.equals(referenceArray[i])) {
                referenceArray[i] = strCurrentVal;
                changeWasMade = true;
                // The 'null' test below allows us to continue updating reference values while
                // only showing the complaint one time.  We should allow the loop to fully
                // execute and not bail out early as soon as we see that the user has messed
                // with the values, because we need to continue updating ALL references even
                // if the puzzle is now hosed (it may not be, or it may be fixable, but after
                // every time this method is called, the referenceArray should be correct/complete).
                if (clearedValMsg == null) {
                    if (strCurrentVal.equals(" ")) {  // It previously held a value; now, does not.
                        clearedValMsg = "One or more previously set squares has been cleared, so";
                        clearedValMsg += "\n the decisions that led up to this point may be heading";
                        clearedValMsg += "\n to the wrong overall solution.  You can ignore this and";
                        clearedValMsg += "\n continue by clicking on 'Next', or to fix it you can either";
                        clearedValMsg += "\n restore the values or start over by clicking on 'Reset'";
                        if (hadPendingReductions) {
                            // We only need to issue this warning in the case that there have
                            // ever been any reduction methodologies used to this point in the
                            // solution; otherwise the sweep method will restore possibilities
                            // that were removed due to assignments that are no longer present.
                            showMessage(clearedValMsg);
                        } // end if
                    } // end if a square was un-set
                } // end if the message text had not yet been set
            } // end if
        } // end for i - each square

        if (changeWasMade) resetReductionVars();
        if (clearedValMsg != null) sweep();
    } // end checkReferences


    //-------------------------------------------------------------------------
    // Method Name: findNext
    //
    //-------------------------------------------------------------------------
    public void findNext() {
        //-----------------------------------------------------------------------
        // This method calls all other methods that can identify the next square
        //   of interest.  The 'find' methods all work in a two-pass fashion.

        // In the first pass, if the objective is found then controlling flags are set
        // such that the second pass will occur rather than going on to the next
        // 'find' method.  Then it will highlight the solution area and set the
        // content for the 'Explain' menu item.  Control then returns from this method
        // back to the user so they can examine (and potentially act on) the results.
        //
        // The second pass does not come until 'Next' is clicked again, but if in the
        // meanwhile the user
        // has explicitly turned off the highlight or has made a change to a square,
        // the second pass is cancelled, all intersects are recalculated and tool tips
        // updated, and 'Next' will
        // function as though it is conducting an entirely new search.

        // The second pass will find the same area that the first pass highlighted and
        // will make the assignment or reduction(s) and return control to the user.
        // Highlighting will already be off by virtue of the fact that it goes off with
        //   every click of 'Next'.

        // This is our first method of attack; if we have a 'hit' here then
        //   it is the easiest solution for the user to see.
        boolean b = findHighlander();

        // If that didn't work, we become Tonto -
        if (!b) b = findLoneRanger(BOX);
        if (!b) b = findLoneRanger(COL);
        if (!b) b = findLoneRanger(ROW);

        // The two methods above can result in a simple assignment.
        // All other methods below involve the reduction of the remaining
        //   possibilities for the squares.  They do not identify the next
        //   solution square, but may make it possible to do so, after the
        //   next resweep.


        // For development testing of a reduction methodology, move it to the top (here), to ensure
        // that some other method does not change the conditions that we want to find.

        // Naked pair
        if (!b) b = findNakedPair(BOX);
        if (!b) b = findNakedPair(COL);
        if (!b) b = findNakedPair(ROW);

        // Hidden pair
        if (!b) b = findHiddenPair(BOX);
        if (!b) b = findHiddenPair(COL);
        if (!b) b = findHiddenPair(ROW);

        // X-wing
        if (!b) b = findX(ROW);
        if (!b) b = findX(COL);

        // Box interaction
        if (!b) b = findInsideBox(ROW);
        if (!b) b = findInsideBox(COL);
//    if(!b) b = findOutsideBox(ROW);
//    if(!b) b = findOutsideBox(COL);

        if (!b) {
            showMessage("Cannot find another square to assign or reduce logically; " +
                    System.lineSeparator() +
                    "you can try again after entering one or more on your own.");
            setAutomatic(false);
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
                            pendingReductions = false;
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
        for (int index = 0; index < (9 * 9); index++) {
            jl = squares.elementAt(index);
            if (!jl.getText().trim().equals("")) continue;
            if (jl.getToolTipText().trim().length() == 1) {
                // There is only one possibility.
                int theRow = getPosition(ROW, index);
                int theCol = getPosition(COL, index);
                int theBox = getPosition(BOX, index);
                String theChar = jl.getToolTipText().trim();
                if (pendingAssignment) {
                    setPendingAssignment(false);
                    jl.setForeground(Color.green.darker());
                    jl.setText(jl.getToolTipText().trim());
                } else {
                    String theMessage = "Highlander value: " + theChar;
                    theMessage += " found in Row " + String.valueOf(theRow+1);
                    theMessage += ", Col " + String.valueOf(theCol+1);
                    theMessage += ", Box " + String.valueOf(theBox+1);
                    System.out.println(theMessage);

                    // Highlight the correct row, column, and box as intersects of this square
                    theHighlightBackground = Color.green;
                    highlightOn(ROW, theRow);
                    highlightOn(COL, theCol);
                    highlightOn(BOX, theBox);

                    jl.setBorder(BorderFactory.createLineBorder(Color.red, 2));
                    jl.setBackground(Color.white);
                    setPendingAssignment(true);
                } // end if

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
                        pendingReductions = false;
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
                    if (pendingAssignment) {
                        jl.setForeground(Color.magenta.darker());
                        jl.setText(theChar);
                        setPendingAssignment(false);
                    } else {
                        String theMessage = "Lone Ranger value: " + theChar;
                        theMessage += " found in " + getAreaTypeString(intAreaType);
                        theMessage += " " + String.valueOf(getPosition(intAreaType, i)+1);
                        System.out.println(theMessage);
                        theHighlightBackground = Color.magenta;
                        highlightOn(intAreaType, theArea);
                        jl.setBorder(BorderFactory.createLineBorder(Color.red, 2));
                        jl.setBackground(Color.white);
                        setPendingAssignment(true);
                    } // end if
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
                        } // end if
                        return true;
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
    public boolean findX(int areaType) {
        int area1, area2;
        int alt1, alt2;
        String strValues;
        String aValue;
        int theIndex;
        int aCount;
        String allValues = "123456789";
        int counterIndex;
        int theCounts[][] = new int[9][9];

        String areaName = getAreaTypeString(areaType);

        int altAreaType;
        int altArray[][];
        String altName;

        debug = false;
        String dMessage;

        if(areaType == ROW) {
            theArray = rowIndices;
            altAreaType = COL;
            altArray = colIndices;
            altName = getAreaTypeString(COL);
        }
        else {
            theArray = colIndices;
            altAreaType = ROW;
            altArray = rowIndices;
            altName = getAreaTypeString(ROW);
        }

        if (theArray == null) return false;

        //--------------------------------------------------------------
        // Scan the matrix and get a count of ALL possibilities in each area.
        //   Place the results into 'theCounts[][]', where the
        //   first dimension is the area, the second is the value
        //   that is being counted.
        //--------------------------------------------------------------
        for (int i = 0; i < 9; i++) { // for each area
            strValues = "";
            for (int j = 0; j < 9; j++) { // for each square in the area
                theIndex = theArray[i][j];

                // If the square is not empty -
                if (!squares.elementAt(theIndex).getText().trim().equals("")) continue;

                // Get all the possible answers for the entire area
                strValues += squares.elementAt(theIndex).getToolTipText().trim();
            } // end for j

            dMessage = "The possibilities for all open squares on " + areaName;
            dMessage += " " + String.valueOf(i+1) + " are " + strValues;
            loggit(dMessage);

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
                //  System.out.print("In area " + String.valueOf(i+1));
                //  System.out.println(" possible value " + aValue + " found " + aCount + " times");
            } // end for k
        } // end for i
        //--------------------------------------------------------------

        // Now scan 'theCounts' for values of 2.  If we find two areas for
        //   the same value, we have our areas.
        for (int i = 0; i < 8; i++) {  // For each area (except the last)
            for (int j = 0; j < 9; j++) { // For each value
                area1 = -1;
                area2 = -1;
                if (theCounts[i][j] != 2) continue;

                // The count is 2; we need to look further...
                for (int k = i + 1; k < 9; k++) { // For each subsequent area
                    if (theCounts[k][j] == 2) {
                        area1 = i;
                        area2 = k;
                    } // end if
                } // end for

                if (area2 == -1) continue; // We found a first area but not a second.

                loggit("Found a possible X on " + areaName +
                        "s " + String.valueOf(area1+1) + ", " + String.valueOf(area2+1) + " for " +
                        "value" + " " + String.valueOf(j+1));

                // Now look to see that the values appear in the same columns.
                alt1 = -1;
                alt2 = -1;

                // For area1 we can just set the altAreas; no checking required.
                for (int k = 0; k < 9; k++) { // For each square in area1
                    if (squares.elementAt(theArray[area1][k]).getToolTipText().contains(String.valueOf(j + 1))) {
                        if (alt1 == -1) {
                            alt1 = k;
                            //System.out.print("  First " + altName + ": " + String.valueOf(k+1));
                            continue;
                        } // end if
                        if (alt2 == -1) {
                            alt2 = k;
                            //System.out.println ("\tSecond " + altName + ": " + String.valueOf(k+1));
                            break; // This is the 2nd on this row and there are only 2.
                        } // end if
                    } // end if the square possibilities contain the value
                } // end for k - each square

                // For area2 we need to check that the value appears in the same altAreas.
                //   If it does not, we keep looking.
                if (!squares.elementAt(theArray[area2][alt1]).getToolTipText().contains(String.valueOf(j + 1))) {
                    dMessage = "   but the value is not present at " + areaName +
                            " " + (area2+1) + " " + altName + " " + (alt1+1);
                    loggit(dMessage);
                    continue; // on to the next value
                } // end if the square possibilities do not contain the value

                if (!squares.elementAt(theArray[area2][alt2]).getToolTipText().contains(String.valueOf(j + 1))) {
                    dMessage = "   but the value is not present at " + areaName +
                            " " + (area2+1) + " " + altName + " " + (alt2+1);
                    loggit(dMessage);
                    continue; // on to the next value
                } // end if the square possibilities do not contain the value

                // If we made it here, we have an X.

                // But it does us no good if it is not reducible -
                String theValue = String.valueOf(j + 1);
                if (!isAreaXreducible(areaType, area1, area2, alt1, alt2, theValue)) {
                    //System.out.println("\t but not reducible; continuing on..");
                    continue;
                }

                // So - if we're here, we have a reducible X
                if (!pendingReductions) { // Highlight the X and return true
                    setPendingReductions(true);
                    // System.out.println("  Reducible: " + pendingXReductions);
                    System.out.print("Found an X for value [" + theValue + "] on ");
                    System.out.print(areaName);
                    System.out.print("s " +
                            String.valueOf(area1 + 1) + ", " + String.valueOf(area2 + 1));
                    System.out.print(" & " + altName);
                    System.out.println("s " + String.valueOf(alt1 + 1) + ", " + String.valueOf(alt2 + 1));

                    JLabel jl;

                    theHighlightBackground = Color.yellow.darker();
                    highlightOn(altAreaType, alt1);
                    highlightOn(altAreaType, alt2);

                    theHighlightBackground = Color.yellow.brighter();
                    // Top Left
                    jl = squares.elementAt(altArray[alt1][area1]);
                    jl.setBackground(theHighlightBackground);

                    // Bottom Right
                    jl = squares.elementAt(altArray[alt2][area2]);
                    jl.setBackground(theHighlightBackground);

                    // Bottom Left
                    jl = squares.elementAt(altArray[alt1][area2]);
                    jl.setBackground(theHighlightBackground);

                    // Top Right
                    jl = squares.elementAt(altArray[alt2][area1]);
                    jl.setBackground(theHighlightBackground);
                } else { // There are pending reductions
                    stripX(areaType, area1, area2, alt1, alt2, theValue);
                    resetReductionVars();
                } // end if pendingReductions or not
                return true;
            } // end for j - each value
        } // end for i - each row

        return false;
    } // end findX


    //-----------------------------------------------------------------------
    // Method Name: getAreaTypeString
    //
    // A convenience method to convert the int value
    //   for area type to meaningful text.
    //-----------------------------------------------------------------------
    public String getAreaTypeString(int intAreaType) {
        switch (intAreaType) {
            case BOX:
                return "Box";
            case ROW:
                return "Row";
            case COL:
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


    public void handleMenuBar(String what) {
        if (what.equals("Open...")) {
            // Create a file chooser
            final JFileChooser fc = new JFileChooser("data");

            // Turn off any auto-solution in progress
            setAutomatic(false);

            // A previous solution may have left these 'on'.
            jbAuto.setVisible(false);
            jbReset.setVisible(false);

            int returnVal = fc.showOpenDialog(theFrame);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                mi_view_1.setEnabled(true);
                theFrame.setContentPane(this);
                File file = fc.getSelectedFile();
                definitionName = file.getName();
                System.out.println("Opening: " + definitionName + ".");
                loadFile(file.getAbsolutePath());
                theFrame.setTitle(baseTitle + " - " + definitionName);
                jbSave.setVisible(true);
            } else {
                System.out.println("The 'File/Open' action was cancelled by user.");
            }
        } // end if

        if (what.equals("Exit")) System.exit(0);

        if (what.equals("Remove Highlighting")) {
            highlightOff();
            resetReductionVars();
        } // end if

        if (what.equals("Restart")) {
            setAutomatic(false); // Ensure there is no auto-solution in progress
            mi_view_1.setEnabled(false);
            mi_view_2.setEnabled(false);
            autoDelay = 700;

            theFrame.setContentPane(new InitialInfo(this));
            theFrame.getContentPane().revalidate();
            theFrame.repaint();
            theFrame.setTitle(baseTitle);
        } // end if

        if (what.equals("Documentation")) {
            setAutomatic(false); // Ensure there is no auto-solution in progress
            mi_view_1.setEnabled(false);
            mi_view_2.setEnabled(false);

            try {
                Runtime.getRuntime().exec("hh help" + File.separatorChar + "ukodus.chm");
            } catch(IOException ioe) {
                System.out.println(ioe.getMessage());
            } // end try/catch
        } // end if

        if (what.equals("Set Auto-solution delay...")) {
            try {
                int ans = Integer.parseInt( (String) JOptionPane.showInputDialog(
                    theFrame, "Enter a value (in milliseconds) for the delay",
                    what, JOptionPane.QUESTION_MESSAGE, null, null, autoDelay));
                autoDelay = ans < 1 ? 1:ans;
            } catch(NumberFormatException nfe) {
                System.out.println(nfe.getMessage());
                System.out.println("Value remains unchanged - " + autoDelay + "ms");
            }
        }

        System.out.println("handleMenuBar " + what);

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
        // If so, cancel any in-progress methodology and reset
        //   the reference array.
        checkReferences();

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
                        Thread.sleep(autoDelay); //Thread.sleep(700);
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

        mi_view_2.setEnabled(false);
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

        mi_view_2.setEnabled(true);
    } // end highlightOn


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


    // Given an X as implied by the supplied areas, altAreas and value,
    //   the returned boolean will indicate if the value is anywhere
    //   else in the two alternate areas.  An area type is either a row or a column,
    //   and an altArea type is also a row or column, but not the same as the area type.
    private boolean isAreaXreducible(int areaType, int area1, int area2, int altArea1, int altArea2, String theVal) {
        String theValues;

        int [][] searchArray;
        if(areaType == ROW) {
            searchArray = colIndices;
        }
        else {
            searchArray = rowIndices;
        }

        for (int i = 0; i < 9; i++) { // Check altArea1
            if (i == area1) continue; // The square at [altArea1][area1] is already known to contain theVal
            if (i == area2) continue; // The square at [altArea1][area2] is already known to contain theVal
            theValues = squares.elementAt(searchArray[altArea1][i]).getToolTipText().trim();
            if (theValues.contains(theVal)) {
                return true;
            }
        } // end for

        for (int i = 0; i < 9; i++) { // Check altArea2
            if (i == area1) continue;
            if (i == area2) continue;
            theValues = squares.elementAt(searchArray[altArea2][i]).getToolTipText().trim();
            if (theValues.contains(theVal)) {
                return true;
            }
        } // end for

        return false;
    } // end isAreaXreducible


    //--------------------------------------------------------------------------
    // Method Name: loadFile
    //
    // Loads in the specified file.  Full file/path should be provided.
    //--------------------------------------------------------------------------
    public void loadFile(String filename) {
        clear();
        intState = DEFINING;
        jbStart.setText("Start");
        String theString;

        int theValue;
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            theString = in.readLine();
            in.close();

            if (theString.length() > 81) theString = theString.substring(0, 80);
            for (int index = 0; index < theString.length(); index++) {
                // Remember that the substring method uses endIndex-1; it will not
                // throw an exception if the value is one more than the last valid index.
                theValue = Integer.parseInt(theString.substring(index, index + 1));
                if (theValue != 0) {
                    JLabel jl = squares.elementAt(index);
                    jl.setText(String.valueOf(theValue));
                    jl.setToolTipText(String.valueOf(theValue));
                }
            } // end for i
            //System.out.println(str);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } // end try/catch
    } // end loadFile


    //--------------------------------------------------------------------------
    // Method Name: loadLast
    //
    // Loads in the last matrix that was evaluated, if it was possible
    //   to save it upon definition and if it is still there.
    //--------------------------------------------------------------------------
    public void loadLast() {
        loadFile("last.txt");
    } // end loadLast


    // These settings need to be done in multiple places
    private void resetReductionVars() {
        pendingReductions = false;
        pendingAssignment = false;

        intNakedIndex1 = -1;
        intNakedIndex2 = -1;
        intNakedIndex3 = -1;
    } // end resetReductionVars


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
    private void setPendingAssignment(boolean b) {
        pendingAssignment = b;
    } // end setPendingAssignment


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
                theArray = boxIndices;
                break;
            case COL:
                theArray = colIndices;
                break;
            case ROW:
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
    private void stripX(int areaType, int row1, int row2, int col1, int col2, String theVal) {
        String theValues;
        int theIndex;

        //System.out.print("In stripX for val: [" + theVal + "] on rows: ");
        //System.out.print(row1 + ", " + row2);
        //System.out.println(" & cols: " + col1 + ", " + col2);

        int [][] searchArray;
        if(areaType == ROW) {
            searchArray = colIndices;
        } else {
            searchArray = rowIndices;
        }

        for (int i = 0; i < 9; i++) { // Check Column 1
            if (i == row1) continue;
            if (i == row2) continue;
            theValues = squares.elementAt(searchArray[col1][i]).getToolTipText().trim();
            if (theValues.length() == 1) continue;

            if (theValues.contains(theVal)) {
                theIndex = theValues.indexOf(theVal);
                theValues = theValues.substring(0, theIndex) + theValues.substring(theIndex + 1);
                squares.elementAt(searchArray[col1][i]).setToolTipText(" " + theValues);
            } // end if
        } // end for

        for (int i = 0; i < 9; i++) { // Check Column 2
            if (i == row1) continue;
            if (i == row2) continue;
            theValues = squares.elementAt(searchArray[col2][i]).getToolTipText().trim();
            if (theValues.length() == 1) continue;

            if (theValues.contains(theVal)) {
                theIndex = theValues.indexOf(theVal);
                theValues = theValues.substring(0, theIndex) + theValues.substring(theIndex + 1);
                squares.elementAt(searchArray[col2][i]).setToolTipText(" " + theValues);
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

            pendingAssignment = false;
            pendingReductions = false;
            // Since we were on auto before this, the sleep from that thread will expire,
            // we will do one more 'findNext', and thanks to the above settings it will
            // re-highlight the most recent method instead of going on one more step.
        } else {
            blnAutoSolve = true;
            jbAuto.setText("Auto Off");

            handleNextClick();
        } // end if
    } // end setAutomatic

    // A way of sending output to the screen, that can easily be 'silenced'
    // with a change to a global boolean flag.
    private void loggit(String message) {
        if (debug) System.out.println(message);
    }

} // end class
