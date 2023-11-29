package com.example.chessserver3.service;

import com.example.chessserver3.model.board.Board;
import com.example.chessserver3.model.board.Move;
import com.example.chessserver3.model.board.Piece;
import com.example.chessserver3.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@EnableAsync
public class AutoMoveService {


    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private SimpMessagingTemplate template;

    @Async
    public void autoMove(Board board, boolean white) throws InterruptedException {
        Board bestBoard = getHighestAdvantageBoard(board, white, 3);
        board.move(bestBoard.getHistory().get(bestBoard.getHistory().size() - 1).getMoveCode(), white);
        boardRepository.update(board);
        template.convertAndSend("/board/" + board.getId(), "update");
        Thread.currentThread().interrupt();
    }

    private Board getHighestAdvantageBoard(Board board, boolean white, int depth) {
        if (depth == 0) {
            return board;
        } else {
            int highestAdvantage = 0;
            Board bestBoard = board.shallowCopy(board.getCurrentMove());
            Set<Move> moves = board.getPieces().values().stream().filter(piece -> piece.isWhite() == board.isWhiteToMove()).map(Piece::getMoves).flatMap(Set::stream).collect(Collectors.toSet());
            Optional<Move> randomMove = moves.stream().findAny();
            for (Move move : moves) {
                Board shallowCopy = board.shallowCopy(board.getCurrentMove());
                shallowCopy.move(move.getMoveCode(), white);
                int advantage = getHighestAdvantageBoard(shallowCopy, !white, depth - 1).getAdvantage();
                if ((white && advantage > highestAdvantage) || (!white && advantage < highestAdvantage)) {
                    highestAdvantage = advantage;
                    bestBoard = shallowCopy;
                    //System.out.println(shallowCopy);
                }
            }
            if (highestAdvantage == 0 && randomMove.isPresent()) {
                bestBoard.move(randomMove.get().getMoveCode(), white);
            }
            return bestBoard;
        }
    }

}
