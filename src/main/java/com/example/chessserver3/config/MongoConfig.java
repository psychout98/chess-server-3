package com.example.chessserver3.config;

import com.example.chessserver3.model.*;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ConnectionPoolSettings;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class MongoConfig {

    private final static String connectionString = "mongodb+srv://psychout09:qPCygAa684N7tup7@chess.nibctnb.mongodb.net/?retryWrites=true&w=majority";

    @Bean
    public MongoClient mongoClient() {
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .retryWrites(true)
                .applyConnectionString(new ConnectionString(connectionString))
                .applyToConnectionPoolSettings((ConnectionPoolSettings.Builder builder) -> {
                    builder.maxSize(100)
                            .minSize(5)
                            .maxConnectionLifeTime(30, TimeUnit.MINUTES)
                            .maxConnectionIdleTime(10000, TimeUnit.MILLISECONDS);
                })
                .applyToSocketSettings(builder -> {
                    builder.connectTimeout(2000, TimeUnit.MILLISECONDS);
                })
                .serverApi(serverApi)
                .build();
        return MongoClients.create(settings);
    }

    @Bean
    public MongoCollection<Board> boards() {
        ClassModel<Board> boardPojo = ClassModel.builder(Board.class).enableDiscriminator(true).build();
        ClassModel<Piece> pieceModel = ClassModel.builder(Piece.class).enableDiscriminator(true).build();
        ClassModel<Pawn> pawnModel = ClassModel.builder(Pawn.class).enableDiscriminator(true).build();
        ClassModel<Rook> rookModel = ClassModel.builder(Rook.class).enableDiscriminator(true).build();
        ClassModel<Knight> knightModel = ClassModel.builder(Knight.class).enableDiscriminator(true).build();
        ClassModel<Bishop> bishopModel = ClassModel.builder(Bishop.class).enableDiscriminator(true).build();
        ClassModel<Queen> queenModel = ClassModel.builder(Queen.class).enableDiscriminator(true).build();
        ClassModel<King> kingModel = ClassModel.builder(King.class).enableDiscriminator(true).build();
        ClassModel<Player> playerModel = ClassModel.builder(Player.class).enableDiscriminator(true).build();
        ClassModel<Move> moveModel = ClassModel.builder(Move.class).enableDiscriminator(true).build();
        CodecProvider pojoCodecProvider = PojoCodecProvider.builder().register(boardPojo, pieceModel, pawnModel, rookModel, knightModel, bishopModel, queenModel, kingModel, playerModel, moveModel).build();
        CodecRegistry pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(), fromProviders(pojoCodecProvider));
        MongoDatabase database = mongoClient().getDatabase("chess").withCodecRegistry(pojoCodecRegistry);
        return database.getCollection("boards", Board.class);
    }
}