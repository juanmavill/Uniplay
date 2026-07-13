package edu.eci.uniplay.game.application.port.out;

import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.SecretWord;

import java.util.List;

public interface WordDeckProvider {

    SecretWord nextWord(RoomCode roomCode, String deck, List<String> customWords);
}
