package com.example.chessserver3.model.computer;

import java.util.Comparator;

public class AdvantageComparator implements Comparator<AnalysisBoard> {

    public static final AdvantageComparator comparator = new AdvantageComparator();

    @Override
    public int compare(AnalysisBoard a1, AnalysisBoard a2) {
        return Integer.compare(a1.getAdvantage(), a2.getAdvantage());
    }
}
