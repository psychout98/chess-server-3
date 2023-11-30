package com.example.chessserver3.service;

        import com.example.chessserver3.exception.InvalidMoveException;
        import com.example.chessserver3.model.board.Board;
        import com.example.chessserver3.model.board.Move;
        import com.example.chessserver3.model.board.Piece;
        import com.example.chessserver3.repository.BoardRepository;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.messaging.simp.SimpMessagingTemplate;
        import org.springframework.scheduling.annotation.Async;
        import org.springframework.scheduling.annotation.EnableAsync;
        import org.springframework.stereotype.Service;

        import java.util.Optional;
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
    public void autoMove(final Board board, final boolean white) throws InterruptedException, ExecutionException {
        Future<Board> bestBoard = generateBestBoard(board,1);
        String moveCode = bestBoard.get().getHistory().get(board.getCurrentMove() + 1).getMoveCode();
        System.out.println(bestBoard.get().getHistory().stream().map(Move::getMoveString).toList());
        board.move(moveCode, white);
        boardRepository.update(board);
        template.convertAndSend("/board/" + board.getId(), "update");
        Thread.currentThread().interrupt();
    }

    @Async
    public Future<Board> generateBestBoard(final Board board, int depth) {
        CompletableFuture<Board> completableFuture = new CompletableFuture<>();
        Board bestBoard = board.copy(false, board.getCurrentMove());
        Set<Move> moves = board.getPieces().values().stream().filter(piece -> piece.isWhite() == board.isWhiteToMove()).map(Piece::getMoves).flatMap(Set::stream).collect(Collectors.toSet());
        int highestAdvantage = 0;
        Optional<Move> randomMove = moves.stream().findAny();
        if (randomMove.isPresent()) {
            bestBoard.move(randomMove.get().getMoveCode(), board.isWhiteToMove());
        } else {
            throw new InvalidMoveException("No moves");
        }
        if (depth > 0) {
            Set<Future<Board>> futures = moves.stream().map(move -> {
                try {
                    return futureBoard(board, move, depth);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toSet());
            Set<Board> futureBoards = futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toSet());
            for (Board futureBoard : futureBoards) {
                int advantage = futureBoard.calculateAdvantage();
                if (advantage < highestAdvantage) {
//                    System.out.println("\n" + advantage);
//                    System.out.println(futureBoard.getHistory().stream().map(Move::getMoveString).toList());
                    highestAdvantage = advantage;
                    bestBoard = futureBoard;
                }
            }
        }
        completableFuture.complete(bestBoard);
        return completableFuture;
    }

    @Async
    public Future<Board> futureBoard(final Board board, Move move, int depth) throws InterruptedException {
        CompletableFuture<Board> completableFuture = new CompletableFuture<>();
        Board deepCopy = board.copy(false, board.getCurrentMove());
        deepCopy.move(move.getMoveCode(), board.isWhiteToMove());
        completableFuture.complete(deepCopy);
        return deepCopy.isCheckmate() ? completableFuture : generateBestBoard(deepCopy, depth - 1);
    }

}
