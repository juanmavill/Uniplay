package edu.eci.uniplay.game.application.port.out;

import edu.eci.uniplay.game.domain.model.RoomCode;
import edu.eci.uniplay.game.domain.model.SecretWord;

public interface WordDeckProvider {

    SecretWord nextWord(RoomCode roomCode);
}
