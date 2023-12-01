package com.example.chessserver3.service;

        import com.example.chessserver3.model.board.AnalysisBoard;
        import com.example.chessserver3.model.board.Board;
        import com.example.chessserver3.model.board.Move;
        import com.example.chessserver3.model.board.Piece;
        import com.example.chessserver3.repository.BoardRepository;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.messaging.simp.SimpMessagingTemplate;
        import org.springframework.scheduling.annotation.Async;
        import org.springframework.scheduling.annotation.EnableAsync;
        import org.springframework.stereotype.Service;
        import java.util.Set;
        import java.util.concurrent.*;
        import java.util.stream.Collectors;

@Service
@EnableAsync
public class AutoMoveService {


    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private SimpMessagingTemplate template;

    @Async
    public void autoMove(final Board board, final boolean white) {
        ForkJoinPool commonPool = ForkJoinPool.commonPool();
        AnalysisBoard analysisBoard = new AnalysisBoard(board, 1);
        Board bestBoard = commonPool.invoke(analysisBoard);
        String moveCode = bestBoard.getHistory().get(board.getCurrentMove() + 1).getMoveCode();
        //System.out.println(bestBoard.getHistory().stream().map(Move::getMoveString).collect(Collectors.toList()));
        board.move(moveCode, white);
        boardRepository.update(board);
        template.convertAndSend("/board/" + board.getId(), "update");
        Thread.currentThread().interrupt();
    }

}
