package com.example.chessserver3.model.board;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TreeView extends JFrame implements ActionListener {

    HashMap<String, JButton> buttons = new HashMap<>();
    JPanel buttonPanel = new JPanel();
    JPanel backPanel = new JPanel();
    JButton backButton = new JButton("back");
    Move move = null;
    Stack<Move> lastMove = new Stack<>();
    Map<String, Move> futures = new HashMap<>();

    public TreeView(Move move) {
        super("tree");
        setSize(800, 800);
        setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        buttonPanel.setLayout(new GridLayout(10, 10));
        this.add(buttonPanel, BorderLayout.CENTER);
        this.add(backPanel, BorderLayout.NORTH);
        this.move = move;
        display();
    }

    public void display() {
        if (move != null) {
            setTitle(move.getMoveString() + "(" + move.getAdvantage() + ") " + (move.isWhite() ? "black to move" : "white to move"));
            backPanel.removeAll();
            if (!lastMove.isEmpty()) {
                backButton = new JButton("go back to " + lastMove.lastElement().getMoveString());
                backButton.addActionListener(this);
                backPanel.add(backButton);
            }
            buttons = new HashMap<>();
            buttonPanel.removeAll();
            futures = move.getFutures().stream().filter(Move::isValid).collect(Collectors.toMap(Move::getMoveCode, Function.identity()));
            for (Move future : futures.values()) {
                JButton futureButton = new JButton(future.getMoveString() + "(" + future.getAdvantage() + ")");
                futureButton.addActionListener(this);
                buttons.put(future.getMoveCode(), futureButton);
                buttonPanel.add(futureButton);
            }
        }
        SwingUtilities.updateComponentTreeUI(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (String moveCode : buttons.keySet()) {
            if (e.getSource().equals(buttons.get(moveCode))) {
                lastMove.push(move);
                move = futures.get(moveCode);
                display();
            }
        }
        if (e.getSource().equals(backButton)) {
            move = lastMove.pop();
            display();
        }
    }
}
