package com.example.chessserver3.model.computer;

import java.util.Comparator;
import java.util.Objects;

public class BoardComparator implements Comparator<AnalysisBoard> {

    public static final BoardComparator comparator = new BoardComparator();

    @Override
    public int compare(AnalysisBoard o1, AnalysisBoard o2) {
        if (Objects.equals(o1.getShortFen().getBoardCode(), o2.getShortFen().getBoardCode())) {
            return 0;
        }
        if (o1.getAdvantage() < o2.getAdvantage()) {
            return -1;
        } else {
            return 1;
        }
    }
}
